package com.example.carplayer.shared.helpers

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.MetadataInputBuffer
import androidx.media3.extractor.metadata.SimpleMetadataDecoder
import java.nio.ByteBuffer

@OptIn(UnstableApi::class)
class CustomIcyDecoder : SimpleMetadataDecoder() {

    override fun decode(
        inputBuffer: MetadataInputBuffer,
        buffer: ByteBuffer
    ): Metadata? {
        // Read the byte buffer into a string
        val icyMetadata = String(buffer.array(), buffer.position(), buffer.remaining(), Charsets.UTF_8)

        var title: String? = null
        var url: String? = null

        // Split the ICY metadata and extract StreamTitle and StreamUrl
        icyMetadata.split(";").forEach { field ->
            if (field.startsWith("StreamTitle='")) {
                title = field.removePrefix("StreamTitle='").removeSuffix("'")
            }
            if (field.startsWith("StreamUrl='")) {
                url = field.removePrefix("StreamUrl='").removeSuffix("'")
            }
        }

        // Create CustomIcyInfo (Metadata.Entry) based on extracted title and url
        val entries = mutableListOf<Metadata.Entry>()
        if (title != null || url != null) {
            entries.add(CustomIcyInfo(title, url))  // CustomIcyInfo implements Metadata.Entry
        }

        // Return Metadata object containing the list of entries
        return Metadata(entries)
    }
}
