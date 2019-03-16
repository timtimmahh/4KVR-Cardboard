package com.xojot.vrplayer

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.input_url_dialog.*
import java.net.MalformedURLException
import java.net.URL

class InputUrlDialogFragment : DialogFragment() {
	private var onPositiveButtonClickListener: OnPositiveButtonClickListener? = null

	interface OnPositiveButtonClickListener {
		fun onClicked(url: URL)
	}

	override fun onCreateDialog(bundle: Bundle?): Dialog {
		val inflate = (activity !!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.input_url_dialog, null)
		val builder = AlertDialog.Builder(activity !!)
		builder.setTitle(R.string.open_media_url).setView(inflate).setPositiveButton(R.string.dialog_ok) { dialogInterface, i ->
			if (this@InputUrlDialogFragment.onPositiveButtonClickListener != null) {
				try {
					this@InputUrlDialogFragment.onPositiveButtonClickListener !!.onClicked(URL(urlEditText.text.toString()))
				} catch (e: MalformedURLException) {
					e.printStackTrace()
				}

			}
		}.setNegativeButton(R.string.dialog_cancel) { dialogInterface, i -> }
		return builder.create()
	}

	fun setOnPositiveButtonClickListener(onPositiveButtonClickListener: OnPositiveButtonClickListener) {
		this.onPositiveButtonClickListener = onPositiveButtonClickListener
	}
}
