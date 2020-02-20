package com.github.skjolber.mockito.soap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

public class SoapServiceExtension extends SoapExtension {

	private Map<String, Server> servers = new HashMap<>();

	@Override
	public <T> void proxy(T target, Class<T> port, String address, String wsdlLocation, List<String> schemaLocations, Map<String, Object> properties) {
		assertValidParams(target, port, address);

		if(servers.containsKey(address)) {
			throw new IllegalArgumentException("Server " + address + " already exists");
		}

		T serviceInterface = SoapServiceProxy.newInstance(target);

		JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
		svrFactory.setServiceClass(port);
		svrFactory.setAddress(address);
		svrFactory.setServiceBean(serviceInterface);

		if(wsdlLocation != null) {
			svrFactory.setWsdlLocation(wsdlLocation);
		}

		if(schemaLocations != null) {
			svrFactory.setSchemaLocations(schemaLocations);
		}

		svrFactory.setProperties(processProperties(properties, wsdlLocation, schemaLocations));

		Server server = svrFactory.create();

		servers.put(address, server);

		server.start();
	}

	@Override
	protected void assertValidAddress(String address) {
		if (address != null && address.startsWith("local://")) {
			return;
		}
		super.assertValidAddress(address);
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		Class<?> target = parameterContext.getParameter().getType();
		if(target == SoapServiceExtension.class) {
			return true;
		}
		
		return false;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		Class<?> target = parameterContext.getParameter().getType();
		if(target == SoapServiceExtension.class) {
			return this;
		}
		throw new RuntimeException();
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		start();
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		reset();
	}
	
	public void stop() {
		servers.values().forEach(Server::stop);
	}

	public void start() {
		servers.values().forEach(Server::start);
	}

	public void reset() {
		servers.values().forEach(server -> {
			server.destroy();
			((EndpointImpl)server.getEndpoint()).getBus().shutdown(true);
		});
		servers.clear();
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		
	}	

}
