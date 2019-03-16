package com.xojot.vrplayer.media

import android.app.Dialog
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import com.xojot.vrplayer.R
import kotlinx.android.synthetic.main.media_format_dialog.*

class MediaFormatDialogFragment(private val mediaManager: MediaFormatManager) :
	DialogFragment() {
	
	override fun onConfigurationChanged(configuration: Configuration) {
		verifyConfiguration(configuration).also { super.onConfigurationChanged(configuration) }
	}
	
	private fun verifyConfiguration(configuration: Configuration) {
		media_dialog_layout.orientation =
			if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
				LinearLayout.HORIZONTAL
			else LinearLayout.VERTICAL
	}
	
	override fun onCreateDialog(bundle: Bundle?): Dialog {
		(activity?.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater)
			.inflate(R.layout.media_format_dialog, null)
		verifyConfiguration(resources.configuration)
		
		val dialog = Dialog(activity!!).apply {
			requestWindowFeature(1)
			setContentView(media_dialog_root !!)
			window !!.apply { setBackgroundDrawable(ColorDrawable(0)); setFlags(1024, 256) }
		}
		
		radio_group_projection.check(mediaManager.projectionType.type)
		radio_group_projection.setOnCheckedChangeListener { group, i ->
			mediaManager.onCheckedChanged(group, i)
			check_stereo_stretch.visibility = mediaManager.stereoType.takeIf {
				i == R.id.radio_equirectangular && it is StereoType.Mono
			}?.let { View.INVISIBLE } ?: View.VISIBLE
		}
		check_stereo_stretch.setOnClickListener(mediaManager)
		radio_group_stereo.check(R.id.radio_mono)
		check_stereo_stretch.visibility =
			if (mediaManager.stereoType is StereoType.Mono
				|| mediaManager.projectionType is ProjectionType.Equirectangular)
				View.INVISIBLE else View.VISIBLE
		check_stereo_stretch.isChecked = mediaManager.stereoType.checked
			/*when (stereoType) {
				is StereoType.Mono           -> false
				is StereoType.SideBySide     -> false
				is StereoType.SideBySideComp -> true
				is StereoType.OverUnder      -> false
				is StereoType.OverUnderComp  -> true
			}*/
		radio_group_stereo.setOnCheckedChangeListener { group, i ->
			mediaManager.onCheckedChanged(group, i)
			check_stereo_stretch.visibility =
				if (i == R.id.radio_mono || mediaManager.projectionType === ProjectionType.Equirectangular)
					View.INVISIBLE
				else View.VISIBLE
		}
		return dialog
	}
}