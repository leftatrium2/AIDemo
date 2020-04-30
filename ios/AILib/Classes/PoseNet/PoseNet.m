#import <CoreMedia/CMMetadata.h>
#import <Accelerate/Accelerate.h>
#import "AIConstants.h"
#import "PoseNet.h"
#import "VImageUtils.h"

@implementation Position

- (instancetype)init {
    self = [super init];
    if(self){
        _x = 0;
        _y = 0;
    }
    return self;
}

@end

@implementation SNKeyPoint

- (instancetype)init {
    self = [super init];
    if(self){
        _bodyPart = NOSE;
        _position = [[Position alloc] init];
        _score = 0.f;
    }
    return self;
}

@end

@implementation SNPerson

- (instancetype)init {
    self = [super init];
    if(self) {
        _score = 0.f;
    }
    return self;
}

@end

@interface SNPoseNet() {
    BOOL isInited;
}

@property (nonatomic,strong) TFLInterpreter *interpreter;
@property (nonatomic,strong) TFLTensor *inputTensor;
@property (nonatomic,strong) TFLTensor *heatsTensor;
@property (nonatomic,strong) TFLTensor *offsetsTensor;

@end

@interface SNPoseNet()
{
    PairJoints joints[PAIR_JOINTS_NUM];
}

@end

@implementation SNPoseNet


