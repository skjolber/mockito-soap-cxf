package com.skjolberg.mockito.soap;

import java.util.ArrayList;
import java.util.List;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import org.apache.cxf.jaxws.EndpointImpl;

public class SoapServiceRule extends org.junit.rules.ExternalResource {

	public static SoapServiceRule newInstance() {
		return new SoapServiceRule();
	}

	private List<EndpointImpl> endspoints = new ArrayList<EndpointImpl>();

	public <T> T mock(T mock, Class<T> port, String address) {
		T serviceInterface = SoapServiceProxy.newInstance(mock);

		// publish the mock 
		EndpointImpl endpoint = (EndpointImpl) Endpoint.create(serviceInterface);

		WebService webService = port.getAnnotation(WebService.class);

		// we have to use the following CXF dependent code, to specify qname, so that it resource locator finds it 
		QName serviceName = new QName(webService.targetNamespace(), webService.name());
		endpoint.setServiceName(serviceName);
		endpoint.setAddress(address);
		endpoint.publish();

		endspoints.add(endpoint);

		return mock;
	}

	public <T> T mock(Class<T> port, String address) {
		// wrap the evaluator mock in proxy
		T mock = org.mockito.Mockito.mock(port);

		return mock(mock, port, address);
	}

	protected void before() throws Throwable {
		super.before();
	}

	protected void after() {
		stop();
	}

	public void stop() {
		for (EndpointImpl endpointImpl : endspoints) {
			endpointImpl.stop();
		}
	}
	
	public void start() {
		for (EndpointImpl endpointImpl : endspoints) {
			endpointImpl.publish();
		}
	}

}
