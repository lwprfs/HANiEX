package com.yenaly.han1meviewer.XXXTube.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface XXXTubeService {
    @GET
    suspend fun get(@Url url: String): Response<ResponseBody>
}
