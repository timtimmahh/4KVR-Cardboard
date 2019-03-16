package com.xojot.vrplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.net.Uri
import android.os.AsyncTask
import androidx.recyclerview.widget.ItemTouchHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class BitmapManager(private var bitmap: Bitmap? = null, private var onPreparedListener: OnPreparedListener? = null) {
	
	private fun getHttpBitmap() = GlobalScope.launch {
	
	}

	private class HttpGetBitmap(manager: BitmapManager) : AsyncTask<URL, Void, Void>() {
		private val weakReference: WeakReference<BitmapManager> = WeakReference(manager)
		
		/* Access modifiers changed, original: protected|varargs */
		public override fun doInBackground(vararg urlArr: URL): Void? {
			try {
				val httpURLConnection = urlArr[0].openConnection() as HttpURLConnection
				httpURLConnection.requestMethod = "GET"
				httpURLConnection.connect()
				val manager = weakReference.get()
				if (httpURLConnection.responseCode == ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION && manager != null) {
					manager.getBitmapFromInputStream(httpURLConnection.inputStream)
				}
				httpURLConnection.disconnect()
			} catch (e: IOException) {
				e.printStackTrace()
			}

			return null
		}
	}

	interface OnPreparedListener {
		fun onPrepared(bitmap: Bitmap?)
	}
	
	/*constructor(context: Context, uri: Uri) {
		setDataSource(context, uri)
	}

	constructor(context: Context, i: Int) {
		val stringBuilder = "android.resource://" +
				context.packageName +
				"/" +
				i
		setDataSource(context, Uri.parse(stringBuilder))
	}*/

	fun setDataSource(context: Context, uri: Uri) {
		if ("http" == uri.scheme) {
			try {
				HttpGetBitmap(this).execute(URL(uri.toString()))
				return
			} catch (e: MalformedURLException) {
				e.printStackTrace()
				return
			}

		}
		try {
			getBitmapFromInputStream(context.contentResolver.openInputStream(uri))
		} catch (e2: FileNotFoundException) {
			e2.printStackTrace()
		}

	}

	private fun getBitmapFromInputStream(inputStream: InputStream?) {
		val options = Options()
		options.inPreferredConfig = Config.ARGB_8888
		this.bitmap = BitmapFactory.decodeStream(inputStream, null, options)
		try {
			inputStream !!.close()
		} catch (e: IOException) {
			e.printStackTrace()
		}

		if (onPreparedListener != null)
			this.onPreparedListener !!.onPrepared(this.bitmap)
	}

	fun setOnPreparedListener(onPreparedListener: OnPreparedListener) {
		this.onPreparedListener = onPreparedListener
	}

	fun release() {
		this.bitmap !!.recycle()
	}
}
