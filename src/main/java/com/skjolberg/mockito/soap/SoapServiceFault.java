package com.skjolberg.mockito.soap;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
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

	/**
	 * Create SOAP fault without detail.
	 * 
	 * @return SOAP fault
	 */
	
	public static SoapFault createFault() {
		QName qName = SoapFault.FAULT_CODE_SERVER;
		SoapFault fault = new SoapFault("message", qName);
		return fault;
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
	
	public static <T> SoapFault createFault(String detail) {
		
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
	
	public static <T> SoapFault createFault(Object detail) {
		// not for production use; does not reuse JAXB context 
		try {
			JAXBContext context = JAXBContext.newInstance(detail.getClass());
	
			DOMResult result = new DOMResult();
	
			Marshaller marshaller = context.createMarshaller();
	
			marshaller.marshal(detail, result);
			
	        return createFault(result.getNode().getFirstChild());
		} catch(Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

    /**
     * Create SOAP fault with detail.
     * <p>Workaround for auto-generated classes not having the @XmlRootElement annotation:
     * use JAXBElement wrapper objects, which provide the same information as @XmlRootElement,
     * but in the form of an object, rather than an annotation.
     * </p>
     *
     * @param detail JAXB-serializable detail
     * @param clazz JAXB-serializable detail class
     * @return SOAP fault
     */
    public static <T> SoapFault createFault(T detail, Class<T> clazz) {
        // not for production use; does not reuse JAXB context
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            DOMResult result = new DOMResult();
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(new JAXBElement<T>(new QName("", "local"), clazz, detail), result);
            return SoapServiceFault.createFault(result.getNode().getFirstChild());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

}
