package com.skjolberg.mockito.soap;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;
import java.io.InputStream;
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
		int port1 = 12345;
		int port2 = 12345;
		String address1 = "http://localhost:" + port1 + "/service1";
		String address2 = "http://localhost:" + port2 + "/service2";

		soap.mock(BankCustomerServicePortType.class, address1);
		soap.mock(BankCustomerServicePortType.class, address2);

		URL url1 = new URL(address1 + "?wsdl");
		URL url2 = new URL(address2 + "?wsdl");

		soap.stop();

		try (InputStream in = url1.openStream()) {
			Assert.fail();
		} catch(FileNotFoundException e) {
			// pass
		}
		try (InputStream in = url2.openStream()) {
			Assert.fail();
		} catch(FileNotFoundException e) {
			// pass
		}
		// ports are still taken
		Assert.assertFalse(PortManager.isPortAvailable(port1));
		Assert.assertFalse(PortManager.isPortAvailable(port2));

		soap.start();

		try (InputStream in = url1.openStream()) {
			String wsdl1 = IOUtils.toString(in);
			assertThat(wsdl1, containsString("wsdl:definitions"));
		}

		try (InputStream in = url2.openStream()) {
			String wsdl2 = IOUtils.toString(url2.openStream());
			assertThat(wsdl2, containsString("wsdl:definitions"));
		}

		soap.destroy();

		Assert.assertTrue(PortManager.isPortAvailable(port1));
		Assert.assertTrue(PortManager.isPortAvailable(port2));
	}

}
