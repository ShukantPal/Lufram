package com.zexfer.lufram

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.zexfer.lufram.adapters.ThumbnailListAdapter
import com.zexfer.lufram.database.models.WallpaperCollection
import com.zexfer.lufram.database.models.WallpaperCollection.Companion.FORMAT_IMAGES
import com.zexfer.lufram.database.tasks.PutWallpaperTask

class WCEditorFragment :
    Fragment(),
    View.OnClickListener,
    ThumbnailListAdapter.OnCloseThumbListener {

    private var editName: TextInputEditText? = null
    private var rvThumbs: RecyclerView? = null
    private var btnAddWallpapers: FloatingActionButton? = null
    private var btnSubmit: Button? = null

    private var thumbSize: Size? = null
    private var thumbListAdapter: ThumbnailListAdapter? = null

    private var wcId: Int? = null
    private var wallpaperUris: MutableList<Uri>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        thumbSize = DisplayMetrics().also {
            activity?.windowManager?.defaultDisplay?.getMetrics(it)
        }.let {
            Size(it.widthPixels / 3, it.heightPixels / 3)
        }

        thumbListAdapter = ThumbnailListAdapter(context!!, thumbSize!!, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_wc_editor, container, false).also { root ->
            editName = root.findViewById(R.id.text_name)
            rvThumbs = root.findViewById(R.id.rv_wallpaper_thumbnails)
            btnAddWallpapers = root.findViewById(R.id.fab_add_wp)
            btnSubmit = root.findViewById(R.id.btn_submit)

            btnAddWallpapers!!.setOnClickListener(this)
            btnSubmit!!.setOnClickListener(this)

            rvThumbs!!.adapter = thumbListAdapter
            (rvThumbs!!.layoutManager as GridLayoutManager).spanCount = 3

            if (savedInstanceState !== null) {
                wcId = savedInstanceState.getInt("wc_id").let { if (it != -1) it else null }
                editName!!.setText(savedInstanceState.getString("wc_name") ?: "")
                wallpaperUris = savedInstanceState.getParcelableArrayList("wc_uris")
            } else if (arguments !== null) {
                val source = arguments!!.getParcelable<WallpaperCollection>("source")

                if (source != null) {
                    wcId = source.id
                    editName!!.setText(source.label)
                    wallpaperUris = source.sources.toMutableList()
                }
            } else {
                wallpaperUris = mutableListOf()
            }

            thumbListAdapter!!.submitList(ArrayList(wallpaperUris!!))
        }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.fab_add_wp -> {
                startActivityForResult(
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    },
                    REQUEST_APPEND_WALLPAPERS
                )
            }
            R.id.btn_submit -> {
                if (wallpaperUris!!.size > 0) {
                    val wallpaper = WallpaperCollection(
                        wallpaperUris!!.toTypedArray(),
                        FORMAT_IMAGES,
                        editName!!.text.toString(),
                        wcId
                    )

                    PutWallpaperTask().execute(wallpaper)
                }

                findNavController().navigateUp()
            }
        }
    }

    override fun onClose(position: Int, uri: Uri) {
        if (wallpaperUris!![position] !== uri) {
            Log.w(
                "Lufram", "wallpaperUris was modified before a " +
                        "selected wallpaper could be deleted!"
            )
        }

        wallpaperUris!!.removeAt(position)
        thumbListAdapter!!.submitList(ArrayList(wallpaperUris!!))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        editName = null
        rvThumbs = null
        btnAddWallpapers = null
        btnSubmit = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("wc_id", wcId ?: -1)
        outState.putString("wc_name", editName!!.text.toString())
        outState.putParcelableArrayList("wc_uris", ArrayList())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data === null) {
            return
        }

        when (requestCode) {
            REQUEST_APPEND_WALLPAPERS -> {
                val uriDeltaArray = arrayListOf<Uri>()

                if (data.dataString !== null) {
                    uriDeltaArray.add(Uri.parse(data.dataString))
                } else {
                    val clipData = data.clipData as ClipData

                    for (i in 0 until clipData.itemCount) {
                        uriDeltaArray.add(clipData.getItemAt(i).uri)
                    }
                }

                for (uri in uriDeltaArray) {
                    context?.contentResolver?.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                wallpaperUris!!.addAll(uriDeltaArray)
                thumbListAdapter!!.submitList(ArrayList(wallpaperUris!!))
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val REQUEST_APPEND_WALLPAPERS = 5001
    }
}