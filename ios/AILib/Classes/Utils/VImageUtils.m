#import <CoreImage/CoreImage.h>
#import <UIKit/UIKit.h>
#import "VImageUtils.h"

//CVPixelBuffer并不会自己清楚位图数据，需要用这个callback来调用
void releaseCallback(void *releaseRefCon,const void *baseAddress) {
    free((void *)baseAddress);
}

@implementation VImageUtils

+ (NSData *) copyDataFromPixelBuffer:(CVPixelBufferRef) ref {
    NSData *data = nil;
    CVPixelBufferLockBaseAddress(ref, 0);
    
    size_t height = CVPixelBufferGetHeight(ref);
    size_t bytesPerRow = CVPixelBufferGetBytesPerRow(ref);
    uint8_t *src = (uint8_t *) CVPixelBufferGetBaseAddress(ref);
    
    size_t length = height * bytesPerRow;
    float *buffer = malloc(length * sizeof(float));
    float mean = 128.0f;
    float std = 128.0f;
    for(int i=0;i<length;i++) {
        buffer[i] = (float)((src[i] - mean) / std);
    }
    
    data = [NSData dataWithBytes:buffer length:length * sizeof(float)];
    
    CVPixelBufferUnlockBaseAddress(ref, 0);
    if(buffer != NULL) {
        free(buffer);
    }
    return data;
}

//image存储为RRRRRRRR GGGGGGGG BBBBBBBB 00000000
//bytesPerRow中前三个uint8有效，后面一位alpha值无效
+ (NSData *) copyDataFromUIImage:(UIImage *) image {
    NSData *data = nil;
    CGImageRef ref = image.CGImage;
    size_t height = CGImageGetHeight(ref);
    size_t width = CGImageGetWidth(ref);
    CGDataProviderRef dataProvider = CGImageGetDataProvider(ref);
    CFDataRef dataRef = CGDataProviderCopyData(dataProvider);
    uint8_t *pixels = (uint8_t *)CFDataGetBytePtr(dataRef);
    UInt8 *pixelsPoint = pixels;
    
    size_t length = height * width * 3;
    size_t imageSize = width * height;
    float *buffer = malloc(length * sizeof(float));
    float *bufferPoint = buffer;
    float maxRGBValue = 255.f;
    //循环224*224次
    for(int i=0;i<imageSize;i++) {
        UInt8 red = *pixelsPoint;
        *bufferPoint = (float)(red / maxRGBValue);
        pixelsPoint++;
        bufferPoint++;
        UInt8 green = *pixelsPoint;
        *bufferPoint = (float)(green / maxRGBValue);
        pixelsPoint++;
        bufferPoint++;
        UInt8 blue = *pixelsPoint;
        *bufferPoint = (float)(blue / maxRGBValue);
        //跳过alpha
        pixelsPoint+=2;
        bufferPoint++;
    }
    
    data = [NSData dataWithBytes:buffer length:length * sizeof(float)];
    
    if(buffer != NULL) {
        free(buffer);
    }
    CFRelease(dataRef);
    CGImageRelease(ref);
    return data;
}

//+ (NSData *) validUint8ToFloat32:(UInt8 *)src height:(size_t) height bytesPerRow:(size_t) bytesPerRow {
//    size_t length = height * bytesPerRow;
//    float *buffer = malloc(length * sizeof(float));
//    float maxRGBValue = 255.f;
//    for(int i=0;i<length;i++) {
//        buffer[i] = (float)(src[i] / maxRGBValue);
//    }
//
//    NSData *data = [NSData dataWithBytes:buffer length:length * sizeof(float)];
//    if(buffer != NULL) {
//        free(buffer);
//    }
//    return data;
//}

//+ (void) printPixelBufferFormat:(CVPixelBufferRef) ref {
//    OSType type = CVPixelBufferGetPixelFormatType(ref);
//    if(type == kCVPixelFormatType_32BGRA){
//        NSLog(@"kCVPixelFormatType_32BGRA");
//    }else if (type == kCVPixelFormatType_32ARGB){
//        NSLog(@"kCVPixelFormatType_32ARGB");
//    }
//}

