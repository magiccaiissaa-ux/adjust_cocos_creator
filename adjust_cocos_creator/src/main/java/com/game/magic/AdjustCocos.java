package com.game.magic;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustDeeplink;
import com.adjust.sdk.AdjustEvent;
import com.adjust.sdk.AdjustPlayStorePurchase;
import com.adjust.sdk.AdjustPurchaseVerificationResult;
import com.adjust.sdk.GooglePlayInstallReferrerDetails;
import com.adjust.sdk.LogLevel;
import com.adjust.sdk.OnGooglePlayInstallReferrerReadListener;
import com.cocos.lib.JsbBridgeWrapper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdjustCocos {

    private static final String TAG = "AdjustCocos";

    public static final String EVENT_SESSION_SUCCESS = "adjustOnSessionTrackingSucceeded";
    public static final String EVENT_SESSION_FAIL = "adjustOnSessionTrackingFailed";
    public static final String EVENT_EVENT_SUCCESS = "adjustOnEventTrackingSucceeded";
    public static final String EVENT_EVENT_FAIL = "adjustOnEventTrackingFailed";
    public static final String EVENT_ATTRIBUTION_CHANGED = "adjustOnAttributionChanged";
    public static final String EVENT_DEFERRED_DEEPLINK = "adjustOnDeferredDeeplinkResponse";
    public static final String EVENT_ADID_READ = "adjustOnAdidRead";
    public static final String EVENT_GOOGLE_ADID_READ = "adjustOnGoogleAdIdRead";
    public static final String EVENT_AMAZON_ADID_READ = "adjustOnAmazonAdIdRead";
    public static final String EVENT_LAST_DEEPLINK_READ = "adjustOnLastDeeplinkRead";
    public static final String EVENT_SDK_VERSION_READ = "adjustOnSdkVersionRead";
    public static final String EVENT_PLAY_STORE_PURCHASE_VERIFIED = "adjustOnPlayStorePurchaseVerified";
    public static final String EVENT_INSTALL_REFERRER_READ = "adjustInstallReferrerRead";
    protected static final AtomicBoolean isInit = new AtomicBoolean(false);
    private static final AtomicBoolean automaticLifecycleHandlingEnabled = new AtomicBoolean(false);


    public static void init(AdjustCocosConfig adjustCocosConfig){
        if (adjustCocosConfig == null){
            Log.e(TAG,"init error adjustCocosConfig is null");
            return;
        }
        if (isInit.compareAndSet(false, true)) {
            Context context = getContext();
            if (context == null) {
                Log.e(TAG, "init failed: context is null");
                isInit.set(false);
                return;
            }
            if (isBlank(adjustCocosConfig.appToken) || isBlank(adjustCocosConfig.environment)) {
                Log.e(TAG, "init failed: appToken or environment is blank");
                isInit.set(false);
                return;
            }

            try {
                AdjustConfig adjustConfig = buildAdjustConfig(context, adjustCocosConfig);

                bindCallbacks(adjustConfig, adjustCocosConfig);
                Adjust.initSdk(adjustConfig);
            } catch (Exception e) {
                Log.e(TAG, "adjust init error: " + e.getMessage(), e);
                isInit.set(false);
            }
        }
    }




    public static void init(String appToken, String product, String logLevel) {
        AdjustCocosConfig config = new AdjustCocosConfig();
        config.appToken = appToken;
        config.environment = getProduct(product);
        config.logLevel = logLevel;
        init(config);
    }

    public static void trackEvent(String eventInfo) {
        if (!isInit.get()){
            return;
        }
        if (isBlank(eventInfo)) {
            Log.e(TAG, "trackEvent failed: eventInfo is blank");
            return;
        }

        try {
            AdjustCocosEventPayload payload = parseEventPayload(eventInfo);
            if (payload == null) {
                Log.e(TAG, "trackEvent failed: payload is null");
                return;
            }
            if (isBlank(payload.eventToken)) {
                Log.e(TAG, "trackEvent failed: eventToken is blank");
                return;
            }

            AdjustEvent adjustEvent = new AdjustEvent(payload.eventToken);

            if (payload.revenue != null && isNotBlank(payload.currency)) {
                adjustEvent.setRevenue(payload.revenue, payload.currency);
            }

            if (isNotBlank(payload.orderId)) {
                try {
                    adjustEvent.setOrderId(payload.orderId);
                } catch (Throwable ignore) {
                    Log.w(TAG, "setOrderId not supported in current Adjust SDK");
                }
            }

            if (isNotBlank(payload.deduplicationId)) {
                try {
                    adjustEvent.setDeduplicationId(payload.deduplicationId);
                } catch (Throwable ignore) {
                    Log.w(TAG, "setDeduplicationId not supported in current Adjust SDK");
                }
            }

            if (isNotBlank(payload.callbackId)) {
                try {
                    adjustEvent.setCallbackId(payload.callbackId);
                } catch (Throwable ignore) {
                    Log.w(TAG, "setCallbackId not supported in current Adjust SDK");
                }
            }

            if (isNotBlank(payload.productId)) {
                try {
                    adjustEvent.setProductId(payload.productId);
                } catch (Throwable ignore) {
                    Log.w(TAG, "setProductId not supported in current Adjust SDK");
                }
            }

            appendParams(adjustEvent, payload.callbackParams, true);
            appendParams(adjustEvent, payload.partnerParams, false);

            Adjust.trackEvent(adjustEvent);
        } catch (Exception e) {
            Log.e(TAG, "trackEvent error: " + e.getMessage(), e);
        }
    }

    public static void trackAdRevenue(String revenueInfo) {
        if (!isInit.get()){
            return;
        }
        if (isBlank(revenueInfo)) {
            Log.e(TAG, "trackAdRevenue failed: revenueInfo is blank");
            return;
        }

        try {
            AdjustCocosAdRevenuePayload payload = parseAdRevenuePayload(revenueInfo);
            if (payload == null) {
                Log.e(TAG, "trackAdRevenue failed: payload is null");
                return;
            }
            if (isBlank(payload.source)) {
                Log.e(TAG, "trackAdRevenue failed: source is blank");
                return;
            }

            AdjustAdRevenue adRevenue = new AdjustAdRevenue(payload.source);

            if (payload.revenue != null && isNotBlank(payload.currency)) {
                adRevenue.setRevenue(payload.revenue, payload.currency);
            }

            if (payload.adImpressionsCount != null) {
                try {
                    adRevenue.setAdImpressionsCount(payload.adImpressionsCount);
                } catch (Throwable ignore) {
                    Log.w(TAG, "setAdImpressionsCount not supported in current Adjust SDK");
                }
            }

            if (isNotBlank(payload.adRevenueNetwork)) {
                adRevenue.setAdRevenueNetwork(payload.adRevenueNetwork);
            }

            if (isNotBlank(payload.adRevenueUnit)) {
                adRevenue.setAdRevenueUnit(payload.adRevenueUnit);
            }

            if (isNotBlank(payload.adRevenuePlacement)) {
                adRevenue.setAdRevenuePlacement(payload.adRevenuePlacement);
            }

            if (payload.callbackParameters != null) {
                for (Map.Entry<String, String> entry : payload.callbackParameters.entrySet()) {
                    if (isNotBlank(entry.getKey()) && entry.getValue() != null) {
                        adRevenue.addCallbackParameter(entry.getKey(), entry.getValue());
                    }
                }
            }

            if (payload.partnerParameters != null) {
                for (Map.Entry<String, String> entry : payload.partnerParameters.entrySet()) {
                    if (isNotBlank(entry.getKey()) && entry.getValue() != null) {
                        adRevenue.addPartnerParameter(entry.getKey(), entry.getValue());
                    }
                }
            }

            Adjust.trackAdRevenue(adRevenue);
        } catch (Exception e) {
            Log.e(TAG, "trackAdRevenue error: " + e.getMessage(), e);
        }
    }

    public static void getAdid() {
        try {
            Adjust.getAdid(adid -> {
                dispatchToScript(EVENT_ADID_READ, createSingleValueMap("adid", adid));
            });
        } catch (Exception e) {
            dispatchToScript(EVENT_ADID_READ, createSingleValueMap("adid", ""));
            Log.e(TAG, "getAdid error: " + e.getMessage(), e);
        }
    }

    public static void getAttribution() {

        try {
            Adjust.getAttribution(attribution -> {
                dispatchToScript(EVENT_ATTRIBUTION_CHANGED, AdjustJsonUtil.toAttributionMap(attribution));
            });
        } catch (Exception e) {
            dispatchToScript(EVENT_ATTRIBUTION_CHANGED, new HashMap<>());
            Log.e(TAG, "getAttribution error " + e.getMessage(), e);
        }
    }

    public static void getGooglePlayInstallReferrer(){
        try {

            Adjust.getGooglePlayInstallReferrer(getContext(), new OnGooglePlayInstallReferrerReadListener() {
                @Override
                public void onInstallReferrerRead(GooglePlayInstallReferrerDetails googlePlayInstallReferrerDetails) {
                    AdjustInstallReferrerDetails details = new AdjustInstallReferrerDetails(true,"",googlePlayInstallReferrerDetails);

                    dispatchToScript(EVENT_INSTALL_REFERRER_READ,details);
                }

                @Override
                public void onFail(String s) {
                    AdjustInstallReferrerDetails details = new AdjustInstallReferrerDetails(false,s,null);

                    dispatchToScript(EVENT_INSTALL_REFERRER_READ,details);
                }
            });
        }catch (Exception e){
            AdjustInstallReferrerDetails details = new AdjustInstallReferrerDetails(false,e.getMessage(),null);
            dispatchToScript(EVENT_INSTALL_REFERRER_READ,details);
        }
    }

    public static void getGoogleAdId() {
        try {
            Adjust.getGoogleAdId(getContext(), googleAdId ->
                    dispatchToScript(EVENT_GOOGLE_ADID_READ, createSingleValueMap("googleAdId", googleAdId)));
        } catch (Exception e) {
            dispatchToScript(EVENT_GOOGLE_ADID_READ, createSingleValueMap("googleAdId", ""));
            Log.e(TAG, "getGoogleAdId error: " + e.getMessage(), e);
        }
    }

    public static void getAmazonAdId() {
        try {
            Adjust.getAmazonAdId(getContext(), amazonAdId ->
                    dispatchToScript(EVENT_AMAZON_ADID_READ, createSingleValueMap("amazonAdId", amazonAdId)));
        } catch (Exception e) {
            dispatchToScript(EVENT_AMAZON_ADID_READ, createSingleValueMap("amazonAdId", ""));
            Log.e(TAG, "getAmazonAdId error: " + e.getMessage(), e);
        }
    }

    public static void getLastDeeplink() {
        try {
            Adjust.getLastDeeplink(getContext(), lastDeeplink ->
                    dispatchToScript(EVENT_LAST_DEEPLINK_READ, createSingleValueMap("lastDeeplink", lastDeeplink != null ? lastDeeplink.toString() : "")));
        } catch (Exception e) {
            dispatchToScript(EVENT_LAST_DEEPLINK_READ, createSingleValueMap("lastDeeplink", ""));
            Log.e(TAG, "getLastDeeplink error: " + e.getMessage(), e);
        }
    }

    public static void getSdkVersion() {
        try {
            Adjust.getSdkVersion(sdkVersion ->
                    dispatchToScript(EVENT_SDK_VERSION_READ, createSingleValueMap("sdkVersion", sdkVersion)));
        } catch (Exception e) {
            dispatchToScript(EVENT_SDK_VERSION_READ, createSingleValueMap("sdkVersion", ""));
            Log.e(TAG, "getSdkVersion error: " + e.getMessage(), e);
        }
    }

    public static void verifyPlayStorePurchase(String purchaseInfo) {
        if (isBlank(purchaseInfo)) {
            dispatchToScript(EVENT_PLAY_STORE_PURCHASE_VERIFIED, createPurchaseVerificationResult(null));
            Log.e(TAG, "verifyPlayStorePurchase failed: purchaseInfo is blank");
            return;
        }

        try {
            AdjustPlayStorePurchasePayload payload = parsePlayStorePurchasePayload(purchaseInfo);
            if (payload == null || isBlank(payload.productId) || isBlank(payload.purchaseToken)) {
                dispatchToScript(EVENT_PLAY_STORE_PURCHASE_VERIFIED, createPurchaseVerificationResult(null));
                Log.e(TAG, "verifyPlayStorePurchase failed: payload is invalid");
                return;
            }

            AdjustPlayStorePurchase purchase = new AdjustPlayStorePurchase(payload.productId, payload.purchaseToken);
            Adjust.verifyPlayStorePurchase(purchase, result ->
                    dispatchToScript(EVENT_PLAY_STORE_PURCHASE_VERIFIED, createPurchaseVerificationResult(result)));
        } catch (Exception e) {
            dispatchToScript(EVENT_PLAY_STORE_PURCHASE_VERIFIED, createPurchaseVerificationResult(null));
            Log.e(TAG, "verifyPlayStorePurchase error: " + e.getMessage(), e);
        }
    }

    public static void processDeeplink(String url) {
        if (isBlank(url)) {
            Log.e(TAG, "processDeeplink failed: url is blank");
            return;
        }

        try {
            Context context = getContext();
            if (context == null) {
                Log.e(TAG, "processDeeplink failed: context is null");
                return;
            }

            Uri uri = Uri.parse(url);
            Adjust.processDeeplink(new AdjustDeeplink(uri), context);
        } catch (Exception e) {
            Log.e(TAG, "processDeeplink error: " + e.getMessage(), e);
        }
    }



    public static void enabled() {
        if (!isInit.get()){
            return;
        }
        Adjust.enable();
    }
    public static void disable(){
        if (!isInit.get()){
            return;
        }
        Adjust.disable();
    }

    public static void onResume() {
        if (automaticLifecycleHandlingEnabled.get()) {
            Log.w(TAG, "onResume ignored because automatic lifecycle handling is enabled");
            return;
        }
        doResume();
    }

    public static void onPause() {
        if (automaticLifecycleHandlingEnabled.get()) {
            Log.w(TAG, "onPause ignored because automatic lifecycle handling is enabled");
            return;
        }
        doPause();
    }

    static void onAutoResume() {
        doResume();
    }

    static void onAutoPause() {
        doPause();
    }

    static void setAutomaticLifecycleHandlingEnabled(boolean enabled) {
        automaticLifecycleHandlingEnabled.set(enabled);
    }

    private static void doResume() {
        try {
            Adjust.onResume();
        } catch (Exception e) {
            Log.e(TAG, "onResume error: " + e.getMessage(), e);
        }
    }

    private static void doPause() {
        try {
            Adjust.onPause();
        } catch (Exception e) {
            Log.e(TAG, "onPause error: " + e.getMessage(), e);
        }
    }

    private static void appendParams(AdjustEvent adjustEvent, Map<String, String> params, boolean callback) {
        if (params == null || params.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (isBlank(key) || value == null) {
                continue;
            }
            if (callback) {
                adjustEvent.addCallbackParameter(key, value);
            } else {
                adjustEvent.addPartnerParameter(key, value);
            }
        }
    }

    private static void dispatchToScript(String eventName, Object data) {
        try {
            if (data == null){
                JsbBridgeWrapper.getInstance().dispatchEventToScript(eventName, null);
            }else {
                if (data instanceof String){
                    JsbBridgeWrapper.getInstance().dispatchEventToScript(eventName, (String) data);
                }else {
                    String json = AdjustJsonUtil.toJson(data);
                    JsbBridgeWrapper.getInstance().dispatchEventToScript(eventName, json);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "dispatchToScript error: " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> createSingleValueMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private static Context getContext() {
       return AdjustCocosBridge.getContext();
    }

    private static AdjustConfig buildAdjustConfig(Context context, AdjustCocosConfig config) {
        boolean allowSuppressLogLevel = Boolean.TRUE.equals(config.allowSuppressLogLevel);
        AdjustConfig adjustConfig = new AdjustConfig(
                context,
                config.appToken,
                getProduct(config.environment),
                allowSuppressLogLevel
        );

        if (isNotBlank(config.logLevel)) {
            adjustConfig.setLogLevel(getLogLevel(config.logLevel));
        }
        if (isNotBlank(config.processName)) {
            adjustConfig.setProcessName(config.processName);
        }
        if (isNotBlank(config.defaultTracker)) {
            adjustConfig.setDefaultTracker(config.defaultTracker);
        }
        if (isNotBlank(config.externalDeviceId)) {
            adjustConfig.setExternalDeviceId(config.externalDeviceId);
        }
        if (config.urlStrategyDomains != null && !config.urlStrategyDomains.isEmpty()) {
            adjustConfig.setUrlStrategy(
                    config.urlStrategyDomains,
                    Boolean.TRUE.equals(config.useSubdomains),
                    Boolean.TRUE.equals(config.isDataResidency)
            );
        }
        if (Boolean.TRUE.equals(config.enableSendingInBackground)) {
            adjustConfig.enableSendingInBackground();
        }
        if (Boolean.TRUE.equals(config.enableCostDataInAttribution)) {
            adjustConfig.enableCostDataInAttribution();
        }
        if (Boolean.TRUE.equals(config.enableDeviceIdsReadingOnce)) {
            adjustConfig.enableDeviceIdsReadingOnce();
        }
        if (Boolean.TRUE.equals(config.enableCoppaCompliance)) {
            adjustConfig.enableCoppaCompliance();
        }
        if (config.eventDeduplicationIdsMaxSize != null) {
            adjustConfig.setEventDeduplicationIdsMaxSize(config.eventDeduplicationIdsMaxSize);
        }
        if (Boolean.TRUE.equals(config.enableFirstSessionDelay)) {
            adjustConfig.enableFirstSessionDelay();
        }
        if (Boolean.TRUE.equals(config.enablePreinstallTracking)) {
            adjustConfig.enablePreinstallTracking();
        }
        if (isNotBlank(config.preinstallFilePath)) {
            adjustConfig.setPreinstallFilePath(config.preinstallFilePath);
        }
        if (Boolean.TRUE.equals(config.enablePlayStoreKidsCompliance)) {
            adjustConfig.enablePlayStoreKidsCompliance();
        }
        if (isNotBlank(config.fbAppId)) {
            adjustConfig.setFbAppId(config.fbAppId);
        }
        if (Boolean.TRUE.equals(config.disableAppSetIdReading)) {
            adjustConfig.disableAppSetIdReading();
        }
        return adjustConfig;
    }

    private static void bindCallbacks(AdjustConfig adjustConfig, AdjustCocosConfig config) {
        adjustConfig.setOnSessionTrackingSucceededListener(success ->
                dispatchToScript(EVENT_SESSION_SUCCESS, AdjustJsonUtil.toSessionSuccessMap(success)));

        adjustConfig.setOnSessionTrackingFailedListener(failure ->
                dispatchToScript(EVENT_SESSION_FAIL, AdjustJsonUtil.toSessionFailureMap(failure)));

        adjustConfig.setOnEventTrackingSucceededListener(success ->
                dispatchToScript(EVENT_EVENT_SUCCESS, AdjustJsonUtil.toEventSuccessMap(success)));

        adjustConfig.setOnEventTrackingFailedListener(failure ->
                dispatchToScript(EVENT_EVENT_FAIL, AdjustJsonUtil.toEventFailureMap(failure)));

        adjustConfig.setOnAttributionChangedListener(attribution ->
                dispatchToScript(EVENT_ATTRIBUTION_CHANGED, AdjustJsonUtil.toAttributionMap(attribution)));

        adjustConfig.setOnDeferredDeeplinkResponseListener(deeplink -> {
            boolean shouldLaunch = !Boolean.FALSE.equals(config.launchDeferredDeeplink);
            dispatchToScript(EVENT_DEFERRED_DEEPLINK, createDeferredDeeplinkMap(deeplink, shouldLaunch));
            return shouldLaunch;
        });
    }

    private static String getProduct(String product) {
        if (isBlank(product)) {
            return AdjustConfig.ENVIRONMENT_SANDBOX;
        }
        return AdjustConfig.ENVIRONMENT_PRODUCTION.equalsIgnoreCase(product)
                ? AdjustConfig.ENVIRONMENT_PRODUCTION
                : AdjustConfig.ENVIRONMENT_SANDBOX;
    }

    private static LogLevel getLogLevel(String logLevel) {
        if (isBlank(logLevel)) {
            return LogLevel.INFO;
        }
        switch (logLevel.trim().toUpperCase()) {
            case "VERBOSE":
                return LogLevel.VERBOSE;
            case "DEBUG":
                return LogLevel.DEBUG;
            case "WARN":
                return LogLevel.WARN;
            case "ERROR":
                return LogLevel.ERROR;
            case "ASSERT":
                return LogLevel.ASSERT;
            case "SUPPRESS":
                return LogLevel.SUPPRESS;
            default:
                return LogLevel.INFO;
        }
    }

    private static boolean isBlank(String string) {
        return string == null || string.trim().isEmpty();
    }

    private static boolean isNotBlank(String string) {
        return !isBlank(string);
    }

    private static AdjustCocosEventPayload parseEventPayload(String json) {
        JSONObject object = parseJsonObject(json);
        if (object == null) {
            return null;
        }

        AdjustCocosEventPayload payload = new AdjustCocosEventPayload();
        payload.eventToken = object.optString("eventToken", null);
        payload.revenue = object.has("revenue") && !object.isNull("revenue") ? object.optDouble("revenue") : null;
        payload.currency = object.optString("currency", null);
        payload.orderId = object.optString("orderId", null);
        payload.deduplicationId = object.optString("deduplicationId", null);
        payload.callbackId = object.optString("callbackId", null);
        payload.productId = object.optString("productId", null);
        payload.callbackParams = parseStringMap(object.optJSONObject("callbackParams"));
        payload.partnerParams = parseStringMap(object.optJSONObject("partnerParams"));
        return payload;
    }

    private static AdjustCocosAdRevenuePayload parseAdRevenuePayload(String json) {
        JSONObject object = parseJsonObject(json);
        if (object == null) {
            return null;
        }

        AdjustCocosAdRevenuePayload payload = new AdjustCocosAdRevenuePayload();
        payload.source = object.optString("source", null);
        payload.revenue = object.has("revenue") && !object.isNull("revenue") ? object.optDouble("revenue") : null;
        payload.currency = object.optString("currency", null);
        payload.adImpressionsCount = object.has("adImpressionsCount") && !object.isNull("adImpressionsCount")
                ? object.optInt("adImpressionsCount")
                : null;
        payload.adRevenueNetwork = object.optString("adRevenueNetwork", null);
        payload.adRevenueUnit = object.optString("adRevenueUnit", null);
        payload.adRevenuePlacement = object.optString("adRevenuePlacement", null);
        payload.callbackParameters = parseStringMap(object.optJSONObject("callbackParameters"));
        payload.partnerParameters = parseStringMap(object.optJSONObject("partnerParameters"));
        return payload;
    }

    private static AdjustPlayStorePurchasePayload parsePlayStorePurchasePayload(String json) {
        JSONObject object = parseJsonObject(json);
        if (object == null) {
            return null;
        }

        AdjustPlayStorePurchasePayload payload = new AdjustPlayStorePurchasePayload();
        payload.productId = object.optString("productId", null);
        payload.purchaseToken = object.optString("purchaseToken", null);
        return payload;
    }

    private static JSONObject parseJsonObject(String json) {
        if (isBlank(json)) {
            return null;
        }
        try {
            return new JSONObject(json);
        } catch (Exception e) {
            Log.e(TAG, "parse json error: " + e.getMessage(), e);
            return null;
        }
    }

    private static Map<String, String> parseStringMap(JSONObject object) {
        if (object == null) {
            return null;
        }

        Map<String, String> result = new HashMap<>();
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (isBlank(key) || object.isNull(key)) {
                continue;
            }
            result.put(key, String.valueOf(object.opt(key)));
        }
        return result;
    }

    static AdjustCocosConfig parseConfigPayload(String json) {
        JSONObject object = parseJsonObject(json);
        if (object == null) {
            return null;
        }

        AdjustCocosConfig config = new AdjustCocosConfig();
        config.appToken = object.optString("appToken", null);
        config.environment = object.optString("environment", object.optString("product", null));
        config.launchDeferredDeeplink = optBoolean(object, "launchDeferredDeeplink");
        config.allowSuppressLogLevel = optBoolean(object, "allowSuppressLogLevel");
        config.enableSendingInBackground = optBoolean(object, "enableSendingInBackground");
        config.enableCostDataInAttribution = optBoolean(object, "enableCostDataInAttribution");
        config.processName = object.optString("processName", null);
        config.defaultTracker = object.optString("defaultTracker", null);
        config.externalDeviceId = object.optString("externalDeviceId", null);
        config.urlStrategyDomains = parseStringList(object.optJSONArray("urlStrategyDomains"));
        config.useSubdomains = optBoolean(object, "useSubdomains");
        config.isDataResidency = optBoolean(object, "isDataResidency");
        config.logLevel = object.optString("logLevel", null);
        config.enableDeviceIdsReadingOnce = optBoolean(object, "enableDeviceIdsReadingOnce");
        config.enableCoppaCompliance = optBoolean(object, "enableCoppaCompliance");
        config.eventDeduplicationIdsMaxSize = object.has("eventDeduplicationIdsMaxSize") && !object.isNull("eventDeduplicationIdsMaxSize")
                ? object.optInt("eventDeduplicationIdsMaxSize")
                : null;
        config.enableFirstSessionDelay = optBoolean(object, "enableFirstSessionDelay");
        config.enablePreinstallTracking = optBoolean(object, "enablePreinstallTracking");
        config.preinstallFilePath = object.optString("preinstallFilePath", null);
        config.enablePlayStoreKidsCompliance = optBoolean(object, "enablePlayStoreKidsCompliance");
        config.fbAppId = object.optString("fbAppId", null);
        config.disableAppSetIdReading = optBoolean(object, "disableAppSetIdReading");
        return config;
    }

    private static Boolean optBoolean(JSONObject object, String key) {
        if (!object.has(key) || object.isNull(key)) {
            return null;
        }
        return object.optBoolean(key);
    }

    private static List<String> parseStringList(org.json.JSONArray array) {
        if (array == null || array.length() == 0) {
            return null;
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, null);
            if (isNotBlank(value)) {
                result.add(value);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private static Map<String, Object> createDeferredDeeplinkMap(Uri deeplink, boolean shouldLaunch) {
        Map<String, Object> result = new HashMap<>();
        result.put("deeplink", deeplink != null ? deeplink.toString() : null);
        result.put("shouldLaunch", shouldLaunch);
        return result;
    }

    private static Map<String, Object> createPurchaseVerificationResult(AdjustPurchaseVerificationResult result) {
        Map<String, Object> payload = new HashMap<>();
        if (result == null) {
            payload.put("verificationStatus", null);
            payload.put("code", -1);
            payload.put("message", null);
            return payload;
        }
        payload.put("verificationStatus", result.getVerificationStatus());
        payload.put("code", result.getCode());
        payload.put("message", result.getMessage());
        return payload;
    }

    private static class AdjustPlayStorePurchasePayload {
        String productId;
        String purchaseToken;
    }
}
