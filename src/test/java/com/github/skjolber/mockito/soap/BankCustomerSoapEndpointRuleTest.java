package com.github.skjolber.mockito.soap;

import static com.github.skjolber.mockito.soap.SoapServiceFault.createFault;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapFault;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.skjolber.bank.example.v1.BankCustomerServicePortType;
import com.github.skjolber.bank.example.v1.BankException;
import com.github.skjolber.bank.example.v1.BankRequestHeader;
import com.github.skjolber.bank.example.v1.CustomerException;
import com.github.skjolber.bank.example.v1.GetAccountsRequest;
import com.github.skjolber.bank.example.v1.GetAccountsResponse;
import com.github.skjolber.mockito.soap.SoapEndpointRule;
import com.github.skjolber.shop.example.v1.ShopCustomerServicePortType;

/**
 * Test without reserving ports (as a {@linkplain Rule}.
 *
 * @author skjolber
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:/spring/beans.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("dev1")
public class BankCustomerSoapEndpointRuleTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Rule
	public SoapEndpointRule soap = SoapEndpointRule.newInstance();

	/**
	 * Endpoint address (full url), typically pointing to localhost for unit testing, remote host otherwise.
	 */
	@Value("${bankcustomer.service}")
	private String bankCustomerServiceAddress;

	@Value("${shopcustomer.service}")
	private String shopCustomerServiceAddress;

	/**
	 * Mock object proxied by SOAP service
	 */
	private BankCustomerServicePortType bankServiceMock;
	private ShopCustomerServicePortType shopServiceMock;

	/**
	 * Business code which calls the SOAP service via an autowired client
	 */
	@Autowired
	private BankCustomerService bankCustomerService;

	@Before
	public void setup() throws Exception {
		bankServiceMock = soap.mock(BankCustomerServicePortType.class, bankCustomerServiceAddress, Arrays.asList("classpath:wsdl/BankCustomerService.xsd"));
		shopServiceMock = soap.mock(ShopCustomerServicePortType.class, shopCustomerServiceAddress);
	}

	/**
	 * Webservice call which results in regular response returned to the client.
	 */
	@Test
	public void processNormalSoapCall() throws Exception {
		// add mock response
		GetAccountsResponse mockResponse = new GetAccountsResponse();
		List<String> accountList = mockResponse.getAccount();
		accountList.add("1234");
		accountList.add("5678");

		when(bankServiceMock.getAccounts(any(GetAccountsRequest.class), any(BankRequestHeader.class))).thenReturn(mockResponse);

		String customerNumber = "123456789"; // must be all numbers, if not schema validation fails
		String secret = "abc";

		// actually do something
		GetAccountsResponse accounts = bankCustomerService.getAccounts(customerNumber, secret);

		ArgumentCaptor<GetAccountsRequest> argument1 = ArgumentCaptor.forClass(GetAccountsRequest.class);
		ArgumentCaptor<BankRequestHeader> argument2 = ArgumentCaptor.forClass(BankRequestHeader.class);
		verify(bankServiceMock, times(1)).getAccounts(argument1.capture(), argument2.capture());

		GetAccountsRequest request = argument1.getValue();
		assertThat(request.getCustomerNumber(), is(customerNumber));

		BankRequestHeader header = argument2.getValue();
		assertThat(header.getSecret(), is(secret));

		assertThat(accounts.getAccount(), is(accountList));
	}

	/**
	 * Webservice call which results in soap fault being returned to the client.
	 */
	@Test
	public void processSoapCallWithException1() throws Exception {
		// add mock response
		GetAccountsResponse mockResponse = new GetAccountsResponse();
		List<String> accountList = mockResponse.getAccount();
		accountList.add("1234");
		accountList.add("5678");

		String errorCode = "myErrorCode";
		String errorMessage = "myErrorMessage";

		BankException bankException = new BankException();
		bankException.setCode(errorCode);
		bankException.setMessage(errorMessage);

		SoapFault fault = createFault(bankException);

		when(bankServiceMock.getAccounts(any(GetAccountsRequest.class), any(BankRequestHeader.class))).thenThrow(fault);

		String customerNumber = "123456789";
		String secret = "abc";

		exception.expect(Exception.class);

		// actually do something
		bankCustomerService.getAccounts(customerNumber, secret);
	}

	@Test
	public void processSoapCallWithException2() throws Exception {
		// add mock response
		GetAccountsResponse mockResponse = new GetAccountsResponse();
		List<String> accountList = mockResponse.getAccount();
		accountList.add("1234");
		accountList.add("5678");

		String status = "DEAD";

		CustomerException customerException = new CustomerException();
		customerException.setStatus(status);

		SoapFault fault = createFault(customerException, new QName("http://soap.spring.example.skjolberg.com/v1", "customerException"));

		when(bankServiceMock.getAccounts(any(GetAccountsRequest.class), any(BankRequestHeader.class))).thenThrow(fault);

		String customerNumber = "123456789";
		String secret = "abc";

		exception.expect(Exception.class);

		// actually do something
		bankCustomerService.getAccounts(customerNumber, secret);
	}

	@Test
	public void processValidationException() throws Exception {
		// add mock response
		GetAccountsResponse mockResponse = new GetAccountsResponse();
		List<String> accountList = mockResponse.getAccount();
		accountList.add("1234");
		accountList.add("5678");

		when(bankServiceMock.getAccounts(any(GetAccountsRequest.class), any(BankRequestHeader.class))).thenReturn(mockResponse);

		String customerNumber = "abcdef"; // must be all numbers, if not schema validation fails
		String secret = "abc";

		exception.expect(Exception.class);

		// actually do something
		bankCustomerService.getAccounts(customerNumber, secret);
	}

}
