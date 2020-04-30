//
//  骨架图识别
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import "PoseNet.h"
#import "AIConstants.h"

NS_ASSUME_NONNULL_BEGIN

@interface Skeleton : NSObject

- (void) initSync:(int) threadCount;
- (void) closeSync;
- (NSArray *) classifyFrame:(UIImage *) image;

@end

NS_ASSUME_NONNULL_END
