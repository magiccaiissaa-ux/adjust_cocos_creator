import { native, sys } from 'cc';
import type { AdjustRuntimePlatform } from './adjust_manager';

export type AdjustDirectMethod =
    | 'enable'
    | 'disable'
    | 'switchToOfflineMode'
    | 'switchBackToOnlineMode'
    | 'addGlobalCallbackParameter'
    | 'addGlobalPartnerParameter'
    | 'removeGlobalCallbackParameter'
    | 'removeGlobalPartnerParameter'
    | 'removeGlobalCallbackParameters'
    | 'removeGlobalPartnerParameters'
    | 'endFirstSessionDelay'
    | 'enableCoppaComplianceInDelay'
    | 'disableCoppaComplianceInDelay'
    | 'enablePlayStoreKidsComplianceInDelay'
    | 'disablePlayStoreKidsComplianceInDelay'
    | 'setExternalDeviceIdInDelay';

export interface AdjustPlatformAdapter {
    readonly platform: AdjustRuntimePlatform;
    readonly implemented: boolean;
    initBridge(): boolean;
    addNativeEventListener(event: string, listener: (data: string) => void): void;
    dispatchToNative(event: string, payload: string): void;
    invokeDirect(method: AdjustDirectMethod, ...args: string[]): boolean;
}

type WarnFn = (message: string) => void;

const ANDROID_ADJUST_CLASS = 'com.adjust.sdk.Adjust';
const ANDROID_BRIDGE_CLASS = 'com.game.magic.AdjustCocosInit';

const ANDROID_METHOD_SIGNATURES: Record<AdjustDirectMethod, string> = {
    enable: '()V',
    disable: '()V',
    switchToOfflineMode: '()V',
    switchBackToOnlineMode: '()V',
    addGlobalCallbackParameter: '(Ljava/lang/String;Ljava/lang/String;)V',
    addGlobalPartnerParameter: '(Ljava/lang/String;Ljava/lang/String;)V',
    removeGlobalCallbackParameter: '(Ljava/lang/String;)V',
    removeGlobalPartnerParameter: '(Ljava/lang/String;)V',
    removeGlobalCallbackParameters: '()V',
    removeGlobalPartnerParameters: '()V',
    endFirstSessionDelay: '()V',
    enableCoppaComplianceInDelay: '()V',
    disableCoppaComplianceInDelay: '()V',
    enablePlayStoreKidsComplianceInDelay: '()V',
    disablePlayStoreKidsComplianceInDelay: '()V',
    setExternalDeviceIdInDelay: '(Ljava/lang/String;)V',
};

class AndroidAdjustAdapter implements AdjustPlatformAdapter {
    readonly platform = 'android' as const;
    readonly implemented = true;

    initBridge(): boolean {
        if (!native?.reflection || sys.os !== sys.OS.ANDROID) {
            return false;
        }

        return !!native.reflection.callStaticMethod(
            ANDROID_BRIDGE_CLASS,
            'initBridge',
            '()Z',
        );
    }

    addNativeEventListener(event: string, listener: (data: string) => void): void {
        native.jsbBridgeWrapper.addNativeEventListener(event, listener);
    }

    dispatchToNative(event: string, payload: string): void {
        native.jsbBridgeWrapper.dispatchEventToNative(event, payload);
    }

    invokeDirect(method: AdjustDirectMethod, ...args: string[]): boolean {
        if (!native?.reflection || sys.os !== sys.OS.ANDROID) {
            return false;
        }

        native.reflection.callStaticMethod(
            ANDROID_ADJUST_CLASS,
            method,
            ANDROID_METHOD_SIGNATURES[method],
            ...args,
        );
        return true;
    }
}

class ReservedIOSAdjustAdapter implements AdjustPlatformAdapter {
    readonly platform = 'ios' as const;
    readonly implemented = false;

    constructor(private readonly warn: WarnFn) {}

    initBridge(): boolean {
        this.warn('[AdjustManager] iOS adapter is reserved but not implemented yet');
        return false;
    }

    addNativeEventListener(): void {}

    dispatchToNative(): void {
        this.warn('[AdjustManager] dispatchToNative is reserved for future iOS support');
    }

    invokeDirect(method: AdjustDirectMethod): boolean {
        this.warn(`[AdjustManager] ${method} is reserved for future iOS support`);
        return false;
    }
}

class NoopAdjustAdapter implements AdjustPlatformAdapter {
    readonly implemented = false;

    constructor(
        readonly platform: AdjustRuntimePlatform,
        private readonly warn?: WarnFn,
    ) {}

    initBridge(): boolean {
        return false;
    }

    addNativeEventListener(): void {}

    dispatchToNative(): void {
        if (this.platform !== 'web') {
            this.warn?.(`[AdjustManager] ${this.platform} platform is not supported`);
        }
    }

    invokeDirect(): boolean {
        return false;
    }
}

export function createAdjustPlatformAdapter(
    platform: AdjustRuntimePlatform,
    warn: WarnFn,
): AdjustPlatformAdapter {
    switch (platform) {
        case 'android':
            return new AndroidAdjustAdapter();
        case 'ios':
            return new ReservedIOSAdjustAdapter(warn);
        case 'web':
            return new NoopAdjustAdapter(platform);
        default:
            return new NoopAdjustAdapter(platform, warn);
    }
}
