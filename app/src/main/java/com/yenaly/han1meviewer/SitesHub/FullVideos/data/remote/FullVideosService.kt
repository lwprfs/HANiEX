package com.yenaly.han1meviewer.SitesHub.FullVideos.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface FullVideosService {
    @GET
    suspend fun get(@Url url: String): Response<ResponseBody>
}