- (instancetype)init {
    self = [super init];
    if(self){
        isInited = NO;
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

- (BOOL) initSync:(int) threadCount {
    NSString *modelPath = [[NSBundle  mainBundle] pathForResource:POSENET_MODEL_NAME ofType:@"tflite"];
    if(modelPath==nil){
        NSLog(@"posenet model is not exists!");
        return NO;
    }
    TFLInterpreterOptions *options = [[TFLInterpreterOptions alloc] init];
    [options setNumberOfThreads:threadCount];
    
    //初始化TFLite
    NSError *error = nil;
    self.interpreter = [[TFLInterpreter alloc] initWithModelPath:modelPath options:options error:&error];
    NSAssert(error == nil, @"error:%@",[error localizedDescription]);
    
    //初始化input&output
    //申请内存
    [self.interpreter allocateTensorsWithError:&error];
    NSAssert(error == nil, @"error:%@",[error localizedDescription]);
    
    //设置input&output
    self.inputTensor = [self.interpreter inputTensorAtIndex:0 error:&error];
    self.heatsTensor = [self.interpreter outputTensorAtIndex:0 error:&error];
    self.offsetsTensor = [self.interpreter outputTensorAtIndex:1 error:&error];
    
    
    //检查一下模型的输入端，是否是Float32？现在处理的输入端数据，是通过:
    //1.cube化
    //2.resize(257*257)
    //3.cut alpha(抽掉Alpha通道）
    //4.通过uint8/255.f，得到的float
    NSAssert([self.inputTensor dataType] == TFLTensorDataTypeFloat32, @"inputTensor is not TFLTensorDataTypeUInt8!!");
    
    //检查输入部分
    NSArray *inputShapes = [self.inputTensor shapeWithError:&error];
    NSAssert(inputShapes != nil, @"input Tensor's shape is nil!");
    NSAssert(([[inputShapes objectAtIndex:0] intValue] == INPUT_BATCH_SIZE) &&
             ([[inputShapes objectAtIndex:1] intValue] == INPUT_HEIGHT) &&
             ([[inputShapes objectAtIndex:2] intValue] == INPUT_WIDTH) &&
             ([[inputShapes objectAtIndex:3] intValue] == INPUT_CHANNEL_SIZE),@"input tensor is not correct!");
    
    //检查输出部分-heats
    NSArray *heatsShapes = [self.heatsTensor shapeWithError:&error];
    NSAssert(heatsShapes != nil, @"heats Tensor's shape is nil!");
    NSAssert(([[heatsShapes objectAtIndex:0] intValue] == OUTPUT_BATCH_SIZE) &&
             ([[heatsShapes objectAtIndex:1] intValue] == OUTPUT_HEIGHT) &&
             ([[heatsShapes objectAtIndex:2] intValue] == OUTPUT_WIDTH) &&
             ([[heatsShapes objectAtIndex:3] intValue] == OUTPUT_KEYPONINT_SIZE), @"heats tensor is not correct");
    
    //检查输出部分-offsets
    NSArray *offsetsShapes = [self.offsetsTensor shapeWithError:&error];
    NSAssert(offsetsShapes != nil, @"offsets Tensor's Shapes");
    NSAssert(([[offsetsShapes objectAtIndex:0] intValue] == OUTPUT_BATCH_SIZE) &&
             ([[offsetsShapes objectAtIndex:1] intValue] == OUTPUT_HEIGHT) &&
             ([[offsetsShapes objectAtIndex:2] intValue] == OUTPUT_WIDTH) &&
             ([[offsetsShapes objectAtIndex:3] intValue] == OUTPUT_OFFSET_SIZE), @"offsets tensor is not correct");
             
    isInited = YES;
    return YES;
}

- (void) closeSync {
}

- (SNPerson *) estimateSinglePose:(CVPixelBufferRef) ref {
    if(!isInited) {
        return nil;
    }
    NSError *error = nil;
    BOOL result = NO;
    //将预处理好的cvpixel转义为nsdata
    NSData *data = [VImageUtils copyDataFromPixelBuffer:ref];
    NSParameterAssert(data);
    
    //输入输入
    result = [self.inputTensor copyData:data error:&error];
    NSAssert(result, @"error:%@",[error localizedDescription]);
    
    //开始运算模型
    result = [self.interpreter invokeWithError:&error];
    NSAssert(result, @"error:%@",[error localizedDescription]);
    
//    [self printOuputTensor:self.heatsTensor];
    
onExit:
    return [self postprocess];
}

- (void) printOuputTensor:(TFLTensor *) tensor {
    NSArray *arr = [tensor shapeWithError:nil];
    NSData *data = [tensor dataWithError:nil];
    NSLog(@"tensor shapes is :");
    for(int i=0;i<[arr count];i++) {
        NSLog(@"i:%d,value:%d",i,[[arr objectAtIndex:i] intValue]);
    }
    
    int s1 = [[arr objectAtIndex:0] intValue];
    int s2 = [[arr objectAtIndex:1] intValue];
    int s3 = [[arr objectAtIndex:2] intValue];
    int s4 = [[arr objectAtIndex:3] intValue];
    
    float (*point)[s1][s2][s3][s4];
    point = (float (*)[s1][s2][s3][s4])data.bytes;
    
    NSMutableString *str = [NSMutableString string];
    for(int i=0;i<s1;i++) {
        [str appendFormat:@"s1:\n"];
        for(int j=0;j<s2;j++) {
            [str appendFormat:@"s2:\n"];
            for(int k=0;k<s3;k++) {
                [str appendFormat:@"s3:\n"];
                for(int l=0;l<s4;l++) {
                    [str appendFormat:@"%f ",(*point)[i][j][k][l]];
                }
                [str appendFormat:@"\n"];
            }
        }
    }
    NSLog(@"%@",str);
}

//处理模型返回的数据
- (SNPerson *) postprocess{
    NSError *error = nil;
    
    //heats
    NSData *heatsData = [self.heatsTensor dataWithError:&error];
    NSAssert(error == nil, @"error:%@",[error localizedDescription]);
    NSArray *heatsShape = [self.heatsTensor shapeWithError:&error];
    NSAssert(error == nil, @"error:%@",[error localizedDescription]);
    const int heatsBatchSize = [[heatsShape objectAtIndex:0] intValue];
    const int heatsHeight = [[heatsShape objectAtIndex:1] intValue];
    const int heatsWidth = [[heatsShape objectAtIndex:2] intValue];
    const int heatsNumKeypoints = [[heatsShape objectAtIndex:3] intValue];
    float (*heatPoint)[heatsBatchSize][heatsHeight][heatsWidth][heatsNumKeypoints];
    heatPoint = (float (*)[heatsBatchSize][heatsHeight][heatsWidth][heatsNumKeypoints])heatsData.bytes;
    float heatmaps[heatsBatchSize][heatsHeight][heatsWidth][heatsNumKeypoints];
    memset(&heatmaps, 0, heatsBatchSize * heatsHeight * heatsWidth * heatsNumKeypoints * sizeof(float));
    for(int i=0;i<heatsBatchSize;i++){
        for(int j=0;j<heatsHeight;j++){
            for(int k=0;k<heatsWidth;k++){
                for(int z=0;z<heatsNumKeypoints;z++){
                    heatmaps[i][j][k][z] = (*heatPoint)[i][j][k][z];
                }
            }
        }
    }
    
    
    //offset
    NSData *offsetsData = [self.offsetsTensor dataWithError:&error];
    NSAssert(error == nil, @"error:%@",[error localizedDescription]);
    NSArray *offsetsShape = [self.offsetsTensor shapeWithError:&error];
    NSAssert(error == nil, @"error:%@",[error localizedDescription]);
    const int offsetsBatchSize = [[offsetsShape objectAtIndex:0] intValue];
    const int offsetsHeight = [[offsetsShape objectAtIndex:1] intValue];
    const int offsetsWidth = [[offsetsShape objectAtIndex:2] intValue];
    const int offsetsNumKeypoints = [[offsetsShape objectAtIndex:3] intValue];
    float (*offsetPoint)[offsetsBatchSize][offsetsHeight][offsetsWidth][offsetsNumKeypoints];
    offsetPoint = (float (*)[offsetsBatchSize][offsetsHeight][offsetsWidth][offsetsNumKeypoints])offsetsData.bytes;
    float offsets[offsetsBatchSize][offsetsHeight][offsetsWidth][offsetsNumKeypoints];
    memset(&offsets, 0, offsetsBatchSize * offsetsHeight * offsetsWidth * offsetsNumKeypoints * sizeof(float));
    for(int i=0;i<offsetsBatchSize;i++){
        for(int j=0;j<offsetsHeight;j++){
            for(int k=0;k<offsetsWidth;k++){
                for(int z=0;z<offsetsNumKeypoints;z++){
                    offsets[i][j][k][z] = (*offsetPoint)[i][j][k][z];
                }
            }
        }
    }
    
    int height = heatsHeight;
    int width = heatsWidth;
    int numKeypoints = heatsNumKeypoints;
    
    
    PosePair keypointPositions[numKeypoints];
    memset(&keypointPositions, 0, numKeypoints * sizeof(PosePair));
    
    for(int keypoint = 0;keypoint < numKeypoints;keypoint++){
        float maxVal = heatmaps[0][0][0][keypoint];
        int maxRow = 0;
        int maxCol = 0;
        for(int row = 0;row < height; row ++){
            for(int col = 0;col < width; col++){
                if(heatmaps[0][row][col][keypoint] > maxVal) {
                    maxVal = heatmaps[0][row][col][keypoint];
                    maxRow = row;
                    maxCol = col;
                }
            }
        }
        keypointPositions[keypoint].row = maxRow;
        keypointPositions[keypoint].col = maxCol;
    }
    
    int xCoords[numKeypoints];
    memset(&xCoords, 0, sizeof(int) * numKeypoints);
    int yCoords[numKeypoints];
    memset(&yCoords, 0, sizeof(int) * numKeypoints);
    float confidenceScores[numKeypoints];
    memset(&confidenceScores, 0, sizeof(float) * numKeypoints);
    for (int idx = 0; idx < numKeypoints; idx++) {
        int positionY = keypointPositions[idx].row;
        int positionX = keypointPositions[idx].col;
        yCoords[idx] = (int) (keypointPositions[idx].row / (float) (height - 1) * INPUT_HEIGHT + offsets[0][positionY][positionX][idx]);
        xCoords[idx] = (int) (keypointPositions[idx].col / (float) (width - 1) * INPUT_WIDTH + offsets[0][positionY][positionX][idx + numKeypoints]);
        confidenceScores[idx] = [VImageUtils sigmoid:heatmaps[0][positionY][positionX][idx]];
    }
    
    SNPerson *person = [[SNPerson alloc] init];
    NSMutableArray *keypointList = [NSMutableArray arrayWithCapacity:numKeypoints];
    
    float totalScore = 0.f;
    for(int idx = 0; idx < numKeypoints; idx++){
        SNKeyPoint *point = [[SNKeyPoint alloc] init];
        point.bodyPart = idx;
        point.position.x = xCoords[idx];
        point.position.y = yCoords[idx];
        point.score = confidenceScores[idx];
        totalScore += confidenceScores[idx];
        [keypointList addObject:point];
    }
    
    person.keyPoints = keypointList;
    person.score = totalScore;
    
onExit:
    return person;
}

- (CGContextRef) drawSkeletonToCGContext:(SNPerson *) result size:(CGSize) size{
    //计算画布大小，比例等等
    int screenWidth,screenHeight,left,top;
    if(size.height > size.width) {
        screenWidth = size.width;
        screenHeight = size.width;
        left = 0;
        top = (size.height - size.width) / 2;
    }else{
        screenWidth = size.height;
        screenHeight = size.height;
        left = (size.width - size.height) / 2;
        top = 0;
    }
    float widthRatio = (float) screenWidth / INPUT_WIDTH;
    float heightRatio = (float) screenHeight / INPUT_HEIGHT;
    //设置笔刷尺寸、颜色
    size_t bytesPerRow = screenWidth * 4;
    CGColorSpaceRef colorSpace=CGColorSpaceCreateDeviceRGB();
    CGContextRef context;
    if(SK_SHOW_IN_SCREEN){
        context = UIGraphicsGetCurrentContext();
        CGContextTranslateCTM(context, 0, screenHeight);
        CGContextTranslateCTM(context, 1.0f, -1.0f);
    }else{
        context = CGBitmapContextCreate(NULL, screenWidth, screenHeight, 8, bytesPerRow, colorSpace, kCGBitmapByteOrder32Big|kCGImageAlphaNoneSkipLast);
    }
    CGContextSetLineWidth(context, 1.f);
    CGContextSetStrokeColorWithColor(context, [UIColor whiteColor].CGColor);
    //开始绘制骨架图
    //1.画关键点
    CGContextBeginPath(context);
    NSUInteger heatsSize = [result.keyPoints count];
    for(int i=0;i<heatsSize;i++) {
        SNKeyPoint *point = [result.keyPoints objectAtIndex:i];
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
        SNKeyPoint *fromKeypoints = [result.keyPoints objectAtIndex:from];
        SNKeyPoint *toKeypoints = [result.keyPoints objectAtIndex:to];
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
    return context;
}

- (UIImage *) drawSkeleton:(SNPerson *) result size:(CGSize) size{
    CGImageRef imageRef;
    NSAssert((result!=nil && result.keyPoints!=nil), @"result is nil or result.keyPoints is nil!!!");
    
    CGContextRef context = [self drawSkeletonToCGContext:result size:size];
    
    imageRef = CGBitmapContextCreateImage(context);
    UIImage *img = [UIImage imageWithCGImage:imageRef];
    CGImageRelease(imageRef);
    CGContextRelease(context);
    return img;
}

@end
