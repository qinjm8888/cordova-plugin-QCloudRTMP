package com.tx.play;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import com.tencent.rtmp.ITXLivePushListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePushConfig;
import com.tencent.rtmp.TXLivePusher;
import com.tencent.rtmp.ui.TXCloudVideoView;

import com.tx.play.QinR;

public class LuPlayActivity extends Activity implements View.OnClickListener , ITXLivePushListener, SeekBar.OnSeekBarChangeListener/*, ImageReader.OnImageAvailableListener*/{
    private static final String TAG = LuPlayActivity.class.getSimpleName();

    private QinR qinR;

    private TXLivePushConfig mLivePushConfig;
    private TXLivePusher mLivePusher;
    private TXCloudVideoView mCaptureView;

    private LinearLayout mBitrateLayout;
    private LinearLayout mFaceBeautyLayout;
    private SeekBar mBeautySeekBar;
    private SeekBar mWhiteningSeekBar;
    private RadioGroup mRadioGroupBitrate;
    private Button mBtnBitrate;
    private Button mBtnPlay;
    private Button mBtnFaceBeauty;
    private Button mBtnFlashLight;
    private Button mBtnTouchFocus;
    private Button mBtnHWEncode;
    private Button mBtnOrientation;
    private Button back;
    private boolean          mPortrait = true;         //手动切换，横竖屏推流

    private boolean          mVideoPublish;
    private boolean          mFrontCamera = true;
    private boolean          mHWVideoEncode = false;
    private boolean          mFlashTurnOn = false;
    private boolean          mTouchFocus  = true;
    private boolean          mHWListConfirmDialogResult = false;
    private int              mBeautyLevel = 0;
    private int              mWhiteningLevel = 0;

    private Handler mHandler = new Handler();

    private Bitmap mBitmap;

    private Context mContext;
    private String rtmpUrl;

    // 关注系统设置项“自动旋转”的状态切换
    private RotationObserver mRotationObserver = null;

    private Bitmap decodeResource(Resources resources, int id) {
        TypedValue value = new TypedValue();
        resources.openRawResource(id, value);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inTargetDensity = value.density;
        return BitmapFactory.decodeResource(resources, id, opts);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        qinR = new QinR(this);

        int[] sdkver = TXLivePusher.getSDKVersion();
        if (sdkver != null && sdkver.length >= 3) {
            Log.d("rtmpsdk","rtmp sdk version is:" + sdkver[0] + "." + sdkver[1] + "." + sdkver[2]);
        }

        Intent intent = getIntent();
        rtmpUrl = intent.getStringExtra("luVideoUrl");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(qinR.getId("layout", "activity_publish"));//R.layout.activity_publish

        mLivePusher     = new TXLivePusher(this);
        mLivePushConfig = new TXLivePushConfig();

        mBitmap         = decodeResource(getResources(),qinR.getId("drawable", "watermark"));//R.drawable.watermark

        mRotationObserver = new RotationObserver(new Handler());
        mRotationObserver.startObserver();
        mContext = this;

        init();
    }


