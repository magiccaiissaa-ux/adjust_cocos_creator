import { native, sys } from 'cc';
import { createAdjustPlatformAdapter, type AdjustPlatformAdapter } from './adjust_adapter';
import { AdjustConfig, normalizeAdjustConfig } from './adjust_config';
import { AdjustInitListeners, AdjustMessageDispatcher } from './adjust_message_dispatcher';
import {
    AdjustAdRevenuePayload,
    AdjustAttribution,
    AdjustDeferredDeeplink,
    AdjustEventFailure,
    AdjustEventPayload,
    AdjustEventSuccess,
    AdjustGoogleAdId,
    AdjustInstallReferrerDetails,
    AdjustInstallReferrerResult,
    AdjustAmazonAdId,
    AdjustLastDeeplink,
    AdjustPlayStorePurchasePayload,
    AdjustPurchaseVerificationResult,
    AdjustSdkVersion,
    AdjustSessionFailure,
    AdjustSessionSuccess,
    NativeListener,
} from './adjust_types';

export interface AdjustInitOptions extends AdjustConfig {}
export type AdjustRuntimePlatform = 'android' | 'ios' | 'web' | 'unsupported';

export class AdjustManager {
    private static _inited = false;
    private static _bridgeBootstrapped = false;
    private static _nativeListenersRegistered = false;
    private static _adapter: AdjustPlatformAdapter | null = null;
    private static readonly warnedMessages = new Set<string>();
    private static readonly DEFAULT_TIMEOUT_MS = 5000;
    private static readonly WEB_CALLBACK_DELAY_MS = 0;
    private static readonly dispatcher = new AdjustMessageDispatcher(this.WEB_CALLBACK_DELAY_MS);

    static readonly EVENT_SESSION_SUCCESS = 'adjustOnSessionTrackingSucceeded';
    static readonly EVENT_SESSION_FAIL = 'adjustOnSessionTrackingFailed';
    static readonly EVENT_EVENT_SUCCESS = 'adjustOnEventTrackingSucceeded';
    static readonly EVENT_EVENT_FAIL = 'adjustOnEventTrackingFailed';
    static readonly EVENT_ATTRIBUTION_CHANGED = 'adjustOnAttributionChanged';
    static readonly EVENT_DEFERRED_DEEPLINK = 'adjustOnDeferredDeeplinkResponse';
    static readonly EVENT_ADID_READ = 'adjustOnAdidRead';
    static readonly EVENT_GOOGLE_ADID_READ = 'adjustOnGoogleAdIdRead';
    static readonly EVENT_AMAZON_ADID_READ = 'adjustOnAmazonAdIdRead';
    static readonly EVENT_LAST_DEEPLINK_READ = 'adjustOnLastDeeplinkRead';
    static readonly EVENT_SDK_VERSION_READ = 'adjustOnSdkVersionRead';
    static readonly EVENT_PLAY_STORE_PURCHASE_VERIFIED = 'adjustOnPlayStorePurchaseVerified';
    static readonly EVENT_INSTALL_REFERRER = 'adjustInstallReferrerRead';

    private static readonly NATIVE_JSON_EVENTS = [
        this.EVENT_SESSION_SUCCESS,
        this.EVENT_SESSION_FAIL,
        this.EVENT_EVENT_SUCCESS,
        this.EVENT_EVENT_FAIL,
        this.EVENT_ATTRIBUTION_CHANGED,
        this.EVENT_DEFERRED_DEEPLINK,
        this.EVENT_ADID_READ,
        this.EVENT_GOOGLE_ADID_READ,
        this.EVENT_AMAZON_ADID_READ,
        this.EVENT_LAST_DEEPLINK_READ,
        this.EVENT_SDK_VERSION_READ,
        this.EVENT_PLAY_STORE_PURCHASE_VERIFIED,
        this.EVENT_INSTALL_REFERRER,
    ] as const;

    static getRuntimePlatform(): AdjustRuntimePlatform {
        if (!sys.isNative) {
            return 'web';
        }
        if (sys.os === sys.OS.ANDROID) {
            return 'android';
        }
        if (sys.os === sys.OS.IOS) {
            return 'ios';
        }
        return 'unsupported';
    }

    static isPlatformImplemented(): boolean {
        return this.getAdapter().implemented;
    }

    static isBridgeReady(): boolean {
        return !sys.isNative || this._bridgeBootstrapped;
    }

