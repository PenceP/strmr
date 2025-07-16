package com.strmr.ai.data

import com.google.gson.annotations.SerializedName

data class HomeConfig(
    val homePage: HomePageConfig
)

data class HomePageConfig(
    val rows: List<HomeSection>
)

data class HomeSection(
    val id: String,
    val title: String,
    val networks: List<NetworkInfo>?
)

data class NetworkInfo(
    val id: String,
    val name: String,
    @SerializedName("backgroundImageURL")
    val posterUrl: String,
    @SerializedName("dataURL")
    val dataUrl: String?
) 