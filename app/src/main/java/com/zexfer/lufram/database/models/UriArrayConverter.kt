package com.zexfer.lufram.database.models

import android.net.Uri
import androidx.room.TypeConverter

class UriArrayConverter {
    companion object {
        @TypeConverter
        @JvmStatic
        fun toString(uris: Array<Uri>): String =
            uris.fold<Uri, String>("") { acc, uri ->
                acc + "<$uri>"
            }

        @TypeConverter
        @JvmStatic
        fun fromString(foldedUris: String): Array<Uri> =
            foldedUris.replace('<', ' ', false)
                .replace('>', ' ', false)
                .trim()
                .split("  ")
                .map { str: String -> Uri.parse(str) }
                .toTypedArray()
    }
}