    static initBridge() {
        if (this._inited && (!sys.isNative || this._bridgeBootstrapped)) {
            return;
        }

        if (sys.isNative) {
            this._bridgeBootstrapped = this.initNativeBridge() || this._bridgeBootstrapped;
            if (!this._bridgeBootstrapped) {
                return;
            }
            this.registerNativeListeners();
        }
        this._inited = true;
    }

    static init(options: AdjustInitOptions) {
        this.initBridge();
        if (sys.isNative && !this._bridgeBootstrapped) {
            this.warnOnce(`[AdjustManager] init skipped because native bridge is not ready on ${this.getRuntimePlatform()}`);
            return;
        }
        this.bindInitListeners(options);
        if (!sys.isNative) {
            return;
        }
        this.dispatchToNative('adjustInitConfig', JSON.stringify(normalizeAdjustConfig(options)));
    }

    static trackEvent(payload: AdjustEventPayload) {
        if (!sys.isNative) {
            return;
        }
        this.dispatchToNative('adjustTrackEvent', JSON.stringify(payload));
    }

    static trackAdRevenue(payload: AdjustAdRevenuePayload) {
        if (!sys.isNative) {
            return;
        }
        this.dispatchToNative('adjustTrackAdRevenue', JSON.stringify(payload));
    }

    static openDeeplink(url: string) {
        if (!sys.isNative) {
            return;
        }
        this.dispatchToNative('adjustOpenDeeplink', url);
    }
    static getAdid() {
        if (!sys.isNative) {
            this.dispatcher.emitAsync(this.EVENT_ADID_READ, null);
            return;
        }
        this.dispatchToNative('adjustGetAdid', '');
    }

    static async getAdidWithTimeout(timeoutMs: number = this.DEFAULT_TIMEOUT_MS): Promise<{ adid?: string } | null> {
        return this._requestOnceWithTimeout<{ adid?: string } | null>(
            this.EVENT_ADID_READ,
            () => this.getAdid(),
            timeoutMs,
        );
    }

    static getAttribution() {
        if (!sys.isNative) {
            this.dispatcher.emitAsync(this.EVENT_ATTRIBUTION_CHANGED, null);
            return;
        }
        this.dispatchToNative('adjustGetAttribution', '');
    }

    static async getAttributionWithTimeout(timeoutMs: number = this.DEFAULT_TIMEOUT_MS): Promise<AdjustAttribution | null> {
        return this._requestOnceWithTimeout<AdjustAttribution | null>(
            this.EVENT_ATTRIBUTION_CHANGED,
            () => this.getAttribution(),
            timeoutMs,
        );
    }

    static getInstallReferrer() {
        if (!sys.isNative) {
            this.dispatcher.emitAsync(this.EVENT_INSTALL_REFERRER, null);
            return;
        }
        this.dispatchToNative('adjustGetInstallReferrer', '');
    }

    static async getInstallReferrerWithTimeout(timeoutMs: number = this.DEFAULT_TIMEOUT_MS): Promise<AdjustInstallReferrerResult | null> {
        return this._requestOnceWithTimeout<AdjustInstallReferrerResult | null>(
            this.EVENT_INSTALL_REFERRER,
            () => this.getInstallReferrer(),
            timeoutMs,
        );
    }

    static getGoogleAdId() {
        if (!sys.isNative) {
            this.dispatcher.emitAsync(this.EVENT_GOOGLE_ADID_READ, null);
            return;
        }
        this.dispatchToNative('adjustGetGoogleAdId', '');
    }

    static async getGoogleAdIdWithTimeout(timeoutMs: number = this.DEFAULT_TIMEOUT_MS): Promise<AdjustGoogleAdId | null> {
        return this._requestOnceWithTimeout<AdjustGoogleAdId | null>(
            this.EVENT_GOOGLE_ADID_READ,
            () => this.getGoogleAdId(),
            timeoutMs,
        );
    }

    static getAmazonAdId() {
        if (!sys.isNative) {
            this.dispatcher.emitAsync(this.EVENT_AMAZON_ADID_READ, null);
            return;
        }
        this.dispatchToNative('adjustGetAmazonAdId', '');
    }

    static async getAmazonAdIdWithTimeout(timeoutMs: number = this.DEFAULT_TIMEOUT_MS): Promise<AdjustAmazonAdId | null> {
        return this._requestOnceWithTimeout<AdjustAmazonAdId | null>(
            this.EVENT_AMAZON_ADID_READ,
            () => this.getAmazonAdId(),
            timeoutMs,
        );
    }

