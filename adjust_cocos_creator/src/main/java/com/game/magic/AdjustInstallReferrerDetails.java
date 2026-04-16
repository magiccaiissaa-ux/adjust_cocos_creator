package com.game.magic;

import com.adjust.sdk.GooglePlayInstallReferrerDetails;

public class AdjustInstallReferrerDetails {
    public final boolean success;
    public final String message;
    public String installReferrer;
    public long referrerClickTimestampSeconds;
    public long installBeginTimestampSeconds;
    public long referrerClickTimestampServerSeconds;
    public long installBeginTimestampServerSeconds;
    public String installVersion;
    public Boolean googlePlayInstant;
    public AdjustInstallReferrerDetails(boolean success, String message, GooglePlayInstallReferrerDetails details){
        boolean successValue = success;
        String messageValue = message;
        if (!success){
            this.success = successValue;
            this.message = messageValue;
            return;
        }
        if (details == null){
            successValue = false;
            messageValue = "details is null";
        } else {
            installReferrer = details.installReferrer;
            referrerClickTimestampSeconds = details.referrerClickTimestampSeconds;
            installBeginTimestampSeconds = details.installBeginTimestampSeconds;
            referrerClickTimestampServerSeconds = details.referrerClickTimestampServerSeconds;
            installBeginTimestampServerSeconds = details.installBeginTimestampServerSeconds;
            installVersion = details.installVersion;
            googlePlayInstant = details.googlePlayInstant;
        }
        this.success = successValue;
        this.message = messageValue;
    }

    public Boolean getGooglePlayInstant() {
        return googlePlayInstant;
    }

    public String getInstallVersion() {
        return installVersion;
    }

    public long getInstallBeginTimestampServerSeconds() {
        return installBeginTimestampServerSeconds;
    }

    public long getReferrerClickTimestampServerSeconds() {
        return referrerClickTimestampServerSeconds;
    }

    public long getInstallBeginTimestampSeconds() {
        return installBeginTimestampSeconds;
    }

    public long getReferrerClickTimestampSeconds() {
        return referrerClickTimestampSeconds;
    }

    public String getInstallReferrer() {
        return installReferrer;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }
}
