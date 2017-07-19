package com.skjolberg.mockito.soap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SoapServiceRuleTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testProperties() {
		exception.expect(IllegalArgumentException.class);
		SoapServiceRule.properties(true, "string");
	}
}
