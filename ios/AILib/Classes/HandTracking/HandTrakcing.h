//
//  手势侦测-具体实现
//

#import <UIKit/UIKit.h>
#import "Hand.h"

NS_ASSUME_NONNULL_BEGIN

@interface SNHandTrakcing : NSObject

- (void) initSync:(int) threadCount;
- (void) closeSync;
- (Hand *) tracking:(CVPixelBufferRef) image;

@end

NS_ASSUME_NONNULL_END
