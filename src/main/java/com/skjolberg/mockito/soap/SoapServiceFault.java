package com.skjolberg.mockito.soap;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
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

	/** construct soap fault */
	public static SoapFault createFault(Node payload) {
		QName qName = SoapFault.FAULT_CODE_SERVER;
		SoapFault fault = new SoapFault("message", qName);
		Element detail = fault.getOrCreateDetail();
		detail.appendChild(detail.getOwnerDocument().importNode(payload, true));
		return fault;
	}

	public static <T> SoapFault createFault(String payload) {
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		    factory.setNamespaceAware(true);
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document document = builder.parse(new InputSource(new StringReader(payload)));
	        
	        return createFault(document.getDocumentElement());
		} catch(Exception e) {
			throw new IllegalArgumentException(payload, e);
		}
        
	}
	
	public static <T> SoapFault createFault(Object payload) {

		try {
			JAXBContext context = JAXBContext.newInstance(payload.getClass());
	
			DOMResult result = new DOMResult();
	
			Marshaller marshaller = context.createMarshaller();
	
			marshaller.marshal(payload, result);
			
	        return createFault(result.getNode().getFirstChild());
		} catch(Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

}
