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

public class SoapServerRuleTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Rule
	public SoapServerRule soap = SoapServerRule.newInstance();

	@Test
	public void testInvalidParameters1() {
		exception.expect(IllegalArgumentException.class);

		soap.mock(BankCustomerServicePortType.class, null);
	}
	
	@Test
	public void testInvalidParameters2() {
		exception.expect(IllegalArgumentException.class);

		soap.mock(BankCustomerServicePortType.class, "http://localhost:12345", new ArrayList<>());
	}

	@Test
	public void testInvalidParameters3() {
		exception.expect(IllegalArgumentException.class);

		soap.mock(BankCustomerServicePortType.class, "http://localhost:12345", "");
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
	public void testStartStop() throws Exception {
		String address1 = "http://localhost:12346/service1";
		String address2 = "http://localhost:12346/service2";
		
		soap.mock(BankCustomerServicePortType.class, address1);
		soap.mock(BankCustomerServicePortType.class, address2);
		
		URL url1 = new URL(address1 + "?wsdl");
		URL url2 = new URL(address2 + "?wsdl");
		
		soap.stop();
		
		try {
			url1.openStream();
			
			Assert.fail();
		} catch(FileNotFoundException e) {
			// pass
		}
		try {
			url2.openStream();
			
			Assert.fail();
		} catch(FileNotFoundException e) {
			// pass
		}
		// ports are still taken
		Assert.assertFalse(SoapEndpointRule.isPortAvailable(new URL(address1).getPort()));
		Assert.assertFalse(SoapEndpointRule.isPortAvailable(new URL(address2).getPort()));

		soap.start();
		
		String wsdl1 = IOUtils.toString(url1.openStream());
		assertThat(wsdl1, containsString("wsdl:definitions"));
		
		String wsdl2 = IOUtils.toString(url2.openStream());
		assertThat(wsdl2, containsString("wsdl:definitions"));
		
		soap.destroy();

		// currently, it seems like ports are not freed. TODO
		//Assert.assertTrue(SoapEndpointRule.isPortAvailable(new URL(address1).getPort()));
		//Assert.assertTrue(SoapEndpointRule.isPortAvailable(new URL(address2).getPort()));
	}
	

}
