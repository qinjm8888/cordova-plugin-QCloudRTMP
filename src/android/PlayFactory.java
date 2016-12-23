package com.tx.play;

import com.tx.play.boplay.BoPlay;
import com.tx.play.luplay.LuPlay;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import android.widget.Toast;

/**
 * Created by qinjm on 2016.12.23.
 */
public class PlayFactory extends CordovaPlugin{
    public static final int TYPE_PLAY=1;
    public static final int TYPE_LUPLAY=2;

    public static final String ACT_PLAY="play";

    private Play play;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (ACT_PLAY.equals(action)){
            int type=args.getInt(0);
            String param=args.getString(1);
            switch (type){
                case TYPE_PLAY:{
                    play=new BoPlay();
                    break;
                }
                case TYPE_LUPLAY:{
                    play=new LuPlay();
                    break;
                }
            }
            play.play(cordova,param,callbackContext);
            return true;
        }

        return  false;
    }


}
