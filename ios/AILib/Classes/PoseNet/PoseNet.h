//
//  PoseNet实现类
//

#import <Foundation/Foundation.h>
#import <AVKit/AVKit.h>
#import <TFLTensorFlowLite/TFLTensorFlowLite.h>
#import <AVFoundation/AVFoundation.h>
#import "AIConstants.h"

NS_ASSUME_NONNULL_BEGIN

@interface Position : NSObject

@property (atomic,assign) int x;
@property (atomic,assign) int y;

@end

@interface SNKeyPoint : NSObject

@property (atomic,assign) SNBODYPART bodyPart;
@property (nonatomic,strong) Position *position;
@property (atomic,assign) float score;

@end

@interface  SNPerson: NSObject
@property (nonatomic,strong) NSArray *keyPoints;
@property (atomic,assign) float score;
@end

@interface SNPoseNet : NSObject

- (BOOL) initSync:(int) threadCount;
- (void) closeSync;
- (SNPerson *) estimateSinglePose:(CVPixelBufferRef) ref;
//使用Person生成骨架图
//note:因为CG库只有32&16两种选项，所以，使用32位颜色控件，最后8位alpha空余的方式存储数据
//也就是说，实际有效颜色值为bytePerRows的3/4
- (UIImage *) drawSkeleton:(SNPerson *) result size:(CGSize) size;

@end

NS_ASSUME_NONNULL_END
