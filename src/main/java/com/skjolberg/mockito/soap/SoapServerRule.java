package com.skjolberg.mockito.soap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.mockito.Mockito;

/**
 * Rule for mocking SOAP services using @{@linkplain JaxWsServerFactoryBean} to create {@linkplain Server}s. 
 * Each individual service requires a separate port.
 * 
 * @author thomas.skjolberg@gmail.com
 *
 */

public class SoapServerRule extends SoapServiceRule {

	public static SoapServerRule newInstance() {
		return new SoapServerRule();
	}

	private Map<String, Server> servers = new HashMap();

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
		
		try {
			new URL(address);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Expected valid address: " + address, e);
		}
		if(servers.containsKey(address)) {
			throw new IllegalArgumentException("Server " + address + " already exists");
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
		
		servers.put(address, server);
		
		server.start();
		
	}

	protected void before() throws Throwable {
		super.before();
	}

	protected void after() {
		reset();
	}

	public void destroy() {
		reset();
	}

	public void stop() {
		for (Entry<String, Server> entry : servers.entrySet()) {
			entry.getValue().stop();
		}
	}

	public void start() {
		for (Entry<String, Server> entry : servers.entrySet()) {
			entry.getValue().start();
		}
	}

	public void reset() {
		for (Entry<String, Server> entry : servers.entrySet()) {
			entry.getValue().getDestination().shutdown();
			
			entry.getValue().destroy();
		}
		servers.clear();
	}

}
