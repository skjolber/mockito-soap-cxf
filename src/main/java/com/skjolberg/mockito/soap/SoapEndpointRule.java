package com.skjolberg.mockito.soap;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 */
public class SoapEndpointRule extends SoapServiceRule {

	private static final int PORT_RANGE_START = 1024+1;
	private static final int PORT_RANGE_END = PortManager.PORT_RANGE_MAX;

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

	private PortManager<Destination> portManager;

	public SoapEndpointRule() {
		this(PORT_RANGE_START, PORT_RANGE_END);
	}

	public SoapEndpointRule(String ... portNames) {
		this(PORT_RANGE_START, PORT_RANGE_END, portNames);
	}

	public SoapEndpointRule(int portRangeStart, int portRangeEnd, String ... portNames) {
		portManager = new PortManager<Destination>(portRangeStart, portRangeEnd) {
			@Override
			public Destination reserve(int port) throws Exception {
				return createDestination(port);
			}

			@Override
			public void release(Destination destination) {
				destination.shutdown();
			}
		};

		portManager.add(portNames);
	}

    /**
     * Returns the port number that was reserved for the given name.
     *
     * @param portName port name
     * @return a valid port number if the port has been reserved, -1 otherwise
     */
	public int getPort(String portName) {
		return portManager.getPort(portName);
	}

    /**
     * Returns all port names and respective port numbers.
     *
     * @return a map of port name and port value (a valid port number
     *         if the port has been reserved, or -1 otherwise)
     */
	public Map<String, Integer> getPorts() {
		return portManager.getPorts();
	}

	/**
	 * Attempt to reserve a port by starting a server.
	 *
	 * @param port port to reserve
	 * @return destination if successful
	 * @throws IOException
	 * @throws EndpointException
	 */
	private Destination createDestination(int port) throws IOException, EndpointException {
		JaxWsServiceFactoryBean jaxWsServiceFactoryBean = new JaxWsServiceFactoryBean();

		JaxWsServerFactoryBean serverFactoryBean = new JaxWsServerFactoryBean(jaxWsServiceFactoryBean);
		serverFactoryBean.setAddress("http://localhost:" + port);

		Bus bus = serverFactoryBean.getBus();
		DestinationFactory destinationFactory = bus.getExtension(DestinationFactoryManager.class).getDestinationFactoryForUri(serverFactoryBean.getAddress());

		EndpointInfo ei = new EndpointInfo(null, Integer.toString(port));
		ei.setAddress(serverFactoryBean.getAddress());

		Destination destination = destinationFactory.getDestination(ei, serverFactoryBean.getBus());

		ServiceImpl serviceImpl = new ServiceImpl();

		org.apache.cxf.endpoint.Endpoint endpoint = new org.apache.cxf.endpoint.EndpointImpl(bus, serviceImpl, ei);
		destination.setMessageObserver(new ChainInitiationObserver(endpoint , bus));
		return destination;
	}

	@Override
	public <T> void proxy(T target, Class<T> port, String address, String wsdlLocation, List<String> schemaLocations, Map<String, Object> properties) {
		assertValidParams(target, port, address);

		if(endpoints.containsKey(address)) {
			throw new IllegalArgumentException("Endpoint " + address + " already exists");
		}

		T serviceInterface = SoapServiceProxy.newInstance(target);

		Destination destination = portManager.getData(parsePort(address));

		EndpointImpl endpoint = (EndpointImpl)Provider.provider().createEndpoint(null, serviceInterface);

		if(wsdlLocation != null) {
			endpoint.setWsdlLocation(wsdlLocation);
		}

		if(schemaLocations != null) {
			endpoint.setSchemaLocations(schemaLocations);
		}

		endpoint.setProperties(processProperties(properties, wsdlLocation, schemaLocations));

		if(destination != null) {
			ServerImpl server = endpoint.getServer();
			server.setDestination(destination);
		}

		endpoint.publish(address);

		endpoints.put(address, endpoint);
	}

	@Override
	protected void before() {
		// reserve all ports
		portManager.start();
	}

	@Override
	protected void after() {
		destroy();
	}

	/**
	 * Stop and remove endpoints, keeping port reservations.
	 */
	public void clear() {
		endpoints.values().forEach(EndpointImpl::stop);
		endpoints.clear();
	}

	public void destroy() {
		endpoints.values().forEach(endpoint -> {
			endpoint.stop();
			endpoint.getBus().shutdown(true);
		});
		endpoints.clear();
		portManager.stop();
	}

	@Override
	public void stop() {
		endpoints.values().forEach(endpoint -> endpoint.getServer().stop());
	}

	@Override
	public void start() {
		// republish
		endpoints.values().forEach(endpoint -> endpoint.getServer().start());
	}

}
