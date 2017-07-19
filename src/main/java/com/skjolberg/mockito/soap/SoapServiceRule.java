package com.skjolberg.mockito.soap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public static SoapEndpointRule newInstance(String ... portNames) {
		return new SoapEndpointRule(portNames);
	}

	public static SoapEndpointRule newInstance(int portRangeStart, int portRangeEnd, String ... portNames) {
		return new SoapEndpointRule(portRangeStart, portRangeEnd, portNames);
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

	public <T> void proxy(T target, Class<T> port, String address, String wsdlLocation, List<String> schemaLocations) {
		proxy(target, port, address, wsdlLocation, schemaLocations, null);
	}

	/**
	 * Create (and start) endpoint with properties
	 * 
	 * @param target instance calls are forwarded to
	 * @param port service class
	 * @param address address, i.e. http://localhost:1234
	 * @param wsdlLocation wsdl location, or null
	 * @param schemaLocations schema locations, or null
	 * @param properties additional properties, like mtom-enabled and so
	 * @param <T> mock target - the mock to which server calls are delegated
	 */

	public abstract <T> void proxy(T target, Class<T> port, String address, String wsdlLocation, List<String> schemaLocations, Map<String, Object> properties);

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

		proxy(mock, port, address, null, null, null);
		
		return mock;
	}

	/**
	 * Create (and start) service endpoint with mock delegate. No schema validation.
	 * 
	 * @param port service class
	 * @param address address, i.e. http://localhost:1234
	 * @param properties additional properties, like mtom-enabled and so
	 * @param <T> class to be mocked.
	 * @return mockito mock - the mock to which server calls are delegated
	 */

	public <T> T mock(Class<T> port, String address, Map<String, Object> properties) {
		// wrap the evaluator mock in proxy
		T mock = org.mockito.Mockito.mock(port);

		proxy(mock, port, address, null, null, properties);
		
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
		return mock(port, address, wsdlLocation, null);
	}

	/**
	 * Create (and start) service endpoint with mock delegate.
	 * 
	 * @param port service class
	 * @param address address, i.e. http://localhost:1234
	 * @param wsdlLocation wsdl location, or null
	 * @param properties additional properties, like mtom-enabled and so
	 * @param <T> class to be mocked.
	 * @return mockito mock - the mock to which server calls are delegated
	 */

	public <T> T mock(Class<T> port, String address, String wsdlLocation, Map<String, Object> properties) {
		if(wsdlLocation == null || wsdlLocation.isEmpty()) {
			throw new IllegalArgumentException("Expected WSDL location.");
		}
		// wrap the evaluator mock in proxy
		T mock = org.mockito.Mockito.mock(port);

		proxy(mock, port, address, wsdlLocation, null, properties);
		
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
		return mock(port, address, schemaLocations, null);
	}
	
	/**
	 * Create (and start) service with mock delegate and additional properties.
	 * 
	 * @param port service class
	 * @param address address, i.e. http://localhost:1234
	 * @param schemaLocations schema locations, or null
	 * @param properties additional properties, like mtom-enabled and so
	 * @param <T> class to be mocked.
	 * @return mockito mock - the mock to which server calls are delegated
	 */

	public <T> T mock(Class<T> port, String address, List<String> schemaLocations, Map<String, Object> properties) {
		if(schemaLocations == null || schemaLocations.isEmpty()) {
			throw new IllegalArgumentException("Expected XML Schema location(s).");
		}
		// wrap the evaluator mock in proxy
		T mock = org.mockito.Mockito.mock(port);

		proxy(mock, port, address, null, schemaLocations, properties);
		
		return mock;
	}
	
	public static Map<String, Object> properties(Object... properties) {
		verifyProperties(properties);
		
		HashMap<String, Object> map = new HashMap<String, Object>();
		if(properties != null) {
			for(int i = 0; i < properties.length; i+=2) {
				map.put((String)properties[i], properties[i+1]);
			}
		}
		
		return map;
	}
	
	protected static void verifyProperties(Object... properties) {
		if(properties != null) {
			if(properties.length % 2 != 0) {
				throw new IllegalArgumentException("Expected key-value properties, not length " + properties.length);
			}
			for(int i = 0; i < properties.length; i+=2) {
				if(!(properties[i] instanceof String)) {
					throw new IllegalArgumentException("Expected key-value with string key at index " + i);
				}
			}
		}
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
