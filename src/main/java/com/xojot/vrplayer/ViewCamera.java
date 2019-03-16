package com.xojot.vrplayer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.WindowManager;

public class ViewCamera implements SensorEventListener {
    private static final int ANIMATION_FPS = 55;
    private static final long ANIMATION_THREAD_INTERVAL_MS = 18;
    private static final float EYE_Z_CHANGE_DECAY = 0.85f;
    private static final float EYE_Z_CHANGE_LIMIT = 0.04f;
    private static final float EYE_Z_DEFAULT = 0.0f;
    private static final float EYE_Z_DEFAULT_HMD = 0.7f;
    private static final float EYE_Z_DEFAULT_SPHERICAL = 1.0f;
    private static final float FOV_Y_DEFAULT = ((float) Math.toRadians(70.0d));
    private static final float FOV_Y_DEFAULT_HMD = ((float) Math.toRadians(60.0d));
    private static final float FOV_Y_DEFAULT_MAX = ((float) Math.toRadians(110.0d));
    private static final float FOV_Y_DEFAULT_MIN = ((float) Math.toRadians(20.0d));
    private static final float FOV_Y_HMD_MAX = ((float) Math.toRadians(120.0d));
    private static final float FOV_Y_MAX_DAMPING_DECAY = 0.98f;
    private static final float FOV_Y_MAX_DAMPING_LIMIT = (FOV_Y_DEFAULT_MAX + ((float) Math.toRadians(1.0d)));
    private static final float FOV_Y_MAX_OVER_DECAY = 0.065f;
    private static final float FOV_Y_MAX_OVER_LIMIT = ((float) Math.toRadians(160.0d));
    private static final float FOV_Y_MIN_DAMPING_DECAY = 0.96f;
    private static final float FOV_Y_MIN_DAMPING_LIMIT = (FOV_Y_DEFAULT_MIN - ((float) Math.toRadians(0.5d)));
    private static final float FOV_Y_MIN_OVER_DECAY = 0.3f;
    private static final float FOV_Y_MIN_OVER_LIMIT = ((float) Math.toRadians(10.0d));
    private static final float FOV_Y_RESET_DECAY = 0.95f;
    private static final float FOV_Y_RESET_LIMIT = ((float) Math.toRadians(3.0d));
    private static final float FOV_Y_SPHERICAL_EYE_MAX = ((float) Math.toRadians(140.0d));
    private static final float MOMENTUM_DECAY = 0.95f;
    private static final float MOMENTUM_DECAY_ADJUST = 0.025f;
    private static final float MOMENTUM_LIMIT = 1.0E-4f;
    private static final float PAN_AUTO_DEFAULT = ((float) Math.toRadians(0.14545454545454545d));
    private static final float PAN_RESET_DECAY = 0.92f;
    private static final float PAN_RESET_LIMIT = ((float) Math.toRadians(0.3d));
    private static final float PITCH_DAMPING_DECAY = 0.99f;
    private static final float PITCH_DAMPING_LIMIT = (((float) Math.toRadians(90.0d)) + ((float) Math.toRadians(0.5d)));
    private static final float PITCH_DECAY_ADJUST = 0.02f;
    private static final float PITCH_MAX = ((float) Math.toRadians(360.0d));
    private static final float PITCH_MIN = ((float) Math.toRadians(-360.0d));
    private static final float PITCH_OVER_DECAY = 0.3f;
    private static final float YAW_MAX = ((float) Math.toRadians(90.0d));
    private static final float YAW_MIN = ((float) Math.toRadians(-90.0d));
    private AnimationThread animationThread;
    private boolean autoPanning;
    private Context context;
    private float defaultEyeZ;
    private float defaultFovY;
    private boolean doEyeZResetAnimation;
    private boolean doFovDamping;
    private boolean doFovYResetAnimation;
    private boolean doMomentum;
    private boolean doPanningResetAnimation;
    private boolean doPitchDamping;
    private float eyeZ;
    private boolean fovInitialized;
    private float fovY;
    private float height;
    private boolean hmd;
    private boolean isLandscape;
    private boolean isPointerPivot;
    private float maxFovY;
    private float maxFovYDampingLimit;
    private float maxFovYOverLimit;
    private float minFovY;
    private float minFovYDampingLimit;
    private float minFovYOverLimit;
    private float[] orientation = new float[3];
    private float prevH;
    private float[] prevOrientation = new float[3];
    private float prevR;
    private float prevX;
    private float prevY;
    private float[] remapM = new float[16];
    private float[] rotM = new float[16];
    private float rotX;
    private boolean rotXReset;
    private float rotY;
    private boolean rotYReset;
    private float rotZ;
    private boolean rotZReset;
    private SensorManager sensorManager;
    private boolean sensorMotion;
    private float[] sensorRaw = new float[3];
    private boolean sensorRegistered;
    private float[] sensorXYZ = new float[3];
    private float sensorYaw;
    private boolean spherical;
    private float touchPitch;
    private float touchYaw;
    private boolean turnReverse;
    private float width;
    private float zoom;

