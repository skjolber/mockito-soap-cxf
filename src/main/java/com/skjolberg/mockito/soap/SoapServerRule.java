package com.skjolberg.mockito.soap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

/**
 * Rule for mocking SOAP services using @{@linkplain JaxWsServerFactoryBean} to create {@linkplain Server}s.
 * Each individual service requires a separate port.
 *
 * @author thomas.skjolberg@gmail.com
 */
public class SoapServerRule extends SoapServiceRule {

	public static SoapServerRule newInstance() {
		return new SoapServerRule();
	}

	private Map<String, Server> servers = new HashMap<>();

	@Override
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

		assertValidAddress(address);

		if(servers.containsKey(address)) {
			throw new IllegalArgumentException("Server " + address + " already exists");
		}

		T serviceInterface = SoapServiceProxy.newInstance(target);

		JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
		svrFactory.setServiceClass(port);
		svrFactory.setAddress(address);
		svrFactory.setServiceBean(serviceInterface);

		Map<String, Object> map = properties != null ? new HashMap<>(properties) : new HashMap<>();

		if(wsdlLocation != null || schemaLocations != null) {
			map.put("schema-validation-enabled", true);

			if(wsdlLocation != null) {
				svrFactory.setWsdlLocation(wsdlLocation);
			}

			if(schemaLocations != null) {
				svrFactory.setSchemaLocations(schemaLocations);
			}
		}
		svrFactory.setProperties(map);

		Server server = svrFactory.create();

		servers.put(address, server);

		server.start();
	}

	@Override
	protected void after() {
		destroy();
	}

	public void destroy() {
		reset();
	}

	@Override
	public void stop() {
		servers.values().forEach(Server::stop);
	}

	@Override
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

	private void assertValidAddress(String address) {
		if (address != null && address.startsWith("local://")) {
			return;
		}

		try {
			new URL(address);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Expected valid address: " + address, e);
		}
	}
}
