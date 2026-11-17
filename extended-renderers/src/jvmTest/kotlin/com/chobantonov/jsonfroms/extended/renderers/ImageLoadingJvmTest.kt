package com.chobantonov.jsonfroms.extended.renderers

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ImageViewElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.Base64
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertIs

class ImageLoadingJvmTest {
    private fun presentationSvg(): ImageViewElement {
        val fixture = getBuiltInExamples().first { it.name == "spec-presentation" }
        val categories = assertIs<VerticalLayout>(fixture.uischema).elements
            .filterIsInstance<CategorizationElement>().single().elements
        return categories.filterIsInstance<CategoryElement>().first { it.name == "image" }
            .elements.filterIsInstance<ImageViewElement>().first()
    }

    @Test
    fun importedPresentationSvgDataUrlDecodes() {
        runBlocking {
            val loader = ImageLoader.Builder(PlatformContext.INSTANCE).build()
            try {
                val result = loader.execute(ImageRequest.Builder(PlatformContext.INSTANCE)
                    .data(presentationSvg().src).build())
                assertIs<SuccessResult>(result)
            } finally {
                loader.shutdown()
            }
        }
    }

    @Test
    fun remoteSvgLoadsThroughKtorEngine() {
        val bytes = Base64.getDecoder().decode(presentationSvg().src!!.substringAfter(','))
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/logo.svg") { exchange ->
            exchange.responseHeaders.add("Content-Type", "image/svg+xml")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            runBlocking {
                val loader = ImageLoader.Builder(PlatformContext.INSTANCE).build()
                try {
                    val url = "http://127.0.0.1:${server.address.port}/logo.svg"
                    val result = loader.execute(ImageRequest.Builder(PlatformContext.INSTANCE).data(url).build())
                    assertIs<SuccessResult>(result)
                } finally {
                    loader.shutdown()
                }
            }
        } finally {
            server.stop(0)
        }
    }
}
