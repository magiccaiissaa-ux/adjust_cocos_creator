# Adjust for Cocos Creator

[中文说明](./README.zh-CN.md)

A reusable Adjust wrapper for Cocos Creator Android projects.

This repository includes:

- TypeScript-side wrapper code under `assets/script/adjust/`
- Android native bridge module under `adjust_cocos_creator/`
- A prebuilt Android package: `adjust_cocos_creator-release.aar`

## Quick Integration

There are two integration options.

### Option 1. Reference the prebuilt AAR

Recommended when you only want to reuse the bridge quickly.

1. Add `adjust_cocos_creator-release.aar` to your Android project.
2. Copy `assets/script/adjust/` into your Cocos Creator project.
3. Add the Adjust Android SDK dependency:

```gradle
implementation 'com.adjust.sdk:adjust-android:5.6.0'
```

4. Import and initialize `AdjustManager` in your startup code.

```ts
import { AdjustManager } from './adjust/adjust_manager';

AdjustManager.init({
    appToken: 'your_app_token',
    product: 'production',
    logLevel: 'INFO',
});
```

### Option 2. Reference the source module

Recommended when you want to keep the Android bridge source in your project.

1. Reference `adjust_cocos_creator/` as an Android library module.
2. Copy `assets/script/adjust/` into your Cocos Creator project.
3. Ensure the module can access your Cocos Android environment.
4. Add the Adjust Android SDK dependency:

```gradle
implementation 'com.adjust.sdk:adjust-android:5.6.0'
```

5. Initialize `AdjustManager` from TypeScript.

## Repository Structure

- `assets/script/adjust/adjust_manager.ts`
  Main API used by game code.
- `assets/script/adjust/adjust_config.ts`
  Adjust init config definitions.
- `assets/script/adjust/adjust_types.ts`
  Shared types for payloads and callback results.
- `assets/script/adjust/adjust_message_dispatcher.ts`
  JS/native event dispatch and async request handling.
- `adjust_cocos_creator/src/main/java/com/game/magic/AdjustCocos.java`
  Native Adjust bridge implementation.
- `adjust_cocos_creator/src/main/java/com/game/magic/AdjustCocosBridge.java`
  JSB bridge registration.
- `adjust_cocos_creator/src/main/java/com/game/magic/AdjustCocosInit.java`
  Auto bridge bootstrap entry.

## How It Works

This wrapper uses two paths:

1. Dispatch path
   For APIs that need native callbacks or async results.
2. Reflection path
   For simple static Android Adjust APIs called through `native.reflection`.

The bridge is initialized from TypeScript through `AdjustManager.initBridge()` and `AdjustManager.init(...)`.

## Supported Features

- SDK initialization with `AdjustConfig`
- Event tracking
- Ad revenue tracking
- Attribution, adid, install referrer, deeplink, and sdk version reads
- Deferred deeplink callback forwarding
- Google Play purchase verification
- Direct helper calls to selected static Adjust APIs

## Basic Usage

### Initialize

```ts
import { AdjustManager } from './adjust/adjust_manager';

AdjustManager.init({
    appToken: 'your_app_token',
    product: 'production',
    logLevel: 'INFO',
});
```

### Track an event

```ts
AdjustManager.trackEvent({
    eventToken: 'abc123',
    revenue: 1.99,
    currency: 'USD',
    orderId: 'order_001',
});
```

### Read data with timeout

```ts
const adid = await AdjustManager.getAdidWithTimeout(3000);
const attribution = await AdjustManager.getAttributionWithTimeout(3000);
const googleAdId = await AdjustManager.getGoogleAdIdWithTimeout(3000);
```

### Track ad revenue

```ts
AdjustManager.trackAdRevenue({
    source: 'admob_sdk',
    revenue: 0.12,
    currency: 'USD',
    adRevenueNetwork: 'admob',
    adRevenueUnit: 'rewarded',
    adRevenuePlacement: 'level_complete',
});
```

## Notes

- This wrapper is designed for Android native integration.
- Reflection helpers require Android runtime and `native.reflection`.
- Deferred deeplink native return flow cannot be synchronously controlled by TypeScript.
- The native module currently depends on `com.adjust.sdk:adjust-android:5.6.0`.

## Validation

The Android native module was validated with:

```powershell
./gradlew.bat :adjust_cocos_creator:compileDebugJavaWithJavac
```