    private class AnimationThread extends Thread {
        boolean running;

        private AnimationThread() {
            this.running = true;
        }

        public void run() {
            while (this.running) {
                ViewCamera.this.cameraAnimation();
                try {
                    Thread.sleep(ViewCamera.ANIMATION_THREAD_INTERVAL_MS);
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

    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    ViewCamera(Context context) {
        this.context = context;
    }

    void setView(float f, float f2) {
        this.width = f;
        this.height = f2;
        this.isLandscape = f > f2;
        if (this.fovInitialized && this.prevH != f2) {
            swapFovXY();
        } else if (!this.fovInitialized) {
            initFov();
        }
    }

    public void onResume() {
        Sensor defaultSensor;
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        defaultSensor = this.sensorManager.getDefaultSensor(15);
        if (defaultSensor != null) {
            this.sensorManager.registerListener(this, defaultSensor, 1);
            this.sensorRegistered = true;
        } else {
            this.sensorRegistered = false;
        }
        this.animationThread = new AnimationThread();
        this.animationThread.start();
    }

    public void onPause() {
        if (this.sensorRegistered) {
            this.sensorManager.unregisterListener(this);
            this.sensorRegistered = false;
        }
        this.animationThread.quit();
    }

    private void initFov() {
        if (this.hmd) {
            this.defaultFovY = FOV_Y_DEFAULT_HMD;
            this.maxFovY = FOV_Y_HMD_MAX;
            this.minFovY = FOV_Y_DEFAULT_MIN;
            this.maxFovYDampingLimit = FOV_Y_MAX_DAMPING_LIMIT;
            this.minFovYDampingLimit = FOV_Y_MIN_DAMPING_LIMIT;
            this.maxFovYOverLimit = FOV_Y_MAX_OVER_LIMIT;
            this.minFovYOverLimit = FOV_Y_MIN_OVER_LIMIT;
        } else if (this.width < this.height) {
            this.defaultFovY = FOV_Y_DEFAULT;
            this.maxFovY = this.spherical ? FOV_Y_SPHERICAL_EYE_MAX : FOV_Y_DEFAULT_MAX;
            this.minFovY = FOV_Y_DEFAULT_MIN;
            this.maxFovYDampingLimit = FOV_Y_MAX_DAMPING_LIMIT;
            this.minFovYDampingLimit = FOV_Y_MIN_DAMPING_LIMIT;
            this.maxFovYOverLimit = FOV_Y_MAX_OVER_LIMIT;
            this.minFovYOverLimit = FOV_Y_MIN_OVER_LIMIT;
        } else {
            this.defaultFovY = getFovXtoY(FOV_Y_DEFAULT, this.width, this.height);
            this.maxFovY = getFovXtoY(this.spherical ? FOV_Y_SPHERICAL_EYE_MAX : FOV_Y_DEFAULT_MAX, this.width, this.height);
            this.minFovY = getFovXtoY(FOV_Y_DEFAULT_MIN, this.width, this.height);
            this.maxFovYDampingLimit = getFovXtoY(FOV_Y_MAX_DAMPING_LIMIT, this.width, this.height);
            this.minFovYDampingLimit = getFovXtoY(FOV_Y_MIN_DAMPING_LIMIT, this.width, this.height);
            this.maxFovYOverLimit = getFovXtoY(FOV_Y_MAX_OVER_LIMIT, this.width, this.height);
            this.minFovYOverLimit = getFovXtoY(FOV_Y_MIN_OVER_LIMIT, this.width, this.height);
        }
        this.prevH = this.height;
        this.doFovYResetAnimation = true;
        this.fovInitialized = true;
    }

    private void swapFovXY() {
        this.fovY = getFovXtoY(this.fovY, this.prevH, this.height);
        this.defaultFovY = getFovXtoY(this.defaultFovY, this.prevH, this.height);
        this.maxFovY = getFovXtoY(this.maxFovY, this.prevH, this.height);
        this.minFovY = getFovXtoY(this.minFovY, this.prevH, this.height);
        this.maxFovYDampingLimit = getFovXtoY(this.maxFovYDampingLimit, this.prevH, this.height);
        this.minFovYDampingLimit = getFovXtoY(this.minFovYDampingLimit, this.prevH, this.height);
        this.maxFovYOverLimit = getFovXtoY(this.maxFovYOverLimit, this.prevH, this.height);
        this.minFovYOverLimit = getFovXtoY(this.minFovYOverLimit, this.prevH, this.height);
        this.prevH = this.height;
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0087  */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x007c  */
    /* JADX WARNING: Removed duplicated region for block: B:34:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x00bd  */
    public void cameraPanning(MotionEvent motionEvent) {
        float sqrt = 0.0f;
        int action;
        float f = this.width * 0.5f;
        float f2 = this.height * 0.5f;
        if (this.hmd) {
            f = this.width * 0.5f;
        }
        float f3 = -f2;
        float tan = f2 / ((float) Math.tan((double) (this.fovY * 0.5f)));

        int pointerCount = motionEvent.getPointerCount();
        if (pointerCount == 1) {
            f = motionEvent.getX() - f;
            f3 = -(motionEvent.getY() - f2);
        } else if (pointerCount >= 2) {
            int findPointerIndex = motionEvent.findPointerIndex(motionEvent.getPointerId(0));
            int findPointerIndex2 = motionEvent.findPointerIndex(motionEvent.getPointerId(1));
            float x = motionEvent.getX(findPointerIndex) - f;
            float x2 = motionEvent.getX(findPointerIndex2) - f;
            f = -(motionEvent.getY(findPointerIndex) - f2);
            x2 -= x;
            f2 = (-(motionEvent.getY(findPointerIndex2) - f2)) - f;
            sqrt = (float) Math.sqrt((double) ((x2 * x2) + (f2 * f2)));
            float f4 = (x2 * 0.5f) + x;
            f3 = f + (0.5f * f2);
            f = f4;
        }

        action = motionEvent.getAction() & 255;
        if (action == MotionEvent.ACTION_DOWN) {
            this.touchYaw = 0.0f;
            this.touchPitch = 0.0f;
            this.prevX = f;
            this.prevY = f3;
            this.doMomentum = false;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
            this.doMomentum = true;
            this.doPitchDamping = true;
            this.doFovDamping = true;
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            this.zoom = 0.0f;
            this.prevX = f;
            this.prevY = f3;
            this.prevR = sqrt;
            this.doMomentum = false;
            this.doPitchDamping = false;
            this.doFovDamping = false;
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            this.isPointerPivot = true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            return;
        }
        if (this.isPointerPivot) {
            this.prevX = f;
            this.prevY = f3;
            this.isPointerPivot = false;
            return;
        }
        float f5 = f - this.prevX;
        f2 = f3 - this.prevY;
        float sqrt2 = (float) Math.sqrt((double) (((f5 * f5) + (f2 * f2)) + (tan * tan)));
        f2 /= sqrt2;
        tan /= sqrt2;
        this.touchYaw += (float) Math.atan2((double) (f5 / sqrt2), (double) (tan / (this.eyeZ + EYE_Z_DEFAULT_SPHERICAL)));
        this.touchPitch += (float) Math.atan2((double) f2, (double) (tan / (this.eyeZ + EYE_Z_DEFAULT_SPHERICAL)));
        this.prevX = f;
        this.prevY = f3;
        if (pointerCount >= 2) {
            this.zoom = ((float) Math.atan2(((double) ((sqrt - this.prevR) / sqrt2)) * 0.5d, 1.0d)) * 2.0f;
            this.prevR = sqrt;
            return;
        }
    }

    private void eyeZResetAnimation() {
        if (this.eyeZ < this.defaultEyeZ + EYE_Z_CHANGE_LIMIT && this.eyeZ > this.defaultEyeZ - EYE_Z_CHANGE_LIMIT) {
            this.eyeZ = this.defaultEyeZ;
            this.doEyeZResetAnimation = false;
        } else if (this.eyeZ < this.defaultEyeZ) {
            if (this.eyeZ == 0.0f) {
                this.eyeZ = PITCH_DECAY_ADJUST;
            }
            this.eyeZ /= EYE_Z_CHANGE_DECAY;
        } else {
            if (this.eyeZ == 0.0f) {
                this.eyeZ = PITCH_DECAY_ADJUST;
            }
            this.eyeZ *= EYE_Z_CHANGE_DECAY;
        }
    }

    private void fovYResetAnimation() {
        if (this.fovY < this.defaultFovY + FOV_Y_RESET_LIMIT && this.fovY > this.defaultFovY - FOV_Y_RESET_LIMIT) {
            this.fovY = this.defaultFovY;
            this.doFovYResetAnimation = false;
        } else if (this.fovY < this.defaultFovY) {
            this.fovY /= 0.95f;
        } else {
            this.fovY *= 0.95f;
        }
    }

    private void panningResetAnimation() {
        this.touchYaw = 0.0f;
        this.touchPitch = 0.0f;
        if (this.rotY >= PAN_RESET_LIMIT || this.rotY <= (-PAN_RESET_LIMIT)) {
            this.rotY *= PAN_RESET_DECAY;
        } else {
            this.rotY = 0.0f;
            this.rotYReset = true;
        }
        if (this.sensorMotion) {
            this.rotXReset = true;
            this.rotZReset = true;
        } else {
            if (this.rotX >= PAN_RESET_LIMIT || this.rotX <= (-PAN_RESET_LIMIT)) {
                this.rotX *= PAN_RESET_DECAY;
            } else {
                this.rotX = 0.0f;
                this.rotXReset = true;
            }
            if (this.rotZ >= PAN_RESET_LIMIT || this.rotZ <= (-PAN_RESET_LIMIT)) {
                this.rotZ *= PAN_RESET_DECAY;
            } else {
                this.rotZ = 0.0f;
                this.rotZReset = true;
            }
        }
        if (this.rotXReset && this.rotYReset && this.rotZReset) {
            this.rotXReset = false;
            this.rotYReset = false;
            this.rotZReset = false;
            this.doPanningResetAnimation = false;
        }
    }

    private void initEyeZ() {
        if (this.hmd) {
            this.defaultEyeZ = EYE_Z_DEFAULT_HMD;
        } else if (this.spherical) {
            this.defaultEyeZ = EYE_Z_DEFAULT_SPHERICAL;
        } else {
            this.defaultEyeZ = 0.0f;
        }
        this.doEyeZResetAnimation = true;
    }

    private void cameraAnimation() {
        if (this.doEyeZResetAnimation) {
            eyeZResetAnimation();
        }
        if (this.doFovYResetAnimation) {
            fovYResetAnimation();
        }
        if (this.doPanningResetAnimation) {
            panningResetAnimation();
            return;
        }
        float adjustEyeZFovY;
        this.rotY += this.touchYaw - this.sensorYaw;
        if (!this.sensorMotion) {
            if (this.rotX > YAW_MAX || this.rotX < YAW_MIN) {
                this.touchPitch *= 0.3f - adjustEyeZFovY(0.05f);
            }
            this.rotX -= this.touchPitch;
        }
        this.sensorYaw = 0.0f;
        if (this.autoPanning) {
            if (this.turnReverse) {
                this.rotY -= PAN_AUTO_DEFAULT * (this.eyeZ + EYE_Z_DEFAULT_SPHERICAL);
            } else {
                this.rotY += PAN_AUTO_DEFAULT * (this.eyeZ + EYE_Z_DEFAULT_SPHERICAL);
            }
        }
        if (this.doMomentum) {
            this.turnReverse = this.touchYaw < 0.0f;
            adjustEyeZFovY = 0.95f - adjustEyeZFovY(MOMENTUM_DECAY_ADJUST);
            this.touchYaw *= adjustEyeZFovY;
            this.touchPitch *= adjustEyeZFovY;
            if (this.touchYaw < MOMENTUM_LIMIT && this.touchYaw > -1.0E-4f && this.touchPitch < MOMENTUM_LIMIT && this.touchPitch > -1.0E-4f) {
                this.touchYaw = 0.0f;
                this.touchPitch = 0.0f;
                this.doMomentum = false;
            }
        } else {
            this.touchYaw = 0.0f;
            this.touchPitch = 0.0f;
        }
        if (this.rotY > PITCH_MAX) {
            this.rotY -= PITCH_MAX;
        } else if (this.rotY < PITCH_MIN) {
            this.rotY -= PITCH_MIN;
        }
        if (this.doPitchDamping) {
            if (this.rotX > YAW_MAX) {
                if (this.rotX < PITCH_DAMPING_LIMIT + adjustEyeZFovY((float) Math.toRadians(4.0d))) {
                    this.rotX = YAW_MAX;
                    this.doPitchDamping = false;
                } else {
                    this.rotX *= PITCH_DAMPING_DECAY - adjustEyeZFovY(PITCH_DECAY_ADJUST);
                }
            } else if (this.rotX < YAW_MIN) {
                if (this.rotX > (-PITCH_DAMPING_LIMIT) - adjustEyeZFovY((float) Math.toRadians(4.0d))) {
                    this.rotX = YAW_MIN;
                    this.doPitchDamping = false;
                } else {
                    this.rotX *= PITCH_DAMPING_DECAY - adjustEyeZFovY(PITCH_DECAY_ADJUST);
                }
            }
        }
        adjustEyeZFovY = FOV_Y_MAX_OVER_DECAY;
        if (this.isLandscape) {
            adjustEyeZFovY = 0.105f;
        }
        if (this.fovY > this.maxFovY) {
            this.zoom *= adjustEyeZFovY;
            this.fovY -= this.zoom;
        } else if (this.fovY < this.minFovY) {
            this.zoom *= 0.3f;
            this.fovY -= this.zoom;
        } else {
            this.fovY -= this.zoom;
            this.zoom = 0.0f;
        }
        if (this.doFovDamping) {
            if (this.fovY > this.maxFovY) {
                if (this.fovY < this.maxFovYDampingLimit) {
                    this.fovY = this.maxFovY;
                    this.doFovDamping = false;
                } else {
                    this.fovY *= FOV_Y_MAX_DAMPING_DECAY;
                }
            } else if (this.fovY < this.minFovY) {
                if (this.fovY > this.minFovYDampingLimit) {
                    this.fovY = this.minFovY;
                    this.doFovDamping = false;
                } else {
                    this.fovY /= FOV_Y_MIN_DAMPING_DECAY;
                }
            }
        }
        if (this.fovY > this.maxFovYOverLimit) {
            this.fovY = this.maxFovYOverLimit;
        } else if (this.fovY < this.minFovYOverLimit) {
            this.fovY = this.minFovYOverLimit;
        }
    }

    private float adjustEyeZFovY(float f) {
        return ((this.fovY / this.defaultFovY) * (this.eyeZ + EYE_Z_DEFAULT_SPHERICAL)) * f;
    }

    float getFovY() {
        return this.fovY;
    }

    float getRotX() {
        return this.rotX;
    }

    float getRotY() {
        return this.rotY;
    }

    float getRotZ() {
        return this.rotZ;
    }

    float getEyeZ() {
        return this.eyeZ;
    }

    float getMaxFovY() {
        return this.maxFovY;
    }

    float getMinFovY() {
        return this.minFovY;
    }

    private float getFovXtoY(float f, float f2, float f3) {
        return ((float) Math.atan2((double) (f3 * 0.5f), (double) ((f2 * 0.5f) / ((float) Math.tan((double) (f * 0.5f)))))) * 2.0f;
    }

    float[] getSensorRaw() {
        return this.sensorRaw;
    }

    void setHmd(boolean z) {
        this.hmd = z;
        if (this.width < this.height) {
            this.fovInitialized = false;
        } else {
            initFov();
        }
        initEyeZ();
        this.doFovDamping = true;
    }

    boolean isHmd() {
        return this.hmd;
    }

    void setSpherical(boolean z) {
        this.spherical = z;
        if (z) {
            this.defaultEyeZ = EYE_Z_DEFAULT_SPHERICAL;
            if (this.width < this.height) {
                this.maxFovY = FOV_Y_SPHERICAL_EYE_MAX;
            } else {
                this.maxFovY = getFovXtoY(FOV_Y_SPHERICAL_EYE_MAX, this.width, this.height);
            }
        } else {
            this.defaultEyeZ = 0.0f;
            if (this.width < this.height) {
                this.maxFovY = FOV_Y_DEFAULT_MAX;
            } else {
                this.maxFovY = getFovXtoY(FOV_Y_DEFAULT_MAX, this.width, this.height);
            }
        }
        this.doEyeZResetAnimation = true;
        this.doFovDamping = true;
    }

    boolean isSpherical() {
        return this.spherical;
    }

    void setSensorMotion(boolean z) {
        this.sensorMotion = z;
        float f = PITCH_MAX * 0.5f;
        if (this.rotY > f) {
            this.rotY -= PITCH_MAX;
        } else if (this.rotY < (-f)) {
            this.rotY += PITCH_MAX;
        }
        this.doPanningResetAnimation = true;
    }

    boolean isSensorMotion() {
        return this.sensorMotion;
    }

    boolean isSensorRegistered() {
        return this.sensorRegistered;
    }

    void setAutoPanning(boolean z) {
        this.autoPanning = z;
    }

    boolean isAutoPanning() {
        return this.autoPanning;
    }

    void setCameraReset(boolean z) {
        if (z) {
            float f = PITCH_MAX * 0.5f;
            if (this.rotY > f) {
                this.rotY -= PITCH_MAX;
            } else if (this.rotY < (-f)) {
                this.rotY += PITCH_MAX;
            }
            this.doPanningResetAnimation = true;
            this.doEyeZResetAnimation = true;
            this.doFovYResetAnimation = true;
            return;
        }
        this.rotX = 0.0f;
        this.rotY = 0.0f;
        this.rotZ = 0.0f;
        if (this.spherical) {
            this.eyeZ = EYE_Z_DEFAULT_SPHERICAL;
        } else if (this.hmd) {
            this.eyeZ = EYE_Z_DEFAULT_HMD;
        } else {
            this.eyeZ = 0.0f;
        }
        this.fovY = this.defaultFovY;
    }

    boolean isCameraReset() {
        return this.fovY == this.defaultFovY && this.rotY == 0.0f
                && (this.sensorMotion || this.rotX == 0.0f && this.rotZ == 0.0f);
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        int type = sensorEvent.sensor.getType();
        if ((type == 15 || type == 11) && this.sensorMotion) {
            this.sensorRaw = sensorEvent.values;
            this.sensorXYZ[0] = sensorEvent.values[0];
            this.sensorXYZ[1] = sensorEvent.values[1];
            this.sensorXYZ[2] = sensorEvent.values[2];
            SensorManager.getRotationMatrixFromVector(this.rotM, this.sensorXYZ);
            int rotation = ((WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            if (rotation == 0) {
                SensorManager.remapCoordinateSystem(this.rotM, 1, 3, this.remapM);
            } else if (rotation == 1) {
                SensorManager.remapCoordinateSystem(this.rotM, 3, 129, this.remapM);
            } else if (rotation == 2) {
                SensorManager.remapCoordinateSystem(this.rotM, 129, 131, this.remapM);
            } else if (rotation == 3) {
                SensorManager.remapCoordinateSystem(this.rotM, 131, 1, this.remapM);
            }
            SensorManager.getOrientation(this.remapM, this.orientation);
            this.sensorYaw = this.orientation[0] - this.prevOrientation[0];
            this.rotX = -this.orientation[1];
            this.rotZ = -this.orientation[2];
            this.prevOrientation[0] = this.orientation[0];
        }
    }
}
