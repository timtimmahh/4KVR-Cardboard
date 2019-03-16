package com.xojot.vrplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.xojot.vrplayer.InputUrlDialogFragment.OnPositiveButtonClickListener;
import com.xojot.vrplayer.media.MediaFormatDialogFragment;
import com.xojot.vrplayer.media.MediaFormatManager;
import com.xojot.vrplayer.media.ProjectionType;
import com.xojot.vrplayer.media.StereoType;

import java.lang.ref.WeakReference;
import java.net.URL;

@SuppressWarnings("unused")
public class MainActivity extends AppCompatActivity implements OnSystemUiVisibilityChangeListener, NavigationView.OnNavigationItemSelectedListener, OnSeekBarChangeListener, OnGestureListener, OnDoubleTapListener, OnPositiveButtonClickListener, OnTouchListener {
    private static final long CONTROL_HIDE_INIT_VALUE = 90;
    private static final long FAST_FORWARD_MS = 500;
    private static final int INTENT_OPEN_MEDIA = 2;
    private static final int INTENT_SETTING = 3;
    private static final String LOG_TAG = "VrPlayer MainActivity";
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 1;
    private static final double TABLET_INCH = 6.5d;
    private static final int UI_FPS = 30;
    private static final long UI_THREAD_INTERVAL_MS = 33;
    private boolean askFormat;
    private ImageButton cardBoardButton;
    private long controlHideCounter;
    private Uri currentUri;
    private boolean doCameraReset;
    private boolean doFastForward;
    private DrawerLayout drawerLayout;
    private ImageButton focusButton;
    private GestureDetector gestureDetector;
    private ImageButton indicatorButton;
    private boolean isFastForward;
    private boolean isFastRewind;
    private boolean isUiVisible;
    private MediaFormatManager mediaManager;
    private int pausePosition;
    private ImageButton playButton;
    private int prevProgress;
    private Uri prevUri;
    private boolean prevVideoPlaying;
    private boolean reqMediaFormatDialog;
    private LinearLayout seekLayout;
    private SeekBar seekbar;
    private ImageButton sensorMotionButton;
    private SharedPreferences sharedPreferences;
    private boolean showDebugInfo;
    private boolean showIndicator;
    private ImageButton sphericalButton;
    private boolean stopAutoPanTouch;
    private Toolbar toolbar;
//    private Tracker tracker;
    private ImageButton turnButton;
    private Handler uiHandler;
    private UiThread uiThread;
    private VrView vrView;

    public MainActivity() {}

    private class UiThread extends Thread {
        boolean running;

        private UiThread() {
            this.running = true;
        }

        /* synthetic *//* UiThread(MainActivity mainActivity, AnonymousClass1 anonymousClass1) {
            this();
        }*/

