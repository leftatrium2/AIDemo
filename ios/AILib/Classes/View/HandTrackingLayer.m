#import "HandTrackingLayer.h"
#import "AIConstants.h"

@interface HandTrackingLayer()

@property (nonatomic,strong) Hand *hand;

@end

@implementation HandTrackingLayer

- (void)setResult:(Hand *)hand {
    if(!hand) {
        return;
    }
    self.hand = hand;
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
    float widthRatio = (float) screenWidth / HT_INPUT_WIDTH;
    float heightRatio = (float) screenHeight / HT_INPUT_HEIGHT;
    
    CGContextSetLineWidth(context, 5.f);
    CGContextSetStrokeColorWithColor(context, [UIColor redColor].CGColor);
    
    CGContextBeginPath(context);
    if(self.hand && self.hand.getPoint.count == HT_MODEL_POINT_NUM) {
        //画手指关节点
        for(int i=0;i<self.hand.getPoint.count;i++){
            CGPoint point = [[self.hand.getPoint objectAtIndex:i] CGPointValue];
            float adjustedX = point.x * widthRatio + left;
            float adjustedY = point.y * heightRatio + top;
            CGContextAddEllipseInRect(context, CGRectMake(adjustedX, adjustedY, 2.f, 2.f));
        }
        //画连线
        /**
        * 使用如下数列，将关节点连接为一个手型
        * 0  1  2  3  4
        * 0  5  6  7  8
        * 0  9 10 11 12
        * 0 13 14 15 16
        * 0 17 18 19 20
        */
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                int beginK = j + i * 4;
                int endK = beginK + 1;
                if (j == 0) {
                    beginK = 0;
                }

                CGPoint beginPoint = [[self.hand.getPoint objectAtIndex:beginK] CGPointValue];
                CGPoint endPoint = [[self.hand.getPoint objectAtIndex:endK] CGPointValue];
                float startPositionX = beginPoint.x * widthRatio + left;
                float startPositionY = beginPoint.y * heightRatio + top;
                float endPositionX = endPoint.x * widthRatio + left;
                float endPositionY = endPoint.y * heightRatio + top;
                CGContextMoveToPoint(context, startPositionX, startPositionY);
                CGContextAddLineToPoint(context, endPositionX, endPositionY);
            }
        }
    }
    
    CGContextStrokePath(context);
    UIGraphicsPopContext();
}

@end
