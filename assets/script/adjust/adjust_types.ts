export type Dict<T = any> = Record<string, T>;

export interface AdjustEventPayload {
    eventToken: string;
    revenue?: number;
    currency?: string;
    orderId?: string;
    deduplicationId?: string;
    callbackId?: string;
    productId?: string;
    callbackParams?: Record<string, string>;
    partnerParams?: Record<string, string>;
}

export interface AdjustAdRevenuePayload {
    source: string;
    revenue?: number;
    currency?: string;
    adImpressionsCount?: number;
    adRevenueNetwork?: string;
    adRevenueUnit?: string;
    adRevenuePlacement?: string;
    callbackParameters?: Record<string, string>;
    partnerParameters?: Record<string, string>;
}

export interface AdjustSessionSuccess {
    message?: string;
    timestamp?: string;
    adid?: string;
    jsonResponse?: Dict | null;
}

export interface AdjustSessionFailure {
    message?: string;
    timestamp?: string;
    adid?: string;
    willRetry?: boolean;
    jsonResponse?: Dict | null;
}

export interface AdjustEventSuccess {
    message?: string;
    timestamp?: string;
    adid?: string;
    eventToken?: string;
    callbackId?: string;
    jsonResponse?: Dict | null;
}

export interface AdjustEventFailure {
    message?: string;
    timestamp?: string;
    adid?: string;
    eventToken?: string;
    callbackId?: string;
    willRetry?: boolean;
    jsonResponse?: Dict | null;
}

export interface AdjustAttribution {
    trackerToken?: string;
    trackerName?: string;
    network?: string;
    campaign?: string;
    adgroup?: string;
    creative?: string;
    clickLabel?: string;
    costType?: string;
    costAmount?: number;
    costCurrency?: string;
    fbInstallReferrer?: string;
    jsonResponse?: Dict | null;
}

export interface AdjustInstallReferrerDetails {
    installReferrer?: string;
    referrerClickTimestampSeconds?: number;
    installBeginTimestampSeconds?: number;
    referrerClickTimestampServerSeconds?: number;
    installBeginTimestampServerSeconds?: number;
    installVersion?: string;
    googlePlayInstantParam?: boolean;
}

export interface AdjustInstallReferrerResult {
    success: boolean;
    error?: string;
    details?: AdjustInstallReferrerDetails | null;
}

export interface AdjustDeferredDeeplink {
    deeplink?: string | null;
    shouldLaunch?: boolean;
}

export interface AdjustGoogleAdId {
    googleAdId?: string | null;
}

export interface AdjustAmazonAdId {
    amazonAdId?: string | null;
}

export interface AdjustLastDeeplink {
    lastDeeplink?: string | null;
}

export interface AdjustSdkVersion {
    sdkVersion?: string | null;
}

export interface AdjustPlayStorePurchasePayload {
    productId: string;
    purchaseToken: string;
}

export interface AdjustPurchaseVerificationResult {
    verificationStatus?: string | null;
    code?: number;
    message?: string | null;
}

export type NativeListener<T = any> = (data: T) => void;
