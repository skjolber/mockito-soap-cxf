package com.skjolberg.mockito.soap;


import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.camunda.bpm.example.spring.soap.v1.BankCustomerServicePortType;
import com.camunda.bpm.example.spring.soap.v1.BankException;
import com.camunda.bpm.example.spring.soap.v1.BankException_Exception;
import com.camunda.bpm.example.spring.soap.v1.BankRequestHeader;
import com.camunda.bpm.example.spring.soap.v1.GetAccountsRequest;
import com.camunda.bpm.example.spring.soap.v1.GetAccountsResponse;

@Service
public class BankCustomerService {

	private static Logger log = Logger.getLogger(BankCustomerService.class.getName());

	private BankCustomerServicePortType port;

	@Autowired
	public BankCustomerService(BankCustomerServicePortType port) {
		this.port = port;
	}


	public GetAccountsResponse getAccounts(String customerNumber, String secret) throws Exception {

		GetAccountsRequest request = new GetAccountsRequest();
		request.setCustomerNumber(customerNumber);

		BankRequestHeader requestHeader = getSecurityHeader(secret);

		try {
			return port.getAccounts(request, requestHeader);
		} catch(BankException_Exception e) {

			BankException faultInfo = e.getFaultInfo();
			
			log.warning("Problem getting accounts: " + faultInfo.getCode() + ": " + faultInfo.getMessage());
			
			throw new Exception("unable to recover", e);
		}

	}

	private BankRequestHeader getSecurityHeader(String secret) {
		BankRequestHeader requestHeader = new BankRequestHeader();
		requestHeader.setSecret(secret);
		return requestHeader;
	}

}

