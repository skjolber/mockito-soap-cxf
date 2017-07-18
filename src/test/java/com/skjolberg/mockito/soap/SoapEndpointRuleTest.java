package com.skjolberg.mockito.soap;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.cxf.helpers.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.skjolber.bank.example.v1.BankCustomerServicePortType;

public class SoapEndpointRuleTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Rule
	public SoapEndpointRule soap = SoapEndpointRule.newInstance();

	@Test
	public void testInvalidParameters1() {
		exception.expect(IllegalArgumentException.class);

		soap.mock(BankCustomerServicePortType.class, null);
	}
	
	@Test
	public void testInvalidParameters2() {
		exception.expect(IllegalArgumentException.class);

		soap.mock(BankCustomerServicePortType.class, "http://localhost:12345", new ArrayList<String>());
	}

	@Test
	public void testInvalidParameters3() {
		exception.expect(IllegalArgumentException.class);

		soap.mock(BankCustomerServicePortType.class, "http://localhost:12345", "");
	}
	
	@Test
	public void testInvalidConstructor1() {
		exception.expect(IllegalArgumentException.class);

		SoapServiceRule.newInstance(-1, 1, new String[]{});
	}

	@Test
	public void testInvalidConstructor2() {
		exception.expect(IllegalArgumentException.class);

		SoapServiceRule.newInstance(2, 1, new String[]{});
	}

	@Test
	public void testInvalidConstructor3() {
		exception.expect(IllegalArgumentException.class);

		SoapServiceRule.newInstance(2, Integer.MAX_VALUE, new String[]{});
	}

	@Test
	public void testInvalidConstructor4() {
		exception.expect(IllegalArgumentException.class);

		SoapServiceRule.newInstance(10000, 10001, new String[]{"a", "b", "c"});
	}

	@Test
	public void testWSDL() throws Exception {
		String address = "http://localhost:12345/service";
		
		BankCustomerServicePortType mock = soap.mock(BankCustomerServicePortType.class, address);
		
		URL url = new URL(address + "?wsdl");
		
		String wsdl = IOUtils.toString(url.openStream());

		assertThat(wsdl, containsString("wsdl:definitions"));
	}
	
	/**
	 * Test that stop and (re)start works - simulate service offline down.
	 * 
	 * @throws Exception
	 */
	
	@Test
	public void testEndpointStartStop() throws Exception {
		String address = "http://localhost:12345/service";
		
		soap.mock(BankCustomerServicePortType.class, address);
		
		URL url = new URL(address + "?wsdl");
		
		soap.stop();
		
		try {
			url.openStream();
			
			Assert.fail();
		} catch(FileNotFoundException e) {
			// pass
		}
		Assert.assertFalse(SoapEndpointRule.isPortAvailable(new URL(address).getPort()));
		
		soap.start();
		
		String wsdl = IOUtils.toString(url.openStream());

		assertThat(wsdl, containsString("wsdl:definitions"));
		
		soap.destroy();
		
		// currently, it seems like ports are not freed. TODO
		//Assert.assertTrue(SoapEndpointRule.isPortAvailable(new URL(address).getPort()));
		
	}
	

}
