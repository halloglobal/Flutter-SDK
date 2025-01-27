package io.agora.agorartcengine;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.agora.rtc.RtcEngine;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.agora.rtc.video.BeautyOptions;

import io.flutter.plugin.common.StandardMessageCodec;

/** AgoraRtcEnginePlugin */
public class AgoraRtcEnginePlugin implements MethodCallHandler {

  private final Registrar mRegistrar;
  private final MethodChannel mMethodChannel;
  private RtcEngine mRtcEngine;
  private HashMap<String, SurfaceView> mRendererViews;
  private Handler mEventHandler = new Handler(Looper.getMainLooper());

  void addView(SurfaceView view, int id) {
    mRendererViews.put("" + id, view);
  }

  private void removeView(int id) {
    mRendererViews.remove("" + id);
  }

  private SurfaceView getView(int id) {
    return mRendererViews.get("" + id);
  }

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "agora_rtc_engine");

    AgoraRtcEnginePlugin plugin = new AgoraRtcEnginePlugin(registrar, channel);
    channel.setMethodCallHandler(plugin);

    AgoraRenderViewFactory fac = new AgoraRenderViewFactory(StandardMessageCodec.INSTANCE, plugin);
    registrar.platformViewRegistry().registerViewFactory("AgoraRendererView", fac);
  }

  private AgoraRtcEnginePlugin(Registrar registrar, MethodChannel channel) {
    this.mRegistrar = registrar;
    this.mMethodChannel = channel;
    this.mRendererViews = new HashMap<>();
  }

  private Context getActiveContext() {
    return (mRegistrar.activity() != null) ? mRegistrar.activity() : mRegistrar.context();
  }

  @Override
  public void onMethodCall(MethodCall call, Result rawResult) {
    Context context = getActiveContext();
    MethodChannel.Result result = new MethodResultWrapper(rawResult, mEventHandler);

    switch (call.method) {
      // Core Methods
      case "create": {
        try {
          String appId = call.argument("appId");
          mRtcEngine = RtcEngine.create(context, appId, mRtcEventHandler);
        } catch (Exception e) {
          throw new RuntimeException("NEED TO check rtc sdk init fatal error\n");
        }
      }
      break;
      case "destroy": {
        RtcEngine.destroy();
      }
      break;
      case "setChannelProfile": {
        int profile = call.argument("profile");
        mRtcEngine.setChannelProfile(profile);
      }
      break;
      case "setClientRole": {
        int role = call.argument("role");
        mRtcEngine.setClientRole(role);
      }
      break;
      case "joinChannel": {
        String token = call.argument("token");
        String channel = call.argument("channelId");
        String info = call.argument("info");
        int uid = call.argument("uid");
        mRtcEngine.joinChannel(token, channel, info, uid);
      }
      break;
      case "leaveChannel": {
        mRtcEngine.leaveChannel();
      }
      break;
      case "renewToken": {
        String token = call.argument("token");
        mRtcEngine.renewToken(token);
      }
      break;
      case "enableWebSdkInteroperability": {
        boolean enabled = call.argument("enabled");
        mRtcEngine.enableWebSdkInteroperability(enabled);
      }
      break;
      case "getConnectionState": {
        int state = mRtcEngine.getConnectionState();
        result.success(state);
      }
      break;
      // Core Audio
      case "enableAudio": {
        mRtcEngine.enableAudio();
      }
      break;
      case "disableAudio": {
        mRtcEngine.disableAudio();
      }
      break;
      case "setAudioProfile": {
        int profile = call.argument("profile");
        int scenario = call.argument("scenario");
        mRtcEngine.setAudioProfile(profile, scenario);
      }
      break;
      case "adjustRecordingSignalVolume": {
        int volume = call.argument("volume");
        mRtcEngine.adjustRecordingSignalVolume(volume);
      }
      break;
      case "adjustPlaybackSignalVolume": {
        int volume = call.argument("volume");
        mRtcEngine.adjustPlaybackSignalVolume(volume);
      }
      break;
      case "enableAudioVolumeIndication": {
        int interval = call.argument("interval");
        int smooth = call.argument("smooth");
        mRtcEngine.enableAudioVolumeIndication(interval, smooth);
      }
      break;
      case "enableLocalAudio": {
        boolean enabled = call.argument("enabled");
        mRtcEngine.enableLocalAudio(enabled);
      }
      break;
      case "muteLocalAudioStream": {
        boolean muted = call.argument("muted");
        mRtcEngine.muteLocalAudioStream(muted);
      }
      break;
      case "muteRemoteAudioStream": {
        int uid = call.argument("uid");
        boolean muted = call.argument("muted");
        mRtcEngine.muteRemoteAudioStream(uid, muted);
      }
      break;
      case "muteAllRemoteAudioStreams": {
        boolean muted = call.argument("muted");
        mRtcEngine.muteAllRemoteAudioStreams(muted);
      }
      break;
      case "setDefaultMuteAllRemoteAudioStreams": {
        boolean muted = call.argument("muted");
        mRtcEngine.setDefaultMuteAllRemoteAudioStreams(muted);
      }
      break;
      // Video Pre-process and Post-process
      case "setBeautyEffectOptions": {
        boolean enabled = call.argument("enabled");
        HashMap<String, Object> optionsMap = call.argument("options");
        BeautyOptions options = beautyOptionsFromMap(optionsMap);
        mRtcEngine.setBeautyEffectOptions(enabled, options);
      }
      break;
      // Core Video
      case "enableVideo": {
        mRtcEngine.enableVideo();
      }
      break;
      case "disableVideo": {
        mRtcEngine.disableVideo();
      }
      break;
      case "setVideoEncoderConfiguration": {
        HashMap<String, Object> configDic = call.argument("config");
        VideoEncoderConfiguration config = videoEncoderConfigurationFromMap(configDic);
        mRtcEngine.setVideoEncoderConfiguration(config);
      }
      break;
      case "removeNativeView": {
        int viewId = call.argument("viewId");
        removeView(viewId);
      }
      break;
      case "setupLocalVideo": {
        int localViewId = call.argument("viewId");
        SurfaceView localView = getView(localViewId);
        int localRenderMode = call.argument("renderMode");
        VideoCanvas localCanvas = new VideoCanvas(localView);
        localCanvas.renderMode = localRenderMode;
        mRtcEngine.setupLocalVideo(localCanvas);
      }
      break;
      case "setupRemoteVideo": {
        int remoteViewId = call.argument("viewId");
        SurfaceView view = getView(remoteViewId);
        int remoteRenderMode = call.argument("renderMode");
        int remoteUid = call.argument("uid");
        mRtcEngine.setupRemoteVideo(new VideoCanvas(view, remoteRenderMode, remoteUid));
      }
      break;
      case "setLocalRenderMode": {
        int mode = call.argument("mode");
        mRtcEngine.setLocalRenderMode(mode);
      }
      break;
      case "setRemoteRenderMode": {
        int uid = call.argument("uid");
        int mode = call.argument("mode");
        mRtcEngine.setRemoteRenderMode(uid, mode);
      }
      break;
      case "startPreview": {
        mRtcEngine.startPreview();
      }
      break;
      case "stopPreview": {
        mRtcEngine.stopPreview();
      }
      break;
      case "enableLocalVideo": {
        boolean enabled = call.argument("enabled");
        mRtcEngine.enableLocalVideo(enabled);
      }
      break;
      case "muteLocalVideoStream": {
        boolean muted = call.argument("muted");
        mRtcEngine.muteLocalVideoStream(muted);
      }
      break;
      case "muteRemoteVideoStream": {
        int uid = call.argument("uid");
        boolean muted = call.argument("muted");
        mRtcEngine.muteRemoteVideoStream(uid, muted);
      }
      break;
      case "muteAllRemoteVideoStreams": {
        boolean muted = call.argument("muted");
        mRtcEngine.muteAllRemoteVideoStreams(muted);
      }
      break;
      case "setDefaultMuteAllRemoteVideoStreams": {
        boolean muted = call.argument("muted");
        mRtcEngine.setDefaultMuteAllRemoteVideoStreams(muted);
      }
      break;

      // Audio Routing Controller
      case "setDefaultAudioRouteToSpeaker": {
        boolean defaultToSpeaker = call.argument("defaultToSpeaker");
        mRtcEngine.setDefaultAudioRoutetoSpeakerphone(defaultToSpeaker);
      }
      break;
      case "setEnableSpeakerphone": {
        boolean enabled = call.argument("enabled");
        mRtcEngine.setEnableSpeakerphone(enabled);
      }
      break;
      case "isSpeakerphoneEnabled": {
        boolean enabled = mRtcEngine.isSpeakerphoneEnabled();
        result.success(enabled);
      }
      break;

      // Stream Fallback
      case "setRemoteUserPriority": {
        int uid = call.argument("uid");
        int userPriority = call.argument("userPriority");
        mRtcEngine.setRemoteUserPriority(uid, userPriority);
      }
      case "setLocalPublishFallbackOption": {
        int option = call.argument("option");
        mRtcEngine.setLocalPublishFallbackOption(option);
      }
      break;
      case "setRemoteSubscribeFallbackOption": {
        int option = call.argument("option");
        mRtcEngine.setRemoteSubscribeFallbackOption(option);
      }
      break;

      // Dual-stream Mode
      case "enableDualStreamMode": {
        boolean enabled = call.argument("enabled");
        mRtcEngine.enableDualStreamMode(enabled);
      }
      break;
      case "setRemoteVideoStreamType": {
        int uid = call.argument("uid");
        int streamType = call.argument("streamType");
        mRtcEngine.setRemoteVideoStreamType(uid, streamType);
      }
      break;
      case "setRemoteDefaultVideoStreamType": {
        int streamType = call.argument("streamType");
        mRtcEngine.setRemoteDefaultVideoStreamType(streamType);
      }
      break;

      // Encryption
      case "setEncryptionSecret": {
        String secret = call.argument("secret");
        mRtcEngine.setEncryptionSecret(secret);
      }
      break;
      case "setEncryptionMode": {
        String encryptionMode = call.argument("encryptionMode");
        mRtcEngine.setEncryptionMode(encryptionMode);
      }
      break;

      // Camera Control
      case "switchCamera": {
        mRtcEngine.switchCamera();
      }
      break;

      // Miscellaneous Methods
      case "getSdkVersion": {
        String version = RtcEngine.getSdkVersion();
        result.success(version);
      }
      break;

      default:
        result.notImplemented();
    }
  }

  private VideoEncoderConfiguration.ORIENTATION_MODE orientationFromValue(int value) {
    switch (value) {
      case 0:
        return VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE;
      case 1:
        return VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_LANDSCAPE;
      case 2:
        return VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT;
      default:
        return VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE;
    }
  }

  private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
    @Override
    public void onWarning(int warn) {
      super.onWarning(warn);
      HashMap<String, Object> map = new HashMap<>();
      map.put("warn", warn);
      invokeMethod("onWarning", map);
    }

    @Override
    public void onError(int err) {
      super.onError(err);
      HashMap<String, Object> map = new HashMap<>();
      map.put("err", err);
      invokeMethod("onError", map);
    }

    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
      super.onJoinChannelSuccess(channel, uid, elapsed);
      HashMap<String, Object> map = new HashMap<>();
      map.put("channel", channel);
      map.put("uid", uid);
      map.put("elapsed", elapsed);
      invokeMethod("onJoinChannelSuccess", map);
    }

    @Override
    public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
      super.onRejoinChannelSuccess(channel, uid, elapsed);
      HashMap<String, Object> map = new HashMap<>();
      map.put("channel", channel);
      map.put("uid", uid);
      map.put("elapsed", elapsed);
      invokeMethod("onRejoinChannelSuccess", map);
    }

    @Override
    public void onLeaveChannel(RtcStats stats) {
      super.onLeaveChannel(stats);
      HashMap<String, Object> map = new HashMap<>();
      map.put("stats", mapFromStats(stats));
      invokeMethod("onLeaveChannel", map);
    }

    @Override
    public void onClientRoleChanged(int oldRole, int newRole) {
      super.onClientRoleChanged(oldRole, newRole);
      HashMap<String, Object> map = new HashMap<>();
      map.put("oldRole", oldRole);
      map.put("newRole", newRole);
      invokeMethod("onClientRoleChanged", map);
    }

    @Override
    public void onUserJoined(int uid, int elapsed) {
      super.onUserJoined(uid, elapsed);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("elapsed", elapsed);
      invokeMethod("onUserJoined", map);
    }

    @Override
    public void onUserOffline(int uid, int reason) {
      super.onUserOffline(uid, reason);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("reason", reason);
      invokeMethod("onUserOffline", map);
    }

    @Override
    public void onConnectionStateChanged(int state, int reason) {
      super.onConnectionStateChanged(state, reason);
      HashMap<String, Object> map = new HashMap<>();
      map.put("state", state);
      map.put("reason", reason);
      invokeMethod("onConnectionStateChanged", map);
    }

    @Override
    public void onConnectionLost() {
      super.onConnectionLost();
      invokeMethod("onConnectionLost", null);
    }

    @Override
    public void onNetworkTypeChanged (int type) {
      super.onNetworkTypeChanged(type);
      HashMap<String, Object> map = new HashMap<>();
      map.put("type", type);
      invokeMethod("onNetworkTypeChanged", map);
    }

    @Override
    public void onApiCallExecuted(int error, String api, String result) {
      super.onApiCallExecuted(error, api, result);
      HashMap<String, Object> map = new HashMap<>();
      map.put("error", error);
      map.put("api", api);
      map.put("result", result);
      invokeMethod("onApiCallExecuted", map);
    }

    @Override
    public void onTokenPrivilegeWillExpire(String token) {
      super.onTokenPrivilegeWillExpire(token);
      HashMap<String, Object> map = new HashMap<>();
      map.put("token", token);
      invokeMethod("onTokenPrivilegeWillExpire", map);
    }

    @Override
    public void onRequestToken() {
      super.onRequestToken();
      invokeMethod("onRequestToken", null);
    }

    @Override
    public void onMicrophoneEnabled(boolean enabled) {
      super.onMicrophoneEnabled(enabled);
      HashMap<String, Object> map = new HashMap<>();
      map.put("enabled", enabled);
      invokeMethod("onMicrophoneEnabled", map);
    }

    @Override
    public void onAudioVolumeIndication(AudioVolumeInfo[] speakers, int totalVolume) {
      super.onAudioVolumeIndication(speakers, totalVolume);
      HashMap<String, Object> map = new HashMap<>();
      map.put("totalVolume", totalVolume);
      map.put("speakers", arrayFromSpeakers(speakers));
      invokeMethod("onAudioVolumeIndication", map);
    }

    @Override
    public void onActiveSpeaker(int uid) {
      super.onActiveSpeaker(uid);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      invokeMethod("onActiveSpeaker", map);
    }

    @Override
    public void onFirstLocalAudioFrame(int elapsed) {
      super.onFirstLocalAudioFrame(elapsed);
      HashMap<String, Object> map = new HashMap<>();
      map.put("elapsed", elapsed);
      invokeMethod("onFirstLocalAudioFrame", map);
    }

    @Override
    public void onFirstRemoteAudioFrame(int uid, int elapsed) {
      super.onFirstRemoteAudioFrame(uid, elapsed);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("elapsed", elapsed);
      invokeMethod("onFirstRemoteAudioFrame", map);
    }

    @Override
    public void onFirstRemoteAudioDecoded(int uid, int elapsed) {
      super.onFirstRemoteAudioDecoded(uid, elapsed);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("elapsed", elapsed);
      invokeMethod("onFirstRemoteAudioDecoded", map);
    }

    @Override
    public void onFirstLocalVideoFrame(int width, int height, int elapsed) {
      super.onFirstLocalVideoFrame(width, height, elapsed);
      HashMap<String, Object> map = new HashMap<>();
      map.put("width", width);
      map.put("height", height);
      map.put("elapsed", elapsed);
      invokeMethod("onFirstLocalVideoFrame", map);
    }

    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
      super.onFirstRemoteVideoDecoded(uid, width, height, elapsed);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("width", width);
      map.put("height", height);
      map.put("elapsed", elapsed);
      invokeMethod("onFirstRemoteVideoDecoded", map);
    }

    @Override
    public void onFirstRemoteVideoFrame(int uid, int width, int height, int elapsed) {
      super.onFirstRemoteVideoFrame(uid, width, height, elapsed);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("width", width);
      map.put("height", height);
      map.put("elapsed", elapsed);
      invokeMethod("onFirstRemoteVideoFrame", map);
    }

    @Override
    public void onUserMuteAudio(int uid, boolean muted) {
      super.onUserMuteAudio(uid, muted);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("muted", muted);
      invokeMethod("onUserMuteAudio", map);
    }

    @Override
    public void onUserMuteVideo(int uid, boolean muted) {
      super.onUserMuteVideo(uid, muted);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("muted", muted);
      invokeMethod("onUserMuteVideo", map);
    }

    @Override
    public void onUserEnableVideo(int uid, boolean enabled) {
      super.onUserEnableVideo(uid, enabled);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("enabled", enabled);
      invokeMethod("onUserEnableVideo", map);
    }

    @Override
    public void onUserEnableLocalVideo(int uid, boolean enabled) {
      super.onUserEnableLocalVideo(uid, enabled);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("enabled", enabled);
      invokeMethod("onUserEnableLocalVideo", map);
    }

    @Override
    public void onVideoSizeChanged(int uid, int width, int height, int rotation) {
      super.onVideoSizeChanged(uid, width, height, rotation);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("width", width);
      map.put("height", height);
      map.put("rotation", rotation);
      invokeMethod("onVideoSizeChanged", map);
    }

    @Override
    public void onRemoteVideoStateChanged(int uid, int state) {
      super.onRemoteVideoStateChanged(uid, state);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("state", state);
      invokeMethod("onRemoteVideoStateChanged", map);
    }

    @Override
    public void onLocalPublishFallbackToAudioOnly(boolean isFallbackOrRecover) {
      super.onLocalPublishFallbackToAudioOnly(isFallbackOrRecover);
      HashMap<String, Object> map = new HashMap<>();
      map.put("isFallbackOrRecover", isFallbackOrRecover);
      invokeMethod("onLocalPublishFallbackToAudioOnly", map);
    }

    @Override
    public void onRemoteSubscribeFallbackToAudioOnly(int uid, boolean isFallbackOrRecover) {
      super.onRemoteSubscribeFallbackToAudioOnly(uid, isFallbackOrRecover);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("isFallbackOrRecover", isFallbackOrRecover);
      invokeMethod("onRemoteSubscribeFallbackToAudioOnly", map);
    }

    @Override
    public void onAudioRouteChanged(int routing) {
      super.onAudioRouteChanged(routing);
      HashMap<String, Object> map = new HashMap<>();
      map.put("routing", routing);
      invokeMethod("onAudioRouteChanged", map);
    }

    @Override
    public void onCameraFocusAreaChanged(Rect rect) {
      super.onCameraFocusAreaChanged(rect);
      HashMap<String, Object> map = new HashMap<>();
      map.put("rect", mapFromRect(rect));
      invokeMethod("onCameraFocusAreaChanged", map);
    }

    @Override
    public void onCameraExposureAreaChanged(Rect rect) {
      super.onCameraExposureAreaChanged(rect);
      HashMap<String, Object> map = new HashMap<>();
      map.put("rect", mapFromRect(rect));
      invokeMethod("onCameraExposureAreaChanged", map);
    }

    @Override
    public void onRtcStats(RtcStats stats) {
      super.onRtcStats(stats);
      HashMap<String, Object> map = new HashMap<>();
      map.put("stats", mapFromStats(stats));
      invokeMethod("onRtcStats", map);
    }

    @Override
    public void onLastmileQuality(int quality) {
      super.onLastmileQuality(quality);
      HashMap<String, Object> map = new HashMap<>();
      map.put("quality", quality);
      invokeMethod("onLastmileQuality", map);
    }

    @Override
    public void onNetworkQuality(int uid, int txQuality, int rxQuality) {
      super.onNetworkQuality(uid, txQuality, rxQuality);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("txQuality", txQuality);
      map.put("rxQuality", rxQuality);
      invokeMethod("onNetworkQuality", map);
    }

    @Override
    public void onLocalVideoStats(LocalVideoStats stats) {
      super.onLocalVideoStats(stats);
      HashMap<String, Object> map = new HashMap<>();
      map.put("stats", mapFromLocalVideoStats(stats));
      invokeMethod("onLocalVideoStats", map);
    }

    @Override
    public void onRemoteVideoStats(RemoteVideoStats stats) {
      super.onRemoteVideoStats(stats);
      HashMap<String, Object> map = new HashMap<>();
      map.put("stats", mapFromRemoteVideoStats(stats));
      invokeMethod("onRemoteVideoStats", map);
    }

    @Override
    public void onRemoteAudioStats(RemoteAudioStats stats) {
      super.onRemoteAudioStats(stats);
      HashMap<String, Object> map = new HashMap<>();
      map.put("stats", mapFromRemoteAudioStats(stats));
      invokeMethod("onRemoteAudioStats", map);
    }

    @Override
    public void onRemoteAudioTransportStats(int uid, int delay, int lost, int rxKBitRate) {
      super.onRemoteAudioTransportStats(uid, delay, lost, rxKBitRate);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("delay", delay);
      map.put("lost", lost);
      map.put("rxKBitRate", rxKBitRate);
      invokeMethod("onRemoteAudioTransportStats", map);
    }

    @Override
    public void onRemoteVideoTransportStats(int uid, int delay, int lost, int rxKBitRate) {
      super.onRemoteVideoTransportStats(uid, delay, lost, rxKBitRate);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("delay", delay);
      map.put("lost", lost);
      map.put("rxKBitRate", rxKBitRate);
      invokeMethod("onRemoteVideoTransportStats", map);
    }

    @Override
    public void onLocalVideoStateChanged(int localVideoState, int error) {
      super.onLocalVideoStateChanged(localVideoState, error);
      HashMap<String, Object> map = new HashMap<>();
      map.put("localVideoState", localVideoState);
      map.put("error", error);
      invokeMethod("onLocalVideoStateChanged", map);
    }

    @Override
    public void onAudioEffectFinished(int soundId) {
      super.onAudioEffectFinished(soundId);
      HashMap<String, Object> map = new HashMap<>();
      map.put("soundId", soundId);
      invokeMethod("onAudioEffectFinished", map);
    }

    @Override
    public void onStreamPublished(String url, int error) {
      super.onStreamPublished(url, error);
      HashMap<String, Object> map = new HashMap<>();
      map.put("url", url);
      map.put("error", error);
      invokeMethod("onStreamPublished", map);
    }

    @Override
    public void onStreamUnpublished(String url) {
      super.onStreamUnpublished(url);
      HashMap<String, Object> map = new HashMap<>();
      map.put("url", url);
      invokeMethod("onStreamUnpublished", map);
    }

    @Override
    public void onTranscodingUpdated() {
      super.onTranscodingUpdated();
      invokeMethod("onTranscodingUpdated", null);
    }

    @Override
    public void onStreamInjectedStatus(String url, int uid, int status) {
      super.onStreamInjectedStatus(url, uid, status);
      HashMap<String, Object> map = new HashMap<>();
      map.put("url", url);
      map.put("uid", uid);
      map.put("status", status);
      invokeMethod("onStreamInjectedStatus", map);
    }

    @Override
    public void onStreamMessage(int uid, int streamId, byte[] data) {
      super.onStreamMessage(uid, streamId, data);
      try {
        String message = new String(data, "UTF-8");
        HashMap<String, Object> map = new HashMap<>();
        map.put("streamId", streamId);
        map.put("uid", uid);
        map.put("message", message);
        invokeMethod("onStreamMessage", map);
      } catch (Exception e) {
          e.printStackTrace();
      }
    }

    @Override
    public void onStreamMessageError(int uid, int streamId, int error, int missed, int cached) {
      super.onStreamMessageError(uid, streamId, error, missed, cached);
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", uid);
      map.put("streamId", streamId);
      map.put("error", error);
      map.put("missed", missed);
      map.put("cached", cached);
      invokeMethod("onStreamMessageError", map);
    }

    @Override
    public void onMediaEngineLoadSuccess() {
      super.onMediaEngineLoadSuccess();
      invokeMethod("onMediaEngineLoadSuccess", null);
    }

    @Override
    public void onMediaEngineStartCallSuccess() {
      super.onMediaEngineStartCallSuccess();
      invokeMethod("onMediaEngineStartCallSuccess", null);
    }

    private HashMap<String, Object> mapFromStats(RtcStats stats) {
      HashMap<String, Object> map = new HashMap<>();
      map.put("duration", stats.totalDuration);
      map.put("txBytes", stats.txBytes);
      map.put("rxBytes", stats.rxBytes);
      map.put("txAudioKBitrate", stats.txAudioKBitRate);
      map.put("rxAudioKBitrate", stats.rxAudioKBitRate);
      map.put("txVideoKBitrate", stats.txVideoKBitRate);
      map.put("rxVideoKBitrate", stats.rxVideoKBitRate);
      map.put("txPacketLossRate", stats.txPacketLossRate);
      map.put("rxPacketLossRate", stats.rxPacketLossRate);
      map.put("lastmileDelay", stats.lastmileDelay);
      map.put("userCount", stats.users);
      map.put("cpuAppUsage", stats.cpuAppUsage);
      map.put("cpuTotalUsage", stats.cpuTotalUsage);
      return map;
    }

    private HashMap<String, Object> mapFromRect(Rect rect) {
      HashMap<String, Object> map = new HashMap<>();

      map.put("x", rect.left);
      map.put("y", rect.top);
      map.put("width", rect.width());
      map.put("height", rect.height());
      return map;
    }

    private HashMap<String, Object> mapFromLocalVideoStats(LocalVideoStats stats) {
      HashMap<String, Object> map = new HashMap<>();
      map.put("sentBitrate", stats.sentBitrate);
      map.put("sentFrameRate", stats.sentFrameRate);
      map.put("encoderOutputFrameRate", stats.encoderOutputFrameRate);
      map.put("rendererOutputFrameRate", stats.rendererOutputFrameRate);
      return map;
    }

    private HashMap<String, Object> mapFromRemoteVideoStats(RemoteVideoStats stats) {
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", stats.uid);
      map.put("width", stats.width);
      map.put("height", stats.height);
      map.put("receivedBitrate", stats.receivedBitrate);
      map.put("decoderOutputFrameRate", stats.decoderOutputFrameRate);
      map.put("rendererOutputFrameRate", stats.rendererOutputFrameRate);
      map.put("rxStreamType", stats.rxStreamType);
      return map;
    }

    private HashMap<String, Object> mapFromRemoteAudioStats(RemoteAudioStats stats) {
      HashMap<String, Object> map = new HashMap<>();
      map.put("uid", stats.uid);
      map.put("quality", stats.quality);
      map.put("networkTransportDelay", stats.networkTransportDelay);
      map.put("jitterBufferDelay", stats.jitterBufferDelay);
      map.put("audioLossRate", stats.audioLossRate);
      return map;
    }

    private ArrayList<HashMap<String, Object>> arrayFromSpeakers(AudioVolumeInfo[] speakers) {
      ArrayList<HashMap<String, Object>> list = new ArrayList<>();

        for (AudioVolumeInfo info: speakers) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("uid", info.uid);
            map.put("volume", info.volume);

            list.add(map);
        }

      return list;
    }
  };

  private BeautyOptions beautyOptionsFromMap(HashMap<String, Object> map) {
    BeautyOptions options = new BeautyOptions();
    options.lighteningContrastLevel = ((Double)(map.get("lighteningContrastLevel"))).intValue();
    options.lighteningLevel = ((Double)(map.get("lighteningLevel"))).floatValue();
    options.smoothnessLevel = ((Double)(map.get("smoothnessLevel"))).floatValue();
    options.rednessLevel = ((Double)(map.get("rednessLevel"))).floatValue();
    return options;
  }

  private VideoEncoderConfiguration videoEncoderConfigurationFromMap(HashMap<String, Object> map) {
    int width = (int)(map.get("width"));
    int height = (int)(map.get("height"));
    int frameRate = (int)(map.get("frameRate"));
    int bitrate = (int)(map.get("bitrate"));
    int minBitrate = (int)(map.get("minBitrate"));
    int orientationMode = (int)(map.get("orientationMode"));

    VideoEncoderConfiguration configuration = new VideoEncoderConfiguration();
    configuration.dimensions = new VideoEncoderConfiguration.VideoDimensions(width, height);
    configuration.frameRate = frameRate;
    configuration.bitrate = bitrate;
    configuration.minBitrate = minBitrate;
    configuration.orientationMode = orientationFromValue(orientationMode);

    return configuration;
  }

  private void invokeMethod(final String method, final HashMap map) {
    mEventHandler.post(new Runnable() {
      @Override
      public void run() {
        mMethodChannel.invokeMethod(method, map);
      }
    });
  }

  private static class MethodResultWrapper implements MethodChannel.Result {
    private MethodChannel.Result mResult;
    private Handler mHandler;

    MethodResultWrapper(MethodChannel.Result result, Handler handler) {
      this.mResult = result;
      this.mHandler = handler;
    }

    @Override
    public void success(final Object result) {
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          mResult.success(result);
        }
      });
    }

    @Override
    public void error(final String errorCode, final String errorMessage, final Object errorDetails) {
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          mResult.error(errorCode, errorMessage, errorDetails);
        }
      });
    }

    @Override
    public void notImplemented() {
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          mResult.notImplemented();
        }
      });
    }
  }
}
