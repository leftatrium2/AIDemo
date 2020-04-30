//
//  手势侦测管理器
//

#import <UIKit/UIKit.h>
#import "Hand.h"

NS_ASSUME_NONNULL_BEGIN

@interface HandTrackingManager : NSObject

+ (instancetype) sharedManager;
- (BOOL) isOk;
- (void) initAsync;
- (void) initNow;
- (void) close;
- (Hand *) tracking:(CVPixelBufferRef) image;

@end

NS_ASSUME_NONNULL_END
