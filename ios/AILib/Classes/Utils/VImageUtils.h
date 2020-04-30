//
//  基于VImage的CVPixcelBuffer的封装库
//

#import <Foundation/Foundation.h>
#import <Accelerate/Accelerate.h>
#import "AIConstants.h"

#ifndef VImageUtils_h
#define VImageUtils_h

// 字节对齐使用，vImage如果不是64字节对齐的，会有额外开销
static inline size_t vImageByteAlign(size_t size, size_t alignment) {
    return ((size + (alignment - 1)) / alignment) * alignment;
}
@interface VImageUtils : NSObject

//从CVPixelBufferRef转到NSData
+ (NSData *) copyDataFromPixelBuffer:(CVPixelBufferRef) ref;

//从UIImage转到NSData
+ (NSData *) copyDataFromUIImage:(UIImage *) image;

//裁剪为正方形并缩小当前帧到合适的尺寸
+ (CVPixelBufferRef) cropAndResize:(CVPixelBufferRef) ref toSize:(CGSize) toSize ;
//+ (CVPixelBufferRef) cropAndResize2:(CVPixelBufferRef) ref toSize:(CGSize) toSize ;
+ (CGRect) cropRect:(CGRect) rect;

+ (CVPixelBufferRef) createPixelBufferWithSize:(CGSize) size;

//将float改成[0..1)区间
+ (float) sigmoid:(float) x;

//只用于调试，不要直接调用
+ (UIImage*)uiImageFromPixelBuffer:(CVPixelBufferRef) buffer;

//只用于调试，不要直接调用
+ (UIImage*)uiImageFromCGImageRef:(CGImageRef) buffer;

//只用于调试，不要直接调用
+ (UIImage *) uiImageFromCIImage:(CIImage *) image;

@end

#endif /* VImageUtils_h */
