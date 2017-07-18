package com.skjolberg.mockito.soap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.net.ServerSocketFactory;
import javax.xml.ws.Endpoint;
import javax.xml.ws.spi.Provider;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.junit.ClassRule;

/**
 * Rule for mocking SOAP services using {@linkplain Endpoint}s. Multiple services can run on the same port.
 * If used as a {@linkplain ClassRule}, the rule can be used to reserve random free ports. 
 * Resulting reserved ports are set as system properties to port names provided by the caller.
 * 
 * @author thomas.skjolberg@gmail.com
 *
 */

public class SoapEndpointRule extends SoapServiceRule {
	
	private static final int PORT_RANGE_MAX = 65535;
	private static final int PORT_RANGE_START = 1024+1;
	private static final int PORT_RANGE_END = PORT_RANGE_MAX;

	private class PortReservation {
		public PortReservation(String portName) {
			this.propertyName = portName;
		}
		private final String propertyName;
		private Destination destination;
		private int port = -1;
		
		public void reserved(int port, Destination destination) {
			this.port = port;
			this.destination = destination;
			
			System.setProperty(propertyName, Integer.toString(port));
		}
		
		public void stop() {
			if(destination != null) {
				destination.shutdown();
				
				System.clearProperty(propertyName);
				
				this.port = -1;
			}
		}

		public void start() {
			// systematically try ports in range
			// starting at random offset
			int portRange = portRangeEnd - portRangeStart + 1;
			
			int offset = new Random().nextInt(portRange);

			for(int i = 0; i < portRange; i++) {
				try {
					int candidatePort = portRangeStart + (offset + portRange) % portRange;
					
					if(isPortAvailable(candidatePort)) {
						Destination destination = reservePort(candidatePort); // port might now be taken
						
						reserved(candidatePort, destination);
						
						return;
					}
				} catch(Exception e) {
					// continue
				}
			}
			throw new RuntimeException("Unable to reserve port for " + propertyName);
			
		}

		public Destination getDestination() {
			return destination;
		}

		public int getPort() {
			return port;
		}

		public String getPropertyName() {
			return propertyName;
		}
		
	}   
	
