package com.example.carplayer.sheets

import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.carplayer.R
import com.example.carplayer.databinding.BottomSheetAddUrlBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddUrlBottomSheet(
    private val onSave: (String) -> Unit
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
            binding.etUrl.textCursorDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.cursor_color)
        }

        binding.btnSaveUrl.setOnClickListener {
            val url = binding.etUrl.text.toString().trim()
            if (url.isNotEmpty() && Patterns.WEB_URL.matcher(url).matches()) {
                onSave(url)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter a valid URL", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnClose.setOnClickListener {
            dismissAllowingStateLoss()
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
}
