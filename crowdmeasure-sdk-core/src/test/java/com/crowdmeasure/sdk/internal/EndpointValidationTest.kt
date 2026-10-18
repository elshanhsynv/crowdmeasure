package com.crowdmeasure.sdk.internal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointValidationTest {

    @Test
    fun acceptsHttpsUrlsInAnyCase() {
        assertTrue(isValidHttpsEndpoint("https://example.com/probe"))
        assertTrue(isValidHttpsEndpoint("HTTPS://example.com/"))
        assertTrue(isValidHttpsEndpoint("  https://example.com:8443/path?x=1  "))
    }

    @Test
    fun rejectsNonHttpsMalformedAndCredentialedUrls() {
        assertFalse(isValidHttpsEndpoint("http://example.com"))
        assertFalse(isValidHttpsEndpoint("https://"))
        assertFalse(isValidHttpsEndpoint("not a url"))
        assertFalse(isValidHttpsEndpoint("https://user:secret@example.com/"))
        assertFalse(isValidHttpsEndpoint("https://user@example.com/"))
    }
}
