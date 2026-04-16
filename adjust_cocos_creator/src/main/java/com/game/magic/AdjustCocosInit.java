package com.game.magic;


import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.cocos.service.SDKWrapper;

import java.util.concurrent.atomic.AtomicBoolean;

public class AdjustCocosInit {

    private static final String TAG = "AdjustCocosInit";

    private static final AtomicBoolean isInit = new AtomicBoolean(false);

    // call with cocos
    public static boolean initBridge(){

        if (isInit.compareAndSet(false,true)){
            boolean success = false;
            try {
                SDKWrapper sdkWrapper = SDKWrapper.shared();
                Activity activity = sdkWrapper.getActivity();
                if (activity == null){
                    Log.e(TAG,"init Bridge fail,activity is null");
                    return false;
                }
                AdjustCocosBridge.init(activity);
                AdjustCocos.setAutomaticLifecycleHandlingEnabled(true);
                activity.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {

                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                        if (activity.equals(sdkWrapper.getActivity())){
                            AdjustCocosBridge.onPause();
                        }
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                        if (activity.equals(sdkWrapper.getActivity())){
                            AdjustCocosBridge.onResume();
                        }
                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

                    }

                    @Override
                    public void onActivityStarted(Activity activity) {

                    }

                    @Override
                    public void onActivityStopped(Activity activity) {

                    }
                });
                success = true;
            }catch (Exception e){
                Log.e(TAG,"init Bridge fail ",e);
            }finally {
                Log.i(TAG,"init Bridge success = "+success);
                isInit.set(success);
            }

            return success;
        }

        return true;
    }
}
