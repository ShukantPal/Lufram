package com.zexfer.lufram.database.models

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity
data class WallpaperCollection(
    @TypeConverters(UriArrayConverter::class)
    var sources: Array<Uri>,

    @ColumnInfo(name = "format_type")
    var formatType: Int = FORMAT_IMAGES,

    var label: String = "Untitled Collection",

    @ColumnInfo(name = "last_updater_id")
    var lastUpdaterId: Int = -1,

    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
) : Parcelable {
    val readOnly: Boolean
        get() {
            return formatType != FORMAT_IMAGES
        }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (!(other is WallpaperCollection)) {
            return false
        }

        return this.formatType == other.formatType &&
                this.sources.contentEquals(other.sources)
    }

    override fun hashCode(): Int {
        throw UnsupportedOperationException(
            "WallpaperCollection does not " +
                    "support hashCode()!"
        )
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelableArray(sources, flags)
        dest.writeInt(formatType)
        dest.writeString(label)
        dest.writeInt(lastUpdaterId)
        dest.writeInt(id ?: -1)
    }

    companion object {
        val FORMAT_IMAGES = 0
        val FORMAT_IMAGE_SEQUENCE = 1

        @JvmField
        val CREATOR = object : Parcelable.Creator<WallpaperCollection> {
            override fun createFromParcel(source: Parcel) =
                WallpaperCollection(
                    castToUriArray(
                        source.readParcelableArray(
                            WallpaperCollection::class.java.classLoader
                        ) ?: arrayOf()
                    ),
                    source.readInt(),
                    source.readString() ?: "",
                    source.readInt(),
                    source.readInt().let { if (it == -1) null else it }
                )

            override fun newArray(size: Int) =
                arrayOfNulls<WallpaperCollection>(size)
        }

        fun castToUriArray(uris: Array<out Parcelable>) =
            Array(uris.size) { i ->
                uris[i] as Uri
            }
    }
}