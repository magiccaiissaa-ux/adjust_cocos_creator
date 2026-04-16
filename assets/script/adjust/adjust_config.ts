import type { AdjustInitListeners } from './adjust_message_dispatcher';

export type AdjustEnvironment = 'production' | 'sandbox' | string;

export type AdjustLogLevel =
    | 'VERBOSE'
    | 'DEBUG'
    | 'INFO'
    | 'WARN'
    | 'ERROR'
    | 'ASSERT'
    | 'SUPPRESS'
    | string;

export interface AdjustConfig extends AdjustInitListeners {
    appToken: string;
    environment?: AdjustEnvironment;
    product?: AdjustEnvironment;
    launchDeferredDeeplink?: boolean;
    allowSuppressLogLevel?: boolean;
    enableSendingInBackground?: boolean;
    enableCostDataInAttribution?: boolean;
    processName?: string;
    defaultTracker?: string;
    externalDeviceId?: string;
    urlStrategyDomains?: string[];
    useSubdomains?: boolean;
    isDataResidency?: boolean;
    logLevel?: AdjustLogLevel;
    enableDeviceIdsReadingOnce?: boolean;
    enableCoppaCompliance?: boolean;
    eventDeduplicationIdsMaxSize?: number;
    enableFirstSessionDelay?: boolean;
    enablePreinstallTracking?: boolean;
    preinstallFilePath?: string;
    enablePlayStoreKidsCompliance?: boolean;
    fbAppId?: string;
    disableAppSetIdReading?: boolean;
}

export function normalizeAdjustConfig(config: AdjustConfig): AdjustConfig {
    // Listener callbacks stay on the JS side and must not be serialized to native.
    const {
        onSessionTrackingSucceeded,
        onSessionTrackingFailed,
        onEventTrackingSucceeded,
        onEventTrackingFailed,
        onAttributionChanged,
        onDeferredDeeplinkResponse,
        onAdidRead,
        onGoogleAdIdRead,
        onAmazonAdIdRead,
        onLastDeeplinkRead,
        onSdkVersionRead,
        onPlayStorePurchaseVerified,
        onInstallReferrerRead,
        ...nativeConfig
    } = config;

    return {
        ...nativeConfig,
        environment: config.environment ?? config.product ?? 'sandbox',
        logLevel: config.logLevel ?? 'INFO',
    };
}