	protected static boolean isPortAvailable(int port) {
		try {
			ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(port, 1, InetAddress.getByName("localhost"));
			serverSocket.close();
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}
	
	public static SoapEndpointRule newInstance() {
		return new SoapEndpointRule();
	}
	
	public static SoapEndpointRule newInstance(String ... portNames) {
		return new SoapEndpointRule(portNames);
	}

	public static SoapEndpointRule newInstance(int portRangeStart, int portRangeEnd, String ... portNames) {
		return new SoapEndpointRule(portRangeStart, portRangeEnd, portNames);
	}

	private Map<String, EndpointImpl> endpoints = new HashMap<>();

	private List<PortReservation> reservations = new ArrayList<>();

	private final int portRangeStart;
	private final int portRangeEnd;
	
	public SoapEndpointRule() {
		this(PORT_RANGE_START, PORT_RANGE_END);
	}
	
	public SoapEndpointRule(String ... portNames) {
		this(PORT_RANGE_START, PORT_RANGE_END, portNames);
	}
	
	public SoapEndpointRule(int portRangeStart, int portRangeEnd, String ... portNames) {
		if(portRangeStart <= 0) {
			throw new IllegalArgumentException("Port range start must be greater than 0.");
		}
		if(portRangeEnd < portRangeStart) {
			throw new IllegalArgumentException("Port range end must not be lower than port range end.");
		}
		if(portRangeEnd > PORT_RANGE_MAX) {
			throw new IllegalArgumentException("Port range end must not be larger than " + PORT_RANGE_MAX + ".");
		}
		if(portNames != null && portNames.length > (portRangeEnd - portRangeStart + 1)) {
			throw new IllegalArgumentException("Cannot reserve " + portNames.length + " in range " + portRangeStart + "-" + portRangeEnd + ".");
		}

		this.portRangeStart = portRangeStart;
		this.portRangeEnd = portRangeEnd;
		
		if(portNames != null) {
			for(String portName : portNames) {
				reservations.add(new PortReservation(portName));
			}
		}
	}
	
	/**
	 * Get resvered ports.
	 * 
	 * @return map of portName and port value; &gt; 1 if a port has been reserved, -1 otherwise
	 */
	
	public Map<String, Integer> getPorts() {
		HashMap<String, Integer> ports = new HashMap<>();
		for (PortReservation portReservation : reservations) {
			ports.put(portReservation.getPropertyName(), portReservation.getPort());
		}
		return ports;
	}
	
	/**
	 * Get a specific reserved port by its portName (as passed to the constructor).
	 * 
	 * @param name port name
	 * @return a port &gt; 1 if a port has been reserved, -1 otherwise
	 */
	
	public int getPort(String name) {
		for (PortReservation portReservation : reservations) {
			if(name.equals(portReservation.getPropertyName())) {
				return portReservation.getPort();
			}
		}
		throw new IllegalArgumentException("No reserved port for '" + name + "'.");
	}
	
	
	/**
	 * Attempt to reserve a port by starting a server. The server 
	 * 
	 * @param port port to reserve
	 * @return destination if succsesful
	 * @throws IOException
	 * @throws EndpointException
	 */

	private Destination reservePort(int port) throws IOException, EndpointException {
		JaxWsServiceFactoryBean jaxWsServiceFactoryBean = new JaxWsServiceFactoryBean();
		
		JaxWsServerFactoryBean serverFactoryBean = new JaxWsServerFactoryBean(jaxWsServiceFactoryBean);
		serverFactoryBean.setAddress("http://localhost:" + port);
		
		Bus bus = serverFactoryBean.getBus();
		DestinationFactory destinationFactory = (DestinationFactory) bus.getExtension(DestinationFactoryManager.class).getDestinationFactoryForUri(serverFactoryBean.getAddress());;
		
		EndpointInfo ei = new EndpointInfo(null, Integer.toString(port));
		ei.setAddress(serverFactoryBean.getAddress());
		
		Destination destination = destinationFactory.getDestination(ei, serverFactoryBean.getBus());
		
		ServiceImpl serviceImpl = new ServiceImpl();
		
		org.apache.cxf.endpoint.Endpoint endpoint = new org.apache.cxf.endpoint.EndpointImpl(bus, serviceImpl, ei);
		destination.setMessageObserver(new ChainInitiationObserver(endpoint , bus));
		return destination;
	}

	public <T> void proxy(T target, Class<T> port, String address, String wsdlLocation, List<String> schemaLocations, Map<String, Object> properties) {
		if(target == null) {
			throw new IllegalArgumentException("Expected proxy target");
		}
		if(port == null) {
			throw new IllegalArgumentException("Expect port class");
		}
		if(address == null) {
			throw new IllegalArgumentException("Expected address");
		}
		URL url;
		try {
			url = new URL(address);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Expected valid address: " + address, e);
		}
		if(endpoints.containsKey(address)) {
			throw new IllegalArgumentException("Endpoint " + address + " already exists");
		}
		
		T serviceInterface = SoapServiceProxy.newInstance(target);

		Destination destination = getDestination(url.getPort());
		
		EndpointImpl endpoint = (EndpointImpl)Provider.provider().createEndpoint(null, serviceInterface);

		Map<String, Object> map = properties != null ? new HashMap<String, Object>(properties) : new HashMap<String, Object>();

		if(wsdlLocation != null || schemaLocations != null) {
			map.put("schema-validation-enabled", true);
			
			if(wsdlLocation != null) {
				endpoint.setWsdlLocation(wsdlLocation);
			}
			
			if(schemaLocations != null) {
				endpoint.setSchemaLocations(schemaLocations);
			}
		}
		endpoint.setProperties(map);
		
		if(destination != null) {
			ServerImpl server = endpoint.getServer();
			server.setDestination(destination);
		}
		
		endpoint.publish(address);
		
		endpoints.put(address, endpoint);
	}

	private Destination getDestination(int port) {
		for(PortReservation reservation : reservations) {
			if(reservation.getPort() == port) {
				return reservation.getDestination();
			}
		}
		return null;
	}

	protected void before() throws Throwable {
		// reserve ports for all ports 
		for(PortReservation reservation : reservations) {
			reservation.start();
		}
	}

	protected void after() {
		destroy();
	}
	
	/**
	 * Stop and remove endpoints, keeping port reservations.
	 * 
	 */

	public void clear() {
		for (Entry<String, EndpointImpl> entry : endpoints.entrySet()) {
			entry.getValue().stop();
		}
		endpoints.clear();
	}

	public void destroy() {
		for (Entry<String, EndpointImpl> entry : endpoints.entrySet()) {
			entry.getValue().getServer().stop();
			entry.getValue().stop();
		}
		for(PortReservation reservation : reservations) {
			reservation.stop();
		}
	}

	public void stop() {
		// stop endpoints
		for (Entry<String, EndpointImpl> entry : endpoints.entrySet()) {
			entry.getValue().getServer().stop();
		}
	}

	@Override
	public void start() {
		// republish 
		for (Entry<String, EndpointImpl> entry : endpoints.entrySet()) {
			entry.getValue().getServer().start();
		}
	}

}
