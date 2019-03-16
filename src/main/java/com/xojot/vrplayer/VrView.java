package com.xojot.vrplayer;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;

import com.xojot.vrplayer.media.Media;
import com.xojot.vrplayer.media.Media.OnContentChangedListener;
import com.xojot.vrplayer.media.ProjectionType;
import com.xojot.vrplayer.media.StereoType;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

public class VrView extends GLSurfaceView implements Renderer, OnFrameAvailableListener, OnContentChangedListener {
    private static final long FPS_MEASURE_INTERVAL_NS = 1000000000;
    private static final String LOG_TAG = "VrPlayer VrView";
    private static final int MAX_TEXTURE_SIZE_LIMIT = 4096;
    private static final int RENDER_FPS = 55;
    private static final long RENDER_THREAD_INTERVAL_MS = 18;
    private Bitmap bitmap;
    private boolean contentLoaded;
    private Context context;
    private boolean doFramePause;
    private boolean doFrameStart;
    private float fps;
    private int fpsCount;
    private boolean frameAvailable;
    private boolean isParameterChange;
    private int maxGLTextureSize;
    private Media media;
    private MediaPlayer mediaPlayer;
    private long prevNanoTime;
    private RenderThread renderThread;
    private int scaleHeight;
    private int scaleWidth;
    private SurfaceTexture surfaceTexture;
    private ViewCamera viewCamera;

    private static class ContextFactory implements EGLContextFactory {
        private static int EGL_CONTEXT_CLIENT_VERSION = 12440;

        private ContextFactory() {
        }

        public EGLContext createContext(EGL10 egl10, EGLDisplay eGLDisplay, EGLConfig eGLConfig) {
            return egl10.eglCreateContext(eGLDisplay, eGLConfig, EGL10.EGL_NO_CONTEXT, new int[]{EGL_CONTEXT_CLIENT_VERSION, 2, 12344});
        }

        public void destroyContext(EGL10 egl10, EGLDisplay eGLDisplay, EGLContext eGLContext) {
            egl10.eglDestroyContext(eGLDisplay, eGLContext);
        }
    }

    private class RenderThread extends Thread {
        boolean running;

        private RenderThread() {
            this.running = true;
        }

