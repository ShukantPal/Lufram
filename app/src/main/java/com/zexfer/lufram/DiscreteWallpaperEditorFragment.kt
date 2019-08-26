package com.zexfer.lufram

import android.content.ClipData
import android.content.Context
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
import android.widget.EditText
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.zexfer.lufram.adapters.ThumbnailListAdapter
import com.zexfer.lufram.database.models.DiscreteWallpaper

/**
 * A simple editor for discrete wallpapers; it can take an input
 * instance and provides the new instance on submission.
 */
class DiscreteWallpaperEditorFragment : Fragment(), View.OnClickListener, ThumbnailListAdapter.OnCloseThumbListener {

    private var listener: OnSubmitClickListener? = null

    private lateinit var inputName: EditText
    private lateinit var inputIntervalMinutes: EditText
    private lateinit var inputRandomizeOrder: Switch

    private lateinit var rvWallpaperThumbnails: RecyclerView
    private lateinit var btnSubmit: Button
    private lateinit var fabAddWallpaper: FloatingActionButton

    private lateinit var thumbSize: Size
    private lateinit var thumbAdapter: ThumbnailListAdapter
    private lateinit var wallpaperUris: MutableList<Uri>

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnSubmitClickListener) {
            listener = context
        } else {
            throw IllegalArgumentException(
                "Context must implement " +
                        "DiscreteWallpaperEditorFragment.OnSubmitClickListener"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState === null) {
            thumbSize = DisplayMetrics().also {
                activity?.windowManager?.defaultDisplay?.getMetrics(it)
            }.let {
                Size(it.widthPixels / 3, it.heightPixels / 3)
            }

            thumbAdapter = ThumbnailListAdapter(context as Context, thumbSize, this)
            wallpaperUris = mutableListOf()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_discrete_wallpaper_editor, container, false).also {
            inputName = it.findViewById(R.id.text_name)
            inputIntervalMinutes = it.findViewById(R.id.text_interval)
            inputRandomizeOrder = it.findViewById(R.id.switch_randomize_order)
            rvWallpaperThumbnails = it.findViewById(R.id.rv_wallpaper_thumbnails)

            btnSubmit = it.findViewById(R.id.btn_submit)
            fabAddWallpaper = it.findViewById(R.id.fab_add_wp)

            btnSubmit.setOnClickListener(this)
            fabAddWallpaper.setOnClickListener(this)

            (rvWallpaperThumbnails.layoutManager as GridLayoutManager).spanCount = 3
            rvWallpaperThumbnails.adapter = thumbAdapter

            if (savedInstanceState !== null) {
                inputName.setText(savedInstanceState.getString("wp_name") ?: "")
                inputIntervalMinutes.setText(savedInstanceState.getString("wp_interval") ?: "")
                inputRandomizeOrder.isActivated = (savedInstanceState.getBoolean("wp_randomize"))
                wallpaperUris = DiscreteWallpaper.castToUriArray(
                    savedInstanceState.getParcelableArray("wp_uris") as Array
                ).toMutableList()
            } else {
                val wallpaper = arguments?.getParcelable(ARG_WALLPAPER) as DiscreteWallpaper?

                if (wallpaper === null)
                    return@also

                inputName.setText(wallpaper.name)
                inputIntervalMinutes.setText((wallpaper.interval.toFloat() / 60000).toString())
                inputRandomizeOrder.isChecked = wallpaper.randomizeOrder
                wallpaperUris = wallpaper.inputURIs.toMutableList()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("wp_name", inputName.text.toString())
        outState.putString("wp_interval", inputIntervalMinutes.text.toString())
        outState.putBoolean("wp_randomize", inputRandomizeOrder.isActivated)
        outState.putParcelableArray("wp_uris", wallpaperUris.toTypedArray())
    }

    override fun onDestroyView() {
        btnSubmit.setOnClickListener(null)
        fabAddWallpaper.setOnClickListener(null)
        super.onDestroyView()
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    override fun onClick(target: View?) {
        when (target?.id) {
            R.id.btn_submit -> {
                ViewModelProviders.of(activity as FragmentActivity)[WallpaperViewModel::class.java]
                    .targetWallpaper = DiscreteWallpaper(
                    inputName.text.toString(),
                    wallpaperUris.toTypedArray(),
                    (inputIntervalMinutes.text.toString().toDouble() * 60000).toLong(),
                    inputRandomizeOrder.isActivated
                )
                listener?.onSubmitClick()
            }
            R.id.fab_add_wp -> startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                },
                APPEND_WALLPAPERS
            )
        }
    }

    override fun onClose(position: Int, uri: Uri) {
        if (wallpaperUris[position] !== uri) {
            Log.w(
                "Lufram", "wallpaperUris was modified before a " +
                        "selected wallpaper could be deleted!"
            )
        }

        wallpaperUris.removeAt(position)
        thumbAdapter.submitList(ArrayList(wallpaperUris))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data == null) {
            return
        }

        when (requestCode) {
            APPEND_WALLPAPERS -> {
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

                wallpaperUris.addAll(uriDeltaArray)
                thumbAdapter.submitList(ArrayList(wallpaperUris))
            }
            else ->
                super.onActivityResult(requestCode, resultCode, data)
        }
    }

    interface OnSubmitClickListener {
        fun onSubmitClick()
    }

    companion object {
        @JvmStatic
        val ARG_WALLPAPER = "WallpaperToEdit"

        @JvmStatic
        val APPEND_WALLPAPERS = 1
    }
}
