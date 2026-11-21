package com.yenaly.han1meviewer.util

import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.SiteCheckIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadLayoutTest {

    private fun codeFor(site: SiteType): String = when (site) {
        SiteType.HANIME, SiteType.JAVCHU -> "123456"
        SiteType.BUONDUA -> SiteCheckIn.dishCode(site, "my-album/Videos/clip01")
        SiteType.FAPFOLDER -> SiteCheckIn.dishCode(site, "Profiles/some_name/Videos/42")
        else -> SiteCheckIn.dishCode(site, "abc123")
    }

    @Test
    fun roundTripForEverySite() {
        for (site in SiteType.entries) {
            val code = codeFor(site)
            assertEquals(
                "round trip failed for $site",
                code,
                DownloadLayout.videoCodeFromSegments(DownloadLayout.segmentsFor(code)),
            )
        }
    }

    @Test
    fun siteFolderMatchingIsCaseInsensitive() {
        assertEquals("onair:abc", DownloadLayout.videoCodeFromSegments(listOf("onair", "abc")))
        assertEquals("onair:abc", DownloadLayout.videoCodeFromSegments(listOf("ONAIR", "abc")))
        assertEquals("777", DownloadLayout.videoCodeFromSegments(listOf("HANIME_DOWNLOAD", "777")))
    }

    @Test
    fun unknownOrIncompleteFoldersAreRejected() {
        assertNull(DownloadLayout.videoCodeFromSegments(emptyList()))
        assertNull(DownloadLayout.videoCodeFromSegments(listOf("SomethingElse", "abc")))
        assertNull(DownloadLayout.videoCodeFromSegments(listOf("OnAir")))
        assertNull(DownloadLayout.videoCodeFromSegments(listOf("hanime_download", "a", "b")))
    }

    @Test
    fun videoFileNameParsing() {
        assertEquals("My_Title" to "720P", DownloadScanner.parseVideoFileName("My_Title_720P.mp4"))
        assertEquals("Title" to "default", DownloadScanner.parseVideoFileName("Title_default.ts"))
        assertEquals("NoUnderscore" to "default", DownloadScanner.parseVideoFileName("NoUnderscore.mp4"))
        assertEquals("A_B_C" to "default", DownloadScanner.parseVideoFileName("A_B_C_default.mkv"))
    }

    @Test
    fun videoExtensionsIncludeHlsAndCaseInsensitive() {
        assertEquals(true, DownloadLayout.isVideoFileName("clip.TS"))
        assertEquals(true, DownloadLayout.isVideoFileName("clip.m4v"))
        assertEquals(false, DownloadLayout.isVideoFileName("cover.png"))
        assertEquals(false, DownloadLayout.isVideoFileName("info.json"))
        assertEquals(false, DownloadLayout.isVideoFileName(null))
    }

    @Test
    fun infoJsonV2AndLegacyAreBothParsed() {
        val v2 = DownloadScanner.parseInfo(
            """{"version":2,"videoCode":"onair:abc","title":"T","quality":"default","videoFile":"T_default.mp4","addDate":5,
               "videoUrls":{"default":{"link":"https://x/y.mp4"}}}"""
        )!!
        assertEquals("onair:abc", v2.videoCode)
        assertEquals("T_default.mp4", v2.videoFile)
        assertEquals("https://x/y.mp4", v2.videoUrls["default"])

        val legacy = DownloadScanner.parseInfo("""{"title":"Old","coverUrl":"https://c","videoUrls":{"720P":{"link":"https://v"}}}""")!!
        assertNull(legacy.videoCode)
        assertEquals("Old", legacy.title)
        assertEquals("https://v", legacy.videoUrls["720P"])

        assertNull(DownloadScanner.parseInfo("not json"))
    }
}
