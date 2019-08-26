package com.zexfer.lufram.adapters

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koushikdutta.ion.Ion
import com.zexfer.lufram.R
import com.zexfer.lufram.adapters.ThumbnailListAdapter.ThumbnailViewHolder

class ThumbnailListAdapter(
    private val context: Context, private val thumbSize: Size,
    private val closeThumbListener: OnCloseThumbListener
) :
    ListAdapter<Uri, ThumbnailViewHolder>(DIFF_CALLBACK) {

    private val layoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        return ThumbnailViewHolder(
            layoutInflater.inflate(R.layout.layout_thumbnail, parent, false),
            this
        )
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    fun onCloseThumb(position: Int, boundUri: Uri) {
        closeThumbListener.onClose(position, boundUri)
    }

    companion object {
        @JvmStatic
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Uri>() {
            override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
                return oldItem.equals(newItem)
            }

            override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
                return oldItem.equals(newItem)
            }

        }
    }

    interface OnCloseThumbListener {
        fun onClose(position: Int, uri: Uri)
    }

    class ThumbnailViewHolder(private val rootView: View, private val thumbAdapter: ThumbnailListAdapter) :
        RecyclerView.ViewHolder(rootView),
        View.OnClickListener {

        private var imageThumbnail: ImageView = rootView.findViewById(R.id.image_thumbnail)
        private var btnClose: AppCompatImageButton = rootView.findViewById(R.id.btn_close)

        private lateinit var boundUri: Uri

        init {
            btnClose.setOnClickListener(this)
        }

        fun bindTo(uri: Uri) {
            boundUri = uri

            Ion.with(rootView.context)
                .load(uri.toString())
                .asBitmap()
                .get()
                .let {
                    imageThumbnail.setImageBitmap(
                        Bitmap.createScaledBitmap(
                            it,
                            thumbAdapter.thumbSize.width,
                            thumbAdapter.thumbSize.height,
                            true
                        )
                    )
                }
        }

        override fun onClick(view: View?) {
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return
            }

            thumbAdapter.onCloseThumb(adapterPosition, boundUri)
        }
    }
}