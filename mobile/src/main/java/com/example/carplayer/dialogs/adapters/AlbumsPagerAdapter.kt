package com.example.carplayer.dialogs.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.carplayer.fragments.UrlPageFragment

enum class TracMediaType {
    ALL, FAVOURITES
}


class AlbumsPagerAdapter(
    fragmentActivity: FragmentActivity,
    val onTrackSelect: (streamUrl: String) -> Unit
) : FragmentStateAdapter(fragmentActivity) {

    private val fragments = listOf(
        UrlPageFragment.newInstance(TracMediaType.ALL,).apply {
            setCallback(onTrackSelect)
        },
        UrlPageFragment.newInstance(TracMediaType.FAVOURITES).apply {
            setCallback(onTrackSelect)
        },
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}
