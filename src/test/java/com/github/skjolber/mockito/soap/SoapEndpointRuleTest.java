package com.github.skjolber.mockito.soap;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.cxf.helpers.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.skjolber.bank.example.v1.BankCustomerServicePortType;
import com.github.skjolber.mockito.soap.PortManager;
import com.github.skjolber.mockito.soap.SoapEndpointRule;
import com.github.skjolber.mockito.soap.SoapServiceRule;

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

		soap.mock(BankCustomerServicePortType.class, "http://localhost:12345", new ArrayList<>());
	}

	@Test
	public void testInvalidParameters3() {
		exception.expect(IllegalArgumentException.class);

		soap.mock(BankCustomerServicePortType.class, "http://localhost:12345", "");
	}

	@Test
	public void testInvalidConstructor1() {
		exception.expect(IllegalArgumentException.class);

		SoapServiceRule.newInstance(-1, 1);
	}

	@Test
	public void testInvalidConstructor2() {
		exception.expect(IllegalArgumentException.class);

		SoapServiceRule.newInstance(2, 1);
	}

	@Test
	public void testInvalidConstructor3() {
		exception.expect(IllegalArgumentException.class);

		SoapServiceRule.newInstance(2, Integer.MAX_VALUE);
	}

	@Test
	public void testInvalidConstructor4() {
		exception.expect(IllegalArgumentException.class);

		SoapServiceRule.newInstance(10000, 10001, "a", "b", "c");
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
		int port = 12344;
		String address = "http://localhost:" + port + "/service";

		soap.mock(BankCustomerServicePortType.class, address);

		URL url = new URL(address + "?wsdl");

		soap.stop();

		try {
			url.openStream();

			Assert.fail();
		} catch(FileNotFoundException e) {
			// pass
		}
		Assert.assertFalse(PortManager.isPortAvailable(port));

		soap.start();

		try (InputStream in = url.openStream()) {
			String wsdl = IOUtils.toString(in);
			assertThat(wsdl, containsString("wsdl:definitions"));
		}

		soap.destroy();

		Assert.assertTrue(PortManager.isPortAvailable(port));
	}

	@Test
	public void testPortRange() throws IOException {
		int start = 40000;
		int end = start + 10000;
		String[] portNames = { "port1", "port2", "port3" };
		SoapEndpointRule soap = SoapEndpointRule.newInstance(start, end, portNames);
		soap.before();
		try {
			Assert.assertEquals(new HashSet<>(Arrays.asList(portNames)), soap.getPorts().keySet());
			for (Integer port : soap.getPorts().values()) {
				// verify port is within the range
				Assert.assertTrue(port >= start && port <= end);
				// and make sure it's really the port being used
				String address = "http://localhost:" + port + "/service";
				URL url = new URL(address + "?wsdl");
				soap.mock(BankCustomerServicePortType.class, address);
				try (InputStream in = url.openStream()) {
					String wsdl = IOUtils.toString(in);
					assertThat(wsdl, containsString("wsdl:definitions"));
				}
			}
		} finally {
			soap.after();
		}
	}

}
