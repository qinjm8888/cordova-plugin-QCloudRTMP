package com.tx.play.luplay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import android.content.Intent;


import com.tx.play.Play;
import com.tx.play.LuPlayActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;


/**
 * Created by qinjm on 2016.12.
 */
public class LuPlay extends Play{
    private Context context;
    private CordovaInterface cordova1;
    @Override
    public void play(final CordovaInterface cordova, final String param, final CallbackContext callbackContext) {
        cordova1 = cordova;
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Intent intent=new Intent(cordova.getActivity(),LuPlayActivity.class);
                intent.putExtra("luVideoUrl",param);

                mHandler.sendEmptyMessage(1);
                (cordova.getActivity()).startActivity(intent);
                if(true){
                    callbackContext.success("success");
                }else{
                    callbackContext.error("error");
                }
            }
        });

    }
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //msg.what
            Toast.makeText(cordova1.getActivity(), "开启中...", Toast.LENGTH_SHORT).show();
        }
    };
}
