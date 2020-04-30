//
//  骨架图识别管理类
//

#import <Foundation/Foundation.h>
#import <AVKit/AVKit.h>
#import "Skeleton.h"
#import "PoseNet.h"
#import "AIConstants.h"

NS_ASSUME_NONNULL_BEGIN

@interface SkeletonManager : NSObject

+ (instancetype) sharedManager;
- (void) initNow;
- (void) close;
- (NSArray *) classifyFrame:(UIImage *) image;

@end

NS_ASSUME_NONNULL_END
