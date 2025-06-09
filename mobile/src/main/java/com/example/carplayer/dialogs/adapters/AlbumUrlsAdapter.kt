package com.example.carplayer.dialogs.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.carplayer.R
import com.example.carplayer.databinding.ItemAlbumUrlBinding
import com.example.carplayer.shared.database.CarPlayerDatabase
import com.example.carplayer.shared.models.TrackAlbumModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlbumUrlsAdapter(
    val onPlayClick: (album: TrackAlbumModel, index: Int) -> Unit,
    val onEditClick: (album: TrackAlbumModel) -> Unit,
    val onOpenLinkClick: (album: TrackAlbumModel) -> Unit,
) :
    ListAdapter<TrackAlbumModel, AlbumUrlsAdapter.AlbumUrlViewHolder>(object :
        DiffUtil.ItemCallback<TrackAlbumModel>() {
        override fun areItemsTheSame(
            oldItem: TrackAlbumModel,
            newItem: TrackAlbumModel
        ): Boolean {
            return oldItem.id == newItem.id

        }

        override fun areContentsTheSame(
            oldItem: TrackAlbumModel,
            newItem: TrackAlbumModel
        ): Boolean {
            return oldItem.streamUrl == newItem.streamUrl && oldItem.isPlaying == newItem.isPlaying && oldItem.isFavourite == newItem.isFavourite
        }

    }), ItemTouchHelperAdapter {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AlbumUrlViewHolder {
        return AlbumUrlViewHolder(
            ItemAlbumUrlBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: AlbumUrlViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        val mutableList = currentList.toMutableList()
        val item = mutableList.removeAt(fromPosition)
        mutableList.add(toPosition, item)
        submitList(mutableList.toList())
        return true
    }


    inner class AlbumUrlViewHolder(val binding: ItemAlbumUrlBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(album: TrackAlbumModel) = with(binding) {

            Log.d("AlbumUrlsAdapter", "bind: -> ${album.title} is paying ->${album.isPlaying}")

            tvUrl.text = album.streamUrl

            tvTitle.text = if (album.title.isEmpty()) "Unknown" else album.title

            tvChannel.text = "Channel ${album.channelNumber}"


            if (album.isPlaying) {
                btnPlay.visibility = View.GONE
                animationView.visibility = View.VISIBLE
                pbLoading.visibility = View.GONE

            } else {
                btnPlay.visibility = View.VISIBLE
                animationView.visibility = View.INVISIBLE
                pbLoading.visibility = View.GONE
            }

            if (album.isFavourite) btnFav.setImageResource(R.drawable.ic_fav) else btnFav.setImageResource(
                R.drawable.ic_fav_outlined
            )

            btnFav.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    CarPlayerDatabase.getInstance(binding.root.context).albumsDao()
                        .updateFavourite(
                            isFavourite = if (album.isFavourite) 0 else 1,
                            streamUrl = album.streamUrl
                        )
                }
            }

            btnOptions.setOnClickListener { showEditDeletePopupMenu(it, album) }



            btnPlay.setOnClickListener {
                root.performClick()
            }
            if (album.playBoxUrl.isNullOrEmpty()) {
                btnBoxLauncher.alpha = 0.3f
            } else {
                btnBoxLauncher.alpha = 1f
            }

            btnBoxLauncher.setOnClickListener {
                if (album.playBoxUrl.isNullOrEmpty()) return@setOnClickListener
                onOpenLinkClick.invoke(album)
            }

            root.setOnClickListener {
                pbLoading.visibility = View.VISIBLE
                animationView.visibility = View.INVISIBLE
                btnPlay.visibility = View.GONE
                CoroutineScope(Dispatchers.IO).launch {
                    CarPlayerDatabase.getInstance(binding.root.context).albumsDao().resetPlaying()
                }
                onPlayClick.invoke(album, bindingAdapterPosition)
            }
        }

        private fun showEditDeletePopupMenu(anchor: View, album: TrackAlbumModel) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menuInflater.inflate(R.menu.edit_delete_menu, popup.menu)

            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_edit -> {
                        onEditClick.invoke(album)
                        true
                    }

                    R.id.menu_delete -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            CarPlayerDatabase.getInstance(binding.root.context).albumsDao()
                                .delete(album)
                        }
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }
    }


}