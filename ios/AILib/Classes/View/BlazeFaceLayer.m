#import "BlazeFaceLayer.h"
#import "AIConstants.h"

@interface BlazeFaceLayer()

@property (nonatomic,strong) Face *face;

@end

@implementation BlazeFaceLayer

- (void)setResult:(Face *)face {
    if(!face){
        return;
    }
    self.face = face;
    dispatch_async(dispatch_get_main_queue(), ^{
        [self setNeedsDisplay];
    });
}

- (void)drawInContext:(CGContextRef)context {
    UIGraphicsPushContext(context);
    CGSize size = self.frame.size;
    int screenWidth,screenHeight,left,top;
    screenWidth = size.width;
    screenHeight = size.height;
    top = 0;
    left = 0;
    float widthRatio = (float) screenWidth / BF_INPUT_WIDTH;
    float heightRatio = (float) screenHeight / BF_INPUT_HEIGHT;

    CGContextSetLineWidth(context, 5.f);
    CGContextSetStrokeColorWithColor(context, [UIColor redColor].CGColor);
    
    CGContextBeginPath(context);
    
    if(self.face.keyPoints!=nil && [self.face.keyPoints count]!=0){
        for(FaceKeyPoint *keyPoint in self.face.keyPoints){
            CGPoint position = keyPoint.position;
            float adjustedX = (float) position.x * widthRatio + left;
            float adjustedY = (float) position.y * heightRatio + top;
            CGContextAddEllipseInRect(context, CGRectMake(adjustedX, adjustedY, 2.f, 2.f));
        }
    }
    
    if(self.face.description != nil){
        CGContextStrokeRect(context, self.face.detectionRect);
    }
    
    CGContextStrokePath(context);
    UIGraphicsPopContext();
    
}

@end
