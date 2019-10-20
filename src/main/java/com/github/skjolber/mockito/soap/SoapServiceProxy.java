package com.github.skjolber.mockito.soap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Utility class to wrap the webservice implementation in a mock.
 */
public class SoapServiceProxy implements InvocationHandler {
	private Object obj;

	public static <T> T newInstance(T obj) {
		SoapServiceProxy proxy = new SoapServiceProxy(obj);
		Class<?> clazz = obj.getClass();
		return (T)Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), proxy);
	}

	SoapServiceProxy(Object obj) {
		this.obj = obj;
	}

	@Override
	public Object invoke(Object proxy, Method m, Object[] args) throws Exception {
		try {
			return m.invoke(obj, args);
		} catch (Exception e) {
			if(e.getCause() instanceof org.apache.cxf.binding.soap.SoapFault) {
				throw (Exception)e.getCause();
			}
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
