module com.github.skjolber.mockito.soap {
	
	requires junit;
	requires org.junit.jupiter.api;
	requires org.mockito;

	requires jakarta.xml.bind;
	requires jakarta.activation;
	requires jakarta.xml.ws;
	
	requires java.net.http;
	requires java.base;
	
	requires org.apache.cxf.core;
	requires org.apache.cxf.binding.soap;
	requires org.apache.cxf.frontend.jaxws;
	requires org.apache.cxf.transport.http;
	requires org.apache.cxf.frontend.jaxrs;
	requires org.apache.cxf.frontend.simple;
	
	exports com.github.skjolber.mockito.soap;
	
}