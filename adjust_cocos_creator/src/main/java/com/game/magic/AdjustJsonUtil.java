package com.game.magic;

import android.util.Log;

import com.adjust.sdk.AdjustAttribution;
import com.adjust.sdk.AdjustEventFailure;
import com.adjust.sdk.AdjustEventSuccess;
import com.adjust.sdk.AdjustSessionFailure;
import com.adjust.sdk.AdjustSessionSuccess;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AdjustJsonUtil {

    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            Object jsonValue = toJsonValue(object);
            if (jsonValue == null || jsonValue == JSONObject.NULL) {
                return null;
            }
            if (jsonValue instanceof JSONObject || jsonValue instanceof JSONArray) {
                return jsonValue.toString();
            }
            return jsonValue.toString();
        } catch (Exception e) {
            Log.e("AdjustJsonUtil", "toJson error " + e.getMessage(), e);
            return null;
        }
    }

    public static Map<String, Object> toSessionSuccessMap(AdjustSessionSuccess success) {
        Map<String, Object> result = new HashMap<>();
        if (success == null) {
            return result;
        }
        result.put("message", success.message);
        result.put("timestamp", success.timestamp);
        result.put("adid", success.adid);
        result.put("jsonResponse", jsonObjectToMap(success.jsonResponse));
        return result;
    }

    public static Map<String, Object> toSessionFailureMap(AdjustSessionFailure failure) {
        Map<String, Object> result = new HashMap<>();
        if (failure == null) {
            return result;
        }
        result.put("message", failure.message);
        result.put("timestamp", failure.timestamp);
        result.put("adid", failure.adid);
        result.put("willRetry", failure.willRetry);
        result.put("jsonResponse", jsonObjectToMap(failure.jsonResponse));
        return result;
    }

    public static Map<String, Object> toEventSuccessMap(AdjustEventSuccess success) {
        Map<String, Object> result = new HashMap<>();
        if (success == null) {
            return result;
        }
        result.put("message", success.message);
        result.put("timestamp", success.timestamp);
        result.put("adid", success.adid);
        result.put("eventToken", success.eventToken);
        result.put("callbackId", success.callbackId);
        result.put("jsonResponse", jsonObjectToMap(success.jsonResponse));
        return result;
    }

    public static Map<String, Object> toEventFailureMap(AdjustEventFailure failure) {
        Map<String, Object> result = new HashMap<>();
        if (failure == null) {
            return result;
        }
        result.put("message", failure.message);
        result.put("timestamp", failure.timestamp);
        result.put("adid", failure.adid);
        result.put("eventToken", failure.eventToken);
        result.put("callbackId", failure.callbackId);
        result.put("willRetry", failure.willRetry);
        result.put("jsonResponse", jsonObjectToMap(failure.jsonResponse));
        return result;
    }

    public static Map<String, Object> toAttributionMap(AdjustAttribution attribution) {
        Map<String, Object> result = new HashMap<>();
        if (attribution == null) {
            return result;
        }
        result.put("trackerToken", attribution.trackerToken);
        result.put("trackerName", attribution.trackerName);
        result.put("network", attribution.network);
        result.put("campaign", attribution.campaign);
        result.put("adgroup", attribution.adgroup);
        result.put("creative", attribution.creative);
        result.put("clickLabel", attribution.clickLabel);
        result.put("costType", attribution.costType);
        result.put("costAmount", attribution.costAmount);
        result.put("costCurrency", attribution.costCurrency);
        result.put("fbInstallReferrer", attribution.fbInstallReferrer);
        result.put("jsonResponse", attribution.jsonResponse);
        return result;
    }

    public static Map<String, Object> toInstallReferrerMap(AdjustInstallReferrerDetails details) {
        Map<String, Object> result = new HashMap<>();
        if (details == null) {
            return result;
        }
        result.put("success", details.isSuccess());
        result.put("error", details.getMessage());

        if (details.isSuccess()) {
            Map<String, Object> detailMap = new HashMap<>();
            detailMap.put("installReferrer", details.getInstallReferrer());
            detailMap.put("referrerClickTimestampSeconds", details.getReferrerClickTimestampSeconds());
            detailMap.put("installBeginTimestampSeconds", details.getInstallBeginTimestampSeconds());
            detailMap.put("referrerClickTimestampServerSeconds", details.getReferrerClickTimestampServerSeconds());
            detailMap.put("installBeginTimestampServerSeconds", details.getInstallBeginTimestampServerSeconds());
            detailMap.put("installVersion", details.getInstallVersion());
            detailMap.put("googlePlayInstantParam", details.getGooglePlayInstant());
            result.put("details", detailMap);
        } else {
            result.put("details", null);
        }
        return result;
    }



    private static Map<String, Object> jsonObjectToMap(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        try {
            Map<String, Object> result = new HashMap<>();
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                result.put(key, fromJsonValue(jsonObject.opt(key)));
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static Object fromJsonValue(Object value) {
        if (value == null || value == JSONObject.NULL) {
            return null;
        }
        if (value instanceof JSONObject) {
            return jsonObjectToMap((JSONObject) value);
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            java.util.ArrayList<Object> list = new java.util.ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                list.add(fromJsonValue(array.opt(i)));
            }
            return list;
        }
        return value;
    }

    private static Object toJsonValue(Object object) {
        if (object == null) {
            return JSONObject.NULL;
        }
        if (object instanceof JSONObject || object instanceof JSONArray) {
            return object;
        }
        try {
            if (object instanceof Map<?, ?>) {
                JSONObject jsonObject = new JSONObject();
                Map<?, ?> map = (Map<?, ?>) object;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    jsonObject.put(key, toJsonValue(entry.getValue()));
                }
                return jsonObject;
            }
            if (object instanceof Iterable<?>) {
                JSONArray jsonArray = new JSONArray();
                for (Object item : (Iterable<?>) object) {
                    jsonArray.put(toJsonValue(item));
                }
                return jsonArray;
            }
            if (object.getClass().isArray()) {
                JSONArray jsonArray = new JSONArray();
                int length = Array.getLength(object);
                for (int i = 0; i < length; i++) {
                    jsonArray.put(toJsonValue(Array.get(object, i)));
                }
                return jsonArray;
            }
            if (object instanceof String || object instanceof Boolean) {
                return object;
            }
            if (object instanceof Double) {
                return sanitizeDouble((Double) object);
            }
            if (object instanceof Float) {
                return sanitizeFloat((Float) object);
            }
            if (object instanceof Number) {
                return object;
            }
            if (object instanceof AdjustInstallReferrerDetails) {
                return toJsonValue(toInstallReferrerMap((AdjustInstallReferrerDetails) object));
            }
        }catch (Exception e){
            Log.e("AdjustJsonUtil", "toJsonValue error " + e.getMessage(), e);
        }

        return String.valueOf(object);
    }

    private static Object sanitizeDouble(Double value) {
        if (value == null) {
            return JSONObject.NULL;
        }
        if (value.isNaN() || value.isInfinite()) {
            return JSONObject.NULL;
        }
        return value;
    }

    private static Object sanitizeFloat(Float value) {
        if (value == null) {
            return JSONObject.NULL;
        }
        if (value.isNaN() || value.isInfinite()) {
            return JSONObject.NULL;
        }
        return value;
    }
}
