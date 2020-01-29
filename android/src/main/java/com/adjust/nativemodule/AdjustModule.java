//
//  Adjust.java
//  Adjust SDK
//
//  Created by Abdullah Obaied (@Obaied) on 19th October 2016.
//  Copyright (c) 2016-2018 Adjust GmbH. All rights reserved.
//

package com.adjust.nativemodule;

import android.net.Uri;
import android.util.Log;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.json.JSONObject;
import org.json.JSONException;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.*;
import com.adjust.sdk.*;

public class AdjustModule extends ReactContextBaseJavaModule implements LifecycleEventListener,
                OnAttributionChangedListener,
                OnEventTrackingSucceededListener,
                OnEventTrackingFailedListener,
                OnSessionTrackingSucceededListener,
                OnSessionTrackingFailedListener,
                OnDeeplinkResponseListener {
    private static String TAG = "AdjustBridge";
    private boolean attributionCallback;
    private boolean eventTrackingSucceededCallback;
    private boolean eventTrackingFailedCallback;
    private boolean sessionTrackingSucceededCallback;
    private boolean sessionTrackingFailedCallback;
    private boolean deferredDeeplinkCallback;
    private boolean shouldLaunchDeeplink = true;

    public AdjustModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "Adjust";
    }

    @Override
    public void initialize() {
        getReactApplicationContext().addLifecycleEventListener(this);
    }

    @Override
    public void onHostPause() {
        Adjust.onPause();
    }

    @Override
    public void onHostResume() {
        Adjust.onResume();
    }

    @Override
    public void onHostDestroy() {}

    @Override
    public void onAttributionChanged(AdjustAttribution attribution) {
        sendEvent(getReactApplicationContext(), "adjust_attribution", AdjustUtil.attributionToMap(attribution));
    }

    @Override
    public void onFinishedEventTrackingSucceeded(AdjustEventSuccess event) {
        sendEvent(getReactApplicationContext(), "adjust_eventTrackingSucceeded", AdjustUtil.eventSuccessToMap(event));
    }

    @Override
    public void onFinishedEventTrackingFailed(AdjustEventFailure event) {
        sendEvent(getReactApplicationContext(), "adjust_eventTrackingFailed", AdjustUtil.eventFailureToMap(event));
    }

    @Override
    public void onFinishedSessionTrackingSucceeded(AdjustSessionSuccess session) {
        sendEvent(getReactApplicationContext(), "adjust_sessionTrackingSucceeded", AdjustUtil.sessionSuccessToMap(session));
    }

    @Override
    public void onFinishedSessionTrackingFailed(AdjustSessionFailure session) {
        sendEvent(getReactApplicationContext(), "adjust_sessionTrackingFailed", AdjustUtil.sessionFailureToMap(session));
    }

    @Override
    public boolean launchReceivedDeeplink(Uri uri) {
        sendEvent(getReactApplicationContext(), "adjust_deferredDeeplink", AdjustUtil.deferredDeeplinkToMap(uri));
        return this.shouldLaunchDeeplink;
    }

    @ReactMethod
    public void create(ReadableMap mapConfig) {
        if (mapConfig == null) {
            return;
        }

        String appToken = null;
        String environment = null;
        String logLevel = null;
        String sdkPrefix = null;
        String userAgent = null;
        String processName = null;
        String defaultTracker = null;
        long secretId  = 0L;
        long info1 = 0L;
        long info2 = 0L;
        long info3 = 0L;
        long info4 = 0L;
        double delayStart = 0.0;
        boolean isDeviceKnown = false;
        boolean sendInBackground = false;
        boolean isLogLevelSuppress = false;
        boolean shouldLaunchDeeplink = false;
        boolean eventBufferingEnabled = false;
        boolean readMobileEquipmentIdentity = false;

        // Suppress log level.
        if (checkKey(mapConfig, "logLevel")) {
            logLevel = mapConfig.getString("logLevel");
            if (logLevel.equals("SUPPRESS")) {
                isLogLevelSuppress = true;
            }
        }

        // App token.
        if (checkKey(mapConfig, "appToken")) {
            appToken = mapConfig.getString("appToken");
        }

        // Environment.
        if (checkKey(mapConfig, "environment")) {
            environment = mapConfig.getString("environment");
        }

        final AdjustConfig adjustConfig = new AdjustConfig(getReactApplicationContext(), appToken, environment, isLogLevelSuppress);
        if (!adjustConfig.isValid()) {
            return;
        }

        // Log level.
        if (checkKey(mapConfig, "logLevel")) {
            logLevel = mapConfig.getString("logLevel");
            if (logLevel.equals("VERBOSE")) {
                adjustConfig.setLogLevel(LogLevel.VERBOSE);
            } else if (logLevel.equals("DEBUG")) {
                adjustConfig.setLogLevel(LogLevel.DEBUG);
            } else if (logLevel.equals("INFO")) {
                adjustConfig.setLogLevel(LogLevel.INFO);
            } else if (logLevel.equals("WARN")) {
                adjustConfig.setLogLevel(LogLevel.WARN);
            } else if (logLevel.equals("ERROR")) {
                adjustConfig.setLogLevel(LogLevel.ERROR);
            } else if (logLevel.equals("ASSERT")) {
                adjustConfig.setLogLevel(LogLevel.ASSERT);
            } else if (logLevel.equals("SUPPRESS")) {
                adjustConfig.setLogLevel(LogLevel.SUPRESS);
            } else {
                adjustConfig.setLogLevel(LogLevel.INFO);
            }
        }

        // Event buffering.
        if (checkKey(mapConfig, "eventBufferingEnabled")) {
            eventBufferingEnabled = mapConfig.getBoolean("eventBufferingEnabled");
            adjustConfig.setEventBufferingEnabled(eventBufferingEnabled);
        }

        // SDK prefix.
        if (checkKey(mapConfig, "sdkPrefix")) {
            sdkPrefix = mapConfig.getString("sdkPrefix");
            adjustConfig.setSdkPrefix(sdkPrefix);
        }

        // Main process name.
        if (checkKey(mapConfig, "processName")) {
            processName = mapConfig.getString("processName");
            adjustConfig.setProcessName(processName);
        }

        // Default tracker.
        if (checkKey(mapConfig, "defaultTracker")) {
            defaultTracker = mapConfig.getString("defaultTracker");
            adjustConfig.setDefaultTracker(defaultTracker);
        }

        // User agent.
        if (checkKey(mapConfig, "userAgent")) {
            userAgent = mapConfig.getString("userAgent");
            adjustConfig.setUserAgent(userAgent);
        }

        // App secret.
        if (checkKey(mapConfig, "secretId")
                && checkKey(mapConfig, "info1")
                && checkKey(mapConfig, "info2")
                && checkKey(mapConfig, "info3")
                && checkKey(mapConfig, "info4")) {
            try {
                secretId = Long.parseLong(mapConfig.getString("secretId"), 10);
                info1 = Long.parseLong(mapConfig.getString("info1"), 10);
                info2 = Long.parseLong(mapConfig.getString("info2"), 10);
                info3 = Long.parseLong(mapConfig.getString("info3"), 10);
                info4 = Long.parseLong(mapConfig.getString("info4"), 10);
                adjustConfig.setAppSecret(secretId, info1, info2, info3, info4);
            } catch (NumberFormatException ignore) {}
        }

        // Background tracking.
        if (checkKey(mapConfig, "sendInBackground")) {
            sendInBackground = mapConfig.getBoolean("sendInBackground");
            adjustConfig.setSendInBackground(sendInBackground);
        }

        // Set device known.
        if (checkKey(mapConfig, "isDeviceKnown")) {
            isDeviceKnown = mapConfig.getBoolean("isDeviceKnown");
            adjustConfig.setDeviceKnown(isDeviceKnown);
        }

        // Deprecated.
        // Set read mobile equipment ID.
        // if (checkKey(mapConfig, "readMobileEquipmentIdentity")) {
        //     readMobileEquipmentIdentity = mapConfig.getBoolean("readMobileEquipmentIdentity");
        //     adjustConfig.setReadMobileEquipmentIdentity(readMobileEquipmentIdentity);
        // }

        // Launching deferred deep link.
        if (checkKey(mapConfig, "shouldLaunchDeeplink")) {
            shouldLaunchDeeplink = mapConfig.getBoolean("shouldLaunchDeeplink");
            this.shouldLaunchDeeplink = shouldLaunchDeeplink;
        }

        // Delayed start.
        if (checkKey(mapConfig, "delayStart")) {
            delayStart = mapConfig.getDouble("delayStart");
            adjustConfig.setDelayStart(delayStart);
        }

        // Attribution callback.
        if (attributionCallback) {
            adjustConfig.setOnAttributionChangedListener(this);
        }

        // Event tracking succeeded callback.
        if (eventTrackingSucceededCallback) {
            adjustConfig.setOnEventTrackingSucceededListener(this);
        }

        // Event tracking failed callback.
        if (eventTrackingFailedCallback) {
            adjustConfig.setOnEventTrackingFailedListener(this);
        }

        // Session tracking succeeded callback.
        if (sessionTrackingSucceededCallback) {
            adjustConfig.setOnSessionTrackingSucceededListener(this);
        }

        // Session tracking failed callback.
        if (sessionTrackingFailedCallback) {
            adjustConfig.setOnSessionTrackingFailedListener(this);
        }

        // Deferred deeplink callback.
        if (deferredDeeplinkCallback) {
            adjustConfig.setOnDeeplinkResponseListener(this);
        }

        Adjust.onCreate(adjustConfig);
        Adjust.onResume();
    }

    @ReactMethod
    public void trackEvent(ReadableMap mapEvent) {
        if (mapEvent == null) {
            return;
        }

        double revenue = -1.0;
        String eventToken = null;
        String currency = null;
        String transactionId = null;
        String callbackId = null;
        Map<String, Object> callbackParameters = null;
        Map<String, Object> partnerParameters = null;

        // Event token.
        if (checkKey(mapEvent, "eventToken")) {
            eventToken = mapEvent.getString("eventToken");
        }

        final AdjustEvent event = new AdjustEvent(eventToken);
        if (!event.isValid()) {
            return;
        }

        // Revenue.
        if (checkKey(mapEvent, "revenue") || checkKey(mapEvent, "currency")) {
            try {
                revenue = Double.parseDouble(mapEvent.getString("revenue"));
            } catch (NumberFormatException ignore) {}
            currency = mapEvent.getString("currency");
            event.setRevenue(revenue, currency);
        }

        // Callback parameters.
        if (checkKey(mapEvent, "callbackParameters")) {
            callbackParameters = AdjustUtil.toMap(mapEvent.getMap("callbackParameters"));
            if (null != callbackParameters) {
                for (Map.Entry<String, Object> entry : callbackParameters.entrySet()) {
                    event.addCallbackParameter(entry.getKey(), entry.getValue().toString());
                }
            }
        }

        // Partner parameters.
        if (checkKey(mapEvent, "partnerParameters")) {
            partnerParameters = AdjustUtil.toMap(mapEvent.getMap("partnerParameters"));
            if (null != partnerParameters) {
                for (Map.Entry<String, Object> entry : partnerParameters.entrySet()) {
                    event.addPartnerParameter(entry.getKey(), entry.getValue().toString());
                }
            }
        }

        // Revenue deduplication.
        if (checkKey(mapEvent, "transactionId")) {
            transactionId = mapEvent.getString("transactionId");
            if (null != transactionId) {
                event.setOrderId(transactionId);
            }
        }

        // Callback ID.
        if (checkKey(mapEvent, "callbackId")) {
            callbackId = mapEvent.getString("callbackId");
            if (null != callbackId) {
                event.setCallbackId(callbackId);
            }
        }

        Adjust.trackEvent(event);
    }

    @ReactMethod
    public void setEnabled(Boolean enabled) {
        Adjust.setEnabled(enabled);
    }

    @ReactMethod
    public void isEnabled(Callback callback) {
        callback.invoke(Adjust.isEnabled());
    }

    @ReactMethod
    public void setReferrer(String referrer) {
        Adjust.setReferrer(referrer, getReactApplicationContext());
    }

    @ReactMethod
    public void setOfflineMode(Boolean enabled) {
        Adjust.setOfflineMode(enabled);
    }

    @ReactMethod
    public void setPushToken(String token) {
        Adjust.setPushToken(token, getReactApplicationContext());
    }

    @ReactMethod
    public void appWillOpenUrl(String strUri) {
        final Uri uri = Uri.parse(strUri);
        Adjust.appWillOpenUrl(uri, getReactApplicationContext());
    }

    @ReactMethod
    public void sendFirstPackages() {
        Adjust.sendFirstPackages();
    }

    @ReactMethod
    public void addSessionCallbackParameter(String key, String value) {
        Adjust.addSessionCallbackParameter(key, value);
    }

    @ReactMethod
    public void addSessionPartnerParameter(String key, String value) {
        Adjust.addSessionPartnerParameter(key, value);
    }

    @ReactMethod
    public void removeSessionCallbackParameter(String key) {
        Adjust.removeSessionCallbackParameter(key);
    }

    @ReactMethod
    public void removeSessionPartnerParameter(String key) {
        Adjust.removeSessionPartnerParameter(key);
    }

    @ReactMethod
    public void resetSessionCallbackParameters() {
        Adjust.resetSessionCallbackParameters();
    }

    @ReactMethod
    public void resetSessionPartnerParameters() {
        Adjust.resetSessionPartnerParameters();
    }

    @ReactMethod
    public void gdprForgetMe() {
        Adjust.gdprForgetMe(getReactApplicationContext());
    }

    @ReactMethod
    public void getIdfa(Callback callback) {
        callback.invoke("");
    }

    @ReactMethod
    public void getGoogleAdId(final Callback callback) {
        Adjust.getGoogleAdId(getReactApplicationContext(), new com.adjust.sdk.OnDeviceIdsRead() {
            @Override
            public void onGoogleAdIdRead(String googleAdId) {
                callback.invoke(googleAdId);
            }
        });
    }

    @ReactMethod
    public void getAdid(Callback callback) {
        callback.invoke(Adjust.getAdid());
    }

    @ReactMethod
    public void getAmazonAdId(Callback callback) {
        callback.invoke(Adjust.getAmazonAdId(getReactApplicationContext()));
    }

    @ReactMethod
    public void getAttribution(Callback callback) {
        callback.invoke(AdjustUtil.attributionToMap(Adjust.getAttribution()));
    }

    @ReactMethod
    public void getSdkVersion(String sdkPrefix, Callback callback) {
        String sdkVersion = Adjust.getSdkVersion();
        if (sdkVersion == null) {
            callback.invoke("");
        } else {
            callback.invoke(sdkPrefix + "@" + sdkVersion);
        }
    }

    @ReactMethod
    public void convertUniversalLink(final String url, final String scheme, final Callback callback) {
        callback.invoke("");
    }

    @ReactMethod
    public void setAttributionCallbackListener() {
        this.attributionCallback = true;
    }

    @ReactMethod
    public void setEventTrackingSucceededCallbackListener() {
        this.eventTrackingSucceededCallback = true;
    }

    @ReactMethod
    public void setEventTrackingFailedCallbackListener() {
        this.eventTrackingFailedCallback = true;
    }

    @ReactMethod
    public void setSessionTrackingSucceededCallbackListener() {
        this.sessionTrackingSucceededCallback = true;
    }

    @ReactMethod
    public void setSessionTrackingFailedCallbackListener() {
        this.sessionTrackingFailedCallback = true;
    }

    @ReactMethod
    public void setDeferredDeeplinkCallbackListener() {
        this.deferredDeeplinkCallback = true;
    }

    @ReactMethod
    public void teardown() {
        this.attributionCallback = false;
        this.eventTrackingSucceededCallback = false;
        this.eventTrackingFailedCallback = false;
        this.sessionTrackingSucceededCallback = false;
        this.sessionTrackingFailedCallback = false;
        this.deferredDeeplinkCallback = false;
    }

    @ReactMethod
    public void setTestOptions(ReadableMap mapTest) {
        if (mapTest == null) {
            return;
        }

        final AdjustTestOptions testOptions = new AdjustTestOptions();
        if (checkKey(mapTest, "hasContext")) {
            boolean value = mapTest.getBoolean("hasContext");
            if (value) {
                testOptions.context = getReactApplicationContext();
            }
        }
        if (checkKey(mapTest, "baseUrl")) {
            String value = mapTest.getString("baseUrl");
            testOptions.baseUrl = value;
        }
        if (checkKey(mapTest, "gdprUrl")) {
            String value = mapTest.getString("gdprUrl");
            testOptions.gdprUrl = value;
        }
        if (checkKey(mapTest, "basePath")) {
            String value = mapTest.getString("basePath");
            testOptions.basePath = value;
        }
        if (checkKey(mapTest, "gdprPath")) {
            String value = mapTest.getString("gdprPath");
            testOptions.gdprPath = value;
        }
        if (checkKey(mapTest, "useTestConnectionOptions")) {
            boolean value = mapTest.getBoolean("useTestConnectionOptions");
            testOptions.useTestConnectionOptions = value;
        }
        if (checkKey(mapTest, "timerIntervalInMilliseconds")) {
            try {
                Long value = Long.parseLong(mapTest.getString("timerIntervalInMilliseconds"));
                testOptions.timerIntervalInMilliseconds = value;
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                Log.d(TAG, "Can't format number");
            }
        }
        if (checkKey(mapTest, "timerStartInMilliseconds")) {
            try {
                Long value = Long.parseLong(mapTest.getString("timerStartInMilliseconds"));
                testOptions.timerStartInMilliseconds = value;
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                Log.d(TAG, "Can't format number");
            }
        }
        if (checkKey(mapTest, "sessionIntervalInMilliseconds")) {
            try {
                Long value = Long.parseLong(mapTest.getString("sessionIntervalInMilliseconds"));
                testOptions.sessionIntervalInMilliseconds = value;
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                Log.d(TAG, "Can't format number");
            }
        }
        if (checkKey(mapTest, "subsessionIntervalInMilliseconds")) {
            try {
                Long value = Long.parseLong(mapTest.getString("subsessionIntervalInMilliseconds"));
                testOptions.subsessionIntervalInMilliseconds = value;
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                Log.d(TAG, "Can't format number");
            }
        }
        if (checkKey(mapTest, "noBackoffWait")) {
            boolean value = mapTest.getBoolean("noBackoffWait");
            testOptions.noBackoffWait = value;
        }
        if (checkKey(mapTest, "teardown")) {
            boolean value = mapTest.getBoolean("teardown");
            testOptions.teardown = value;
        }

        Adjust.setTestOptions(testOptions);
    }

    @ReactMethod
    public void onResume() {
        Adjust.onResume();
    }

    @ReactMethod
    public void onPause() {
        Adjust.onPause();
    }

    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    private boolean checkKey(ReadableMap map, String key) {
        return map.hasKey(key) && !map.isNull(key);
    }
}
