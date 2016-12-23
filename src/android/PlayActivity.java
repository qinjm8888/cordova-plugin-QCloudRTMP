package com.tx.play;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.rtmp.ITXLivePlayListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayConfig;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.TXLivePusher;
import com.tencent.rtmp.ui.TXCloudVideoView;

import com.tx.play.QinR;

public class PlayActivity extends Activity implements ITXLivePlayListener, View.OnClickListener{
    private static final String TAG = PlayActivity.class.getSimpleName();

    private QinR qinR;

    private TXLivePlayer     mLivePlayer = null;
    private boolean          mVideoPlay;
    private TXCloudVideoView mPlayerView;
    private ImageView mLoadingView;
    private boolean          mHWDecode   = false;

    private Button           mBtnPlay;
    private Button           mBtnRenderRotation;
    private Button           mBtnRenderMode;
    private Button           mBtnHWDecode;
    private SeekBar mSeekBar;
    private TextView mTextDuration;
    private TextView         mTextStart;
    private Button           back;

    private static final int  CACHE_STRATEGY_FAST  = 1;  //极速
    private static final int  CACHE_STRATEGY_SMOOTH = 2;  //流畅
    private static final int  CACHE_STRATEGY_AUTO = 3;  //自动

    private static final int  CACHE_TIME_FAST = 1;
    private static final int  CACHE_TIME_SMOOTH = 5;

    private int              mCacheStrategy = 0;
    private Button           mBtnCacheStrategy;
    private Button           mRatioFast;
    private Button           mRatioSmooth;
    private Button           mRatioAuto;
    private Button           mBtnStop;
    private LinearLayout     mLayoutCacheStrategy;

    private int              mCurrentRenderMode;
    private int              mCurrentRenderRotation;

    private long             mTrackingTouchTS = 0;
    private boolean          mStartSeek = false;
    private boolean          mVideoPause = false;
    private int              mPlayType = TXLivePlayer.PLAY_TYPE_LIVE_RTMP;
    private TXLivePlayConfig mPlayConfig;
    private long             mStartPlayTS = 0;
    private Context mContext;
    private String playUrl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        qinR = new QinR(this);

        int[] sdkver = TXLivePusher.getSDKVersion();
        if (sdkver != null && sdkver.length >= 3) {
            Log.d("rtmpsdk","rtmp sdk version is:" + sdkver[0] + "." + sdkver[1] + "." + sdkver[2]);
        }

        Intent intent = getIntent();
        playUrl = intent.getStringExtra("videoUrl");
//        Toast.makeText(this, playUrl, Toast.LENGTH_SHORT).show();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mContext = this;

        mCurrentRenderMode     = TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION;
        mCurrentRenderRotation = TXLiveConstants.RENDER_ROTATION_PORTRAIT;

        mPlayConfig = new TXLivePlayConfig();

//        setContentView(R.layout.activity_play);
        setContentView(qinR.getId("layout", "activity_main"));

