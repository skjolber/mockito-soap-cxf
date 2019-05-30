package com.skjolberg.mockito.soap;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMResult;

import org.apache.cxf.binding.soap.SoapFault;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class SoapServiceFault {

	/**
	 * Create SOAP fault without detail.
	 *
	 * @return SOAP fault
	 */

	public static SoapFault createFault() {
		QName qName = SoapFault.FAULT_CODE_SERVER;
		return new SoapFault("message", qName);
	}

	/**
	 * Create SOAP fault with detail.
	 *
	 * @param detail fault detail
	 * @return SOAP fault
	 */

	public static SoapFault createFault(Node detail) {
		QName qName = SoapFault.FAULT_CODE_SERVER;
		SoapFault fault = new SoapFault("message", qName);
		Element detailElement = fault.getOrCreateDetail();
		detailElement.appendChild(detailElement.getOwnerDocument().importNode(detail, true));
		return fault;
	}

	/**
	 * Create SOAP fault with detail.
	 *
	 * @param detail XML string fault detail
	 * @return SOAP fault
	 */

	public static SoapFault createFault(String detail) {

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(detail)));

			return createFault(document.getDocumentElement());
		} catch(Exception e) {
			throw new IllegalArgumentException(detail, e);
		}

	}

	/**
	 * Create SOAP fault with detail.
	 *
	 * @param detail JAXB-serializable detail
	 * @return SOAP fault
	 */

	public static SoapFault createFault(Object detail) {
		return createFault(detail, null);
	}

	public static <T> SoapFault createFault(Object detail, QName qname) {
		// not for production use; does not reuse JAXB context
		try {
			JAXBContext context = JAXBContext.newInstance(detail.getClass());

			DOMResult result = new DOMResult();

			Marshaller marshaller;
			if(detail.getClass().isAnnotationPresent(XmlRootElement.class)) {
				marshaller = getMarshaller(context, false);
			} else {
				detail = new JAXBElement(qname, detail.getClass(), detail);
				marshaller = getMarshaller(context, true);
			}

			marshaller.marshal(detail, result);

			return createFault(result.getNode().getFirstChild());
		} catch(Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	protected static Marshaller getMarshaller(JAXBContext context, boolean fragment) throws JAXBException {
		Marshaller marshaller = context.createMarshaller();

		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, fragment);

		return marshaller;
	}

}
