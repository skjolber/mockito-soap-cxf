[![Build Status](https://travis-ci.org/skjolber/mockito-soap-cxf.svg?branch=master)](https://travis-ci.org/skjolber/mockito-soap-cxf)

# mockito-soap-cxf
SOAP web-service mocking utility which creates real service endpoints on local ports - for traffic over HTTP.

Users will benefit from
 * full stack testing
   * interceptors
   * handlers
 * simple mock setup
 * SOAP fault helper

all with the regular advantages of Mockito.

Bugs, feature suggestions and help requests can be filed with the [issue-tracker].

## License
[Apache 2.0]

# Obtain
The project is based on [Maven] and is available form central Maven repository.

Example dependency config:

```xml
<dependency>
	<groupId>com.github.skjolber</groupId>
	<artifactId>mockito-soap-cxf</artifactId>
	<version>1.0.0</version>
	<scope>test</scope>
</dependency>
```

### CXF version < 3
Add an exclusion for the `cxf-core` artifact

```xml
<exclusions>
	<exclusion>
		<groupId>org.apache.cxf</groupId>
		<artifactId>cxf-core</artifactId>
	</exclusion>
</exclusions>
```

# Usage
If you prefer skipping to a full example, see [this unit test](src/test/java/com/skjolberg/mockito/soap/BankCustomerServiceTest.java). 

# Basics
Create a `SoapServiceRule`
```java
@Rule
public SoapServiceRule soap = SoapServiceRule.newInstance();
```

and mock service endpoints by using

```java
MyServicePortType serviceMock = soap.mock(MyServicePortType.class, "http://localhost:12345"); 
```
or, preferably

```java
MyServicePortType serviceMock = soap.mock(MyServicePortType.class, "http://localhost:12345", "classpath:/wsdl/MyService.wsdl"); 
```
for schema validation. The returned `serviceMock` instance is a normal Mockito mock(..) object. 

# Details
Create mock response via code

```java
// init response
GetAccountsResponse mockResponse = new GetAccountsResponse();
List<String> accountList = mockResponse.getAccount();
accountList.add("1234");
accountList.add("5678");
```

or from XML  

```java
GetAccountsResponse response = jaxbUtil.readResource("/my/test/GetAccountsResponse1.xml", GetAccountsResponse.class);
```
using your favorite JAXB utility. Then mock
```java
when(serviceMock.getAccounts(any(GetAccountsRequest.class))).thenReturn(mockResponse);
```
and apply standard Mockito test approach. Verify number of method calls

```java
ArgumentCaptor<GetAccountsRequest> argument1 = ArgumentCaptor.forClass(GetAccountsRequest.class);
verify(serviceMock, times(1)).getAccounts(argument1.capture());
```
and request details

```java
GetAccountsRequest request = argument1.getValue();
assertThat(request.getCustomerNumber(), is(customerNumber));
```
# SOAP Faults
Mock SOAP faults by adding import
```java
import static com.skjolberg.mockito.soap.SoapServiceFault.*;
```
then mock doing

```java
when(serviceMock.getAccounts(any(GetAccountsRequest.class))).thenThrow(createFault(exception));
```

or mock directly using an XML string / w3c DOM node.

# SOAP service mock as a field
Wrap mock creation using a @Before method if you prefer using fields for your mocks:

```java
@Value("${bankcustomer.service}")
private String bankCustomerServiceAddress;

private BankCustomerServicePortType serviceMock;

@Before
public void mockService() {
	serviceMock = soap.mock(BankCustomerServicePortType.class, bankCustomerServiceAddress);
}
```

# Troubleshooting
There seems to be an issue with the use of the `-exsh` parameter for passing headers into the mock and schema validation. Rather than supplying the wsdl location, supply the XSD locations to work around the problem until a solution can be found.

### CXF version 2.x
If you see exception cause by

> No binding factory for namespace http://schemas.xmlsoap.org/soap/ registered.

then you're mixing CXF version 2 and 3 - see above about excluding `cxf-core` artifact.

# History

 - [1.0.0]: Initial version

[Apache 2.0]:          	http://www.apache.org/licenses/LICENSE-2.0.html
[issue-tracker]:       	https://github.com/skjolber/mockito-soap-cxf/issues
[Maven]:                http://maven.apache.org/
[1.0.0]:				https://github.com/skjolber/mockito-soap-cxf/releases/tag/mockito-soap-cxf-1.0.0
