package com.github.skjolber.mockito.soap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.skjolber.mockito.soap.SoapServiceRule;

public class SoapServiceRuleTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testProperties() {
		exception.expect(IllegalArgumentException.class);
		SoapServiceRule.properties(true, "string");
	}
}
