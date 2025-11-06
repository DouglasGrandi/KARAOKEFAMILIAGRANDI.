package com.familiagrandi.karaoke

data class DriveVideo(
    val id: String,
    val name: String,
    val mimeType: String
) {
    fun streamUrl(apiKey: String): String =
        "https://www.googleapis.com/drive/v3/files/$id?alt=media&key=$apiKey"
}