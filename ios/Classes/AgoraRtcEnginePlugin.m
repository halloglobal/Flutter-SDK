#import "AgoraRtcEnginePlugin.h"
#import <AgoraRtcEngineKit/AgoraRtcEngineKit.h>

@interface AgoraRendererView()
@property (nonatomic, strong) UIView *renderView;
@property (nonatomic, assign) int64_t viewId;
@end

@implementation AgoraRendererView
- (instancetype)initWithFrame:(CGRect)frame viewIdentifier:(int64_t)viewId {
  if (self = [super init]) {
    self.renderView = [[UIView alloc] initWithFrame:frame];
    self.renderView.backgroundColor = [UIColor blackColor];
    self.viewId = viewId;
  }
  return self;
}

- (nonnull UIView *)view {
  return self.renderView;
}
@end

@interface AgoraRenderViewFactory : NSObject<FlutterPlatformViewFactory>
@end

@interface AgoraRtcEnginePlugin()<AgoraRtcEngineDelegate>
@property (strong, nonatomic) AgoraRtcEngineKit *agoraRtcEngine;
@property (strong, nonatomic) FlutterMethodChannel *methodChannel;
@property (strong, nonatomic) NSMutableDictionary *rendererViews;
@end

@implementation AgoraRtcEnginePlugin
#pragma mark - renderer views
+ (instancetype)sharedPlugin {
  static AgoraRtcEnginePlugin* plugin = nil;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    plugin = [[AgoraRtcEnginePlugin alloc] init];
  });
  return plugin;
}

- (NSMutableDictionary *)rendererViews {
  if (!_rendererViews) {
    _rendererViews = [[NSMutableDictionary alloc] init];
  }
  return _rendererViews;
}

+ (void)addView:(UIView *)view id:(NSNumber *)viewId {
  if (!viewId) {
    return;
  }
  if (view) {
    [[[AgoraRtcEnginePlugin sharedPlugin] rendererViews] setObject:view forKey:viewId];
  } else {
    [self removeViewForId:viewId];
  }
}

+ (void)removeViewForId:(NSNumber *)viewId {
  if (!viewId) {
    return;
  }
  [[[AgoraRtcEnginePlugin sharedPlugin] rendererViews] removeObjectForKey:viewId];
}

+ (UIView *)viewForId:(NSNumber *)viewId {
  if (!viewId) {
    return nil;
  }
  return [[[AgoraRtcEnginePlugin sharedPlugin] rendererViews] objectForKey:viewId];
}

