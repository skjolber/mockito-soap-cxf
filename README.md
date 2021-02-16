[![Build Status](https://travis-ci.org/skjolber/mockito-soap-cxf.svg?branch=master)](https://travis-ci.org/skjolber/mockito-soap-cxf)

# mockito-soap-cxf
SOAP web-service mocking utility which creates real service endpoints on local ports using webserver instances. These endpoints delegate requests directly to mocks.

Users will benefit from
 * full stack client testing
   * interceptors
   * handlers
 * simple JUnit Rule setup
 * SOAP-Fault helper

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
    <version>1.2.0</version>
    <scope>test</scope>
</dependency>
```

For JDK9+, add module `com.github.skjolber.mockito.soap`.

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

## Junit 5
If you prefer skipping to a full example, see [this unit test](src/test/java/com/github/skjolber/mockito/soap/BankCustomerSoapServerExtensionTest.java).

### Basics
Add a `SoapServiceExtension`

```java
@ExtendWith(SoapServiceExtension.class)
```

and mock service endpoints by using

```java
private MyServicePortType serviceMock;

@BeforeEach
public void setup(SoapServiceExtension soap) {
  serviceMock = soap.mock(MyServicePortType.class, "http://localhost:12345");
}
```

or, preferably

```java
private MyServicePortType serviceMock;

@BeforeEach
public void setup(SoapServiceExtension soap) {
  soap.mock(MyServicePortType.class, "http://localhost:12345", Arrays.asList("classpath:wsdl/MyService.xsd"));
}
```

for schema validation. The returned `serviceMock` instance is a normal Mockito mock(..) object.

## JUnit 4
If you prefer skipping to a full example, see [this unit test](src/test/java/com/github/skjolber/mockito/soap/BankCustomerSoapEndpointRuleTest.java).

### Basics
Create a `SoapServiceRule`

```java
@Rule
public SoapServiceRule soap = SoapServiceRule.newInstance();
```

add a field

```java
private MyServicePortType serviceMock;
```

and mock service endpoints by using

```java
@Before
public void mockService() {
  serviceMock = serviceMock = soap.mock(MyServicePortType.class, "http://localhost:12345");
}
```

or, preferably

```java
serviceMock = soap.mock(MyServicePortType.class, "http://localhost:12345", Arrays.asList("classpath:wsdl/MyService.xsd"));
```

for schema validation. The returned `serviceMock` instance is a normal Mockito mock(..) object.

# Mocking
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

and apply standard Mockito test approach. After triggering calls to the mock service, verify number of method calls

```java
ArgumentCaptor<GetAccountsRequest> argument1 = ArgumentCaptor.forClass(GetAccountsRequest.class);
verify(serviceMock, times(1)).getAccounts(argument1.capture());
```

and request details

```java
GetAccountsRequest request = argument1.getValue();
assertThat(request.getCustomerNumber(), is(customerNumber));
```

### SOAP Faults
Mock SOAP faults by adding import

```java
import static com.skjolberg.mockito.soap.SoapServiceFault.*;
```

then mock doing

```java
when(serviceMock.getAccounts(any(GetAccountsRequest.class))).thenThrow(createFault(exception));
```

or mock directly using an XML string / w3c DOM node.

# MTOM (binary attachments)
CXF SOAP clients support MTOM of out the box, enable MTOM in the service mock using

```java
serviceMock = soap.mock(BankCustomerServicePortType.class, bankCustomerServiceAddress, properties("mtom-enabled", Boolean.TRUE));
```

and add a `DataHandler` to the mock response using

```java
byte[] mockData = new byte[] {0x00, 0x01};
DataSource source = new ByteArrayDataSource(mockData, "application/octet-stream");
mockResponse.setCertificate(new DataHandler(source)); // MTOM-enabled base64binary
```

See [MTOM unit test](src/test/java/com/skjolberg/mockito/soap/BankCustomerSoapServerRuleMtomTest.java) for an example.

# Running in parallel (Junit 4 only)
For use-cases which require test-cases to run in parallel, it is possible to mock endpoints on random (free) ports. For the `SoapEndpointRule` methods

```java
@ClassRule
public static SoapEndpointRule soap = SoapEndpointRule.newInstance("myPort", "yourPort");
```

or with port range

```java
@ClassRule
public static SoapEndpointRule soap = SoapEndpointRule.newInstance(10000, 30000, "myPort", "yourPort");
```

there will be reserved two random free ports. Ports numbers can be retrieved using.
```java
int myPort = soap.getPort("myPort");
```
and

```java
String myPort = System.getProperty("myPort");
```

In other words, for property resolvers which include system-properties, the reserved ports are readily available. For example the Spring property expression

```
http://localhost:${myPort}/selfservice/bank
```

would effectively point to the mocked webservice at `myPort`. For a more complete example, see
[this spring unit test](src/test/java/com/skjolberg/mockito/soap/BankCustomerSoapEndpointClassRuleTest.java).

# Troubleshooting
There seems to be an issue with the use of the `-exsh` parameter for passing headers into the mock and schema validation. Rather than supplying the wsdl location, supply the XSD locations to work around the problem until a solution can be found.

### CXF version 2.x
If you see exception cause by

> No binding factory for namespace http://schemas.xmlsoap.org/soap/ registered.

then you're mixing CXF version 2 and 3 - see above about excluding `cxf-core` artifact.

# History

 - 1.2.0: JUnit 5 support.
 - 1.1.0: Automatic module name; renamed packages accordingly.
 - 1.0.5: A lot of refactorings and code cleanups, update dependencies and fix port release - many thanks to [amichair](https://github.com/amichair)!
 - 1.0.4: Allow the usage of local:// transport - compliments of [aukevanleeuwen](https://github.com/aukevanleeuwen)
 - 1.0.3: MTOM support
 - 1.0.2: Support for mocking on (random) free ports (via SoapEndpointRule).
 - 1.0.1: Improved JAXB helper methods in SoapServiceFault
 - 1.0.0: Initial version

[Apache 2.0]:           http://www.apache.org/licenses/LICENSE-2.0.html
[issue-tracker]:        https://github.com/skjolber/mockito-soap-cxf/issues
[Maven]:                http://maven.apache.org/
[1.0.4]:                https://github.com/skjolber/mockito-soap-cxf/releases