//+ (CVPixelBufferRef) cropAndResize2:(CVPixelBufferRef) ref toSize:(CGSize) toSize {
//    CIContext *context = [CIContext context];
//    CIImage *src = [CIImage imageWithCVPixelBuffer:ref];
//    CGRect cubeRect = [SNVImageUtils getCropRect:ref];
//    CIImage *cropSrc = [src imageByCroppingToRect:cubeRect];
//    CIFilter * filter = [CIFilter filterWithName:@"CIStretchCrop" keysAndValues:kCIInputImageKey,image2,@"inputCenterStretchAmount",@1,@"inputCropAmount",@0.5,@"inputSize",[[CIVector alloc] initWithX:300 Y:150],nil];
//}

+ (CVPixelBufferRef) createPixelBufferWithSize:(CGSize) size {
    return NULL;
}

+ (CVPixelBufferRef) cropAndResize:(CVPixelBufferRef) ref toSize:(CGSize) toSize {
    //lock
    CVPixelBufferLockBaseAddress(ref, 0);
//    [SNVImageUtils printPixelBufferFormat:ref];
    CVPixelBufferRef cropPixelBuffer = NULL;
    NSDictionary *options;
    CVReturn status;
    
    vImage_Error err;
    CGRect cubeRect = [VImageUtils getCropRect:ref];
    int cropX0, cropY0, cropHeight, cropWidth, outWidth, outHeight;
    cropX0 = cubeRect.origin.x;
    cropY0 = cubeRect.origin.y;
    cropWidth = cubeRect.size.width;
    cropHeight = cubeRect.size.height;
    outWidth = cropWidth;
    outHeight = cropHeight;
    uint8_t *baseAddress = (uint8_t *) CVPixelBufferGetBaseAddress(ref);
    size_t bytesPerRow = CVPixelBufferGetBytesPerRow(ref);
    
    //1.裁剪
    vImage_Buffer inBuff = {NULL,0,0,0};
    inBuff.height = cropHeight;
    inBuff.width = cropWidth;
    inBuff.rowBytes = bytesPerRow;
    size_t startPos = cropY0 * bytesPerRow + INPUT_CAPTURE_CHANNEL_NUM * cropX0;
    inBuff.data = baseAddress + startPos;
    unsigned char *outImg= (unsigned char *) malloc(INPUT_CAPTURE_CHANNEL_NUM * outWidth * outHeight);
    vImage_Buffer outBuff = {outImg, outHeight, outWidth, INPUT_CAPTURE_CHANNEL_NUM * outWidth};
    err = vImageScale_ARGB8888(&inBuff, &outBuff, NULL, kvImageNoFlags);
    if(err != kvImageNoError) {
        goto onExit;
    }
    
    //2.缩小到制定大小
    vImage_Buffer outBuff2 = {NULL,0,0,0};
    outBuff2.width = toSize.width;
    outBuff2.height = toSize.height;
    outBuff2.rowBytes = outBuff2.width * INPUT_CAPTURE_CHANNEL_NUM;
    outBuff2.data = malloc(outBuff2.rowBytes * outBuff2.height);
    if (!outBuff2.data) {
        goto onExit;
    }
    err = vImageScale_ARGB8888(&outBuff, &outBuff2, NULL, kvImageHighQualityResampling);
    
    //3.抽掉一个Alpha通道，原来是32ARGB8888，改为32RGB888
    vImage_Buffer outBuff3 = {NULL,0,0,0};
    outBuff3.width = toSize.width;
    outBuff3.height = toSize.height;
    outBuff3.rowBytes = outBuff3.width * INPUT_POSENET_CHANNEL_NUM;
    outBuff3.data = malloc(outBuff3.rowBytes * outBuff3.height);
    if(!outBuff3.data){
        goto onExit;
    }
    err = vImageConvert_BGRA8888toRGB888(&outBuff2, &outBuff3, kvImageNoFlags);
    if(err != kvImageNoError){
        goto onExit;
    }

    //4.转换为CVPixelBuffer
    options = [NSDictionary dictionaryWithObjectsAndKeys:
               [NSNumber numberWithBool : YES],           kCVPixelBufferCGImageCompatibilityKey,
               [NSNumber numberWithBool : YES],           kCVPixelBufferCGBitmapContextCompatibilityKey,
               [NSNumber numberWithInt  : toSize.width],  kCVPixelBufferWidthKey,
               [NSNumber numberWithInt  : toSize.height], kCVPixelBufferHeightKey,
               nil];
    status = CVPixelBufferCreateWithBytes(NULL, toSize.width, toSize.height, kCVPixelFormatType_24RGB, outBuff3.data, outBuff3.rowBytes, releaseCallback, NULL, NULL, &cropPixelBuffer);
    if(status != kCVReturnSuccess) {
        //转换失败
        NSLog(@"转换失败,code:%d",status);
        cropPixelBuffer = NULL;
        goto onExit;
    }
    
onExit:
    //unlock
    CVPixelBufferUnlockBaseAddress(ref, 0);
    if(outBuff.data != NULL) {
        free(outBuff.data);
    }
    if(outBuff2.data != NULL) {
        free(outBuff2.data);
    }
    return cropPixelBuffer;
}