        init();
        if (startPlayRtmp()) {
            mVideoPlay = !mVideoPlay;
        }
    }

    public void init() {

        if (mLivePlayer == null){
            mLivePlayer = new TXLivePlayer(mContext);
        }

        mPlayerView = (TXCloudVideoView) findViewById(qinR.getId("id", "video_view"));//R.id.video_view
        mLoadingView = (ImageView) findViewById(qinR.getId("id", "loadingImageView"));//R.id.loadingImageView


        mVideoPlay = false;

        back = (Button) findViewById(qinR.getId("id", "back"));//R.id.back
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mBtnPlay = (Button) findViewById(qinR.getId("id", "btnPlay"));//R.id.btnPlay
        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "click playbtn isplay:" + mVideoPlay+" ispause:"+mVideoPause+" playtype:"+mPlayType);
                if (mVideoPlay) {
                    if (mPlayType == TXLivePlayer.PLAY_TYPE_VOD_FLV || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_HLS || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_MP4) {
                        if (mVideoPause) {
                            mLivePlayer.resume();
                            mBtnPlay.setBackgroundResource(qinR.getId("drawable", "play_pause"));//R.drawable.play_pause
                        } else {
                            mLivePlayer.pause();
                            mBtnPlay.setBackgroundResource(qinR.getId("drawable", "play_start"));//R.drawable.play_start
                        }
                        mVideoPause = !mVideoPause;

                    } else {
                        stopPlayRtmp();
                        mVideoPlay = !mVideoPlay;
                    }

                } else {
                    if (startPlayRtmp()) {
                        mVideoPlay = !mVideoPlay;
                    }
                }
            }
        });

        //停止按钮
        mBtnStop = (Button) findViewById(qinR.getId("id", "btnStop"));//R.id.btnStop
        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlayRtmp();
                mVideoPlay = false;
                mVideoPause = false;
                if (mTextStart != null) {
                    mTextStart.setText("00:00");
                }
                if (mSeekBar != null) {
                    mSeekBar.setProgress(0);
                }
            }
        });

        //横屏|竖屏
        mBtnRenderRotation = (Button) findViewById(qinR.getId("id", "btnOrientation"));//R.id.btnOrientation
        mBtnRenderRotation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLivePlayer == null) {
                    return;
                }

                if (mCurrentRenderRotation == TXLiveConstants.RENDER_ROTATION_PORTRAIT) {
                    mBtnRenderRotation.setBackgroundResource(qinR.getId("drawable", "portrait"));//R.drawable.portrait
                    mCurrentRenderRotation = TXLiveConstants.RENDER_ROTATION_LANDSCAPE;
                } else if (mCurrentRenderRotation == TXLiveConstants.RENDER_ROTATION_LANDSCAPE) {
                    mBtnRenderRotation.setBackgroundResource(qinR.getId("drawable", "landscape"));//R.drawable.landscape
                    mCurrentRenderRotation = TXLiveConstants.RENDER_ROTATION_PORTRAIT;
                }

                mLivePlayer.setRenderRotation(mCurrentRenderRotation);
            }
        });

        //平铺模式
        mBtnRenderMode = (Button) findViewById(qinR.getId("id", "btnRenderMode"));//R.id.btnRenderMode
        mBtnRenderMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLivePlayer == null) {
                    return;
                }

                if (mCurrentRenderMode == TXLiveConstants.RENDER_MODE_FULL_FILL_SCREEN) {
                    mLivePlayer.setRenderMode(TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION);
                    mBtnRenderMode.setBackgroundResource(qinR.getId("drawable", "fill_mode"));//R.drawable.fill_mode
                    mCurrentRenderMode = TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION;
                } else if (mCurrentRenderMode == TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION) {
                    mLivePlayer.setRenderMode(TXLiveConstants.RENDER_MODE_FULL_FILL_SCREEN);
                    mBtnRenderMode.setBackgroundResource(qinR.getId("drawable", "adjust_mode"));//R.drawable.adjust_mode
                    mCurrentRenderMode = TXLiveConstants.RENDER_MODE_FULL_FILL_SCREEN;
                }
            }
        });

        //硬件解码
        mBtnHWDecode = (Button) findViewById(qinR.getId("id", "btnHWDecode"));//R.id.btnHWDecode
        mBtnHWDecode.getBackground().setAlpha(mHWDecode ? 255 : 100);
        mBtnHWDecode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHWDecode = !mHWDecode;
                mBtnHWDecode.getBackground().setAlpha(mHWDecode ? 255 : 100);

                if (mHWDecode) {
                    Toast.makeText(mContext, "已开启硬件解码加速，切换会重启播放流程!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "已关闭硬件解码加速，切换会重启播放流程!", Toast.LENGTH_SHORT).show();
                }

                if (mVideoPlay) {
                    stopPlayRtmp();
                    startPlayRtmp();
                    if (mVideoPause) {
                        if (mPlayerView != null){
                            mPlayerView.onResume();
                        }
                        mVideoPause = false;
                    }
                }
            }
        });

        mSeekBar = (SeekBar) findViewById(qinR.getId("id", "seekbar"));//R.id.seekbar
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean bFromUser) {
                mTextStart.setText(String.format("%02d:%02d",progress/60, progress%60));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mStartSeek = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if ( mLivePlayer != null) {
                    mLivePlayer.seek(seekBar.getProgress());
                }
                mTrackingTouchTS = System.currentTimeMillis();
                mStartSeek = false;
            }
        });

        mTextDuration = (TextView) findViewById(qinR.getId("id", "duration"));//R.id.duration
        mTextStart = (TextView)findViewById(qinR.getId("id", "play_start"));//R.id.play_start
        mTextDuration.setTextColor(Color.rgb(255, 255, 255));
        mTextStart.setTextColor(Color.rgb(255, 255, 255));
        //缓存策略
        mBtnCacheStrategy = (Button)findViewById(qinR.getId("id", "btnCacheStrategy"));//R.id.btnCacheStrategy
        mLayoutCacheStrategy = (LinearLayout)findViewById(qinR.getId("id", "layoutCacheStrategy"));//R.id.layoutCacheStrategy
        mBtnCacheStrategy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLayoutCacheStrategy.setVisibility(mLayoutCacheStrategy.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        this.setCacheStrategy(CACHE_STRATEGY_AUTO);

        mRatioFast = (Button)findViewById(qinR.getId("id", "radio_btn_fast"));//R.id.radio_btn_fast
        mRatioFast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlayActivity.this.setCacheStrategy(CACHE_STRATEGY_FAST);
                mLayoutCacheStrategy.setVisibility(View.GONE);
            }
        });

        mRatioSmooth = (Button)findViewById(qinR.getId("id", "radio_btn_smooth"));//R.id.radio_btn_smooth
        mRatioSmooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlayActivity.this.setCacheStrategy(CACHE_STRATEGY_SMOOTH);
                mLayoutCacheStrategy.setVisibility(View.GONE);
            }
        });

        mRatioAuto = (Button)findViewById(qinR.getId("id", "radio_btn_auto"));//R.id.radio_btn_auto
        mRatioAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlayActivity.this.setCacheStrategy(CACHE_STRATEGY_AUTO);
                mLayoutCacheStrategy.setVisibility(View.GONE);
            }
        });

        View progressGroup = findViewById(qinR.getId("id", "play_progress"));//R.id.play_progress

        // 直播不需要进度条和停止按钮，点播不需要缓存策略
