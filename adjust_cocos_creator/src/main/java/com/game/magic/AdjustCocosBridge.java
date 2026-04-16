package com.game.magic;


import android.content.Context;
import android.util.Log;


import com.cocos.lib.JsbBridgeWrapper;

import org.json.JSONObject;

public class AdjustCocosBridge {

    private static final String TAG = "AdjustCocosBridge";
    private static Context context;
    public static void init(Context context){
        AdjustCocosBridge.context = context;
        JsbBridgeWrapper wrapper = JsbBridgeWrapper.getInstance();
        wrapper.addScriptEventListener("adjustInit", data -> {
            AdjustInitPayload payload = parseInitPayload(data);
            if (payload == null) {
                Log.e(TAG, "adjustInit payload is null");
                return;
            }
            AdjustCocos.init(payload.appToken, payload.product, payload.logLevel);
        });
        wrapper.addScriptEventListener("adjustInitConfig", data -> {
            AdjustCocosConfig payload = AdjustCocos.parseConfigPayload(data);
            if (payload == null) {
                Log.e(TAG, "adjustInitConfig payload is null");
                return;
            }
            AdjustCocos.init(payload);
        });
        wrapper.addScriptEventListener("adjustTrackEvent", AdjustCocos::trackEvent);
        wrapper.addScriptEventListener("adjustTrackAdRevenue", AdjustCocos::trackAdRevenue);
        wrapper.addScriptEventListener("adjustOpenDeeplink", AdjustCocos::processDeeplink);

        wrapper.addScriptEventListener("adjustGetAdid",arg -> AdjustCocos.getAdid());

        wrapper.addScriptEventListener("adjustGetAttribution",arg -> AdjustCocos.getAttribution());
        wrapper.addScriptEventListener("adjustGetGoogleAdId",arg -> AdjustCocos.getGoogleAdId());
        wrapper.addScriptEventListener("adjustGetAmazonAdId",arg -> AdjustCocos.getAmazonAdId());
        wrapper.addScriptEventListener("adjustGetLastDeeplink",arg -> AdjustCocos.getLastDeeplink());
        wrapper.addScriptEventListener("adjustGetSdkVersion",arg -> AdjustCocos.getSdkVersion());
        wrapper.addScriptEventListener("adjustVerifyPlayStorePurchase", AdjustCocos::verifyPlayStorePurchase);
        wrapper.addScriptEventListener("adjustOnResume",data->AdjustCocos.onResume());
        wrapper.addScriptEventListener("adjustOnPause",data->AdjustCocos.onPause());
        wrapper.addScriptEventListener("adjustGetInstallReferrer",data->AdjustCocos.getGooglePlayInstallReferrer());
    }



    private static class AdjustInitPayload {
        public String appToken;
        public String product;
        public String logLevel;
    }

    public static void onResume(){
        AdjustCocos.onAutoResume();
    }

    public static void onPause(){
        AdjustCocos.onAutoPause();
    }

    private static AdjustInitPayload parseInitPayload(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject object = new JSONObject(json);
            AdjustInitPayload payload = new AdjustInitPayload();
            payload.appToken = object.optString("appToken", null);
            payload.product = object.optString("product", null);
            payload.logLevel = object.optString("logLevel", null);
            return payload;
        } catch (Exception e) {
            Log.e(TAG, "parse init payload error: " + e.getMessage(), e);
            return null;
        }
    }
    public static Context getContext(){
        return context;
    }
}
