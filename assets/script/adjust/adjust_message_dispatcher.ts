import {
    AdjustAttribution,
    AdjustDeferredDeeplink,
    AdjustEventFailure,
    AdjustEventSuccess,
    AdjustInstallReferrerResult,
    AdjustGoogleAdId,
    AdjustAmazonAdId,
    AdjustLastDeeplink,
    AdjustPurchaseVerificationResult,
    AdjustSdkVersion,
    AdjustSessionFailure,
    AdjustSessionSuccess,
    NativeListener,
} from './adjust_types';

export class AdjustMessageDispatcher {
    private readonly listeners: Map<string, Set<NativeListener>> = new Map();
    private readonly webCallbackDelayMs: number;

    constructor(webCallbackDelayMs: number = 0) {
        this.webCallbackDelayMs = webCallbackDelayMs;
    }

    on(event: string, listener: NativeListener) {
        let set = this.listeners.get(event);
        if (!set) {
            set = new Set();
            this.listeners.set(event, set);
        }
        set.add(listener);
    }

    off(event: string, listener: NativeListener) {
        const set = this.listeners.get(event);
        if (!set) {
            return;
        }
        set.delete(listener);
        if (set.size === 0) {
            this.listeners.delete(event);
        }
    }

    emit(event: string, data: any) {
        const set = this.listeners.get(event);
        if (!set || set.size === 0) {
            return;
        }

        for (const listener of set) {
            try {
                listener(data);
            } catch (e) {
                console.error(`[AdjustManager] listener error, event=${event}`, e);
            }
        }
    }

    emitAsync(event: string, data: any) {
        setTimeout(() => {
            this.emit(event, data);
        }, this.webCallbackDelayMs);
    }

    // Native bridge payloads are delivered as strings, so parsing belongs here.
    addNativeJsonListener(event: string, addListener: (listener: (data: string) => void) => void) {
        addListener((data: string) => {
            this.emit(event, this.safeParse(data));
        });
    }

    requestOnceWithTimeout<T>(
        event: string,
        request: () => void,
        timeoutMs: number,
        isNative: boolean,
    ): Promise<T> {
        if (!isNative) {
            request();
            return Promise.resolve(null as T);
        }

        // Use a temporary listener so request/response style APIs stay easy to consume.
        return new Promise<T>((resolve, reject) => {
            let settled = false;
            const timeoutId = setTimeout(() => {
                cleanup();
                reject(new Error(`[AdjustManager] ${event} timeout after ${timeoutMs}ms`));
            }, timeoutMs);

            const listener: NativeListener = (data: T) => {
                cleanup();
                resolve(data);
            };

            const cleanup = () => {
                if (settled) {
                    return;
                }
                settled = true;
                clearTimeout(timeoutId);
                this.off(event, listener);
            };

            this.on(event, listener);

            try {
                request();
            } catch (error) {
                cleanup();
                reject(error);
            }
        });
    }

    safeParse(text: string): any {
        if (!text) {
            return null;
        }
        try {
            return JSON.parse(text);
        } catch {
            return text;
        }
    }
}

export interface AdjustInitListeners {
    onSessionTrackingSucceeded?: NativeListener<AdjustSessionSuccess>;
    onSessionTrackingFailed?: NativeListener<AdjustSessionFailure>;
    onEventTrackingSucceeded?: NativeListener<AdjustEventSuccess>;
    onEventTrackingFailed?: NativeListener<AdjustEventFailure>;
    onAttributionChanged?: NativeListener<AdjustAttribution>;
    onDeferredDeeplinkResponse?: NativeListener<AdjustDeferredDeeplink>;
    onAdidRead?: NativeListener<{ adid?: string }>;
    onGoogleAdIdRead?: NativeListener<AdjustGoogleAdId>;
    onAmazonAdIdRead?: NativeListener<AdjustAmazonAdId>;
    onLastDeeplinkRead?: NativeListener<AdjustLastDeeplink>;
    onSdkVersionRead?: NativeListener<AdjustSdkVersion>;
    onPlayStorePurchaseVerified?: NativeListener<AdjustPurchaseVerificationResult>;
    onInstallReferrerRead?: NativeListener<AdjustInstallReferrerResult>;
}
