package com.xojot.vrplayer.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.net.Uri
import android.os.AsyncTask
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.MediaController.MediaPlayerControl
import android.widget.RadioGroup
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import com.xojot.vrplayer.BitmapManager
import com.xojot.vrplayer.R
import kotlinx.android.synthetic.main.media_format_dialog.view.*
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class Media : MediaPlayerControl, OnPreparedListener, OnBufferingUpdateListener, OnVideoSizeChangedListener,
	BitmapManager.OnPreparedListener {
	private var aspect: Float = 0.toFloat()
	private var bitmapManager: BitmapManager? = null
	private var context: Context? = null
	private var dFov: Float = 0.toFloat()
	private var doFormatDetect: Boolean = false
	private var focal: Float = 0.toFloat()
	private var hFov: Float = 0.toFloat()
	var height: Int = 0
		private set
	internal var isPrepared: Boolean = false
	internal var isVideo: Boolean = false
		private set
	private var mediaBufferPercent: Int = 0
	private var mediaDuration: Int = 0
	private var mediaPlayer: MediaPlayer? = null
	private var mediaPosition: Int = 0
	private var onContentChangedListener: OnContentChangedListener? = null
	private var isPortrait: Boolean = false
	private var projectionType: ProjectionType? = null
		set(projectionType) {
			val f: Float = when (projectionType) {
				ProjectionType.Equirectangular -> DEFAULT_V_FOV_EQUIRECTANGULAR
				ProjectionType.Rectilinear     -> DEFAULT_V_FOV_RECTILINEAR
				else                           -> DEFAULT_V_FOV_FISH_EYE
			}
			field = projectionType
			if (this.isPortrait) {
				setVFov(f)
			} else {
				setHFov(f)
			}
		}
	private var stereoType: StereoType? = null
	private var uri: Uri? = null
	private var vFov: Float = 0.toFloat()
	var width: Int = 0
		private set

	internal var isLooping: Boolean
		get() = isVideo && this.mediaPlayer != null && this.mediaPlayer !!.isLooping
		set(z) {
			if (isVideo && this.mediaPlayer != null) {
				this.mediaPlayer !!.isLooping = z
			}
		}

	private class HttpGetMimeType(media: Media) : AsyncTask<URL, Void, Void>() {
		private val mediaWeakReference: WeakReference<Media> = WeakReference(media)
		
		/* Access modifiers changed, original: protected|varargs */
		public override fun doInBackground(vararg urlArr: URL): Void? {
			try {
				val httpURLConnection = urlArr[0].openConnection() as HttpURLConnection
				httpURLConnection.requestMethod = "GET"
				httpURLConnection.connect()
				val media = mediaWeakReference.get()
				if (media != null)
					if (httpURLConnection.responseCode == ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION) {
						val headerField = httpURLConnection.getHeaderField("Content-Type")
						if (! headerField.startsWith("video")) {
							if (! headerField.startsWith("image")) {
								Toast.makeText(media.context,
									R.string.toast_unplayable_media, Toast.LENGTH_LONG).show()
							}
						}
						media.isVideo = headerField.startsWith("video")
						media.initMedia()
					} else {
						Toast.makeText(media.context,
							R.string.toast_http_fail, Toast.LENGTH_LONG).show()
					}
				httpURLConnection.disconnect()
			} catch (e: IOException) {
				e.printStackTrace()
			}

			return null
		}
	}

	interface OnContentChangedListener {
		fun onContentChanged(bitmap: Bitmap)

		fun onContentChanged(mediaPlayer: MediaPlayer)
	}

	override fun getAudioSessionId(): Int {
		return 0
	}

	override fun onVideoSizeChanged(mediaPlayer: MediaPlayer, i: Int, i2: Int) {}

	constructor(context: Context) {
		this.context = context
		projectionType = ProjectionType.Rectilinear
		stereoType = StereoType.Mono
	}

	constructor(context: Context, i: Int, z: Boolean) {
		this.context = context
		setDataSource(i, z, true)
	}

	constructor(context: Context, uri: Uri) {
		this.context = context
		setDataSource(uri, true)
	}

	private fun setDataSource(uri: Uri, z: Boolean) {
		this.uri = uri
		this.doFormatDetect = z
		if ("http" == uri.scheme) {
			try {
				HttpGetMimeType(this).execute(URL(uri.toString()))
				return
			} catch (e: MalformedURLException) {
				e.printStackTrace()
				return
			}

		}
		this.isVideo = getMimeType(uri) !!.startsWith("video")
		initMedia()
	}

	private fun setDataSource(i: Int, z: Boolean, z2: Boolean) {
		this.uri = getUriFromResId(i)
		this.isVideo = z
		this.doFormatDetect = z2
		initMedia()
	}

	private fun getUriFromResId(i: Int): Uri {
		val stringBuilder = "android.resource://" +
				this.context !!.packageName +
				"/" +
				i
		return Uri.parse(stringBuilder)
	}

	private fun initMedia() {
		this.isPrepared = false
		if (this.isVideo) {
			if (this.mediaPlayer != null) {
				this.mediaPlayer !!.stop()
				this.mediaPlayer !!.release()
				this.mediaPlayer = null
			}
			this.mediaPlayer = MediaPlayer()
			this.mediaPlayer !!.setOnPreparedListener(this)
			this.mediaPlayer !!.setOnBufferingUpdateListener(this)
			this.mediaPlayer !!.setOnVideoSizeChangedListener(this)
			try {
				this.mediaPlayer !!.setDataSource(this.context !!, this.uri !!)
			} catch (e: IOException) {
				e.printStackTrace()
			}

			this.mediaPlayer !!.prepareAsync()
			return
		}
		if (this.bitmapManager != null) {
			this.bitmapManager !!.release()
			this.bitmapManager = null
		}
		this.bitmapManager = BitmapManager()
		this.bitmapManager !!.setOnPreparedListener(this)
		this.bitmapManager !!.setDataSource(this.context !!, this.uri !!)
	}

	private fun formatDetect() {
		if (Math.floor((this.aspect * 10.0f).toDouble()).toFloat() / 10.0f == 2.0f || this.aspect == 1.0f || this.aspect == 4.0f) {
			projectionType = ProjectionType.Equirectangular
		} else {
			projectionType = ProjectionType.Rectilinear
		}
		if (this.aspect == 2.0f || this.aspect == 1.5f || this.aspect == 0.6666667f || this.aspect == 1.3333334f || this.aspect == 0.75f || this.aspect == 1.7777778f || this.aspect == 0.5625f) {
			stereoType = StereoType.Mono
		} else if (this.aspect == 4.0f || this.aspect > 1.7777778f) {
			stereoType = StereoType.SideBySide
		} else if (this.aspect == 1.0f || this.aspect < 0.5625f) {
			stereoType = StereoType.OverUnder
		} else {
			stereoType = StereoType.Mono
		}
	}

	private fun getMimeType(uri: Uri): String? {
		return if ("content" == uri.scheme) {
			this.context !!.contentResolver.getType(uri)
		} else MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()).toLowerCase())
	}

	internal fun setOnContentChangedListener(onContentChangedListener: OnContentChangedListener) {
		this.onContentChangedListener = onContentChangedListener
	}

	internal fun getVFov(): Float {
		return this.vFov
	}

	private fun setVFov(f: Float) {
		this.vFov = f
		val d = this.width.toDouble() * 0.5
		val d2 = this.height.toDouble() * 0.5
		val sqrt = Math.sqrt(d * d + d2 * d2)
		this.focal = (d2 / Math.tan(f.toDouble() * 0.5).toFloat().toDouble()).toFloat()
		this.dFov = Math.atan2(sqrt, this.focal.toDouble()).toFloat() * 2.0f
		this.hFov = Math.atan2(d, this.focal.toDouble()).toFloat() * 2.0f
	}

	internal fun getHFov(): Float {
		return this.hFov
	}

	private fun setHFov(f: Float) {
		this.hFov = f
		val d = this.width.toDouble() * 0.5
		val d2 = this.height.toDouble() * 0.5
		val sqrt = Math.sqrt(d * d + d2 * d2).toFloat().toDouble()
		this.focal = (d / Math.tan(f.toDouble() * 0.5).toFloat().toDouble()).toFloat()
		this.dFov = Math.atan2(sqrt, this.focal.toDouble()).toFloat() * 2.0f
		this.vFov = Math.atan2(d2, this.focal.toDouble()).toFloat() * 2.0f
	}

	internal fun getDFov(): Float {
		return this.dFov
	}

	fun setDFov(f: Float) {
		this.dFov = f
		val d = this.width.toDouble() * 0.5
		val d2 = this.height.toDouble() * 0.5
		this.focal = (Math.sqrt(d * d + d2 * d2) / Math.tan(f.toDouble() * 0.5).toFloat().toDouble()).toFloat()
		this.hFov = Math.atan2(d, this.focal.toDouble()).toFloat() * 2.0f
		this.vFov = Math.atan2(d2, this.focal.toDouble()).toFloat() * 2.0f
	}

	override fun onPrepared(mediaPlayer: MediaPlayer) {
		this.mediaDuration = mediaPlayer.duration
		this.width = mediaPlayer.videoWidth
		this.height = mediaPlayer.videoHeight
		this.aspect = this.width.toFloat() / this.height.toFloat()
		this.isPortrait = this.width < this.height
		if (this.doFormatDetect) {
			formatDetect()
		}
		mediaPlayer.seekTo(this.mediaPosition)
		mediaPlayer.isLooping = true
		mediaPlayer.start()
		if (this.onContentChangedListener != null) {
			this.onContentChangedListener !!.onContentChanged(mediaPlayer)
		}
		this.isPrepared = true
	}

	override fun onPrepared(bitmap: Bitmap?) {
		this.width = bitmap !!.width
		this.height = bitmap.height
		this.aspect = this.width.toFloat() / this.height.toFloat()
		this.isPortrait = this.width < this.height
		if (this.doFormatDetect) {
			formatDetect()
		}
		if (this.onContentChangedListener != null) {
			this.onContentChangedListener !!.onContentChanged(bitmap)
		}
		this.isPrepared = true
	}

	override fun onBufferingUpdate(mediaPlayer: MediaPlayer, i: Int) {
		this.mediaBufferPercent = i
	}

	override fun start() {
		if (isVideo && this.mediaPlayer != null) {
			this.mediaPlayer !!.start()
		}
	}

	override fun pause() {
		if (isVideo && this.mediaPlayer != null) {
			this.mediaPlayer !!.pause()
		}
	}

	override fun getDuration(): Int {
		return if (! isVideo || this.mediaPlayer == null) 0 else this.mediaDuration
	}

	override fun getCurrentPosition(): Int {
		return if (! isVideo || this.mediaPlayer == null) 0 else this.mediaPlayer !!.currentPosition
	}

	override fun seekTo(i: Int) {
		if (isVideo && this.mediaPlayer != null) {
			this.mediaPosition = i
			this.mediaPlayer !!.seekTo(this.mediaPosition)
		}
	}

	override fun isPlaying(): Boolean {
		return isVideo && this.mediaPlayer != null && this.mediaPlayer !!.isPlaying
	}

	override fun getBufferPercentage(): Int {
		return if (! isVideo || this.mediaPlayer == null) 0 else this.mediaBufferPercent
	}

	override fun canPause(): Boolean {
		return isVideo && this.mediaPlayer != null
	}

	override fun canSeekBackward(): Boolean {
		return isVideo && this.mediaPlayer != null
	}

	override fun canSeekForward(): Boolean {
		return isVideo && this.mediaPlayer != null
	}

	fun stop() {
		if (isVideo && this.mediaPlayer != null) {
			this.mediaPlayer !!.stop()
		}
	}

	fun reset() {
		if (isVideo && this.mediaPlayer != null) {
			this.mediaPlayer !!.reset()
		}
	}

	companion object {
		private val DEFAULT_V_FOV_EQUIRECTANGULAR = Math.toRadians(0.0).toFloat()
		private val DEFAULT_V_FOV_FISH_EYE = Math.toRadians(130.0).toFloat()
		private val DEFAULT_V_FOV_RECTILINEAR = Math.toRadians(60.0).toFloat()
	}
}


