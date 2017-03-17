package com.skjolberg.mockito.soap;

import java.util.List;

/**
 * Rule for mocking SOAP services. 
 * 
 * @author thomas.skjolberg@gmail.com
 *
 */

public abstract class SoapServiceRule extends org.junit.rules.ExternalResource {

	public static SoapServiceRule newInstance() {
		return new SoapEndpointRule();
	}

	public static SoapEndpointRule newInstance(String ... ports) {
		return new SoapEndpointRule(ports);
	}

	public static SoapEndpointRule newInstance(int portRangeStart, int portRangeEnd, String ... properties) {
		return new SoapEndpointRule(portRangeStart, portRangeEnd, properties);
	}
	
	/**
	 * Create (and start) endpoint.
	 * 
	 * @param target instance calls are forwarded to
	 * @param port service class
	 * @param address address, i.e. http://localhost:1234
	 * @param wsdlLocation wsdl location, or null
	 * @param schemaLocations schema locations, or null
	 * @param <T> mock target - the mock to which server calls are delegated
	 */

	public abstract <T> void proxy(T target, Class<T> port, String address, String wsdlLocation, List<String> schemaLocations);

	/**
	 * Create (and start) service endpoint with mock delegate. No schema validation.
	 * 
	 * @param port service class
	 * @param address address, i.e. http://localhost:1234
	 * @param <T> class to be mocked.
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
	 * @param <T> class to be mocked.
	 * @return mockito mock - the mock to which server calls are delegated
	 */

	public <T> T mock(Class<T> port, String address, String wsdlLocation) {
		if(wsdlLocation == null || wsdlLocation.isEmpty()) {
			throw new IllegalArgumentException("Expected WSDL location.");
		}
		// wrap the evaluator mock in proxy
		T mock = org.mockito.Mockito.mock(port);

		proxy(mock, port, address, wsdlLocation, null);
		
		return mock;
	}
	
	/**
	 * Create (and start) service with mock delegate.
	 * 
	 * @param port service class
	 * @param address address, i.e. http://localhost:1234
	 * @param schemaLocations schema locations, or null
	 * @param <T> class to be mocked.
	 * @return mockito mock - the mock to which server calls are delegated
	 */

	public <T> T mock(Class<T> port, String address, List<String> schemaLocations) {
		if(schemaLocations == null || schemaLocations.isEmpty()) {
			throw new IllegalArgumentException("Expected XML Schema location(s).");
		}
		// wrap the evaluator mock in proxy
		T mock = org.mockito.Mockito.mock(port);

		proxy(mock, port, address, null, schemaLocations);
		
		return mock;
	}

	/**
	 * 
	 * Stop services.
	 * 
	 */

	public abstract void stop();

	/**
	 * 
	 * (Re)start services.
	 * 
	 */

	public abstract void start();

}
