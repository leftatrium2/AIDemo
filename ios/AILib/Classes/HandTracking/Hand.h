//
//  手势侦测-数据结构
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface Hand : NSObject

- (NSArray *) getPoint;
- (void) add:(CGPoint) point;

@end

NS_ASSUME_NONNULL_END