    public void init() {
        mCaptureView = (TXCloudVideoView) findViewById(qinR.getId("id", "video_view"));//R.id.video_view

        mVideoPublish = false;

        back = (Button) findViewById(qinR.getId("id", "back"));//R.id.back
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //美颜部分
        mFaceBeautyLayout = (LinearLayout)findViewById(qinR.getId("id", "layoutFaceBeauty"));//R.id.layoutFaceBeauty
        mBeautySeekBar = (SeekBar) findViewById(qinR.getId("id", "beauty_seekbar"));//R.id.beauty_seekbar
        mBeautySeekBar.setOnSeekBarChangeListener(this);

        mWhiteningSeekBar = (SeekBar) findViewById(qinR.getId("id", "whitening_seekbar"));//R.id.whitening_seekbar
        mWhiteningSeekBar.setOnSeekBarChangeListener(this);

        mBtnFaceBeauty = (Button)findViewById(qinR.getId("id", "btnFaceBeauty"));//R.id.btnFaceBeauty
        mBtnFaceBeauty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFaceBeautyLayout.setVisibility(mFaceBeautyLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        //播放部分
        mBtnPlay = (Button) findViewById(qinR.getId("id", "btnPlay"));//R.id.btnPlay
        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mVideoPublish) {
                    stopPublishRtmp();
                    mVideoPublish = false;
                } else {
                    FixOrAdjustBitrate();  //根据设置确定是“固定”还是“自动”码率
                    mVideoPublish = startPublishRtmp();
//                    StartScreenCapture();
                }
            }
        });


        //切换前置后置摄像头
        final Button btnChangeCam = (Button) findViewById(qinR.getId("id", "btnCameraChange"));//R.id.btnCameraChange
        btnChangeCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFrontCamera = !mFrontCamera;

                if (mLivePusher.isPushing()) {
                    mLivePusher.switchCamera();
                } else {
                    mLivePushConfig.setFrontCamera(mFrontCamera);
                }
                btnChangeCam.setBackgroundResource(mFrontCamera ? qinR.getId("drawable", "camera_change") : qinR.getId("drawable", "camera_change2"));//R.drawable.camera_change R.drawable.camera_change2
            }
        });

        //开启硬件加速
        mBtnHWEncode = (Button) findViewById(qinR.getId("id", "btnHWEncode"));//R.id.btnHWEncode
        mBtnHWEncode.getBackground().setAlpha(mHWVideoEncode ? 255 : 100);
        mBtnHWEncode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean HWVideoEncode = mHWVideoEncode;
                mHWVideoEncode = !mHWVideoEncode;
                mBtnHWEncode.getBackground().setAlpha(mHWVideoEncode ? 255 : 100);
                
                if (mHWVideoEncode){
                    if (mLivePushConfig != null) {
                        if(Build.VERSION.SDK_INT < 18){
                            Toast.makeText(mContext, "硬件加速失败，当前手机API级别过低（最低16）", Toast.LENGTH_SHORT).show();
                            mHWVideoEncode = false;
                        }
                    }
                }
                if(HWVideoEncode != mHWVideoEncode){
                    mLivePushConfig.setHardwareAcceleration(mHWVideoEncode);
                    if(mHWVideoEncode == false){
                        Toast.makeText(mContext, "取消硬件加速", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(mContext, "开启硬件加速", Toast.LENGTH_SHORT).show();
                    }
                }
                if (mLivePusher != null) {
                    mLivePusher.setConfig(mLivePushConfig);
                }
            }
        });

        //码率自适应部分
        mBtnBitrate = (Button)findViewById(qinR.getId("id", "btnBitrate"));//R.id.btnBitrate
        mBitrateLayout = (LinearLayout)findViewById(qinR.getId("id", "layoutBitrate"));//R.id.layoutBitrate
        mBtnBitrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBitrateLayout.setVisibility(mBitrateLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        mRadioGroupBitrate = (RadioGroup)findViewById(qinR.getId("id", "resolutionRadioGroup"));//R.id.resolutionRadioGroup
        mRadioGroupBitrate.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                FixOrAdjustBitrate();
                mBitrateLayout.setVisibility(View.GONE);
            }
        });

        //闪光灯
        mBtnFlashLight = (Button)findViewById(qinR.getId("id", "btnFlash"));//R.id.btnFlash
        mBtnFlashLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLivePusher == null) {
                    return;
                }

                mFlashTurnOn = !mFlashTurnOn;
                if (!mLivePusher.turnOnFlashLight(mFlashTurnOn)) {
                    Toast.makeText(mContext,
                            "打开闪光灯失败（1）大部分前置摄像头并不支持闪光灯（2）该接口需要在启动预览之后调用", Toast.LENGTH_SHORT).show();
                }

                mBtnFlashLight.setBackgroundResource(mFlashTurnOn ? qinR.getId("drawable", "flash_off") : qinR.getId("drawable", "flash_on"));//R.drawable.flash_off R.drawable.flash_on
            }
        });

        //手动对焦/自动对焦
        mBtnTouchFocus = (Button) findViewById(qinR.getId("id", "btnTouchFoucs"));//R.id.btnTouchFoucs
        mBtnTouchFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFrontCamera) {
                    return;
                }

                mTouchFocus = !mTouchFocus;
                mLivePushConfig.setTouchFocus(mTouchFocus);
                v.setBackgroundResource(mTouchFocus ? qinR.getId("drawable", "automatic") : qinR.getId("drawable", "manual"));//R.drawable.automatic R.drawable.manual

                if (mLivePusher.isPushing()) {
                    mLivePusher.stopCameraPreview(false);
                    mLivePusher.startCameraPreview(mCaptureView);
                }

                Toast.makeText(mContext, mTouchFocus ? "已开启手动对焦" : "已开启自动对焦", Toast.LENGTH_SHORT).show();
            }
        });

        //锁定Activity不旋转的情况下，才能进行横屏|竖屏推流切换
        mBtnOrientation = (Button) findViewById(qinR.getId("id", "btnPushOrientation"));//R.id.btnPushOrientation
        if (isActivityCanRotation()) {
            mBtnOrientation.setVisibility(View.GONE);
        }
        mBtnOrientation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPortrait = ! mPortrait;
                int renderRotation = 0;
                if (mPortrait) {
                    mLivePushConfig.setHomeOrientation(TXLiveConstants.VIDEO_ANGLE_HOME_DOWN);
                    mBtnOrientation.setBackgroundResource(qinR.getId("drawable", "landscape"));//R.drawable.landscape
                    renderRotation = 0;
                } else {
                    mLivePushConfig.setHomeOrientation(TXLiveConstants.VIDEO_ANGLE_HOME_RIGHT);
                    mBtnOrientation.setBackgroundResource(qinR.getId("drawable", "portrait"));//R.drawable.portrait
                    renderRotation = 270;
                }
                mLivePusher.setRenderRotation(renderRotation);
                mLivePusher.setConfig(mLivePushConfig);
            }
        });
        findViewById(qinR.getId("id", "root")).setOnClickListener(this);//R.id.root
    }
    protected void HWListConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(LuPlayActivity.this);
        builder.setMessage("警告：当前机型不在白名单中,是否继续尝试硬编码？");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mHWListConfirmDialogResult = true;
                throw new RuntimeException();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mHWListConfirmDialogResult = false;
                throw new RuntimeException();
            }
        });
        builder.setCancelable(false);
        builder.create().show();
        try {
            Looper.loop();
        }catch (Exception e) {}
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                mFaceBeautyLayout.setVisibility(View.GONE);
                mBitrateLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCaptureView != null) {
            mCaptureView.onResume();
        }

        if (mVideoPublish && mLivePusher != null) {
            mLivePusher.resumePusher();
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        if (mCaptureView != null) {
            mCaptureView.onPause();
        }

        if (mVideoPublish && mLivePusher != null) {
            mLivePusher.pausePusher();
        }

    }

	@Override
	public void onDestroy() {
		super.onDestroy();
        stopPublishRtmp();
        if (mCaptureView != null) {
            mCaptureView.onDestroy();
        }

        mRotationObserver.stopObserver();
	}

    private  boolean startPublishRtmp() {
        String rtmpUrl = this.rtmpUrl;
        if (TextUtils.isEmpty(rtmpUrl) || (!rtmpUrl.trim().toLowerCase().startsWith("rtmp://"))) {
            mVideoPublish = false;
            Toast.makeText(mContext, "推流地址不合法，目前支持rtmp推流!", Toast.LENGTH_SHORT).show();
            return false;
        }

        mCaptureView.setVisibility(View.VISIBLE);
        mLivePushConfig.setWatermark(mBitmap, 10, 10);

        int customModeType = 0;

        mLivePushConfig.setVideoFPS(25);


        mLivePushConfig.setCustomModeType(customModeType);

        mLivePushConfig.setPauseImg(300,10);
        Bitmap bitmap = decodeResource(getResources(),qinR.getId("drawable", "pause_publish"));//R.drawable.pause_publish
        mLivePushConfig.setPauseImg(bitmap);
        mLivePushConfig.setPauseFlag(TXLiveConstants.PAUSE_FLAG_PAUSE_VIDEO| TXLiveConstants.PAUSE_FLAG_PAUSE_AUDIO);

        mLivePushConfig.setBeautyFilter(mBeautyLevel, mWhiteningLevel);
        mLivePusher.setConfig(mLivePushConfig);
        mLivePusher.setPushListener(this);
        mLivePusher.startCameraPreview(mCaptureView);
//        mLivePusher.startScreenCapture();
        mLivePusher.startPusher(rtmpUrl.trim());

        mBtnPlay.setBackgroundResource(qinR.getId("drawable", "play_pause"));//R.drawable.play_pause

        return true;
    }

    private void stopPublishRtmp() {

//        StopScreenCapture();

        mLivePusher.stopCameraPreview(true);
        mLivePusher.stopScreenCapture();
        mLivePusher.setPushListener(null);
        mLivePusher.stopPusher();
        mCaptureView.setVisibility(View.GONE);

        if(mBtnHWEncode != null) {
            mLivePushConfig.setHardwareAcceleration(true);
            mBtnHWEncode.setBackgroundResource(qinR.getId("drawable", "quick"));//R.drawable.quick
        }

        mBtnPlay.setBackgroundResource(qinR.getId("drawable", "play_start"));//R.drawable.play_start

        if(mLivePushConfig != null) {
            mLivePushConfig.setPauseImg(null);
        }
    }


    public void FixOrAdjustBitrate() {
        if (mRadioGroupBitrate == null || mLivePushConfig == null || mLivePusher == null) {
            return;
        }

        RadioButton rb = (RadioButton) findViewById(mRadioGroupBitrate.getCheckedRadioButtonId());
        int mode = Integer.parseInt((String) rb.getTag());

        switch (mode) {
            case 4: /*720p*/
                if (mLivePusher != null) {
                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_720_1280);
                    mLivePushConfig.setAutoAdjustBitrate(false);
                    mLivePushConfig.setVideoBitrate(1500);
                    mLivePusher.setConfig(mLivePushConfig);
                }
                mBtnBitrate.setBackgroundResource(qinR.getId("drawable", "fix_bitrate"));//R.drawable.fix_bitrate
                break;
            case 3: /*540p*/
                if (mLivePusher != null) {
                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_540_960);
                    mLivePushConfig.setAutoAdjustBitrate(false);
                    mLivePushConfig.setVideoBitrate(1000);
                    mLivePusher.setConfig(mLivePushConfig);
                }
                mBtnBitrate.setBackgroundResource(qinR.getId("drawable", "fix_bitrate"));//R.drawable.fix_bitrate
                break;
            case 2: /*360p*/
                if (mLivePusher != null) {
                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640);
                    mLivePushConfig.setAutoAdjustBitrate(false);
                    mLivePushConfig.setVideoBitrate(700);
                    mLivePusher.setConfig(mLivePushConfig);
                }
                mBtnBitrate.setBackgroundResource(qinR.getId("drawable", "fix_bitrate"));//R.drawable.fix_bitrate
                break;

            case 1: /*自动*/
                if (mLivePusher != null) {
                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640);
                    mLivePushConfig.setAutoAdjustBitrate(true);
                    mLivePushConfig.setAutoAdjustStrategy(TXLiveConstants.AUTO_ADJUST_BITRATE_STRATEGY_1);
                    mLivePushConfig.setMaxVideoBitrate(1000);
                    mLivePushConfig.setMinVideoBitrate(500);
                    mLivePushConfig.setVideoBitrate(700);
                    mLivePusher.setConfig(mLivePushConfig);
                }
                mBtnBitrate.setBackgroundResource(qinR.getId("drawable", "auto_bitrate"));//R.drawable.auto_bitrate
                break;
            default:
                break;
        }
    }

    @Override
    public void onPushEvent(int event, Bundle param) {
        String msg = param.getString(TXLiveConstants.EVT_DESCRIPTION);
        //错误还是要明确的报一下
        if (event < 0) {
            Toast.makeText(mContext, param.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
        }

        if (event == TXLiveConstants.PUSH_ERR_NET_DISCONNECT) {
            stopPublishRtmp();
            mVideoPublish = false;
        }
        else if (event == TXLiveConstants.PUSH_WARNING_HW_ACCELERATION_FAIL) {
            Toast.makeText(mContext, param.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
            mLivePushConfig.setHardwareAcceleration(false);
            mBtnHWEncode.setBackgroundResource(qinR.getId("drawable", "quick2"));//R.drawable.quick2
            mLivePusher.setConfig(mLivePushConfig);
            mHWVideoEncode = false;
        }
        else if (event == TXLiveConstants.PUSH_ERR_SCREEN_CAPTURE_UNSURPORT) {
            stopPublishRtmp();
        }
        else if (event == TXLiveConstants.PUSH_ERR_SCREEN_CAPTURE_START_FAILED) {
            stopPublishRtmp();
        } else if (event == TXLiveConstants.PUSH_EVT_CHANGE_RESOLUTION) {
            Log.d(TAG, "change resolution to " + param.getInt(TXLiveConstants.EVT_PARAM2) + ", bitrate to" + param.getInt(TXLiveConstants.EVT_PARAM1));
        } else if (event == TXLiveConstants.PUSH_EVT_CHANGE_BITRATE) {
            Log.d(TAG, "change bitrate to" + param.getInt(TXLiveConstants.EVT_PARAM1));
        }
    }

    @Override
    public void onNetStatus(Bundle status) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == qinR.getId("id", "beauty_seekbar")) {//R.id.beauty_seekbar
            mBeautyLevel = progress;
        } else if (seekBar.getId() == qinR.getId("id", "whitening_seekbar")) {//R.id.whitening_seekbar
            mWhiteningLevel = progress;
        }

        if (mLivePusher != null) {
            if (!mLivePusher.setBeautyFilter(mBeautyLevel, mWhiteningLevel)) {
                Toast.makeText(mContext, "当前机型的性能无法支持美颜功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        onActivityRotation();
    }

    protected void onActivityRotation()
    {
        // 自动旋转打开，Activity随手机方向旋转之后，需要改变推流方向
        int mobileRotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_DOWN;
        switch (mobileRotation) {
            case Surface.ROTATION_0:
                pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_DOWN;
                break;
            case Surface.ROTATION_90:
                pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_RIGHT;
                break;
            case Surface.ROTATION_270:
                pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_LEFT;
                break;
            default:
                break;
        }
        mLivePusher.setRenderRotation(0); //因为activity也旋转了，本地渲染相对正方向的角度为0。
        mLivePushConfig.setHomeOrientation(pushRotation);
        mLivePusher.setConfig(mLivePushConfig);
    }

    /**
     * 判断Activity是否可旋转。只有在满足以下条件的时候，Activity才是可根据重力感应自动旋转的。
     * 系统“自动旋转”设置项打开；
     * @return false---Activity可根据重力感应自动旋转
     */
    protected boolean isActivityCanRotation()
    {
        // 判断自动旋转是否打开
        int flag = Settings.System.getInt(this.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
        if (flag == 0) {
            return false;
        }
        return true;
    }

    //观察屏幕旋转设置变化，类似于注册动态广播监听变化机制
    private class RotationObserver extends ContentObserver
    {
        ContentResolver mResolver;

        public RotationObserver(Handler handler)
        {
            super(handler);
            mResolver = LuPlayActivity.this.getContentResolver();
        }

        //屏幕旋转设置改变时调用
        @Override
        public void onChange(boolean selfChange)
        {
            super.onChange(selfChange);
            //更新按钮状态
            if (isActivityCanRotation()) {
                mBtnOrientation.setVisibility(View.GONE);
                onActivityRotation();
            } else {
                mBtnOrientation.setVisibility(View.VISIBLE);
                mPortrait = true;
                mLivePushConfig.setHomeOrientation(TXLiveConstants.VIDEO_ANGLE_HOME_DOWN);
                mBtnOrientation.setBackgroundResource(qinR.getId("drawable", "landscape"));//R.drawable.landscape
                mLivePusher.setRenderRotation(0);
                mLivePusher.setConfig(mLivePushConfig);
            }

        }

        public void startObserver()
        {
            mResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, this);
        }

        public void stopObserver()
        {
            mResolver.unregisterContentObserver(this);
        }
    }
}