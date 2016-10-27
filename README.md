# mockito-soap-cxf
SOAP web-service mocking utility which creates real service endpoints on local ports - offering traffic over HTTP.

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
The project is based on [Maven] and is pending release to central Maven repository.

Example dependency config:

```xml
<dependency>
	<groupId>com.skjolberg</groupId>
	<artifactId>mockito-soap-cxf</artifactId>
	<version>1.0.0-SNAPSHOT</version>
	<scope>test</scope>
</dependency>
```

# Usage
If you prefer skipping to a full example, see [this unit test](src/test/java/com/skjolberg/mockito/soap/SoapServiceRuleTest.java). 

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

The returned `serviceMock` instance is a normal Mockito mock(..) object. 

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

# License
Apache license 2.0.

[Apache 2.0]:          	http://www.apache.org/licenses/LICENSE-2.0.html
[issue-tracker]:       	https://github.com/skjolber/mockito-soap-cxf/issues			