sealed class ProjectionType(val type: Int) {
	object Equirectangular : ProjectionType(R.id.radio_equirectangular)
	object Rectilinear : ProjectionType(R.id.radio_rectilinear)
	object Equidistant : ProjectionType(R.id.radio_equidistant)
	object Stereographic : ProjectionType(R.id.radio_stereographic)
	object Orthographic : ProjectionType(R.id.radio_orthographic)
	object Equisolid : ProjectionType(R.id.radio_equisolid)
}

sealed class StereoType(val type: Int, var checked: Boolean) {
	object Mono : StereoType(R.id.radio_mono, false)
	object SideBySide : StereoType(R.id.radio_side_by_side, false)
	object OverUnder : StereoType(R.id.radio_over_under, false)
	
}


fun StereoType.check() {
	this.checked = !this.checked
}


class MediaFormatManager(var stereoType: StereoType = StereoType.Mono,
                         var projectionType: ProjectionType = ProjectionType.Equirectangular) :
	View.OnClickListener, RadioGroup.OnCheckedChangeListener {
	
	
	override fun onClick(v: View?) {
		if (stereoType !is StereoType.Mono)
			stereoType.check()
	}
	
	override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
		if (group?.id === R.id.radio_group_projection)
			onProjectionCheckedChange(group!!, checkedId)
		else if (group?.id === R.id.radio_group_stereo)
			onStereoCheckedChange(group!!, checkedId)
	}
	
	private fun onProjectionCheckedChange(group: RadioGroup, checkedId: Int) {
		projectionType = when (checkedId) {
			R.id.radio_equirectangular -> ProjectionType.Equirectangular
			R.id.radio_equidistant     -> ProjectionType.Equidistant
			R.id.radio_stereographic   -> ProjectionType.Stereographic
			R.id.radio_orthographic    -> ProjectionType.Orthographic
			R.id.radio_equisolid       -> ProjectionType.Equisolid
			else                       -> ProjectionType.Rectilinear
		}
	}
	
	private fun onStereoCheckedChange(group: RadioGroup, checkedId: Int) {
		stereoType =
			when (checkedId) {
				R.id.radio_mono         -> StereoType.Mono
				R.id.radio_side_by_side -> StereoType.SideBySide
				R.id.radio_over_under   -> StereoType.OverUnder
				else                    -> StereoType.Mono
			}.apply { if (group.check_stereo_stretch.isChecked != checked) check() }
	}
}