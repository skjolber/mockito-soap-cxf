package com.skjolberg.mockito.soap;

import static com.skjolberg.mockito.soap.SoapServiceFault.createFault;

import java.io.IOException;

import org.apache.cxf.helpers.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.skjolberg.example.spring.soap.v1.BankException;

public class SoapServiceFaultTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void createSoapFaultJAXB() {
		BankException bankException = new BankException();
		bankException.setCode("a");
		bankException.setMessage("b");

		createFault(bankException);		
	}
	
	@Test
	public void createSoapFaultNonJAXBException() {
		exception.expect(Exception.class);
		createFault(new Object());		
	}
	
	@Test
	public void createSoapFaultString() throws IOException {
		BankException bankException = new BankException();
		bankException.setCode("a");
		bankException.setMessage("b");

		createFault(IOUtils.toString(getClass().getResourceAsStream("/example/bankException.xml")));
	}

	@Test
	public void createSoapFaultNonXmlStringException() {
		exception.expect(Exception.class);
		createFault("abc");		
	}
}
