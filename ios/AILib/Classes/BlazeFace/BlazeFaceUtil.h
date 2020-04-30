//
//  BlazeFace辅助工具类
//  论文地址：https://arxiv.org/abs/1907.05047
//
//  Created by sunxiao5 on 2020/3/27.
//

#import <Foundation/Foundation.h>
#import "Face.h"

NS_ASSUME_NONNULL_BEGIN

@interface BlazeFaceUtil : NSObject

+ (NSArray *) processCPU:(float *)rawBoxes raw_scores:(float *)raw_scores anchors_:(NSArray *) anchors_;
+ (Detections *)origNms:(NSArray *) detections threshold:(float) threshold;
+ (BOOL) rawBoxWithReshape:(float *)floatArr ret:(float *) ret;
+ (BOOL) rawScoresWithReshape:(float *)floatArr ret:(float *) ret;
+ (NSArray *) genAnchor;

@end

NS_ASSUME_NONNULL_END
