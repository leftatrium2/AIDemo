//
//  手势侦测绘图layer
//

#import <QuartzCore/QuartzCore.h>
#import "Hand.h"

NS_ASSUME_NONNULL_BEGIN

@interface HandTrackingLayer : CALayer

- (void) setResult:(Hand *) hand;

@end

NS_ASSUME_NONNULL_END
