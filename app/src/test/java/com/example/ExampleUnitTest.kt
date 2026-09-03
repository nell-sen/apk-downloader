package com.example

import com.example.core.network.UrlSecurity
import com.example.core.parser.HlsParser
import com.example.core.parser.HlsUrlResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testUrlSecurity_blocksUnsafeSchemes() {
        assertFalse(UrlSecurity.isSafeUrl("file:///sdcard/video.mp4"))
        assertFalse(UrlSecurity.isSafeUrl("javascript:alert(1)"))
        assertFalse(UrlSecurity.isSafeUrl("data:text/html,test"))
        assertFalse(UrlSecurity.isSafeUrl("http://localhost:8080/stream"))
        assertFalse(UrlSecurity.isSafeUrl("http://127.0.0.1/video.mp4"))
        assertFalse(UrlSecurity.isSafeUrl("http://192.168.1.10/video.mp4"))
        assertTrue(UrlSecurity.isSafeUrl("https://example.com/stream.m3u8"))
    }

    @Test
    fun testHlsUrlResolver_resolvesRelativeAndQueries() {
        val base = "https://cdn.example.com/live/master.m3u8?token=xyz123"
        val relative = "1080p/index.m3u8"
        val resolved = HlsUrlResolver.resolveUrl(base, relative)
        assertEquals("https://cdn.example.com/live/1080p/index.m3u8?token=xyz123", resolved)

        val rootRelative = "/streams/chunk001.ts"
        val resolvedRoot = HlsUrlResolver.resolveUrl(base, rootRelative)
        assertEquals("https://cdn.example.com/streams/chunk001.ts?token=xyz123", resolvedRoot)
    }

    @Test
    fun testHlsParser_masterPlaylist() {
        val manifest = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,FRAME-RATE=60.000,CODECS="avc1.64002a,mp4a.40.2"
            1080p/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720,FRAME-RATE=30.000,CODECS="avc1.4d401f,mp4a.40.2"
            720p/index.m3u8
        """.trimIndent()

        assertTrue(HlsParser.isHlsManifest(manifest))
        assertTrue(HlsParser.isMasterPlaylist(manifest))

        val master = HlsParser.parseMasterPlaylist(manifest, "https://cdn.example.com/live/master.m3u8")
        assertEquals(2, master.variants.size)
        assertEquals("1080p", master.variants[0].qualityLabel)
        assertEquals(1080, master.variants[0].height)
        assertEquals("720p", master.variants[1].qualityLabel)
    }

    @Test
    fun testHlsParser_mediaPlaylist() {
        val mediaManifest = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-TARGETDURATION:10
            #EXT-X-MEDIA-SEQUENCE:0
            #EXTINF:9.009,
            segment_000.ts
            #EXTINF:9.009,
            segment_001.ts
            #EXT-X-ENDLIST
        """.trimIndent()

        val parsed = HlsParser.parseMediaPlaylist(mediaManifest, "https://cdn.example.com/live/1080p/index.m3u8")
        assertFalse(parsed.isLive)
        assertEquals(2, parsed.segments.size)
        assertEquals(10, parsed.targetDurationSeconds)
        assertEquals("https://cdn.example.com/live/1080p/segment_000.ts", parsed.segments[0].url)
    }
}
