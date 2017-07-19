#import "RNDFPBannerView.h"

#import <DTBiOSSDK/DTBiOSSDK.h>

#if __has_include(<React/RCTBridgeModule.h>)
#import <React/RCTBridgeModule.h>
#import <React/UIView+React.h>
#import <React/RCTLog.h>
#else
#import "RCTBridgeModule.h"
#import "UIView+React.h"
#import "RCTLog.h"
#endif

@implementation RNDFPBannerView {
    DFPBannerView  *_bannerView;
}

- (void)insertReactSubview:(UIView *)view atIndex:(NSInteger)atIndex
{
    RCTLogError(@"AdMob Banner cannot have any subviews");
    return;
}

- (void)removeReactSubview:(UIView *)subview
{
    RCTLogError(@"AdMob Banner cannot have any subviews");
    return;
}

- (GADAdSize)getAdSizeFromString:(NSString *)bannerSize
{
    if ([bannerSize isEqualToString:@"banner"]) {
        return kGADAdSizeBanner;
    } else if ([bannerSize isEqualToString:@"largeBanner"]) {
        return kGADAdSizeLargeBanner;
    } else if ([bannerSize isEqualToString:@"mediumRectangle"]) {
        return kGADAdSizeMediumRectangle;
    } else if ([bannerSize isEqualToString:@"fullBanner"]) {
        return kGADAdSizeFullBanner;
    } else if ([bannerSize isEqualToString:@"leaderboard"]) {
        return kGADAdSizeLeaderboard;
    } else if ([bannerSize isEqualToString:@"smartBannerPortrait"]) {
        return kGADAdSizeSmartBannerPortrait;
    } else if ([bannerSize isEqualToString:@"smartBannerLandscape"]) {
        return kGADAdSizeSmartBannerLandscape;
    }
    else {
        return kGADAdSizeBanner;
    }
}

-(void)loadBanner {
    if (_adUnitID && _slotUUID && _bannerSize) {
        GADAdSize size = [self getAdSizeFromString:_bannerSize];
        _bannerView = [[DFPBannerView alloc] initWithAdSize:size];
        [_bannerView setAppEventDelegate:self]; //added Admob event dispatch listener
        if(!CGRectEqualToRect(self.bounds, _bannerView.bounds)) {
            if (self.onSizeChange) {
                self.onSizeChange(@{
                    @"width": [NSNumber numberWithFloat: _bannerView.bounds.size.width],
                    @"height": [NSNumber numberWithFloat: _bannerView.bounds.size.height]
                });
            }
        }
        
        _bannerView.delegate = self;
        _bannerView.adUnitID = _adUnitID;
        _bannerView.rootViewController = [UIApplication sharedApplication].delegate.window.rootViewController;
        
 
        DTBAdSize *sizeForDTBAdLoader = [[DTBAdSize alloc] initBannerAdSizeWithWidth:size.size.width height:size.size.height andSlotUUID:_slotUUID];
        
        DTBAdLoader *adLoader = [DTBAdLoader new];
        [adLoader setSizes:sizeForDTBAdLoader, nil];
        [adLoader loadAd:self];
    }
}


- (void)adView:(DFPBannerView *)banner
didReceiveAppEvent:(NSString *)name
      withInfo:(NSString *)info {
    NSLog(@"Received app event (%@, %@)", name, info);
    NSMutableDictionary *myDictionary = [[NSMutableDictionary alloc] init];
    myDictionary[name] = info;
    if (self.onAdmobDispatchAppEvent) {
        self.onAdmobDispatchAppEvent(@{ name: info });
    }
}

- (void)setBannerSize:(NSString *)bannerSize
{
    if(![bannerSize isEqual:_bannerSize]) {
        _bannerSize = bannerSize;
        if (_bannerView) {
            [_bannerView removeFromSuperview];
        }
        [self loadBanner];
    }
}