        public void run() {
            while (this.running) {
                VrView.this.requestRender();
                try {
                    Thread.sleep(VrView.RENDER_THREAD_INTERVAL_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        void quit() {
            this.running = false;
        }
    }

    private int nearPow2(int i) {
        if (i <= 0) {
            return 0;
        }
        if (((i - 1) & i) == 0) {
            return i;
        }
        int i2 = 1;
        while (i > 0) {
            i2 <<= 1;
            i >>= 1;
        }
        return i2;
    }

    public native void jniInit(AssetManager assetManager);

    public native void jniRender(float f, float f2, float f3, float f4, float f5, boolean z);

    public native void jniSetProjectionType(int i, float f, float f2);

    public native void jniSetScreenSize(int i, int i2);

    public native void jniSetStereoType(int i);

    public native void jniSetupModel(Bitmap bitmap);

    public native int jniSetupModelExt();

    public VrView(Context context) {
        super(context);
        this.context = context;
        if (!isInEditMode()) {
            init();
        }
    }

    public VrView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.context = context;
        if (!isInEditMode()) {
            init();
        }
    }

    private void init() {
        System.loadLibrary("native-lib");
        setEGLContextClientVersion(2);
        setEGLContextFactory(new ContextFactory());
        setEGLConfigChooser(8, 8, 8, 8, 1, 1);
        setRenderer(this);
        setRenderMode(0);
        this.viewCamera = new ViewCamera(this.context);
        this.media = new Media(this.context);
        this.media.setOnContentChangedListener$jad_vr(this);
    }

    public void onResume() {
        super.onResume();
        this.viewCamera.onResume();
        this.contentLoaded = false;
        this.isParameterChange = true;
        this.renderThread = new RenderThread();
        this.renderThread.start();
    }

    public void onPause() {
        super.onPause();
        this.viewCamera.onPause();
        this.renderThread.quit();
    }

    public void onSurfaceCreated(GL10 gl10, EGLConfig eGLConfig) {
        jniInit(this.context.getAssets());
        int[] iArr = new int[1];
        GLES20.glGetIntegerv(3379, iArr, 0);
        this.maxGLTextureSize = iArr[0];
        this.contentLoaded = false;
    }

    public void onSurfaceChanged(GL10 gl10, int i, int i2) {
        jniSetScreenSize(i, i2);
        this.viewCamera.setView((float) i, (float) i2);
    }

    public void onDrawFrame(GL10 gl10) {
        if (this.contentLoaded) {
            synchronized (this) {
                if (this.media.isVideo$jad_vr() && this.media.isPrepared$jad_vr() && this.frameAvailable) {
                    this.surfaceTexture.updateTexImage();
                    this.frameAvailable = false;
                    if (this.doFramePause) {
                        this.media.pause();
                        this.doFramePause = false;
                    }
                    if (this.doFrameStart) {
                        this.media.start();
                        this.doFrameStart = false;
                    }
                }
            }
            if (this.isParameterChange) {
                jniSetProjectionType(this.media.getProjectionType().getType(), this.media.getVFov(), this.media.getAspect());
                jniSetStereoType(this.media.getStereoType().getType());
                this.isParameterChange = false;
            }
            jniRender(this.viewCamera.getRotX(), this.viewCamera.getRotY(), this.viewCamera.getRotZ(), this.viewCamera.getFovY(), this.viewCamera.getEyeZ(), this.viewCamera.isHmd());
        } else {
            loadContent();
        }
        measureFps();
    }

    private void loadContent() {
        if (!this.media.isVideo$jad_vr() || !this.media.isPrepared$jad_vr()) {
            jniSetupModel(genTexture());
            this.isParameterChange = true;
            this.contentLoaded = true;
        } else if (this.mediaPlayer != null) {
            synchronized (this) {
                this.frameAvailable = false;
            }
            this.surfaceTexture = new SurfaceTexture(jniSetupModelExt());
            this.surfaceTexture.setOnFrameAvailableListener(this);
            Surface surface = new Surface(this.surfaceTexture);
            this.mediaPlayer.setSurface(surface);
            surface.release();
            this.isParameterChange = true;
            this.contentLoaded = true;
        }
    }

    private Bitmap genTexture() {
        int min = Math.min(this.maxGLTextureSize, 4096);
        int width = this.bitmap.getWidth();
        int height = this.bitmap.getHeight();
        int i = 1;
        do {
            this.scaleWidth = nearPow2(width / i);
            this.scaleHeight = nearPow2(height / i);
            i *= 2;
        } while (this.scaleWidth > min || this.scaleHeight > min);
        if (this.scaleWidth == width && this.scaleHeight == height) {
            return this.bitmap;
        }
        return Bitmap.createScaledBitmap(this.bitmap, this.scaleWidth, this.scaleHeight, true);
    }

    private void measureFps() {
        long nanoTime = System.nanoTime();
        if (FPS_MEASURE_INTERVAL_NS < nanoTime - this.prevNanoTime) {
            this.fps = (((float) this.fpsCount) * 1.0E9f) / ((float) (nanoTime - this.prevNanoTime));
            this.fpsCount = 0;
            this.prevNanoTime = nanoTime;
            return;
        }
        this.fpsCount++;
    }

    public float getFps() {
        return this.fps;
    }

    public boolean isContentLoaded() {
        return this.contentLoaded;
    }

    public void setParameterChange(boolean z) {
        this.isParameterChange = z;
    }

    public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture) {
        this.frameAvailable = true;
    }

    public void onContentChanged(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.contentLoaded = false;
    }

    public void onContentChanged(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
        this.contentLoaded = false;
    }

    public void setDataSource(Uri uri, boolean z) {
        this.media.setDataSource(uri, z);
    }

    public void setDataSource(int i, boolean z, boolean z2) {
        this.media.setDataSource(i, z, z2);
    }

    public void seekTo(int i) {
        this.media.seekTo(i);
    }

    public boolean isPlaying() {
        return this.media.isPlaying();
    }

    public void pause() {
        this.media.pause();
    }

    public void setDoFramePause() {
        this.doFramePause = true;
    }

    public void start() {
        this.media.start();
    }

    public void setDoFrameStart() {
        this.doFrameStart = true;
    }

    public boolean isVideo() {
        return this.media.isVideo();
    }

    public int getDuration() {
        return this.media.getDuration();
    }

    public int getCurrentPosition() {
        return this.media.getCurrentPosition();
    }

    public boolean isPrepared() {
        return this.media.isPrepared();
    }

    public boolean isLooping() {
        return this.media.isLooping();
    }

    public int getBufferPosition() {
        return (int) Math.floor(((double) this.media.getDuration()) * (((double) this.media.getBufferPercentage()) / 100.0d));
    }

    public void setStereoType(StereoType stereoType) {
        this.media.setStereoType(stereoType);
    }

    public StereoType getStereoType() {
        return this.media.getStereoType();
    }

    public void setProjectionType(ProjectionType projectionType) {
        this.media.setProjectionType(projectionType);
    }

    public ProjectionType getProjectionType() {
        return this.media.getProjectionType();
    }

    public float getScaleWidth() {
        return (float) this.scaleWidth;
    }

    public float getScaleHeight() {
        return (float) this.scaleHeight;
    }

    public float getMaxGLTextureSize() {
        return (float) this.maxGLTextureSize;
    }

    public float getMediaWidth() {
        return (float) this.media.getWidth();
    }

    public float getMediaAspect() {
        return this.media.getAspect();
    }

    public float getMediaVFov() {
        return this.media.getVFov();
    }

    public float getMediaHFov() {
        return this.media.getHFov();
    }

    public float getMediaHeight() {
        return (float) this.media.getHeight();
    }

    public float getMediaDFov() {
        return this.media.getDFov();
    }

    public void cameraPanning(MotionEvent motionEvent) {
        this.viewCamera.cameraPanning(motionEvent);
    }

    public float getEyeZ() {
        return this.viewCamera.getEyeZ();
    }

    public float getMaxFovY() {
        return this.viewCamera.getMaxFovY();
    }

    public float getMinFovY() {
        return this.viewCamera.getMinFovY();
    }

    public float getFovY() {
        return this.viewCamera.getFovY();
    }

    public float getRotX() {
        return this.viewCamera.getRotX();
    }

    public float getRotY() {
        return this.viewCamera.getRotY();
    }

    public float getRotZ() {
        return this.viewCamera.getRotZ();
    }

    public float[] getSensorRaw() {
        return this.viewCamera.getSensorRaw();
    }

    public boolean isAutoPanning() {
        return this.viewCamera.isAutoPanning();
    }

    public void setAutoPanning(boolean z) {
        this.viewCamera.setAutoPanning(z);
    }

    public void setCameraReset(boolean z) {
        this.viewCamera.setCameraReset(z);
    }

    public boolean isCameraReset() {
        return this.viewCamera.isCameraReset();
    }

    public void setSensorMotion(boolean z) {
        this.viewCamera.setSensorMotion(z);
    }

    public boolean isSensorMotion() {
        return this.viewCamera.isSensorMotion();
    }

    public void setSpherical(boolean z) {
        this.viewCamera.setSpherical(z);
    }

    public boolean isSpherical() {
        return this.viewCamera.isSpherical();
    }

    public void setHmd(boolean z) {
        this.viewCamera.setHmd(z);
    }

    public boolean isHmd() {
        return this.viewCamera.isHmd();
    }

    public boolean isSensorAvailable() {
        return this.viewCamera.isSensorRegistered();
    }
}
