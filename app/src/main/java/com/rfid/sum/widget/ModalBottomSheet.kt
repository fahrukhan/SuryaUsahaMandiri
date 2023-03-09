package com.rfid.sum.widget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider
import com.rfid.sum.databinding.FragmentBottomSettingSheetBinding
import com.rfid.sum.utils.Tos

class ModalBottomSheet: BottomSheetDialogFragment() {
    private var _binding: FragmentBottomSettingSheetBinding? = null
    private val binding get() = _binding!!
    lateinit var sliderTouchListener: Slider.OnSliderTouchListener
    var currentPower = 1F

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBottomSettingSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

//    companion object{
//        var currentPower = 1F
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding){
            powerSlider.value = if (currentPower < 1F ) 1F else currentPower
            powerSlider.addOnSliderTouchListener(sliderTouchListener)
            "${currentPower.toInt()} dBm".also { powerDbm.text = it }
            powerSlider.addOnChangeListener { _, value, _ ->
                "${value.toInt()} dBm".also { powerDbm.text = it }
            }
        }
    }
}