//就是由长方形屏幕改成正方形的，因为tflite的模型需要正方形的输入
+ (CGRect) getCropRect:(CVPixelBufferRef) ref {
    size_t width = CVPixelBufferGetWidth(ref);
    size_t height = CVPixelBufferGetHeight(ref);
    
    CGRect rect = CGRectMake(0, 0, width, height);
    return [VImageUtils cropRect:rect];
}

+ (CGRect) cropRect:(CGRect) rect {
    size_t height = rect.size.height;
    size_t width = rect.size.width;
    float bitmapRatio = (float) height / width;
    float modelInputRatio = ((float)(INPUT_HEIGHT))/INPUT_WIDTH;
    double maxDifference = 1e-5;
    if(fabs(modelInputRatio - bitmapRatio) < maxDifference){
        return rect;
    }
    if(modelInputRatio < bitmapRatio){
        float widthRadio = (float) width / modelInputRatio;
        float cropHeight = height - widthRadio;
        rect = CGRectMake(0, (int)(cropHeight/2), width, (int)(height - cropHeight));
    }else{
        float cropWidth = width - (float) height / modelInputRatio;
        rect = CGRectMake((int)(cropWidth/2), 0, (int)(width - cropWidth), height);
    }
    
    return rect;
}

+ (float) sigmoid:(float) x {
    return (float)(1.f/(1.f + exp(-x)));
}

//只用于调试，不要直接调用
+ (UIImage*)uiImageFromPixelBuffer:(CVPixelBufferRef) buffer {
    CIImage* ciImage = [CIImage imageWithCVPixelBuffer:buffer];
    CIContext* context = [CIContext contextWithOptions:@{kCIContextUseSoftwareRenderer : @(YES)}];
    CGRect rect = CGRectMake(0, 0, CVPixelBufferGetWidth(buffer), CVPixelBufferGetHeight(buffer));
    CGImageRef videoImage = [context createCGImage:ciImage fromRect:rect];
    UIImage* image = [UIImage imageWithCGImage:videoImage];
    CGImageRelease(videoImage);
    
    return image;
}

+ (UIImage*)uiImageFromCGImageRef:(CGImageRef) buffer {
    UIImage *image = [UIImage imageWithCGImage:buffer];
    return image;
}

+ (UIImage *) uiImageFromCIImage:(CIImage *) image{
    return [UIImage imageWithCIImage:image];
}

@end
