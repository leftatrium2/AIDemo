#import "PoseLayer.h"
#import "AIConstants.h"
#import "PoseNet.h"

@interface PoseLayer()
{
    PairJoints joints[PAIR_JOINTS_NUM];
}

@property (nonatomic,strong) SNPerson *person;

@end

@implementation PoseLayer

- (instancetype)init {
    self = [super init];
    if(self){
        joints[0] = (PairJoints){LEFT_WRIST,LEFT_ELBOW};
        joints[1] = (PairJoints){LEFT_ELBOW,LEFT_SHOULDER};
        joints[2] = (PairJoints){LEFT_SHOULDER,RIGHT_SHOULDER};
        joints[3] = (PairJoints){RIGHT_SHOULDER,RIGHT_ELBOW};
        joints[4] = (PairJoints){RIGHT_ELBOW,RIGHT_WRIST};
        joints[5] = (PairJoints){LEFT_SHOULDER,LEFT_HIP};
        joints[6] = (PairJoints){LEFT_HIP,RIGHT_HIP};
        joints[7] = (PairJoints){RIGHT_HIP,RIGHT_SHOULDER};
        joints[8] = (PairJoints){LEFT_HIP,LEFT_KNEE};
        joints[9] = (PairJoints){LEFT_KNEE,LEFT_ANKLE};
        joints[10] = (PairJoints){RIGHT_HIP,RIGHT_KNEE};
        joints[11] = (PairJoints){RIGHT_KNEE,RIGHT_ANKLE};
    }
    return self;
}

- (void) setResult:(SNPerson *) person {
    self.person = person;
    dispatch_async(dispatch_get_main_queue(), ^{
        [self setNeedsDisplay];
    });
}

- (void)drawInContext:(CGContextRef)context {
    UIGraphicsPushContext(context);
    CGSize size = self.frame.size;
    //计算画布大小，比例等等
    int screenWidth,screenHeight,left,top;
//    if(size.height > size.width) {
//        screenWidth = size.width;
//        screenHeight = size.width;
//        left = 0;
//        top = (size.height - size.width) / 2;
//    }else{
//        screenWidth = size.height;
//        screenHeight = size.height;
//        left = (size.width - size.height) / 2;
//        top = 0;
//    }
    screenWidth = size.width;
    screenHeight = size.height;
    top = 0;
    left = 0;
    float widthRatio = (float) screenWidth / INPUT_WIDTH;
    float heightRatio = (float) screenHeight / INPUT_HEIGHT;
//    CGContextTranslateCTM(context, 0, self.frame.size.height);
//    CGContextTranslateCTM(context, 1.0f, -1.0f);
    
    CGContextSetLineWidth(context, 5.f);
    CGContextSetStrokeColorWithColor(context, [UIColor redColor].CGColor);
    
    //开始绘制骨架图
    //1.画关键点
    CGContextBeginPath(context);
    NSUInteger heatsSize = [self.person.keyPoints count];
    for(int i=0;i<heatsSize;i++) {
        SNKeyPoint *point = [self.person.keyPoints objectAtIndex:i];
        if(point.score > SK_MIN_CONFIDENCE) {
            Position *position = point.position;
            float adjustedX = (float) position.x * widthRatio + left;
            float adjustedY = (float) position.y * heightRatio + top;
            CGContextAddEllipseInRect(context, CGRectMake(adjustedX, adjustedY, 2.f, 2.f));
        }
    }
    //2.连线
    for(int i=0;i<PAIR_JOINTS_NUM;i++){
        SNBODYPART from = joints[i].from;
        SNBODYPART to = joints[i].to;
        SNKeyPoint *fromKeypoints = [self.person.keyPoints objectAtIndex:from];
        SNKeyPoint *toKeypoints = [self.person.keyPoints objectAtIndex:to];
        float fromScore = fromKeypoints.score;
        float toScore = toKeypoints.score;
        if(fromScore > SK_MIN_CONFIDENCE && toScore > SK_MIN_CONFIDENCE){
            float startPositionX = fromKeypoints.position.x * widthRatio + left;
            float startPositionY = fromKeypoints.position.y * heightRatio + top;
            float endPositionX = toKeypoints.position.x * widthRatio + left;
            float endPositionY = toKeypoints.position.y * heightRatio + top;
            CGContextMoveToPoint(context, startPositionX, startPositionY);
            CGContextAddLineToPoint(context, endPositionX, endPositionY);
        }
    }
    CGContextStrokePath(context);
    UIGraphicsPopContext();
}

@end
