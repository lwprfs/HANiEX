package com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface StrawberryService {

    @GET("./")
    suspend fun getHomePage(
        @Query("page") page: Int? = null,
        @Query("q") query: String? = null,
        @Query("field") field: String? = null,
        @Query("length") length: String? = null,
        @Query("date") date: String? = null,
        @Query("sort") sort: String? = null,
    ): Response<ResponseBody>

    @GET("file{videoId}")
    suspend fun getWatchPage(
        @Path("videoId") videoId: String,
        @Query("quality") quality: String? = null,
    ): Response<ResponseBody>

    @GET("obmen/videoes.php")
    suspend fun getPictures(
        @Query("page") page: Int? = null,
        @Query("q") query: String? = null,
    ): Response<ResponseBody>

    @GET("obmen/top.php")
    suspend fun getTags(
        @Query("page") page: Int? = null,
        @Query("q") query: String? = null,
        @Query("sort") sort: String? = null,
    ): Response<ResponseBody>

    @GET("obmen/search_viev.php")
    suspend fun getTagPage(
        @Query("text") tagName: String,
        @Query("page") page: Int? = null,
        @Query("q") query: String? = null,
        @Query("field") field: String? = null,
        @Query("length") length: String? = null,
        @Query("date") date: String? = null,
        @Query("sort") sort: String? = null,
    ): Response<ResponseBody>

    @GET("id{username}")
    suspend fun getUserProfile(
        @Path("username") username: String,
        @Query("page") page: Int? = null,
    ): Response<ResponseBody>

    @GET("news/")
    suspend fun getAnnouncements(): Response<ResponseBody>

    @FormUrlEncoded
    @POST("obmen/search_viev.php")
    suspend fun searchFiles(
        @Field("text") query: String,
        @Query("page") page: Int? = null,
    ): Response<ResponseBody>

    @GET("obmen/search.php")
    suspend fun getSearchPage(): Response<ResponseBody>

    @GET("obmen/popular.php")
    suspend fun getPopularPage(@Query("page") page: Int? = null): Response<ResponseBody>

    @GET("obmen/new.php")
    suspend fun getNewPage(@Query("page") page: Int? = null): Response<ResponseBody>

    @GET("obmen/top.php")
    suspend fun getTopPage(@Query("page") page: Int? = null): Response<ResponseBody>

    @GET("obmen/videoes.php")
    suspend fun getTapePage(@Query("page") page: Int? = null): Response<ResponseBody>

    @GET("chat/")
    suspend fun getChatPage(): Response<ResponseBody>

    @GET("user/mail.php")
    suspend fun getMailPage(): Response<ResponseBody>

    @GET("notific.php")
    suspend fun getNotificationsPage(): Response<ResponseBody>

    @GET("user/settings.php")
    suspend fun getUserSettingsPage(): Response<ResponseBody>

    @GET("register")
    suspend fun getRegistrationPage(): Response<ResponseBody>

    @GET("login.php")
    suspend fun getLoginPage(): Response<ResponseBody>

    @GET("files/load_files.php")
    suspend fun loadThumbnail(@Query("load") encodedLoad: String): Response<ResponseBody>

    @GET("files/load_files_load.php")
    suspend fun downloadFile(@Query("load") encodedLoad: String): Response<ResponseBody>

    @GET
    suspend fun getRaw(
        @Url url: String,
    ): Response<ResponseBody>
}