- (void)setAdUnitID:(NSString *)adUnitID
{
    if(![adUnitID isEqual:_adUnitID]) {
        _adUnitID = adUnitID;
        if (_bannerView) {
            [_bannerView removeFromSuperview];
        }

        [self loadBanner];
    }
}
- (void)setSlotUUID:(NSString *)slotUUID
{
    if(![slotUUID isEqual:_slotUUID]) {
        _slotUUID = slotUUID;
        if (_bannerView) {
            [_bannerView removeFromSuperview];
        }
        
        [self loadBanner];
    }
}

- (void)setTestDeviceID:(NSString *)testDeviceID
{
    if(![testDeviceID isEqual:_testDeviceID]) {
        _testDeviceID = testDeviceID;
        if (_bannerView) {
            [_bannerView removeFromSuperview];
        }
        [self loadBanner];
    }
}

-(void)layoutSubviews
{
    [super layoutSubviews ];

    _bannerView.frame = CGRectMake(
                                   self.bounds.origin.x,
                                   self.bounds.origin.x,
                                   _bannerView.frame.size.width,
                                   _bannerView.frame.size.height);
    [self addSubview:_bannerView];
}

- (void)removeFromSuperview
{
    [super removeFromSuperview];
}

/// Tells the delegate an ad request loaded an ad.
- (void)adViewDidReceiveAd:(DFPBannerView *)adView {
    if (self.onAdViewDidReceiveAd) {
        self.onAdViewDidReceiveAd(@{});
    }
}

/// Tells the delegate an ad request failed.
- (void)adView:(DFPBannerView *)adView
didFailToReceiveAdWithError:(GADRequestError *)error {
    if (self.onDidFailToReceiveAdWithError) {
        self.onDidFailToReceiveAdWithError(@{ @"error": [error localizedDescription] });
    }
}

/// Tells the delegate that a full screen view will be presented in response
/// to the user clicking on an ad.
- (void)adViewWillPresentScreen:(DFPBannerView *)adView {
    if (self.onAdViewWillPresentScreen) {
        self.onAdViewWillPresentScreen(@{});
    }
}

/// Tells the delegate that the full screen view will be dismissed.
- (void)adViewWillDismissScreen:(DFPBannerView *)adView {
    if (self.onAdViewWillDismissScreen) {
        self.onAdViewWillDismissScreen(@{});
    }
}

/// Tells the delegate that the full screen view has been dismissed.
- (void)adViewDidDismissScreen:(DFPBannerView *)adView {
    if (self.onAdViewDidDismissScreen) {
        self.onAdViewDidDismissScreen(@{});
    }
}

/// Tells the delegate that a user click will open another app (such as
/// the App Store), backgrounding the current app.
- (void)adViewWillLeaveApplication:(DFPBannerView *)adView {
    if (self.onAdViewWillLeaveApplication) {
        self.onAdViewWillLeaveApplication(@{});
    }
}

#pragma mark - DTBAdCallback
- (void)onFailure: (DTBAdError)error {
    NSLog(@"Failed to load ad :(");
    /**Please implement the logic to send ad request without our parameters if you want to
       show ads from other ad networks when Amazon ad request fails**/
    DFPRequest *request = [DFPRequest request];
    if(_testDeviceID) {
        if([_testDeviceID isEqualToString:@"EMULATOR"]) {
            request.testDevices = @[kGADSimulatorID];
        } else {
            request.testDevices = @[_testDeviceID];
        }
    }
    [_bannerView loadRequest:request];
}
- (void)onSuccess: (DTBAdResponse *)adResponse {    
    _bannerView.adUnitID = _adUnitID;
    _bannerView.rootViewController = self;
    DFPRequest *request = [DFPRequest request];
    if(_testDeviceID) {
        if([_testDeviceID isEqualToString:@"EMULATOR"]) {
            request.testDevices = @[kGADSimulatorID];
        } else {
            request.testDevices = @[_testDeviceID];
        }
    }
    request.customTargeting = adResponse.customTargetting;
    [_bannerView loadRequest:request];
}

@end
