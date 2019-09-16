//
//  ViewController.m
//  sampleappAKG
//
//  Created by Amara Graham on 8/27/19.
//  Copyright © 2019 Amara Graham. All rights reserved.
//

#import "ViewController.h"
#import <WowzaGoCoderSDK/WowzaGoCoderSDK.h>

@interface ViewController ()
#pragma mark - GoCoder SDK Components
@property (nonatomic, strong) WowzaConfig *goCoderConfig;
@property (nonatomic, strong) WOWZPlayer *player;
@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupConfig];
    
    // Register the GoCoder SDK license key
    NSString *SDKSampleAppLicenseKey = @"KEY-GOES-HERE";
    NSError *goCoderLicensingError = [WowzaGoCoder registerLicenseKey:SDKSampleAppLicenseKey];
    if (goCoderLicensingError != nil) {
        // Handle license key registration failure
    }
    else {
        self.player = [WOWZPlayer new];
        //Set default preroll buffer duration if you want a preroll buffer before a stream starts
        self.player.prerollDuration = 3; // 1-3 second buffer
        //Optionally set up data sink to handle in-stream events
        [self.player registerDataSink:self eventName:@"onTextData"];
    }
}

#pragma mark - WOWZStatusCallback Protocol Instance Methods

- (void) onWOWZStatus:(WOWZStatus *) goCoderStatus {
    // A successful status transition has been reported by the GoCoder SDK
    
    switch (goCoderStatus.state) {
            
        case WOWZStateIdle:
            break;
            
        case WOWZStateStarting:
            // A streaming broadcast session is starting up
            self.player.playerView = self.view;
            
            break;
            
        case WOWZStateRunning:
            
            break;
            
        case WOWZStateStopping:
            break;
            
        case WOWZStateBuffering:
            break;
            
        default:
            break;
    }
}

-(IBAction) didTapPlaybackButton:(id)sender {
    if ([self.player currentPlayState] == WOWZStateIdle) {
        self.player.playerViewGravity = WOWZPlayerViewGravityResizeAspect;
        [self.player play:self.goCoderConfig callback:self];
    }
    else {
        [self.player stop];
    }
}

-(void)setupConfig{
    WowzaConfig *config = [WowzaConfig new];
    config.hostAddress = @"URL";
    config.portNumber = 1935;
    config.streamName = @"STREAM-NAME";
    config.applicationName = @"APP-NAME";
    config.audioEnabled = YES;
    config.videoEnabled = YES;
    
    //config.allowHLSPlayback = YES;
    //config.hlsURL = @"[actual-hls-playback-url-for-fallback]";
    
    //If authentication is required
    config.username = @"someusername";
    config.password = @"somepass";
    
    self.goCoderConfig = config;
}

@end
