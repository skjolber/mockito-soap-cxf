package com.skjolberg.mockito.soap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import static org.mockito.ArgumentMatchers.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.skjolberg.mockito.soap.SoapServiceFault.createFault;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.binding.soap.SoapFault;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.TypedValue;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.camunda.bpm.example.spring.soap.v1.BankCustomerServicePortType;
import com.camunda.bpm.example.spring.soap.v1.BankException;
import com.camunda.bpm.example.spring.soap.v1.BankRequestHeader;
import com.camunda.bpm.example.spring.soap.v1.GetAccountsRequest;
import com.camunda.bpm.example.spring.soap.v1.GetAccountsResponse;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:/spring/beans.xml"})
public class SoapServiceRuleTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Rule
	public SoapServiceRule soap = SoapServiceRule.newInstance();

	/**
	 * Endpoint address, typically localhost for unit testing, remote host otherwise.
	 */

	@Value("${bankcustomer.service}")
	private String bankCustomerServiceAddress;

	/**
	 * Mock object proxied by SOAP service
	 * 
	 */
	private BankCustomerServicePortType serviceMock; 

	/**
	 * Business code which calls the SOAP service via an autowired client
	 * 
	 */
	@Autowired
	private BankCustomerService bankCustomerService;

	@Before
	public void setup() {
		serviceMock = soap.mock(BankCustomerServicePortType.class, bankCustomerServiceAddress);
	}

	@Test
	public void processNormalSoapCall() throws Exception {

		// add mock response
		GetAccountsResponse mockResponse = new GetAccountsResponse();
		List<String> accountList = mockResponse.getAccount();
		accountList.add("1234");
		accountList.add("5678");

		when(serviceMock.getAccounts(any(GetAccountsRequest.class), any(BankRequestHeader.class))).thenReturn(mockResponse);

		String customerNumber = "123456789";
		String secret = "abc";

		// actuall do something
		GetAccountsResponse accounts = bankCustomerService.getAccounts(customerNumber, secret);

		ArgumentCaptor<GetAccountsRequest> argument1 = ArgumentCaptor.forClass(GetAccountsRequest.class);
		ArgumentCaptor<BankRequestHeader> argument2 = ArgumentCaptor.forClass(BankRequestHeader.class);
		verify(serviceMock, times(1)).getAccounts(argument1.capture(), argument2.capture());

		GetAccountsRequest request = argument1.getValue();
		assertThat(request.getCustomerNumber(), is(customerNumber));

		BankRequestHeader header = argument2.getValue();
		assertThat(header.getSecret(), is(secret));

		assertThat(accounts.getAccount(), is(accountList));
	}

	@Test
	public void processSoapCallWithException() throws Exception {

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

		when(serviceMock.getAccounts(any(GetAccountsRequest.class), any(BankRequestHeader.class))).thenThrow(fault);

		String customerNumber = "123456789";
		String secret = "abc";

		exception.expect(Exception.class);
		 
		// actuall do something
		GetAccountsResponse accounts = bankCustomerService.getAccounts(customerNumber, secret);

	}


}
