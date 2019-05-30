package com.skjolberg.mockito.soap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.skjolber.bank.example.v1.BankCustomerServicePortType;
import com.github.skjolber.bank.example.v1.BankRequestHeader;
import com.github.skjolber.bank.example.v1.GetAccountsRequest;
import com.github.skjolber.bank.example.v1.GetAccountsResponse;

import static com.skjolberg.mockito.soap.SoapServiceRule.*;

/**
 * Test use of MTOM (binary attachments).
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:/spring/mtom.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("mtom")
public class BankCustomerSoapEndpointRuleMtomTest {

	@Rule
	public SoapEndpointRule soap = SoapEndpointRule.newInstance();

	/**
	 * Endpoint addresses (full url), typically pointing to localhost for unit testing, remote host otherwise.
	 */
	@Value("${bankcustomer1.service}")
	private String bankCustomerServiceAddress1;

	@Value("${bankcustomer2.service}")
	private String bankCustomerServiceAddress2;

	/**
	 * Mock objects proxied by SOAP service
	 */
	private BankCustomerServicePortType bankServiceMock1;
	private BankCustomerServicePortType bankServiceMock2;

	/**
	 * Business code which calls the SOAP service via an autowired client
	 */
	@Autowired
	@Qualifier("bankCustomerServiceClient1")
	private BankCustomerServicePortType port1;

	@Autowired
	@Qualifier("bankCustomerServiceClient2")
	private BankCustomerServicePortType port2;

	@Before
	public void setup() {
		bankServiceMock1 = soap.mock(BankCustomerServicePortType.class, bankCustomerServiceAddress1, Arrays.asList("classpath:wsdl/BankCustomerService.xsd"));
		bankServiceMock2 = soap.mock(BankCustomerServicePortType.class, bankCustomerServiceAddress2, Arrays.asList("classpath:wsdl/BankCustomerService.xsd"), properties("mtom-enabled", Boolean.TRUE));
	}

	/**
	 * Base64binary field embedded within the XML document
	 */
	@Test
	public void processNormalSoapCallWithoutMTOM() throws Exception {
		processCall(bankServiceMock1, port1);
	}

	/**
	 * Base64binary field referenced in the XML document, transported as multipart binary.
	 */
	@Test
	public void processNormalSoapCallWithMTOM() throws Exception {
		processCall(bankServiceMock2, port2);
	}

	private void processCall(BankCustomerServicePortType bankServiceMock, BankCustomerServicePortType port) throws Exception {
		// add mock response
		GetAccountsResponse mockResponse = new GetAccountsResponse();
		List<String> accountList = mockResponse.getAccount();
		accountList.add("1234");
		accountList.add("5678");

		byte[] payload = new byte[] {0x00, 0x01};
		DataSource source = new InputStreamDataSource(new ByteArrayInputStream(payload), "application/octet-stream");
		mockResponse.setCertificate(new DataHandler(source));

		when(bankServiceMock.getAccounts(any(GetAccountsRequest.class), any(BankRequestHeader.class))).thenReturn(mockResponse);

		String customerNumber = "123456789"; // must be all numbers, if not schema validation fails
		String secret = "abc";

		// actually do something
		GetAccountsResponse accounts = new BankCustomerService(port).getAccounts(customerNumber, secret);

		ArgumentCaptor<GetAccountsRequest> argument1 = ArgumentCaptor.forClass(GetAccountsRequest.class);
		ArgumentCaptor<BankRequestHeader> argument2 = ArgumentCaptor.forClass(BankRequestHeader.class);
		verify(bankServiceMock, times(1)).getAccounts(argument1.capture(), argument2.capture());

		GetAccountsRequest request = argument1.getValue();
		assertThat(request.getCustomerNumber(), is(customerNumber));

		// get data
		byte[] result = IOUtils.toByteArray(accounts.getCertificate().getInputStream());
		assertArrayEquals(result, payload);

		BankRequestHeader header = argument2.getValue();
		assertThat(header.getSecret(), is(secret));

		assertThat(accounts.getAccount(), is(accountList));
	}

}
