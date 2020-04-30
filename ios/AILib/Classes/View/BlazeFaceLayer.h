//
//  面部追踪layer
//

#import <QuartzCore/QuartzCore.h>
#import "Face.h"

NS_ASSUME_NONNULL_BEGIN

@interface BlazeFaceLayer : CALayer

- (void) setResult:(Face *) face;

@end

NS_ASSUME_NONNULL_END
