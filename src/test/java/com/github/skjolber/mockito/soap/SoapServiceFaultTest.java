package com.github.skjolber.mockito.soap;

import static com.github.skjolber.mockito.soap.SoapServiceFault.createFault;

import java.io.IOException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.apache.cxf.helpers.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.skjolber.bank.example.v1.BankException;

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
	public void createSoapFaultJAXBNoXmlRoot() {
		NoXmlRootElement noXmlRoot = new NoXmlRootElement();
		noXmlRoot.setFoo("bar");

		createFault(noXmlRoot, new QName("local"));
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

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = { "foo" })
	private static class NoXmlRootElement {

		@XmlElement(required = true)
		private String foo;

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}
}
