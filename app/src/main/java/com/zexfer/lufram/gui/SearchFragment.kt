package com.zexfer.lufram.gui

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zexfer.lufram.R
import com.zexfer.lufram.database.LuframDatabase
import com.zexfer.lufram.database.models.WallpaperCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Fragment that allows the user to search through the locally stored
 * wallpaper collections. It uses a {@code ShowcaseFragment} internally.
 */
class SearchFragment : Fragment(),
    SearchView.OnQueryTextListener, ShowcaseFragment.ShowcaseProvider {

    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val searchResults = MutableLiveData<List<WallpaperCollection>>()
    private lateinit var searchAction: WeakReference<MenuItem>

    override val editorNavAction: Int = R.id.action_searchFragment_to_wcEditorFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_search, container, false).also {
            childFragmentManager.beginTransaction()
                .add(R.id.frame_root, ShowcaseFragment.newInstance(R.drawable.ic_not_found))
                .commitNow()

            setHasOptionsMenu(true)
            onQueryTextSubmit("")
        }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_search, menu)

        val searchAction = menu.findItem(R.id.action_search)

        (searchAction.actionView as SearchView).apply {
            setOnQueryTextListener(this@SearchFragment)
            setIconifiedByDefault(false)
            (activity!!.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .toggleSoftInput(SHOW_IMPLICIT, 0)
            requestFocus()
        }

        searchAction.expandActionView()
        this.searchAction = WeakReference(searchAction)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            (activity!!.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(activity!!.currentFocus!!.windowToken, 0)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        val queryProper = "%${query ?: ""}%"

        uiScope.launch {
            searchResults.value = LuframDatabase.instance.wcDao().search(queryProper).asList()
        }

        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return onQueryTextSubmit(newText)
    }

    override fun onShowcaseRequired(): LiveData<List<WallpaperCollection>> {
        return searchResults
    }
}