//        if (mActivityType == RTMPBaseActivity.ACTIVITY_TYPE_LIVE_PLAY) {
//            progressGroup.setVisibility(View.GONE);
//            mBtnStop.setVisibility(View.GONE);
//        }
//        else if (mActivityType == RTMPBaseActivity.ACTIVITY_TYPE_VOD_PLAY) {
        mBtnCacheStrategy.setVisibility(View.GONE);
//        }

    }


    /**
     * 核对播放地址
     */
    private boolean checkPlayUrl(String playUrl) {
        if (TextUtils.isEmpty(playUrl) || (!playUrl.startsWith("http://") && !playUrl.startsWith("https://") && !playUrl.startsWith("rtmp://"))) {
            Toast.makeText(mContext, "地址不合法", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (playUrl.startsWith("http://") || playUrl.startsWith("https://")) {
            if (playUrl.contains(".flv")) {
                mPlayType = TXLivePlayer.PLAY_TYPE_VOD_FLV;
            } else if (playUrl.contains(".m3u8")) {
                mPlayType = TXLivePlayer.PLAY_TYPE_VOD_HLS;
            } else if (playUrl.toLowerCase().contains(".mp4")) {
                mPlayType = TXLivePlayer.PLAY_TYPE_VOD_MP4;
            } else {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLivePlayer != null) {
            mLivePlayer.stopPlay(true);
        }
        if (mPlayerView != null){
            mPlayerView.onDestroy();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop(){
        super.onStop();

        if (mPlayType == TXLivePlayer.PLAY_TYPE_VOD_FLV || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_HLS || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_MP4) {
            if (mLivePlayer != null) {
                mLivePlayer.pause();
            }
        } else if (Build.VERSION.SDK_INT >= 23){ //目前android6.0以上暂不支持后台播放
            stopPlayRtmp();
        }

        if (mPlayerView != null){
            mPlayerView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mVideoPlay && !mVideoPause) {
            if (mPlayType == TXLivePlayer.PLAY_TYPE_VOD_FLV || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_HLS || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_MP4) {
                if (mLivePlayer != null) {
                    mLivePlayer.resume();
                }
            }
            else if (Build.VERSION.SDK_INT >= 23) { //目前android6.0以上暂不支持后台播放
                startPlayRtmp();
            }
        }

        if (mPlayerView != null){
            mPlayerView.onResume();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                mLayoutCacheStrategy.setVisibility(View.GONE);
        }
    }

    private boolean startPlayRtmp() {
        String playUrl = this.playUrl;

//        //由于iOS AppStore要求新上架的app必须使用https,所以后续腾讯云的视频连接会支持https,但https会有一定的性能损耗,所以android将统一替换会http
//        if (playUrl.startsWith("https://")) {
//            playUrl = "http://" + playUrl.substring(8);
//        }

        if (!checkPlayUrl(playUrl)) {
            return false;
        }


        int[] ver = TXLivePlayer.getSDKVersion();
        mBtnPlay.setBackgroundResource(qinR.getId("drawable", "play_pause"));//R.drawable.play_pause

        mLivePlayer.setPlayerView(mPlayerView);
        mLivePlayer.setPlayListener(this);

        // 硬件加速在1080p解码场景下效果显著，但细节之处并不如想象的那么美好：
        // (1) 只有 4.3 以上android系统才支持
        // (2) 兼容性我们目前还仅过了小米华为等常见机型，故这里的返回值您先不要太当真
        mLivePlayer.enableHardwareDecode(mHWDecode);
        mLivePlayer.setRenderRotation(mCurrentRenderRotation);
        mLivePlayer.setRenderMode(mCurrentRenderMode);
        //设置播放器缓存策略
        //这里将播放器的策略设置为自动调整，调整的范围设定为1到4s，您也可以通过setCacheTime将播放器策略设置为采用
        //固定缓存时间。如果您什么都不调用，播放器将采用默认的策略（默认策略为自动调整，调整范围为1到4s）
        //mLivePlayer.setCacheTime(5);
        mLivePlayer.setConfig(mPlayConfig);

        int result = mLivePlayer.startPlay(playUrl,mPlayType); // result返回值：0 success;  -1 empty url; -2 invalid url; -3 invalid playType;
        if (result == -2) {
            Toast.makeText(mContext, "非腾讯云链接地址，若要放开限制，请联系腾讯云商务团队", Toast.LENGTH_SHORT).show();
        }
        if (result != 0) {
            mBtnPlay.setBackgroundResource(qinR.getId("drawable", "play_start"));//R.drawable.play_start
            return false;
        }


        startLoadingAnimation();


        mStartPlayTS = System.currentTimeMillis();
        return true;
    }

    private  void stopPlayRtmp() {
        mBtnPlay.setBackgroundResource(qinR.getId("drawable", "play_start"));//R.drawable.play_start
        stopLoadingAnimation();
        if (mLivePlayer != null) {
            mLivePlayer.setPlayListener(null);
            mLivePlayer.stopPlay(true);
        }
    }

    @Override
    public void onPlayEvent(int event, Bundle param) {
        if (event == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) {
            stopLoadingAnimation();
            Log.d("AutoMonitor", "PlayFirstRender,cost=" +(System.currentTimeMillis()-mStartPlayTS));
        } else if (event == TXLiveConstants.PLAY_EVT_PLAY_PROGRESS ) {
            if (mStartSeek) {
                return;
            }
            int progress = param.getInt(TXLiveConstants.EVT_PLAY_PROGRESS);
            int duration = param.getInt(TXLiveConstants.EVT_PLAY_DURATION);
            long curTS = System.currentTimeMillis();

            // 避免滑动进度条松开的瞬间可能出现滑动条瞬间跳到上一个位置
            if (Math.abs(curTS - mTrackingTouchTS) < 500) {
                return;
            }
            mTrackingTouchTS = curTS;

            if (mSeekBar != null) {
                mSeekBar.setProgress(progress);
            }
            if (mTextStart != null) {
                mTextStart.setText(String.format("%02d:%02d",progress/60,progress%60));
            }
            if (mTextDuration != null) {
                mTextDuration.setText(String.format("%02d:%02d",duration/60,duration%60));
            }
            if (mSeekBar != null) {
                mSeekBar.setMax(duration);
            }
            return;
        } else if (event == TXLiveConstants.PLAY_ERR_NET_DISCONNECT || event == TXLiveConstants.PLAY_EVT_PLAY_END) {
            stopPlayRtmp();
            mVideoPlay = false;
            mVideoPause = false;
            if (mTextStart != null) {
                mTextStart.setText("00:00");
            }
            if (mSeekBar != null) {
                mSeekBar.setProgress(0);
            }
        } else if (event == TXLiveConstants.PLAY_EVT_PLAY_LOADING){
            startLoadingAnimation();
        }

        String msg = param.getString(TXLiveConstants.EVT_DESCRIPTION);

        if (event < 0) {
            if (mContext != null) {
                Toast.makeText(mContext, param.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
            }
        }

        else if (event == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) {
            stopLoadingAnimation();
        }
    }

    @Override
    public void onNetStatus(Bundle bundle) {

    }


    public void setCacheStrategy(int nCacheStrategy) {
        if (mCacheStrategy == nCacheStrategy)   return;
        mCacheStrategy = nCacheStrategy;

        switch (nCacheStrategy) {
            case CACHE_STRATEGY_FAST:
                mPlayConfig.setAutoAdjustCacheTime(true);
                mPlayConfig.setMaxAutoAdjustCacheTime(CACHE_TIME_FAST);
                mPlayConfig.setMinAutoAdjustCacheTime(CACHE_TIME_FAST);
                mLivePlayer.setConfig(mPlayConfig);
                break;

            case CACHE_STRATEGY_SMOOTH:
                mPlayConfig.setAutoAdjustCacheTime(false);
                mPlayConfig.setCacheTime(CACHE_TIME_SMOOTH);
                mLivePlayer.setConfig(mPlayConfig);
                break;

            case CACHE_STRATEGY_AUTO:
                mPlayConfig.setAutoAdjustCacheTime(true);
                mPlayConfig.setMaxAutoAdjustCacheTime(CACHE_TIME_SMOOTH);
                mPlayConfig.setMinAutoAdjustCacheTime(CACHE_TIME_FAST);
                mLivePlayer.setConfig(mPlayConfig);
                break;

            default:
                break;
        }
    }

    private void startLoadingAnimation() {
        if (mLoadingView != null) {
            mLoadingView.setVisibility(View.VISIBLE);
            ((AnimationDrawable)mLoadingView.getDrawable()).start();
        }
    }

    private void stopLoadingAnimation() {
        if (mLoadingView != null) {
            mLoadingView.setVisibility(View.GONE);
            ((AnimationDrawable)mLoadingView.getDrawable()).stop();
        }
    }

}