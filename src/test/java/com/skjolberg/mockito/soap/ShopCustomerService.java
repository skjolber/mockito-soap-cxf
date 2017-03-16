package com.skjolberg.mockito.soap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.skjolber.shop.example.v1.GetItemsRequest;
import com.github.skjolber.shop.example.v1.GetItemsResponse;
import com.github.skjolber.shop.example.v1.ShopCustomerServicePortType;
import com.github.skjolber.shop.example.v1.ShopException;
import com.github.skjolber.shop.example.v1.ShopException_Exception;
import com.github.skjolber.shop.example.v1.ShopRequestHeader;

/**
 * 
 * Some kind of service bean which forwards calls to the webservice using the webservice client.
 * 
 * @author thomas
 *
 */

@Service
public class ShopCustomerService {

	private static Logger logger = LoggerFactory.getLogger(ShopCustomerService.class);

	private ShopCustomerServicePortType port; // bankCustomerServiceClient

	@Autowired
	public ShopCustomerService(ShopCustomerServicePortType port) {
		this.port = port;
	}

	public GetItemsResponse getItems(String customerNumber, String secret) throws Exception {

		logger.info("Get items for {} with secret {}", customerNumber, secret) ;
		
		GetItemsRequest request = new GetItemsRequest();
		request.setCustomerNumber(customerNumber);

		ShopRequestHeader requestHeader = getSecurityHeader(secret);

		try {
			return port.getItems(request, requestHeader);
		} catch(ShopException_Exception e) {

			ShopException faultInfo = e.getFaultInfo();
			
			logger.warn("Problem getting items: " + faultInfo.getCode() + ": " + faultInfo.getMessage());
			
			throw new Exception("unable to recover", e);
		}

	}

	private ShopRequestHeader getSecurityHeader(String secret) {
		ShopRequestHeader requestHeader = new ShopRequestHeader();
		requestHeader.setSecret(secret);
		return requestHeader;
	}

}

