package com.zexfer.lufram.database.models

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "discrete_wallpaper")
data class DiscreteWallpaper(
    var name: String = "",

    @TypeConverters(UriArrayConverter::class)
    @ColumnInfo(name = "input_uris")
    var inputURIs: Array<Uri> = arrayOf(),

    var interval: Long = 3600000,

    @ColumnInfo(name = "randomize_order")
    var randomizeOrder: Boolean = false,

    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
) : Parcelable, Wallpaper {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(name)
        dest?.writeParcelableArray(inputURIs, flags)
        dest?.writeLong(interval)
        dest?.writeInt(if (randomizeOrder) 1 else 0)
        dest?.writeInt(id ?: -1)
    }

    override fun equals(other: Any?): Boolean {
        if (other === null)
            return false
        if (other.javaClass !== this.javaClass) {
            return false
        }

        other as DiscreteWallpaper

        return (other.name.equals(this.name) &&
                other.inputURIs.contentDeepEquals(this.inputURIs) &&
                other.interval == this.interval &&
                other.randomizeOrder == this.randomizeOrder &&
                other.id === this.id)
    }

    override fun hashCode(): Int {
        throw UnsupportedOperationException(
            "DiscreteWallpaper does not support hashCode() right now!"
        )
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<DiscreteWallpaper> {
            override fun createFromParcel(source: Parcel?): DiscreteWallpaper =
                DiscreteWallpaper(source?.readString() ?: "",
                    castToUriArray(source?.readParcelableArray(Uri::class.java.classLoader) ?: arrayOf()),
                    source?.readLong() ?: 0,
                    source?.readInt() == 1,
                    source?.readInt().let { if (it == -1) null else it })

            override fun newArray(count: Int): Array<DiscreteWallpaper?> =
                arrayOfNulls<DiscreteWallpaper>(count)
        }

        fun castToUriArray(uris: Array<out Parcelable>) =
            Array(uris.size) { i ->
                uris[i] as Uri
            }
    }
}