        public void run() {
            while (this.running) {
                MainActivity.this.uiHandler.sendEmptyMessage(0);
                try {
                    Thread.sleep(MainActivity.UI_THREAD_INTERVAL_MS);
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

    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return false;
    }

    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    public void onShowPress(MotionEvent motionEvent) {
    }

    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    /* Access modifiers changed, original: protected */
    @SuppressLint("ClickableViewAccessibility")
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
//        this.tracker = GoogleAnalytics.getInstance(this).newTracker(R.xml.app_tracker);
        PreferenceManager.getDefaultSharedPreferences(this);
        getWindow().addFlags(1536);
        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(this.toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayShowTitleEnabled(false);
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        this.drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, this.drawerLayout, this.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        this.drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        ((NavigationView) findViewById(R.id.nav_view)).setNavigationItemSelectedListener(this);
        this.gestureDetector = new GestureDetector(this, this);
        this.vrView = findViewById(R.id.vr_view);
        this.vrView.setOnTouchListener(this);
        this.seekLayout = findViewById(R.id.seek_layout);
        this.seekbar = findViewById(R.id.seek_bar);
        this.seekbar.setOnSeekBarChangeListener(this);
        this.indicatorButton = findViewById(R.id.indicator_button);
        this.indicatorButton.setVisibility(View.GONE);
        this.playButton = findViewById(R.id.play_button);
        this.playButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (MainActivity.this.vrView.isPlaying()) {
                    MainActivity.this.vrView.pause();
                } else {
                    MainActivity.this.vrView.start();
                }
            }
        });
        this.turnButton = findViewById(R.id.turn_button);
        this.turnButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                MainActivity.this.vrView.setAutoPanning(!MainActivity.this.vrView.isAutoPanning());
            }
        });
        this.focusButton = findViewById(R.id.focus_button);
        this.focusButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                MainActivity.this.vrView.setCameraReset(true);
            }
        });
        this.sphericalButton = findViewById(R.id.spherical_button);
        this.sphericalButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                MainActivity.this.vrView.setSpherical(!MainActivity.this.vrView.isSpherical());
            }
        });
        this.cardBoardButton = findViewById(R.id.card_board_button);
        this.cardBoardButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                boolean isHmd = !MainActivity.this.vrView.isHmd();
                MainActivity.this.vrView.setSensorMotion(isHmd);
                MainActivity.this.vrView.setHmd(isHmd);
                if (isHmd) {
                    int rotation = ((WindowManager) MainActivity.this.getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRotation();
                    if (rotation == 0 || rotation == 1) {
                        MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        return;
                    } else if (rotation == 2 || rotation == 3) {
                        MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                        return;
                    } else {
                        return;
                    }
                }
                MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            }
        });
        this.sensorMotionButton = findViewById(R.id.sensor_motion);
        this.sensorMotionButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                boolean z;
                if (MainActivity.this.vrView.isHmd()) {
                    MainActivity.this.vrView.setHmd(false);
                    z = true;
                } else {
                    z = !MainActivity.this.vrView.isSensorMotion();
                }
                MainActivity.this.vrView.setSensorMotion(z);
                if (z) {
                    int rotation = ((WindowManager) MainActivity.this.getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRotation();
                    if (rotation == 0) {
                        MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        return;
                    } else if (rotation == 1) {
                        MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        return;
                    } else if (rotation == 2) {
                        MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                        return;
                    } else if (rotation == 3) {
                        MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                        return;
                    } else {
                        return;
                    }
                }
                MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            }
        });
        ImageButton mediaFormatButton = findViewById(R.id.media_format);
        mediaFormatButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                MainActivity.this.showMediaFormatDialog();
            }
        });
        this.mediaManager = new MediaFormatManager();
        this.uiHandler = new UiHandler(this);
        this.currentUri = getIntent().getData();
        if (storagePermissionGranted()) {
            setContent(this.currentUri);
        } else {
            requireStoragePermission();
        }
    }

    private void showMediaFormatDialog() {
        MediaFormatDialogFragment mediaFormatDialogFragment = new MediaFormatDialogFragment(mediaManager);
        mediaFormatDialogFragment.show(getSupportFragmentManager(), "dialog");
    }

    private void checkButtonState() {
        if (this.isUiVisible) {
            this.indicatorButton.setVisibility(View.GONE);
            if (this.vrView.isCameraReset()) {
                this.focusButton.setImageResource(R.drawable.ic_center_focus_strong_white_24dp);
            } else {
                this.focusButton.setImageResource(R.drawable.ic_center_focus_weak_white_24dp);
            }
            if (!(!this.vrView.isContentLoaded() || this.isFastForward || this.isFastRewind)) {
                if (this.vrView.isPlaying()) {
                    this.playButton.setImageResource(R.drawable.ic_pause_white_24dp);
                } else {
                    this.playButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                }
            }
            if (this.vrView.isAutoPanning()) {
                this.turnButton.setImageResource(R.drawable.ic_turn_stop_white_24dp);
            } else {
                this.turnButton.setImageResource(R.drawable.ic_turn_play_white_24dp);
            }
        } else if (this.showIndicator) {
            this.indicatorButton.setVisibility(View.VISIBLE);
            if (this.doFastForward) {
                this.indicatorButton.setImageResource(R.drawable.ic_fast_forward_white_24dp);
            } else if (this.doCameraReset) {
                this.indicatorButton.setImageResource(R.drawable.ic_center_focus_weak_white_24dp);
            } else {
                this.indicatorButton.setImageResource(R.drawable.ic_center_focus_strong_white_24dp);
                this.indicatorButton.setVisibility(View.GONE);
                this.showIndicator = false;
            }
        } else {
            this.indicatorButton.setVisibility(View.GONE);
        }
        if (this.vrView.isSpherical()) {
            this.sphericalButton.setImageResource(R.drawable.ic_perspective_white_24dp);
        } else {
            this.sphericalButton.setImageResource(R.drawable.ic_sphere_white_24dp);
        }
        if (this.vrView.isHmd()) {
            this.cardBoardButton.setImageResource(R.drawable.ic_cellphone_white_24dp);
            this.sensorMotionButton.setVisibility(View.GONE);
            this.sphericalButton.setVisibility(View.GONE);
            findViewById(R.id.eye_separator).setVisibility(View.VISIBLE);
        } else {
            this.cardBoardButton.setImageResource(R.drawable.ic_cardboard_white_24dp);
            if (this.vrView.isSensorAvailable()) {
                this.sensorMotionButton.setVisibility(View.VISIBLE);
            }
            this.sphericalButton.setVisibility(View.VISIBLE);
            findViewById(R.id.eye_separator).setVisibility(View.GONE);
        }
        if (this.vrView.isSensorMotion()) {
            this.sensorMotionButton.setImageResource(R.drawable.ic_touch_app_white_24dp);
        } else {
            this.sensorMotionButton.setImageResource(R.drawable.ic_explore_white_24dp);
        }
    }

    private void debugInfoSetText() {
        TextView textView = findViewById(R.id.debug_info);
        if (this.showDebugInfo) {
            String stringBuilder = "\nfps: " +
                    this.vrView.getFps() +
                    "\n\nrotX: " +
                    Math.toDegrees((double) this.vrView.getRotX()) +
                    "\nrotY: " +
                    Math.toDegrees((double) this.vrView.getRotY()) +
                    "\nrotZ: " +
                    Math.toDegrees((double) this.vrView.getRotZ()) +
                    "\nfovY: " +
                    Math.toDegrees((double) this.vrView.getFovY()) +
                    "\nmaxFovY: " +
                    Math.toDegrees((double) this.vrView.getMaxFovY()) +
                    "\nminFovY: " +
                    Math.toDegrees((double) this.vrView.getMinFovY()) +
                    "\neyeZ: " +
                    this.vrView.getEyeZ() +
                    "\nscaleWidth: " +
                    this.vrView.getScaleWidth() +
                    "\nscaleHeight: " +
                    this.vrView.getScaleHeight() +
                    "\nmaxTextureSize: " +
                    this.vrView.getMaxGLTextureSize() +
                    "\nsensor X: " +
                    this.vrView.getSensorRaw()[0] +
                    "\nsensor Y: " +
                    this.vrView.getSensorRaw()[1] +
                    "\nsensor Z: " +
                    this.vrView.getSensorRaw()[2] +
                    "\nmedia Width: " +
                    this.vrView.getMediaWidth() +
                    "\nmedia Height: " +
                    this.vrView.getMediaHeight() +
                    "\nmedia Aspect: " +
                    this.vrView.getMediaAspect() +
                    "\nmedia vFov: " +
                    Math.toDegrees((double) this.vrView.getMediaVFov()) +
                    "\nmedia hFov: " +
                    Math.toDegrees((double) this.vrView.getMediaHFov()) +
                    "\nmedia dFov: " +
                    Math.toDegrees((double) this.vrView.getMediaDFov());
            textView.setText(stringBuilder);
            return;
        }
        textView.setText("");
    }

    /* Access modifiers changed, original: protected */
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i2 == -1 && i == 2) {
            this.currentUri = intent.getData();
            if (storagePermissionGranted()) {
                setContent(this.currentUri);
            } else {
                requireStoragePermission();
            }
        }
    }

    private void setContent(Uri uri) {
        if (uri == null) {
            this.vrView.setDataSource(R.drawable.demo, false, true);
            this.vrView.setAutoPanning(true);
            this.vrView.setSpherical(true);
            this.vrView.setCameraReset(false);
        } else if (this.currentUri != this.prevUri) {
            this.vrView.setDataSource(uri, true);
            this.pausePosition = 0;
            this.vrView.setAutoPanning(false);
            this.vrView.setCameraReset(false);
            this.vrView.setDoFrameStart();
            this.reqMediaFormatDialog = true;
        }
        this.prevUri = this.currentUri;
    }

    private boolean storagePermissionGranted() {
        return ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") == 0;
    }

    private void requireStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 1);
    }

    public void onRequestPermissionsResult(int i, @NonNull String[] strArr, @NonNull int[] iArr) {
        if (i != 1) {
            super.onRequestPermissionsResult(i, strArr, iArr);
        } else if (iArr[0] != 0) {
            Toast.makeText(this, R.string.toast_storage_permission, Toast.LENGTH_LONG).show();
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                Uri fromParts = Uri.fromParts("package", getPackageName(), null);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent.setData(fromParts);
                startActivity(intent);
            }
            finish();
        } else {
            setContent(this.currentUri);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onResume() {
        applyPreferences();
        if (this.isUiVisible) {
            this.controlHideCounter = CONTROL_HIDE_INIT_VALUE;
        } else {
            hideControl();
        }
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);
        setFullScreen();
        this.uiThread = new UiThread();
        this.uiThread.start();
        super.onResume();
        this.vrView.onResume();
        this.vrView.seekTo(this.pausePosition);
        showControl();
        this.controlHideCounter = CONTROL_HIDE_INIT_VALUE;
        if (!this.vrView.isSensorAvailable()) {
            this.sensorMotionButton.setVisibility(View.GONE);
        }
    }

    private void applyPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("Prefs", 0);
        this.stopAutoPanTouch = sharedPreferences.getBoolean(getString(R.string.pref_stop_auto_pan_key), true);
        this.askFormat = sharedPreferences.getBoolean(getString(R.string.pref_ask_format_open_media_key), true);
        this.showDebugInfo = sharedPreferences.getBoolean(getString(R.string.pref_app_version_key), false);
    }

    private void setFullScreen() {
        View decorView = getWindow().getDecorView();
        if (VERSION.SDK_INT >= 19) {
            decorView.setSystemUiVisibility(4870);
        } else {
            decorView.setSystemUiVisibility(5);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onPause() {
        super.onPause();
        this.vrView.pause();
        this.pausePosition = this.vrView.getCurrentPosition();
        this.vrView.onPause();
        hideControl();
        this.uiThread.quit();
    }

    public void onBackPressed() {
        if (this.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void onSystemUiVisibilityChange(int i) {
        if ((i & 4) == 0) {
            setFullScreen();
        }
    }

    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.nav_open_photo) {
            return openPhotoLibrary();
        }
        if (itemId == R.id.nav_open_video) {
            return openVideoLibrary();
        }
        if (itemId == R.id.nav_open_link) {
            return openUrl();
        }
        return itemId == R.id.nav_setting && openSetting();
    }

    private boolean openMediaLibrary() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setType("image/*");
        intent.putExtra("android.intent.extra.MIME_TYPES", new String[]{"image/*", "video/*"});
        startActivityForResult(intent, 2);
        this.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean openPhotoLibrary() {
//        this.tracker.setScreenName("open_photo_library");
//        this.tracker.send(new ScreenViewBuilder().build());
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setType("image/*");
        startActivityForResult(intent, 2);
        this.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean openVideoLibrary() {
//        this.tracker.setScreenName("open_video_library");
//        this.tracker.send(new ScreenViewBuilder().build());
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setType("video/*");
        startActivityForResult(intent, 2);
        this.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean openUrl() {
        InputUrlDialogFragment inputUrlDialogFragment = new InputUrlDialogFragment();
        inputUrlDialogFragment.setOnPositiveButtonClickListener(this);
        inputUrlDialogFragment.show(getSupportFragmentManager(), "dialog");
        this.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean openSetting() {
        startActivity(new Intent(this, SettingsActivity.class));
        this.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        if (this.vrView.isVideo() && z) {
            this.vrView.seekTo(i);
            if (i < this.prevProgress) {
                this.isFastRewind = true;
                this.isFastForward = false;
            } else {
                this.isFastForward = true;
                this.isFastRewind = false;
            }
            this.prevProgress = i;
            this.controlHideCounter = CONTROL_HIDE_INIT_VALUE;
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        if (this.vrView.isVideo()) {
            this.prevVideoPlaying = this.vrView.isPlaying();
            if (this.vrView.isPlaying()) {
                this.vrView.pause();
            }
        }
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        if (this.vrView.isVideo() && this.prevVideoPlaying) {
            this.vrView.start();
        }
        this.isFastForward = false;
        this.isFastRewind = false;
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (this.isUiVisible) {
            this.controlHideCounter = CONTROL_HIDE_INIT_VALUE;
        }
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            if (!this.isUiVisible && this.stopAutoPanTouch) {
                this.vrView.setAutoPanning(false);
            }
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if (this.doFastForward && this.prevVideoPlaying) {
                this.vrView.start();
            }
            this.doFastForward = false;
            this.isFastForward = false;
        }
        this.gestureDetector.onTouchEvent(motionEvent);
        this.vrView.cameraPanning(motionEvent);
        return true;
    }

    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        toggleShowControl();
        return true;
    }

    public boolean onDoubleTap(MotionEvent motionEvent) {
        this.doCameraReset = true;
        this.showIndicator = true;
        return true;
    }

    public void onLongPress(MotionEvent motionEvent) {
        if (this.vrView.isVideo()) {
            this.prevVideoPlaying = this.vrView.isPlaying();
            if (this.vrView.isPlaying()) {
                this.vrView.pause();
            }
            this.doFastForward = true;
            this.isFastForward = true;
            this.showIndicator = true;
        }
    }

    private void toggleShowControl() {
        if (this.isUiVisible) {
            hideControl();
            return;
        }
        showControl();
        this.controlHideCounter = CONTROL_HIDE_INIT_VALUE;
    }

    private void showControl() {
        if (this.vrView.isVideo()) {
            this.focusButton.setVisibility(View.VISIBLE);
            this.playButton.setVisibility(View.VISIBLE);
            this.turnButton.setVisibility(View.VISIBLE);
            this.seekLayout.setVisibility(View.VISIBLE);
            this.toolbar.setVisibility(View.VISIBLE);
        } else {
            this.focusButton.setVisibility(View.VISIBLE);
            this.playButton.setVisibility(View.GONE);
            this.turnButton.setVisibility(View.VISIBLE);
            this.seekLayout.setVisibility(View.GONE);
            this.toolbar.setVisibility(View.VISIBLE);
        }
        this.isUiVisible = true;
    }

    private void hideControl() {
        this.focusButton.setVisibility(View.GONE);
        this.playButton.setVisibility(View.GONE);
        this.turnButton.setVisibility(View.GONE);
        this.seekLayout.setVisibility(View.GONE);
        this.toolbar.setVisibility(View.INVISIBLE);
        this.isUiVisible = false;
    }

    private boolean isTablet() {
        Display defaultDisplay = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getMetrics(displayMetrics);
        double d = (double) (((float) displayMetrics.widthPixels) / displayMetrics.xdpi);
        double d2 = (double) (((float) displayMetrics.heightPixels) / displayMetrics.ydpi);
        return Math.sqrt((d * d) + (d2 * d2)) > TABLET_INCH;
    }

    public void onClicked(@NonNull URL url) {
        this.currentUri = Uri.parse(url.toString());
        setContent(this.currentUri);
    }

    public void setProjectionType(ProjectionType projectionType) {
        this.vrView.setProjectionType(projectionType);
        this.vrView.setCameraReset(true);
        this.vrView.setParameterChange(true);
    }

    public void setStereoType(StereoType stereoType) {
        this.vrView.setStereoType(stereoType);
        this.vrView.setCameraReset(true);
        this.vrView.setParameterChange(true);
    }

    public ProjectionType getProjectionType() {
        return this.vrView.getProjectionType();
    }

    public StereoType getStereoType() {
        return this.vrView.getStereoType();
    }

    public void handleMessage(Message message) {
        MainActivity.this.debugInfoSetText();
        MainActivity.this.checkButtonState();
        ProgressBar progressBar = MainActivity.this.findViewById(R.id.progressBar);
        if (MainActivity.this.isUiVisible) {
            if (MainActivity.this.controlHideCounter <= 0) {
                MainActivity.this.hideControl();
            } else {
                MainActivity.this.controlHideCounter = MainActivity.this.controlHideCounter - 1;
            }
        }
        if (MainActivity.this.vrView.isContentLoaded()) {
            progressBar.setVisibility(View.GONE);
            if (MainActivity.this.reqMediaFormatDialog) {
                if (MainActivity.this.askFormat) {
                    MainActivity.this.showMediaFormatDialog();
                }
                MainActivity.this.reqMediaFormatDialog = false;
            }
            if (MainActivity.this.doCameraReset) {
                if (MainActivity.this.vrView.isCameraReset()) {
                    MainActivity.this.doCameraReset = false;
                } else {
                    MainActivity.this.vrView.setCameraReset(true);
                }
            }
            if (MainActivity.this.vrView.isVideo() && MainActivity.this.vrView.isPrepared()) {
                MainActivity.this.seekbar.setMax(MainActivity.this.vrView.getDuration());
                MainActivity.this.seekbar.setSecondaryProgress(MainActivity.this.vrView.getBufferPosition());
                MainActivity.this.seekbar.setProgress(MainActivity.this.vrView.getCurrentPosition());
                if (MainActivity.this.doFastForward) {
                    int currentPosition = (int) (((long) MainActivity.this.vrView.getCurrentPosition()) + MainActivity.FAST_FORWARD_MS);
                    if (MainActivity.this.vrView.isLooping()) {
                        long duration = (long) MainActivity.this.vrView.getDuration();
                        long j = (long) currentPosition;
                        if (duration < j) {
                            currentPosition = (int) (j - duration);
                        }
                    }
                    MainActivity.this.vrView.seekTo(currentPosition);
                    return;
                }
                return;
            }
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
    }


    private static class UiHandler extends Handler {
        private WeakReference<MainActivity> activityWeakReference;

        UiHandler(MainActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        public void handleMessage(Message message) {
            MainActivity activity = activityWeakReference.get();
            if (activity != null)
                activity.handleMessage(message);
        }
    }
}
