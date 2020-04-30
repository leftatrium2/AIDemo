//
//  姿势Layer
//

#import <QuartzCore/QuartzCore.h>
#import "PoseNet.h"

NS_ASSUME_NONNULL_BEGIN

@interface PoseLayer : CALayer

- (void) setResult:(SNPerson *) person;

@end

NS_ASSUME_NONNULL_END