    static getLastDeeplink() {
        if (!sys.isNative) {
            this.dispatcher.emitAsync(this.EVENT_LAST_DEEPLINK_READ, null);
            return;
        }
        this.dispatchToNative('adjustGetLastDeeplink', '');
    }

    static async getLastDeeplinkWithTimeout(timeoutMs: number = this.DEFAULT_TIMEOUT_MS): Promise<AdjustLastDeeplink | null> {
        return this._requestOnceWithTimeout<AdjustLastDeeplink | null>(
            this.EVENT_LAST_DEEPLINK_READ,
            () => this.getLastDeeplink(),
            timeoutMs,
        );
    }

    static getSdkVersion() {
        if (!sys.isNative) {
            this.dispatcher.emitAsync(this.EVENT_SDK_VERSION_READ, null);
            return;
        }
        this.dispatchToNative('adjustGetSdkVersion', '');
    }

    static async getSdkVersionWithTimeout(timeoutMs: number = this.DEFAULT_TIMEOUT_MS): Promise<AdjustSdkVersion | null> {
        return this._requestOnceWithTimeout<AdjustSdkVersion | null>(
            this.EVENT_SDK_VERSION_READ,
            () => this.getSdkVersion(),
            timeoutMs,
        );
    }

    static verifyPlayStorePurchase(payload: AdjustPlayStorePurchasePayload) {
        if (!sys.isNative) {
            this.dispatcher.emitAsync(this.EVENT_PLAY_STORE_PURCHASE_VERIFIED, null);
            return;
        }
        this.dispatchToNative('adjustVerifyPlayStorePurchase', JSON.stringify(payload));
    }

    static async verifyPlayStorePurchaseWithTimeout(
        payload: AdjustPlayStorePurchasePayload,
        timeoutMs: number = this.DEFAULT_TIMEOUT_MS,
    ): Promise<AdjustPurchaseVerificationResult | null> {
        return this._requestOnceWithTimeout<AdjustPurchaseVerificationResult | null>(
            this.EVENT_PLAY_STORE_PURCHASE_VERIFIED,
            () => this.verifyPlayStorePurchase(payload),
            timeoutMs,
        );
    }

    static enable() {
        this.callDirect('enable');
    }

    // These APIs map directly to static methods on com.adjust.sdk.Adjust.
    static disable() {
        this.callDirect('disable');
    }

    static switchToOfflineMode() {
        this.callDirect('switchToOfflineMode');
    }

    static switchBackToOnlineMode() {
        this.callDirect('switchBackToOnlineMode');
    }

    static addGlobalCallbackParameter(key: string, value: string) {
        this.callDirect('addGlobalCallbackParameter', key, value);
    }

    static addGlobalPartnerParameter(key: string, value: string) {
        this.callDirect('addGlobalPartnerParameter', key, value);
    }

    static removeGlobalCallbackParameter(key: string) {
        this.callDirect('removeGlobalCallbackParameter', key);
    }

    static removeGlobalPartnerParameter(key: string) {
        this.callDirect('removeGlobalPartnerParameter', key);
    }

    static removeGlobalCallbackParameters() {
        this.callDirect('removeGlobalCallbackParameters');
    }

    static removeGlobalPartnerParameters() {
        this.callDirect('removeGlobalPartnerParameters');
    }

    static endFirstSessionDelay() {
        this.callDirect('endFirstSessionDelay');
    }

    static enableCoppaComplianceInDelay() {
        this.callDirect('enableCoppaComplianceInDelay');
    }

    static disableCoppaComplianceInDelay() {
        this.callDirect('disableCoppaComplianceInDelay');
    }

    static enablePlayStoreKidsComplianceInDelay() {
        this.callDirect('enablePlayStoreKidsComplianceInDelay');
    }

    static disablePlayStoreKidsComplianceInDelay() {
        this.callDirect('disablePlayStoreKidsComplianceInDelay');
    }

    static setExternalDeviceIdInDelay(externalDeviceId: string) {
        this.callDirect('setExternalDeviceIdInDelay', externalDeviceId);
    }

    static onResume() {
        if (!sys.isNative) {
            return;
        }
        this.dispatchToNative('adjustOnResume', '');
    }

    static onPause() {
        if (!sys.isNative) {
            return;
        }
        this.dispatchToNative('adjustOnPause', '');
    }

    static onSessionTrackingSucceeded(listener: (data: AdjustSessionSuccess) => void) {
        this.on(this.EVENT_SESSION_SUCCESS, listener as NativeListener);
    }

    static onSessionTrackingFailed(listener: (data: AdjustSessionFailure) => void) {
        this.on(this.EVENT_SESSION_FAIL, listener as NativeListener);
    }

