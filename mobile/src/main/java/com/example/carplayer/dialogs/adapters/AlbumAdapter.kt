package com.example.carplayer.dialogs.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.carplayer.R

class AlbumAdapter(private val albums: List<MediaItem>, private val onAlbumClick: (MediaItem) -> Unit) :
    RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    inner class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAlbumArt: ImageView = itemView.findViewById(R.id.ivAlbumArt)
        val tvAlbumTitle: TextView = itemView.findViewById(R.id.tvAlbumTitle)

        init {
            itemView.setOnClickListener {
                onAlbumClick(albums[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = albums[position]
        holder.tvAlbumTitle.text = album.mediaMetadata.title
        // Load album art if available
        holder.ivAlbumArt.load(album.mediaMetadata.artworkUri) {
            crossfade(true)
            placeholder(R.drawable.default_album_art)
            error(R.drawable.default_album_art)
        }
    }

    override fun getItemCount(): Int = albums.size
}