#pragma mark - Flutter
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
                                   methodChannelWithName:@"agora_rtc_engine"
                                   binaryMessenger:[registrar messenger]];
  AgoraRtcEnginePlugin* instance = [[AgoraRtcEnginePlugin alloc] init];
  instance.methodChannel = channel;
  [registrar addMethodCallDelegate:instance channel:channel];
  
  AgoraRenderViewFactory *fac = [[AgoraRenderViewFactory alloc] init];
  [registrar registerViewFactory:fac withId:@"AgoraRendererView"];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  NSString *method = call.method;
  NSDictionary *arguments = call.arguments;
  NSLog(@"plugin handleMethodCall: %@, argus: %@", method, arguments);
  
  if ([@"createEngine" isEqualToString:method]) {
    NSString *appId = [self stringFromArguments:arguments key:@"appId"];
    self.agoraRtcEngine = [AgoraRtcEngineKit sharedEngineWithAppId:appId delegate:self];
  } else if ([@"joinChannel" isEqualToString:method]) {
    NSString *token = [self stringFromArguments:arguments key:@"token"];
    NSString *channel = [self stringFromArguments:arguments key:@"channelId"];
    NSString *info = [self stringFromArguments:arguments key:@"info"];
    NSInteger uid = [self intFromArguments:arguments key:@"uid"];
    
    [self.agoraRtcEngine joinChannelByToken:token channelId:channel info:info uid:uid  joinSuccess:nil];
    if (result) {
      result([NSNumber numberWithBool:YES]);
    }
  } else if ([@"leaveChannel" isEqualToString:method]) {
    BOOL success = (0 == [self.agoraRtcEngine leaveChannel:nil]);
    if (result) {
      result([NSNumber numberWithBool:success]);
    }
  } else if ([@"setupLocalVideo" isEqualToString:method]) {
    NSInteger viewId = [self intFromArguments:arguments key:@"viewId"];
    UIView *view = [AgoraRtcEnginePlugin viewForId:@(viewId)];
    NSInteger renderType = [self intFromArguments:arguments key:@"renderMode"];
    
    AgoraRtcVideoCanvas *canvas = [[AgoraRtcVideoCanvas alloc] init];
    canvas.view = view;
    canvas.renderMode = renderType;
    [self.agoraRtcEngine setupLocalVideo:canvas];
  } else if ([@"setupRemoteVideo" isEqualToString:method]) {
    NSInteger viewId = [self intFromArguments:arguments key:@"viewId"];
    UIView *view = [AgoraRtcEnginePlugin viewForId:@(viewId)];
    NSInteger renderType = [self intFromArguments:arguments key:@"renderMode"];
    NSInteger uid = [self intFromArguments:arguments key:@"uid"];
    
    AgoraRtcVideoCanvas *canvas = [[AgoraRtcVideoCanvas alloc] init];
    canvas.view = view;
    canvas.renderMode = renderType;
    canvas.uid = uid;
    [self.agoraRtcEngine setupRemoteVideo:canvas];
  } else if ([@"enableVideo" isEqualToString:method]) {
    [self.agoraRtcEngine enableVideo];
  } else if ([@"disableVideo" isEqualToString:method]) {
    [self.agoraRtcEngine disableVideo];
  } else if ([@"startPreview" isEqualToString:method]) {
    [self.agoraRtcEngine startPreview];
  } else if ([@"stopPreview" isEqualToString:method]) {
    [self.agoraRtcEngine stopPreview];
  }
  
  else {
    if (result) {
      result(FlutterMethodNotImplemented);
    }
  }
}

- (void)dealloc {
  [self.methodChannel setMethodCallHandler:nil];
}

