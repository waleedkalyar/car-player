package com.example.carplayer.dialogs

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import com.example.carplayer.R
import com.example.carplayer.databinding.DialogAcknowledgmentBinding

class AcknowledgmentDialogFragment(
   val onAgreeClicked: () -> Unit
) : DialogFragment() {

    private var _binding: DialogAcknowledgmentBinding? = null
    private val binding get() = _binding!!


     override fun onCreateView(
         inflater: LayoutInflater,
         container: ViewGroup?,
         savedInstanceState: Bundle?
     ): View? {
         _binding = DialogAcknowledgmentBinding.inflate(inflater, container, false)
         return binding.root
     }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawableResource(R.color.black)
    }


     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
         super.onViewCreated(view, savedInstanceState)
         initViews()
     }

     private fun initViews() = with(binding) {
         initText()
         initClickListeners()
     }

     private fun initText() = with(binding) {
     }

     private fun initClickListeners() = with(binding) {
         agreeButton.setOnClickListener {

         }

         aboutButton.setOnClickListener {
             startActivity(Intent(Intent.ACTION_VIEW,
                 "https://www.mycarplayer.com/about.html".toUri()))
         }

         termsButton.setOnClickListener {
             startActivity(Intent(Intent.ACTION_VIEW,
                 "https://www.mycarplayer.com/terms.html".toUri()))
         }

         btnProceed.setOnClickListener {
             if(agreeButton.isChecked) {
                 onAgreeClicked.invoke()
                 dismiss()
             } else {
                 Toast.makeText(requireContext(),"Please accept the terms by clicking the above checkbox first",Toast.LENGTH_SHORT).show()
             }
         }

     }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
