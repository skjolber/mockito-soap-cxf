package com.skjolberg.mockito.soap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

/**
 * Rule for mocking SOAP endpoints. Each individual service requires a seperate port.
 * 
 * @author thomas.skjolberg@gmail.com
 *
 */

public class SoapServiceRule extends org.junit.rules.ExternalResource {

	public static SoapServiceRule newInstance() {
		return new SoapServiceRule();
	}

	private List<Server> servers = new ArrayList<Server>();
	
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
		T serviceInterface = SoapServiceProxy.newInstance(target);

		JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
		svrFactory.setServiceClass(port);
		svrFactory.setAddress(address);
		svrFactory.setServiceBean(serviceInterface);

		if(wsdlLocation != null || schemaLocations != null) {
			HashMap<String, Object> properties = new HashMap<String, Object>();
			properties.put("schema-validation-enabled", true);
			svrFactory.setProperties(properties);
			
			if(wsdlLocation != null) {
				svrFactory.setWsdlLocation(wsdlLocation);
			}
			
			if(schemaLocations != null) {
				svrFactory.setSchemaLocations(schemaLocations);
			}
		}
		
		Server server = svrFactory.create();
		
		server.start();
		
		servers.add(server);
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
		super.before();
	}

	protected void after() {
		destroy();
	}

	/**
	 * 
	 * Destroy endpoints.
	 * 
	 */

	public void destroy() {
		for (Server endpointImpl : servers) {
			endpointImpl.destroy();
		}
	}

	/**
	 * 
	 * Stop endpoints.
	 * 
	 */

	public void stop() {
		for (Server endpointImpl : servers) {
			endpointImpl.stop();
		}
	}

	/**
	 * 
	 * (Re)start endpoints.
	 * 
	 */

	public void start() {
		for (Server endpointImpl : servers) {
			endpointImpl.start();
		}
	}

}