    static onEventTrackingSucceeded(listener: (data: AdjustEventSuccess) => void) {
        this.on(this.EVENT_EVENT_SUCCESS, listener as NativeListener);
    }

    static onEventTrackingFailed(listener: (data: AdjustEventFailure) => void) {
        this.on(this.EVENT_EVENT_FAIL, listener as NativeListener);
    }

    static onAttributionChanged(listener: (data: AdjustAttribution) => void) {
        this.on(this.EVENT_ATTRIBUTION_CHANGED, listener as NativeListener);
    }

    static onDeferredDeeplinkResponse(listener: (data: AdjustDeferredDeeplink) => void) {
        this.on(this.EVENT_DEFERRED_DEEPLINK, listener as NativeListener);
    }

    static onAdidRead(listener: (data: { adid?: string }) => void) {
        this.on(this.EVENT_ADID_READ, listener as NativeListener);
    }

    static onGoogleAdIdRead(listener: (data: AdjustGoogleAdId) => void) {
        this.on(this.EVENT_GOOGLE_ADID_READ, listener as NativeListener);
    }

    static onAmazonAdIdRead(listener: (data: AdjustAmazonAdId) => void) {
        this.on(this.EVENT_AMAZON_ADID_READ, listener as NativeListener);
    }

    static onLastDeeplinkRead(listener: (data: AdjustLastDeeplink) => void) {
        this.on(this.EVENT_LAST_DEEPLINK_READ, listener as NativeListener);
    }

    static onSdkVersionRead(listener: (data: AdjustSdkVersion) => void) {
        this.on(this.EVENT_SDK_VERSION_READ, listener as NativeListener);
    }

    static onPlayStorePurchaseVerified(listener: (data: AdjustPurchaseVerificationResult) => void) {
        this.on(this.EVENT_PLAY_STORE_PURCHASE_VERIFIED, listener as NativeListener);
    }

    static onInstallReferrerRead(listener: (data: AdjustInstallReferrerResult) => void) {
        this.on(this.EVENT_INSTALL_REFERRER, listener as NativeListener);
    }

    static offSessionTrackingSucceeded(listener: (data: AdjustSessionSuccess) => void) {
        this.off(this.EVENT_SESSION_SUCCESS, listener as NativeListener);
    }

    static offSessionTrackingFailed(listener: (data: AdjustSessionFailure) => void) {
        this.off(this.EVENT_SESSION_FAIL, listener as NativeListener);
    }

    static offEventTrackingSucceeded(listener: (data: AdjustEventSuccess) => void) {
        this.off(this.EVENT_EVENT_SUCCESS, listener as NativeListener);
    }

    static offEventTrackingFailed(listener: (data: AdjustEventFailure) => void) {
        this.off(this.EVENT_EVENT_FAIL, listener as NativeListener);
    }

    static offAttributionChanged(listener: (data: AdjustAttribution) => void) {
        this.off(this.EVENT_ATTRIBUTION_CHANGED, listener as NativeListener);
    }

    static offDeferredDeeplinkResponse(listener: (data: AdjustDeferredDeeplink) => void) {
        this.off(this.EVENT_DEFERRED_DEEPLINK, listener as NativeListener);
    }

    static offAdidRead(listener: (data: { adid?: string }) => void) {
        this.off(this.EVENT_ADID_READ, listener as NativeListener);
    }

    static offGoogleAdIdRead(listener: (data: AdjustGoogleAdId) => void) {
        this.off(this.EVENT_GOOGLE_ADID_READ, listener as NativeListener);
    }

    static offAmazonAdIdRead(listener: (data: AdjustAmazonAdId) => void) {
        this.off(this.EVENT_AMAZON_ADID_READ, listener as NativeListener);
    }

    static offLastDeeplinkRead(listener: (data: AdjustLastDeeplink) => void) {
        this.off(this.EVENT_LAST_DEEPLINK_READ, listener as NativeListener);
    }

    static offSdkVersionRead(listener: (data: AdjustSdkVersion) => void) {
        this.off(this.EVENT_SDK_VERSION_READ, listener as NativeListener);
    }

    static offPlayStorePurchaseVerified(listener: (data: AdjustPurchaseVerificationResult) => void) {
        this.off(this.EVENT_PLAY_STORE_PURCHASE_VERIFIED, listener as NativeListener);
    }

    static offInstallReferrerRead(listener: (data: AdjustInstallReferrerResult) => void) {
        this.off(this.EVENT_INSTALL_REFERRER, listener as NativeListener);
    }

