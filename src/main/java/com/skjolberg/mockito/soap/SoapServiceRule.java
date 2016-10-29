package com.skjolberg.mockito.soap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

public class SoapServiceRule extends org.junit.rules.ExternalResource {

	public static SoapServiceRule newInstance() {
		return new SoapServiceRule();
	}

	private List<Server> servers = new ArrayList<Server>();

	public <T> T mock(T mock, Class<T> port, String address, String wsdlLocation, List<String> schemaLocations) {
		T serviceInterface = SoapServiceProxy.newInstance(mock);

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

		
		return mock;
	}
	
	public <T> T mock(Class<T> port, String address) {
		// wrap the evaluator mock in proxy
		T mock = org.mockito.Mockito.mock(port);

		return mock(mock, port, address, null, null);
	}

	public <T> T mock(Class<T> port, String address, String wsdlLocation) {
		// wrap the evaluator mock in proxy
		T mock = org.mockito.Mockito.mock(port);

		return mock(mock, port, address, wsdlLocation, null);
	}

	public <T> T mock(Class<T> port, String address, List<String> schemaLocations) {
		// wrap the evaluator mock in proxy
		T mock = org.mockito.Mockito.mock(port);

		return mock(mock, port, address, null, schemaLocations);
	}

	protected void before() throws Throwable {
		super.before();
	}

	protected void after() {
		stop();
	}

	public void stop() {
		for (Server endpointImpl : servers) {
			endpointImpl.stop();
		}
	}
	
	public void start() {
		for (Server endpointImpl : servers) {
			endpointImpl.start();
		}
	}

}
