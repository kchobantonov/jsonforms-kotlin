package com.chobantonov.jsonfroms.extended.renderers

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageDataUrlTest {
    @Test fun validAndEncodedBase64PayloadsRemainLoadable() {
        assertFalse(malformedBase64ImageDataUrl("https://example.com/image.png"))
        assertFalse(malformedBase64ImageDataUrl("data:image/png;base64,YQ=="))
        assertFalse(malformedBase64ImageDataUrl("data:image/png;base64,YQ%3D%3D"))
        assertFalse(malformedBase64ImageDataUrl("data:image/svg+xml;base64,PHN2Zz48L3N2Zz4="))
    }

    @Test fun malformedPayloadsFailBeforePlatformLoading() {
        assertTrue(malformedBase64ImageDataUrl("data:image/png;base64,%%%"))
        assertTrue(malformedBase64ImageDataUrl("data:image/png;base64,!!!!"))
        assertTrue(malformedBase64ImageDataUrl("data:image/png;base64,a"))
        assertTrue(malformedBase64ImageDataUrl("data:image/png;base64,"))
    }
}
