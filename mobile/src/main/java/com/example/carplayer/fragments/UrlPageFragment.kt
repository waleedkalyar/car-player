package com.example.carplayer.fragments

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.carplayer.databinding.FragmentUrlPageBinding
import com.example.carplayer.dialogs.adapters.AlbumUrlsAdapter
import com.example.carplayer.dialogs.adapters.TracMediaType
import com.example.carplayer.dialogs.adapters.helper.SimpleItemTouchHelperCallback
import com.example.carplayer.shared.database.CarPlayerDatabase
import com.example.carplayer.shared.models.TrackAlbumModel
import com.example.carplayer.sheets.AddUrlBottomSheet
import com.example.carplayer.utils.detectAudioOrVideoStream
import com.example.carplayer.utils.hideLoadingDialog
import com.example.carplayer.utils.showLoadingDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class UrlPageFragment() : Fragment() {
    var _binding: FragmentUrlPageBinding? = null
    val binding get() = _binding!!

    private var onTrackSelect: ((String) -> Unit)? = null

    fun setCallback(callback: (String) -> Unit) {
        this.onTrackSelect = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUrlPageBinding.inflate(inflater, container, false)
        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadFromDatabase()
    }


    private fun loadFromDatabase() = with(binding) {
        val type: TracMediaType =
            TracMediaType.valueOf(requireArguments().getString(TYPE, TracMediaType.ALL.toString()))

        Log.d("URLPage", "loadFromDatabase: type -> $type")

        val adapter = AlbumUrlsAdapter(onPlayClick = { album, position ->
            Log.d(
                "UrlPageFragment",
                "initViewPager: update received play now -> $position, func -> $onTrackSelect"
            )
            //val index = mediaController.mediaItems.indexOfFirst { it.mediaMetadata.mediaUri.toString() == yourUrl }
            onTrackSelect?.invoke(album.streamUrl)
        }, onEditClick = { album ->
            val bottomSheet = AddUrlBottomSheet { title, url ->
                if (Patterns.WEB_URL.matcher(url).matches()) {
                    // Toast.makeText(context, "Added URL: $url", Toast.LENGTH_SHORT).show()
                    requireActivity().showLoadingDialog()
                    lifecycleScope.launch(Dispatchers.IO) {
                        requireActivity().detectAudioOrVideoStream(url) {
                            Log.d("AlbumsList", "onViewCreated: extension -> $it")
                            album.streamUrl = url
                            album.title = title

                            CarPlayerDatabase.getInstance(requireContext()).albumsDao()
                                .updateAlbum(album)

                            requireActivity().hideLoadingDialog()


                        }


//                        withContext(Dispatchers.Main) {
//                            delay(1500)
//                            loadAlbums()
//                        }
                    }
                } else {
                    Toast.makeText(context, "Please enter some valid Url", Toast.LENGTH_SHORT)
                        .show()
                }

            }
            bottomSheet.arguments = bundleOf(
                AddUrlBottomSheet.TITLE_TAG to album.title,
                AddUrlBottomSheet.URL_TAG to album.streamUrl
            )
            bottomSheet.show(parentFragmentManager, "EditUrlBottomSheet")
        })
        rvAlbums.adapter = adapter
        val callback = SimpleItemTouchHelperCallback(adapter)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(rvAlbums)
        CoroutineScope(Dispatchers.IO).launch {
            CarPlayerDatabase.getInstance(requireContext()).albumsDao().listenAllWithConditions(
                isFavourite = type == TracMediaType.FAVOURITES,
                all = type == TracMediaType.ALL
            )
                .collectLatest { albums ->
                    Log.d("AlbumsList", "loadFromDatabase: collection update received")

                    val updatedAlbums = albums.map { album ->
                        if (album.isPlaying) {
                            // Return the updated version of the playing item
                            album.copy(isPlaying = true)
                        } else {
                            album.copy(isPlaying = false)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        adapter.submitList(updatedAlbums)
                    }
                }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TYPE = "type"
        fun newInstance(
            type: TracMediaType,
        ): UrlPageFragment {
            return UrlPageFragment().apply {
                arguments = bundleOf(TYPE to type.toString())
            }
        }
    }

}