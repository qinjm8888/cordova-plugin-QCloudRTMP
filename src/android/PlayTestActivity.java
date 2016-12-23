package com.tx.play;

import android.content.Intent;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.tencent.rtmp.ITXLivePlayListener;
import com.tencent.rtmp.TXLivePusher;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;

import com.tx.play.QinR;


public class PlayTestActivity extends Activity {
    private QinR qinR;

    private TXCloudVideoView mPlayerView;
    private TXLivePlayer mLivePlayer = null;
    private int mPlayType = TXLivePlayer.PLAY_TYPE_LIVE_RTMP;
    private String playUrl;
    private Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        qinR = new QinR(this);
        setContentView(qinR.getId("layout", "activity_main"));
        /*int[] sdkver = TXLivePusher.getSDKVersion();

        Intent intent = getIntent();
        playUrl = intent.getStringExtra("videoUrl");
        Toast.makeText(this, playUrl, Toast.LENGTH_SHORT).show();

        mContext = this;
        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        setContentView(layout, layoutParams);

        mPlayerView = new TXCloudVideoView(this);
        RelativeLayout.LayoutParams videoViewParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        videoViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        layout.addView(mPlayerView, videoViewParams);
        init();*/


    }

    /*private void init() {
        if (mLivePlayer == null) {
            mLivePlayer = new TXLivePlayer(mContext);
        }
        startPlay();
    }

    *//**
     * 核对播放地址
     *//*
    private boolean checkPlayUrl() {
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

    *//**
     * 开始播放
     *//*
    private void startPlay() {
        if (checkPlayUrl()) {
            mLivePlayer.setPlayerView(mPlayerView);
            mLivePlayer.setPlayListener(this);
            mLivePlayer.enableHardwareDecode(false);
            int result = mLivePlayer.startPlay(playUrl, mPlayType); // result返回值：0 success;  -1 empty url; -2 invalid url; -3 invalid playType;
            if (result == -2) {
                Toast.makeText(mContext, "非腾讯云链接地址", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPlayEvent(int event, Bundle param) {
    }

    @Override
    public void onNetStatus(Bundle status) {
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPlayerView != null) {
            mPlayerView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mPlayerView != null) {
            mPlayerView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLivePlayer != null) {
            mLivePlayer.stopPlay(true);
        }
        if (mPlayerView != null) {
            mPlayerView.onDestroy();
        }
    }*/
}