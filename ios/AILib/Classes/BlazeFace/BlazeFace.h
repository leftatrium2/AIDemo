//
//  面部侦测实现
//
//  Created by sunxiao5 on 2020/3/27.
//  Copyright © 2020 com.sina. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "Face.h"

NS_ASSUME_NONNULL_BEGIN

@interface BlazeFace : NSObject

- (void) initSync:(int) threadCount;
- (void) closeSync;
- (Face *) detector:(CVPixelBufferRef) image size:(CGSize) size;

@end

NS_ASSUME_NONNULL_END