    private static on(event: string, listener: NativeListener) {
        this.dispatcher.on(event, listener);
    }

    private static off(event: string, listener: NativeListener) {
        this.dispatcher.off(event, listener);
    }

    private static _requestOnceWithTimeout<T>(
        event: string,
        request: () => void,
        timeoutMs: number,
    ): Promise<T> {
        this.initBridge();
        return this.dispatcher.requestOnceWithTimeout<T>(event, request, timeoutMs, sys.isNative);
    }

    // The Android bridge is initialized by reflection so app-side Java glue is not required.
    private static initNativeBridge(): boolean {
        if (this._bridgeBootstrapped) {
            return true;
        }
        try {
            return this.getAdapter().initBridge();
        } catch (e) {
            console.error('[AdjustManager] init native bridge failed', e);
            return false;
        }
    }

    private static registerNativeListeners() {
        if (this._nativeListenersRegistered) {
            return;
        }
        for (const event of this.NATIVE_JSON_EVENTS) {
            this.dispatcher.addNativeJsonListener(event, (listener) => {
                this.getAdapter().addNativeEventListener(event, listener);
            });
        }
        this._nativeListenersRegistered = true;
    }

    private static dispatchToNative(event: string, payload: string) {
        this.getAdapter().dispatchToNative(event, payload);
    }

    private static callDirect(methodName: Parameters<AdjustPlatformAdapter['invokeDirect']>[0], ...args: string[]) {
        try {
            this.getAdapter().invokeDirect(methodName, ...args);
        } catch (e) {
            console.error(`[AdjustManager] call ${methodName} failed`, e);
        }
    }

    private static warnOnce(message: string) {
        if (this.warnedMessages.has(message)) {
            return;
        }
        this.warnedMessages.add(message);
        console.warn(message);
    }

    private static getAdapter(): AdjustPlatformAdapter {
        const platform = this.getRuntimePlatform();
        if (!this._adapter || this._adapter.platform !== platform) {
            this._adapter = createAdjustPlatformAdapter(platform, (message) => this.warnOnce(message));
        }
        return this._adapter;
    }

    private static bindInitListeners(options: AdjustInitListeners) {
        if (options.onSessionTrackingSucceeded) {
            this.onSessionTrackingSucceeded(options.onSessionTrackingSucceeded);
        }
        if (options.onSessionTrackingFailed) {
            this.onSessionTrackingFailed(options.onSessionTrackingFailed);
        }
        if (options.onEventTrackingSucceeded) {
            this.onEventTrackingSucceeded(options.onEventTrackingSucceeded);
        }
        if (options.onEventTrackingFailed) {
            this.onEventTrackingFailed(options.onEventTrackingFailed);
        }
        if (options.onAttributionChanged) {
            this.onAttributionChanged(options.onAttributionChanged);
        }
        if (options.onDeferredDeeplinkResponse) {
            this.onDeferredDeeplinkResponse(options.onDeferredDeeplinkResponse);
        }
        if (options.onAdidRead) {
            this.onAdidRead(options.onAdidRead);
        }
        if (options.onGoogleAdIdRead) {
            this.onGoogleAdIdRead(options.onGoogleAdIdRead);
        }
        if (options.onAmazonAdIdRead) {
            this.onAmazonAdIdRead(options.onAmazonAdIdRead);
        }
        if (options.onLastDeeplinkRead) {
            this.onLastDeeplinkRead(options.onLastDeeplinkRead);
        }
        if (options.onSdkVersionRead) {
            this.onSdkVersionRead(options.onSdkVersionRead);
        }
        if (options.onPlayStorePurchaseVerified) {
            this.onPlayStorePurchaseVerified(options.onPlayStorePurchaseVerified);
        }
        if (options.onInstallReferrerRead) {
            this.onInstallReferrerRead(options.onInstallReferrerRead);
        }
    }
}

export type {
    AdjustAdRevenuePayload,
    AdjustAttribution,
    AdjustDeferredDeeplink,
    AdjustEventFailure,
    AdjustEventPayload,
    AdjustEventSuccess,
    AdjustGoogleAdId,
    AdjustInstallReferrerDetails,
    AdjustInstallReferrerResult,
    AdjustAmazonAdId,
    AdjustLastDeeplink,
    AdjustPlayStorePurchasePayload,
    AdjustPurchaseVerificationResult,
    AdjustSdkVersion,
    AdjustSessionFailure,
    AdjustSessionSuccess,
} from './adjust_types';
