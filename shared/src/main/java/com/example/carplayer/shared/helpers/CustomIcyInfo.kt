package com.example.carplayer.shared.helpers


import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.core.net.toUri

/**
 * Custom IcyInfo class for Media3 to hold StreamTitle and StreamUrl from ICY metadata.
 */
@UnstableApi
data class CustomIcyInfo(
    val title: String?,
    val url: String?
) : Metadata.Entry, Parcelable {

    // Parcelable implementation to describe the content
    override fun describeContents(): Int {
        return 0
    }

    // Write CustomIcyInfo to Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(url)
    }

    // Populate MediaMetadata
    override fun populateMediaMetadata(builder: MediaMetadata.Builder) {
        title?.let { builder.setTitle(it) }
        url?.let { builder.setArtworkUri(it.toUri()) }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<CustomIcyInfo> = object : Parcelable.Creator<CustomIcyInfo> {
            override fun createFromParcel(parcel: Parcel): CustomIcyInfo {
                val title = parcel.readString()
                val url = parcel.readString()
                return CustomIcyInfo(title, url)
            }

            override fun newArray(size: Int): Array<CustomIcyInfo?> {
                return arrayOfNulls(size)
            }
        }
    }
}

