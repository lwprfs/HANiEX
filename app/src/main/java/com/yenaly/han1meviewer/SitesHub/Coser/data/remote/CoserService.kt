package com.yenaly.han1meviewer.SitesHub.Coser.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface CoserService {
    @GET
    suspend fun get(@Url url: String): Response<ResponseBody>
}
