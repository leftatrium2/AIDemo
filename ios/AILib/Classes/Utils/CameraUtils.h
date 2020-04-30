//
//  摄像头工具类
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>

NS_ASSUME_NONNULL_BEGIN

@protocol SNCameraUtilsDelegate <NSObject>

@required
- (void)captureOutput:(AVCaptureOutput *)output didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer fromConnection:(AVCaptureConnection *)connection;

@end

@interface CameraUtils : NSObject

- (void) initWithPreview:(UIView *) preView poseLayer:(CALayer *) poseLayer;
- (void) start;
- (void) stop;
- (void) setDelegate:(id<SNCameraUtilsDelegate>) dele;
- (BOOL) isOK;

@end

NS_ASSUME_NONNULL_END