#pragma mark - delegate
- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didOccurWarning:(AgoraWarningCode)warningCode {
  [self.methodChannel invokeMethod:@"onWarning" arguments:@{@"warn": @(warningCode)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didOccurError:(AgoraErrorCode)errorCode {
  [self.methodChannel invokeMethod:@"onError" arguments:@{@"err": @(errorCode)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didJoinChannel:(NSString * _Nonnull)channel withUid:(NSUInteger)uid elapsed:(NSInteger) elapsed {
  [self.methodChannel invokeMethod:@"onJoinChannelSuccess" arguments:@{@"channel": channel, @"uid": @(uid), @"elapsed": @(elapsed)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didRejoinChannel:(NSString * _Nonnull)channel withUid:(NSUInteger)uid elapsed:(NSInteger) elapsed {
  [self.methodChannel invokeMethod:@"onRejoinChannelSuccess" arguments:@{@"channel": channel, @"uid": @(uid), @"elapsed": @(elapsed)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didLeaveChannelWithStats:(AgoraChannelStats * _Nonnull)stats {
  [self.methodChannel invokeMethod:@"onLeaveChannel" arguments:@{@"stats": [self dicFromChannelStats:stats]}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didClientRoleChanged:(AgoraClientRole)oldRole newRole:(AgoraClientRole)newRole {
  [self.methodChannel invokeMethod:@"onClientRoleChanged" arguments:@{@"oldRole": @(oldRole), @"newRole": @(newRole)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didJoinedOfUid:(NSUInteger)uid elapsed:(NSInteger)elapsed {
  [self.methodChannel invokeMethod:@"onUserJoined" arguments:@{@"uid": @(uid), @"elapsed": @(elapsed)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didOfflineOfUid:(NSUInteger)uid reason:(AgoraUserOfflineReason)reason {
  [self.methodChannel invokeMethod:@"onUserOffline" arguments:@{@"uid": @(uid), @"reason": @(reason)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine connectionChangedToState:(AgoraConnectionStateType)state reason:(AgoraConnectionChangedReason)reason {
  [self.methodChannel invokeMethod:@"onConnectionStateChanged" arguments:@{@"state": @(state), @"reason": @(reason)}];
}

- (void)rtcEngineConnectionDidInterrupted:(AgoraRtcEngineKit * _Nonnull)engine {
  [self.methodChannel invokeMethod:@"onConnectionInterrupted" arguments:nil];
}

- (void)rtcEngineConnectionDidLost:(AgoraRtcEngineKit * _Nonnull)engine {
  [self.methodChannel invokeMethod:@"onConnectionLost" arguments:nil];
}

- (void)rtcEngineConnectionDidBanned:(AgoraRtcEngineKit * _Nonnull)engine {
  [self.methodChannel invokeMethod:@"onConnectionBanned" arguments:nil];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didApiCallExecute:(NSInteger)error api:(NSString * _Nonnull)api result:(NSString * _Nonnull)result {
  [self.methodChannel invokeMethod:@"onApiCallExecuted" arguments:@{@"error": @(error), @"api": api, @"result":  result}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine tokenPrivilegeWillExpire:(NSString * _Nonnull)token {
  [self.methodChannel invokeMethod:@"onTokenPrivilegeWillExpire" arguments:@{@"token": token}];
}

- (void)rtcEngineRequestToken:(AgoraRtcEngineKit * _Nonnull)engine {
  [self.methodChannel invokeMethod:@"onRequestToken" arguments:nil];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didMicrophoneEnabled:(BOOL)enabled {
  [self.methodChannel invokeMethod:@"onMicrophoneEnabled" arguments:@{@"enabled": @(enabled)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine reportAudioVolumeIndicationOfSpeakers:(NSArray<AgoraRtcAudioVolumeInfo *> * _Nonnull)speakers totalVolume:(NSInteger)totalVolume {
  [self.methodChannel invokeMethod:@"onAudioVolumeIndication" arguments:@{@"speakers": [self dicFromSpeakers:speakers], @"totalVolume": @(totalVolume)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine activeSpeaker:(NSUInteger)speakerUid {
  [self.methodChannel invokeMethod:@"onActiveSpeaker" arguments:@{@"uid": @(speakerUid)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine firstLocalAudioFrame:(NSInteger)elapsed {
  [self.methodChannel invokeMethod:@"onFirstLocalAudioFrame" arguments:@{@"elapsed": @(elapsed)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine firstRemoteAudioFrameOfUid:(NSUInteger)uid elapsed:(NSInteger)elapsed {
  [self.methodChannel invokeMethod:@"onFirstRemoteAudioFrame" arguments:@{@"uid": @(uid), @"elapsed": @(elapsed)}];
}

- (void)rtcEngineVideoDidStop:(AgoraRtcEngineKit * _Nonnull)engine {
  [self.methodChannel invokeMethod:@"onVideoStopped" arguments:nil];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine firstLocalVideoFrameWithSize:(CGSize)size elapsed:(NSInteger)elapsed {
  [self.methodChannel invokeMethod:@"onFirstLocalVideoFrame" arguments:@{@"width": @(size.width), @"height": @(size.height), @"elapsed": @(elapsed)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine firstRemoteVideoDecodedOfUid:(NSUInteger)uid size:(CGSize)size elapsed:(NSInteger)elapsed {
  [self.methodChannel invokeMethod:@"onFirstRemoteVideoDecoded" arguments:@{@"uid": @(uid), @"width": @(size.width), @"height": @(size.height), @"elapsed": @(elapsed)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine firstRemoteVideoFrameOfUid:(NSUInteger)uid size:(CGSize)size elapsed:(NSInteger)elapsed {
  [self.methodChannel invokeMethod:@"onFirstRemoteVideoFrame" arguments:@{@"uid": @(uid), @"width": @(size.width), @"height": @(size.height), @"elapsed": @(elapsed)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didAudioMuted:(BOOL)muted byUid:(NSUInteger)uid {
  [self.methodChannel invokeMethod:@"onUserMuteAudio" arguments:@{@"muted": @(muted), @"uid": @(uid)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didVideoMuted:(BOOL)muted byUid:(NSUInteger)uid {
  [self.methodChannel invokeMethod:@"onUserMuteVideo" arguments:@{@"muted": @(muted), @"uid": @(uid)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didVideoEnabled:(BOOL)enabled byUid:(NSUInteger)uid {
  [self.methodChannel invokeMethod:@"onUserEnableVideo" arguments:@{@"enabled": @(enabled), @"uid": @(uid)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didLocalVideoEnabled:(BOOL)enabled byUid:(NSUInteger)uid {
  [self.methodChannel invokeMethod:@"onUserEnableLocalVideo" arguments:@{@"enabled": @(enabled), @"uid": @(uid)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine videoSizeChangedOfUid:(NSUInteger)uid size:(CGSize)size rotation:(NSInteger)rotation {
  [self.methodChannel invokeMethod:@"onVideoSizeChanged" arguments:@{@"uid": @(uid), @"width": @(size.width), @"height": @(size.height), @"rotation": @(rotation)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine remoteVideoStateChangedOfUid:(NSUInteger)uid state:(AgoraVideoRemoteState)state {
  [self.methodChannel invokeMethod:@"onRemoteVideoStateChanged" arguments:@{@"uid": @(uid), @"state": @(state)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didLocalPublishFallbackToAudioOnly:(BOOL)isFallbackOrRecover {
  [self.methodChannel invokeMethod:@"onLocalPublishFallbackToAudioOnly" arguments:@{@"isFallbackOrRecover": @(isFallbackOrRecover)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didRemoteSubscribeFallbackToAudioOnly:(BOOL)isFallbackOrRecover byUid:(NSUInteger)uid {
  [self.methodChannel invokeMethod:@"onRemoteSubscribeFallbackToAudioOnly" arguments:@{@"uid": @(uid), @"isFallbackOrRecover": @(isFallbackOrRecover)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didAudioRouteChanged:(AgoraAudioOutputRouting)routing {
  [self.methodChannel invokeMethod:@"onAudioRouteChanged" arguments:@{@"routing": @(routing)}];
}

- (void)rtcEngineCameraDidReady:(AgoraRtcEngineKit * _Nonnull)engine {
  [self.methodChannel invokeMethod:@"onCameraReady" arguments:nil];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine cameraFocusDidChangedToRect:(CGRect)rect {
  [self.methodChannel invokeMethod:@"onCameraFocusAreaChanged" arguments:@{@"rect": [self dicFromRect:rect]}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine cameraExposureDidChangedToRect:(CGRect)rect {
  [self.methodChannel invokeMethod:@"onCameraExposureAreaChanged" arguments:@{@"rect": [self dicFromRect:rect]}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine audioQualityOfUid:(NSUInteger)uid quality:(AgoraNetworkQuality)quality delay:(NSUInteger)delay lost:(NSUInteger)lost {
  [self.methodChannel invokeMethod:@"onAudioQuality" arguments:@{@"uid": @(uid), @"quality": @(quality), @"delay": @(delay), @"lost": @(lost)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine reportRtcStats:(AgoraChannelStats * _Nonnull)stats {
  [self.methodChannel invokeMethod:@"onRtcStats" arguments:@{@"stats": [self dicFromChannelStats:stats]}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine lastmileQuality:(AgoraNetworkQuality)quality {
  [self.methodChannel invokeMethod:@"onLastmileQuality" arguments:@{@"quality": @(quality)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine networkQuality:(NSUInteger)uid txQuality:(AgoraNetworkQuality)txQuality rxQuality:(AgoraNetworkQuality)rxQuality {
  [self.methodChannel invokeMethod:@"onNetworkQuality" arguments:@{@"uid": @(uid), @"txQuality": @(txQuality), @"rxQuality": @(rxQuality)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine localVideoStats:(AgoraRtcLocalVideoStats * _Nonnull)stats {
  [self.methodChannel invokeMethod:@"onLocalVideoStats" arguments:@{@"stats": [self dicFromLocalVideoStats:stats]}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine remoteVideoStats:(AgoraRtcRemoteVideoStats * _Nonnull)stats {
  [self.methodChannel invokeMethod:@"onRemoteVideoStats" arguments:@{@"stats": [self dicFromRemoteVideoStats:stats]}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine remoteAudioStats:(AgoraRtcRemoteAudioStats * _Nonnull)stats {
  [self.methodChannel invokeMethod:@"onRemoteAudioStats" arguments:@{@"stats": stats}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine audioTransportStatsOfUid:(NSUInteger)uid delay:(NSUInteger)delay lost:(NSUInteger)lost rxKBitRate:(NSUInteger)rxKBitRate {
  [self.methodChannel invokeMethod:@"onRemoteAudioTransportStats" arguments:@{@"uid": @(uid), @"delay": @(delay), @"lost": @(lost), @"rxKBitRate": @(rxKBitRate)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine videoTransportStatsOfUid:(NSUInteger)uid delay:(NSUInteger)delay lost:(NSUInteger)lost rxKBitRate:(NSUInteger)rxKBitRate {
  [self.methodChannel invokeMethod:@"onRemoteVideoTransportStats" arguments:@{@"uid": @(uid), @"delay": @(delay), @"lost": @(lost), @"rxKBitRate": @(rxKBitRate)}];
}

- (void)rtcEngineLocalAudioMixingDidFinish:(AgoraRtcEngineKit * _Nonnull)engine {
  [self.methodChannel invokeMethod:@"onAudioMixingFinished" arguments:nil];
}

- (void)rtcEngineRemoteAudioMixingDidStart:(AgoraRtcEngineKit * _Nonnull)engine {
  [self.methodChannel invokeMethod:@"onRemoteAudioMixingStarted" arguments:nil];
}

- (void)rtcEngineRemoteAudioMixingDidFinish:(AgoraRtcEngineKit * _Nonnull)engine {
  [self.methodChannel invokeMethod:@"onRemoteAudioMixingFinished" arguments:nil];
}

- (void)rtcEngineDidAudioEffectFinish:(AgoraRtcEngineKit * _Nonnull)engine soundId:(NSInteger)soundId {
  [self.methodChannel invokeMethod:@"onAudioEffectFinished" arguments:@{@"soundId": @(soundId)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine streamPublishedWithUrl:(NSString * _Nonnull)url errorCode:(AgoraErrorCode)errorCode {
  [self.methodChannel invokeMethod:@"onStreamPublished" arguments:@{@"url": url, @"error": @(errorCode)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine streamUnpublishedWithUrl:(NSString * _Nonnull)url {
  [self.methodChannel invokeMethod:@"onStreamUnpublished" arguments:@{@"url": url}];
}

- (void)rtcEngineTranscodingUpdated:(AgoraRtcEngineKit * _Nonnull)engine {
  [self.methodChannel invokeMethod:@"onTranscodingUpdated" arguments:nil];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine streamInjectedStatusOfUrl:(NSString * _Nonnull)url uid:(NSUInteger)uid status:(AgoraInjectStreamStatus)status {
  [self.methodChannel invokeMethod:@"onStreamInjectedStatus" arguments:@{@"url": url, @"uid": @(uid), @"status": @(status)}];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine receiveStreamMessageFromUid:(NSUInteger)uid streamId:(NSInteger)streamId data:(NSData * _Nonnull)data {
  NSString *message = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
  if (message) {
    [self.methodChannel invokeMethod:@"onStreamMessage" arguments:@{@"uid": @(uid), @"streamId": @(streamId), @"message": message}];
  }
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine didOccurStreamMessageErrorFromUid:(NSUInteger)uid streamId:(NSInteger)streamId error:(NSInteger)error missed:(NSInteger)missed cached:(NSInteger)cached {
  [self.methodChannel invokeMethod:@"onStreamMessageError" arguments:@{@"uid": @(uid), @"streamId": @(streamId), @"error": @(error), @"missed": @(missed), @"cached": @(cached)}];
}

- (void)rtcEngineMediaEngineDidLoaded:(AgoraRtcEngineKit * _Nonnull)engine {
  [self.methodChannel invokeMethod:@"onMediaEngineLoadSuccess" arguments:nil];
}

- (void)rtcEngineMediaEngineDidStartCall:(AgoraRtcEngineKit * _Nonnull)engine {
  [self.methodChannel invokeMethod:@"onMediaEngineStartCallSuccess" arguments:nil];
}

#pragma mark - helper
- (NSString *)stringFromArguments:(NSDictionary *)arguments key:(NSString *)key {
  if (![arguments isKindOfClass:[NSDictionary class]]) {
    return nil;
  }
  
  NSString *value = [arguments valueForKey:key];
  if (![value isKindOfClass:[NSString class]]) {
    return nil;
  } else {
    return value;
  }
}

- (NSInteger)intFromArguments:(NSDictionary *)arguments key:(NSString *)key {
  if (![arguments isKindOfClass:[NSDictionary class]]) {
    return 0;
  }
  
  NSNumber *value = [arguments valueForKey:key];
  if (![value isKindOfClass:[NSNumber class]]) {
    return 0;
  } else {
    return [value integerValue];
  }
}

- (NSDictionary * _Nonnull)dicFromChannelStats:(AgoraChannelStats * _Nonnull)stats {
  return @{@"duration": @(stats.duration),
           @"txBytes": @(stats.txBytes),
           @"rxBytes": @(stats.rxBytes),
           @"txAudioKBitrate": @(stats.txAudioKBitrate),
           @"rxAudioKBitrate": @(stats.rxAudioKBitrate),
           @"txVideoKBitrate": @(stats.txVideoKBitrate),
           @"rxVideoKBitrate": @(stats.rxVideoKBitrate),
           @"lastmileDelay": @(stats.lastmileDelay),
           @"userCount": @(stats.userCount),
           @"cpuAppUsage": @(stats.cpuAppUsage),
           @"cpuTotalUsage": @(stats.cpuTotalUsage),
           };
}

- (NSArray * _Nonnull)dicFromSpeakers:(NSArray<AgoraRtcAudioVolumeInfo *> * _Nonnull)speakers {
  NSMutableArray *array = [[NSMutableArray alloc] init];
  for (AgoraRtcAudioVolumeInfo *speaker in speakers) {
    [array addObject:@{@"uid": @(speaker.uid),
                       @"volume": @(speaker.volume),
                       }];
  }
  
  return [array copy];
}

- (NSDictionary * _Nonnull)dicFromRect:(CGRect)rect {
  return @{@"x": @(rect.origin.x),
           @"y": @(rect.origin.y),
           @"width": @(rect.size.width),
           @"height": @(rect.size.height),
           };
}

- (NSDictionary * _Nonnull)dicFromLocalVideoStats:(AgoraRtcLocalVideoStats * _Nonnull)stats {
  return @{@"sentBitrate": @(stats.sentBitrate),
           @"sentFrameRate": @(stats.sentFrameRate),
           };
}

- (NSDictionary * _Nonnull)dicFromRemoteVideoStats:(AgoraRtcRemoteVideoStats * _Nonnull)stats {
  return @{@"uid": @(stats.uid),
           @"width": @(stats.width),
           @"height": @(stats.height),
           @"receivedBitrate": @(stats.receivedBitrate),
           @"receivedFrameRate": @(stats.receivedFrameRate),
           @"rxStreamType": @(stats.rxStreamType),
           };
}

@end

@implementation AgoraRenderViewFactory
- (nonnull NSObject<FlutterPlatformView> *)createWithFrame:(CGRect)frame viewIdentifier:(int64_t)viewId arguments:(id _Nullable)args {
  AgoraRendererView *rendererView = [[AgoraRendererView alloc] initWithFrame:frame viewIdentifier:viewId];
  [AgoraRtcEnginePlugin addView:rendererView.view id:@(viewId)];
  return rendererView;
}
@end

