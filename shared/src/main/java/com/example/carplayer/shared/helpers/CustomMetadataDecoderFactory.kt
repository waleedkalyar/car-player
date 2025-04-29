package com.example.carplayer.shared.helpers

import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.metadata.MetadataDecoderFactory
import androidx.media3.extractor.metadata.MetadataDecoder


@UnstableApi
class CustomMetadataDecoderFactory : MetadataDecoderFactory {
    override fun supportsFormat(format: androidx.media3.common.Format): Boolean {
        return MimeTypes.APPLICATION_ICY.equals(format.sampleMimeType, ignoreCase = true)
    }
    override fun createDecoder(format: androidx.media3.common.Format): MetadataDecoder {
        return CustomIcyDecoder()
    }
}