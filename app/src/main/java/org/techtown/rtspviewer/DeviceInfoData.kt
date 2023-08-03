package org.techtown.rtspviewer


import com.google.gson.annotations.SerializedName

data class DeviceInfoData(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: List<Data>,
    @SerializedName("header")
    val header: Header,
    @SerializedName("message")
    val message: String
) {
    data class Data(
        @SerializedName("access")
        val access: String,
        @SerializedName("dept")
        val dept: String,
        @SerializedName("display")
        val display: String,
        @SerializedName("extra1")
        val extra1: String,
        @SerializedName("extra2")
        val extra2: String,
        @SerializedName("extra3")
        val extra3: String,
        @SerializedName("group_id")
        val groupId: String,
        @SerializedName("id")
        val id: String,
        @SerializedName("ip")
        val ip: String,
        @SerializedName("mac")
        val mac: String,
        @SerializedName("manufacturer")
        val manufacturer: String,
        @SerializedName("mobile")
        val mobile: String,
        @SerializedName("model")
        val model: String,
        @SerializedName("modify_date")
        val modifyDate: String,
        @SerializedName("name")
        val name: String,
        @SerializedName("ostype")
        val ostype: String,
        @SerializedName("osversion")
        val osversion: String,
        @SerializedName("permission")
        val permission: String,
        @SerializedName("regid")
        val regid: String,
        @SerializedName("type")
        val type: String
    )

    data class Header(
        @SerializedName("requestCode")
        val requestCode: String
    )
}