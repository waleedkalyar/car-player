package com.example.carplayer.dialogs

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.carplayer.R
import com.example.carplayer.databinding.DialogAlbumListBinding
import com.example.carplayer.dialogs.adapters.AlbumUrlsAdapter
import com.example.carplayer.dialogs.adapters.AlbumsPagerAdapter
import com.example.carplayer.shared.database.CarPlayerDatabase
import com.example.carplayer.shared.models.TrackAlbumModel
import com.example.carplayer.shared.services.MyMediaService
import com.example.carplayer.shared.utils.exportAlbumsToCsv
import com.example.carplayer.shared.utils.hasStoragePermission
import com.example.carplayer.shared.utils.importAlbumsFromCsv
import com.example.carplayer.sheets.AddUrlBottomSheet
import com.example.carplayer.utils.detectAudioOrVideoStream
import com.example.carplayer.utils.hideLoadingDialog
import com.example.carplayer.utils.showLoadingDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AlbumListDialogFragment : BottomSheetDialogFragment() {

    private lateinit var mediaController: MediaController
    private val albums = mutableListOf<MediaItem>()
    private lateinit var binding: DialogAlbumListBinding

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
//    }

    private lateinit var storagePermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var csvPickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAlbumListBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet =
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
        bottomSheet?.setBackgroundResource(android.R.color.transparent)
        val behavior = BottomSheetBehavior.from(bottomSheet!!)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        behavior.isDraggable = true //
        connectToMediaService()

        //  loadFromDatabase()


    }


    private fun initViewPager() {
        val adapter = AlbumsPagerAdapter(requireActivity()) { url ->
            //val index = mediaController.mediaItems.indexOfFirst { it.mediaMetadata.mediaUri.toString() == yourUrl }
            val index = mediaController.currentTimeline
                .let { timeline ->
                    (0 until timeline.windowCount).firstOrNull { i ->
                        mediaController.getMediaItemAt(i).localConfiguration?.uri.toString() == url
                    }
                }
            Log.d("AlbumListDialogFragment", "initViewPager: update received play now -> $index")
            if (index != null) {
                mediaController.seekTo(index, 0L)
            } else {
                Log.w("ExoPlayer", "URL not found in media items.")
            }

        }
        binding.pager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.text = when (position) {
                0 -> "All"
                1 -> "Videos"
                2 -> "Audios"
                else -> "Tab ${position + 1}"
            }
        }.attach()
    }

    private fun loadFromDatabase() = with(binding) {
        val adapter = AlbumUrlsAdapter(onPlayClick = { album, position ->
            Log.d(
                "AlbumsList",
                "loadFromDatabase: position clicked -> $position, items count -> ${mediaController.mediaItemCount}"
            )
            mediaController.seekTo(position, 0)
        })
        rvAlbums.adapter = adapter
        CoroutineScope(Dispatchers.IO).launch {
            CarPlayerDatabase.getInstance(requireContext()).albumsDao().listenAll()
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

    private fun connectToMediaService() {
        val sessionToken = SessionToken(
            requireContext(),
            ComponentName(requireContext(), MyMediaService::class.java)
        )
        lifecycleScope.launch {
            val mediaControllerFuture =
                MediaController.Builder(requireContext(), sessionToken).buildAsync()
            mediaControllerFuture.addListener({
                mediaController = mediaControllerFuture.get()
                //  loadAlbums()
            }, ContextCompat.getMainExecutor(requireContext()))
            // loadAlbums()
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViewPager()
        initLaunchers()
        requestStoragePermissionIfNeededAndExport()
//        albumAdapter = AlbumAdapter(albums) {
//            // handle click
//        }
//
//        binding.rvAlbums.layoutManager = LinearLayoutManager(requireContext())
//        binding.rvAlbums.adapter = albumAdapter

        binding.btnBack.setOnClickListener { dismiss() }

        binding.btnMore.setOnClickListener { it ->
            showCsvPopupMenu(it)
        }

        binding.btnAddToPlaylist.setOnClickListener {
            val bottomSheet = AddUrlBottomSheet {title, url ->
                if (Patterns.WEB_URL.matcher(url).matches()) {
                    // Toast.makeText(context, "Added URL: $url", Toast.LENGTH_SHORT).show()
                    requireActivity().showLoadingDialog()
                    lifecycleScope.launch(Dispatchers.IO) {
                        requireActivity().detectAudioOrVideoStream(url) {
                            Log.d("AlbumsList", "onViewCreated: extension -> $it")
                            var isVideo = false
                            if (it != null && it.startsWith("video")) {
                                isVideo = true
                            }

                            CarPlayerDatabase.getInstance(requireContext()).albumsDao().insertAll(
                                TrackAlbumModel(  // ✅ Must use your Room-safe entity, not TrackAlbumModel
                                    id = UUID.randomUUID().toString(),
                                    title = title,
                                    artist = "",
                                    streamUrl = url,
                                    imageUrl = "",
                                    isVideo = isVideo
                                )
                            )

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
            bottomSheet.show(parentFragmentManager, "AddUrlBottomSheet")
        }
    }

    private fun initLaunchers() {
        storagePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                Toast.makeText(requireContext(), "Now you can export your Urls saved", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Storage permission denied, please enable it for secure import export",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        csvPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    CarPlayerDatabase.getInstance(requireContext()).albumsDao().importAlbumsFromCsv(requireContext(), uri)
                } else {
                    Toast.makeText(requireContext(), "No file selected", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    fun requestStoragePermissionIfNeededAndExport() {
        if (!requireContext().hasStoragePermission()) {
            val permissionsToRequest = mutableListOf<String>().apply {
                add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

            storagePermissionLauncher.launch(permissionsToRequest)
        }
    }


    private fun showCsvPopupMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.csv_menu, popup.menu)

        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_import_csv -> {
                    importCsvFile()
                    true
                }
                R.id.menu_export_csv -> {
                    exportAlbums()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }


    fun exportAlbums() {
        val file = CarPlayerDatabase.getInstance(requireContext()).albumsDao().exportAlbumsToCsv(requireContext())
        if (file) {
            Toast.makeText(requireContext(), "Exported to downloads folder", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun importCsvFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // Accept all types, but restrict with MIME filter below
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values", "application/csv"))
        }
        csvPickerLauncher.launch(intent)

    }


    override fun onDestroy() {
        if (::mediaController.isInitialized) {
            mediaController.release()
        }
        super.onDestroy()
    }
}


