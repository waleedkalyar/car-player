package com.example.carplayer.sheets

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.carplayer.R
import com.example.carplayer.databinding.BottomSheetAddUrlBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddUrlBottomSheet(
    private val onSave: (title: String, url: String, playBoxUrl: String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddUrlBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddUrlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.etUrl.textCursorDrawable =
                ContextCompat.getDrawable(requireContext(), R.drawable.cursor_color)
        }

        initOptionalArguments()

        binding.etUrl.postDelayed({
            binding.etUrl.requestFocus()
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }, 200)

        binding.btnSaveUrl.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val url = binding.etUrl.text.toString().trim()
            val playlistBoxUrl = binding.etPlaylistBoxUrl.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if (url.isEmpty() && !Patterns.WEB_URL.matcher(url).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid URL", Toast.LENGTH_SHORT)
                    .show()
            } else if (playlistBoxUrl.isEmpty() && !Patterns.WEB_URL.matcher(playlistBoxUrl)
                    .matches()
            ) {
                Toast.makeText(requireContext(), "Please enter a valid box URL", Toast.LENGTH_SHORT)
                    .show()
            } else if (url.isNotEmpty() && Patterns.WEB_URL.matcher(url).matches()) {
                onSave(title, url, playlistBoxUrl)
                dismiss()
            } else {

            }
        }
        binding.btnClose.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun initOptionalArguments() = with(binding) {
        arguments?.let { args ->
            etTitle.setText(args.getString(TITLE_TAG).toString())
            etUrl.setText(args.getString(URL_TAG).toString())
            if (args.getString(BOX_URL_TAG).toString().isNotEmpty())
                etPlaylistBoxUrl.setText(args.getString(BOX_URL_TAG).toString())
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { dialog ->
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT

            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            behavior.skipCollapsed = false
            behavior.isHideable = true
            behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    companion object {
        const val TITLE_TAG = "title"
        const val URL_TAG = "url"
        const val BOX_URL_TAG = "box-url"

    }
}
