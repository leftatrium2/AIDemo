//
//  PoseNet管理器（入口部分）
//

#import <Foundation/Foundation.h>
#import "PoseNet.h"

NS_ASSUME_NONNULL_BEGIN

@interface PoseNetManager : NSObject

+ (instancetype) sharedManager;
//异步初始化
- (void) initAsync;
//同步初始化
- (void) initNow;
//关闭时候调用
- (void) close;
- (BOOL) isOk;
- (SNPerson *) estimateSinglePose:(CVPixelBufferRef) ref;
- (UIImage *) drawSkeleton:(SNPerson *) result size:(CGSize) size;

@end

NS_ASSUME_NONNULL_END
