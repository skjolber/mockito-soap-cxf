package com.skjolberg.mockito.soap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.skjolber.bank.example.v1.BankCustomerServicePortType;
import com.github.skjolber.bank.example.v1.BankException;
import com.github.skjolber.bank.example.v1.BankException_Exception;
import com.github.skjolber.bank.example.v1.BankRequestHeader;
import com.github.skjolber.bank.example.v1.GetAccountsRequest;
import com.github.skjolber.bank.example.v1.GetAccountsResponse;

/**
 * 
 * Some kind of service bean which forwards calls to the webservice using the webservice client.
 * 
 * @author thomas
 *
 */

@Service
public class BankCustomerService {

	private static Logger logger = LoggerFactory.getLogger(BankCustomerService.class);

	private BankCustomerServicePortType port; // bankCustomerServiceClient

	@Autowired
	public BankCustomerService(BankCustomerServicePortType port) {
		this.port = port;
	}

	public GetAccountsResponse getAccounts(String customerNumber, String secret) throws Exception {

		logger.info("Get accounts for {} with secret {}", customerNumber, secret) ;
		
		GetAccountsRequest request = new GetAccountsRequest();
		request.setCustomerNumber(customerNumber);

		BankRequestHeader requestHeader = getSecurityHeader(secret);

		try {
			return port.getAccounts(request, requestHeader);
		} catch(BankException_Exception e) {

			BankException faultInfo = e.getFaultInfo();
			
			logger.warn("Problem getting accounts: " + faultInfo.getCode() + ": " + faultInfo.getMessage());
			
			throw new Exception("unable to recover", e);
		}

	}

	private BankRequestHeader getSecurityHeader(String secret) {
		BankRequestHeader requestHeader = new BankRequestHeader();
		requestHeader.setSecret(secret);
		return requestHeader;
	}

}

