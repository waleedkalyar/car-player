package com.example.carplayer.dialogs.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.carplayer.databinding.ItemAlbumUrlBinding
import com.example.carplayer.shared.database.CarPlayerDatabase
import com.example.carplayer.shared.models.TrackAlbumModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlbumUrlsAdapter(val onPlayClick: (album: TrackAlbumModel, index:Int) -> Unit) :
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
            return oldItem.streamUrl == newItem.streamUrl && oldItem.isPlaying == newItem.isPlaying
        }

    }) {
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


    inner class AlbumUrlViewHolder(val binding: ItemAlbumUrlBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(album: TrackAlbumModel) = with(binding) {

            tvUrl.text = album.streamUrl

            if (album.isPlaying) {
                btnPlay.visibility = View.GONE
                animationView.visibility = View.VISIBLE
            } else {
                btnPlay.visibility = View.VISIBLE
                animationView.visibility = View.INVISIBLE
            }

            btnDelete.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    CarPlayerDatabase.getInstance(binding.root.context).albumsDao().delete(album)
                }
            }

            root.setOnClickListener {
                onPlayClick.invoke(album,bindingAdapterPosition)
            }
        }
    }
}