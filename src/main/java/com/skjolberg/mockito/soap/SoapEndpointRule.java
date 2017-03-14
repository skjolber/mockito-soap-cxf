package com.skjolberg.mockito.soap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngine;
import org.springframework.util.SocketUtils;

/**
 * Rule for mocking SOAP endpoints. Ports can be reserved and multiple endpoints can be present on the same port.
 * 
 * @author thomas.skjolberg@gmail.com
 *
 */

public class SoapEndpointRule extends org.junit.rules.ExternalResource {

	private class PortReservation {
		public PortReservation(String propertyName) {
			this.propertyName = propertyName;
		}
		private final String propertyName;
		private JettyHTTPDestination destination;
		private int port = -1;
		
		public void reserved(int port, JettyHTTPDestination destination) {
			this.port = port;
			this.destination = destination;
			
			System.setProperty(propertyName, Integer.toString(port));
		}
		
		public void stop() {
			if(destination != null) {
				destination.shutdown();
			}
			this.port = -1;
		}

		public void start() {
			// try 5 times to reserve a port
			int attempt = 0;
			do {
				try {
					int port = SocketUtils.findAvailableTcpPort(1024+1);
				
					JettyHTTPDestination destination = reservePort(port);
					
					reserved(port, destination);
					
					break;
				} catch(Exception e) {
					attempt++;
					
					if(attempt >= 5) {
						throw new RuntimeException("Unable to reserve port for " + propertyName, e);
					}
				}
			} while(true);
			
		}

		public JettyHTTPDestination getDestination() {
			return destination;
		}

		public int getPort() {
			return port;
		}

		public String getPropertyName() {
			return propertyName;
		}
		
	}
	
	public static SoapEndpointRule newInstance() {
		return new SoapEndpointRule();
	}

	public static SoapEndpointRule newInstance(String ... ports) {
		return new SoapEndpointRule(ports);
	}

	private List<Endpoint> endpoints = new ArrayList<Endpoint>();
	private List<PortReservation> reservations = new ArrayList<>();

	public SoapEndpointRule() {
	}
	
	public SoapEndpointRule(String ... properties) {
		for(String property : properties) {
			reservations.add(new PortReservation(property));
		}
	}

	private JettyHTTPDestination reservePort(int port) throws IOException, EndpointException {
		JaxWsServiceFactoryBean jaxWsServiceFactoryBean = new JaxWsServiceFactoryBean();
		
		JaxWsServerFactoryBean serverFactoryBean = new JaxWsServerFactoryBean(jaxWsServiceFactoryBean);
		serverFactoryBean.setAddress("http://localhost:" + port);
		
		Bus bus = serverFactoryBean.getBus();
		DestinationFactory destinationFactory = (DestinationFactory) bus.getExtension(DestinationFactoryManager.class).getDestinationFactoryForUri(serverFactoryBean.getAddress());;
		
		EndpointInfo ei = new EndpointInfo(null, Integer.toString(port));
		ei.setAddress(serverFactoryBean.getAddress());
		
		JettyHTTPDestination destination = (JettyHTTPDestination) destinationFactory.getDestination(ei, serverFactoryBean.getBus());
		
		JettyHTTPServerEngine engine = (JettyHTTPServerEngine) destination.getEngine();
		engine.setPort(port);

		ServiceImpl serviceImpl = new ServiceImpl();
		
		org.apache.cxf.endpoint.Endpoint endpoint = new org.apache.cxf.endpoint.EndpointImpl(bus, serviceImpl, ei);
		destination.setMessageObserver(new ChainInitiationObserver(endpoint , bus));
		return destination;
	}
	
	/**
	 * Create (and start) endpoint.
	 * 
	 * @param target instance calls are forwarded to
	 * @param port service class
	 * @param address address, i.e. http://localhost:1234
	 * @param wsdlLocation wsdl location, or null
	 * @param schemaLocations schema locations, or null
	 */

	public <T> void proxy(T target, Class<T> port, String address, String wsdlLocation, List<String> schemaLocations) {
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
			throw new IllegalArgumentException("Expected valid address", e);
		}

		T serviceInterface = SoapServiceProxy.newInstance(target);

		EndpointImpl endpoint;
		JettyHTTPDestination destination = getDestination(url.getPort());
		if(destination != null) {
			endpoint = (EndpointImpl)Provider.provider().createEndpoint(null, serviceInterface);
			ServerImpl server = endpoint.getServer();
			server.setDestination(destination);
		} else {
			endpoint = (EndpointImpl) Provider.provider().createEndpoint(address, serviceInterface);
		}
		 	
		if(wsdlLocation != null || schemaLocations != null) {
			HashMap<String, Object> properties = new HashMap<String, Object>();
			properties.put("schema-validation-enabled", true);
			endpoint.setProperties(properties);
			
			if(wsdlLocation != null) {
				endpoint.setWsdlLocation(wsdlLocation);
			}
			
			if(schemaLocations != null) {
				endpoint.setSchemaLocations(schemaLocations);
			}
		}
		endpoint.publish(address);
		
		endpoints.add(endpoint);

	}

	private JettyHTTPDestination getDestination(int port) {
		for(PortReservation reservation : reservations) {
			if(reservation.getPort() == port) {
				return reservation.getDestination();
			}
		}
		return null;
	}

	/**
	 * Create (and start) service endpoint with mock delegate. No schema validation.
	 * 
	 * @param port service class
	 * @param address address, i.e. http://localhost:1234
	 * @return mockito mock - the mock to which server calls are delegated
	 */

	public <T> T mock(Class<T> port, String address) {
		// wrap the evaluator mock in proxy
		T mock = org.mockito.Mockito.mock(port);

		proxy(mock, port, address, null, null);
		
		return mock;
	}

	/**
	 * Create (and start) service endpoint with mock delegate.
	 * 
	 * @param port service class
	 * @param address address, i.e. http://localhost:1234
	 * @param wsdlLocation wsdl location, or null
	 * @return mockito mock - the mock to which server calls are delegated
	 */

	public <T> T mock(Class<T> port, String address, String wsdlLocation) {
		if(wsdlLocation == null || wsdlLocation.isEmpty()) {
			throw new IllegalArgumentException("Expected wsdl location");
		}
		// wrap the evaluator mock in proxy
		T mock = org.mockito.Mockito.mock(port);

		proxy(mock, port, address, wsdlLocation, null);
		
		return mock;
	}
	
	/**
	 * Create (and start) service endpoint with mock delegate.
	 * 
	 * @param port service class
	 * @param address address, i.e. http://localhost:1234
	 * @param schemaLocations schema locations, or null
	 * @return mockito mock - the mock to which server calls are delegated
	 */

	public <T> T mock(Class<T> port, String address, List<String> schemaLocations) {
		if(schemaLocations == null || schemaLocations.isEmpty()) {
			throw new IllegalArgumentException("Expected schema locations");
		}
		// wrap the evaluator mock in proxy
		T mock = org.mockito.Mockito.mock(port);

		proxy(mock, port, address, null, schemaLocations);
		
		return mock;
	}

	protected void before() throws Throwable {
		// reserve ports for all ports 
		for(PortReservation reservation : reservations) {
			reservation.start();
		}
	}

	protected void after() {
		clear();
		for(PortReservation reservation : reservations) {
			reservation.stop();
		}
	}

	public void clear() {
		// tear down endpoints
		for (Endpoint endpoint : endpoints) {
			endpoint.stop();
		}
		endpoints.clear();
	}



}
