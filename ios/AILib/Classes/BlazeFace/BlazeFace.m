#import <TFLTensorFlowLite/TFLTensorFlowLite.h>
#import "BlazeFace.h"
#import "AIConstants.h"
#import "VImageUtils.h"
#import "BlazeFaceUtil.h"

/**
* BlazeFace模型，模型很小，可以用在边缘计算领域
* 模型信息：
* [  1 128 128   3]
* [{
* 'name': 'regressors',
* 'index': 175,
* 'shape': array([  1, 896,  16], dtype=int32),
* 'dtype': <class 'numpy.float32'>, 'quantization': (0.0, 0)
* }, {
* 'name': 'classificators',
* 'index': 174,
* 'shape': array([  1, 896,   1], dtype=int32),
* 'dtype': <class 'numpy.float32'>,'quantization': (0.0, 0)
* }]
*/
@interface BlazeFace ()

@property (nonatomic,strong) TFLInterpreter *interpreter;
@property (nonatomic,strong) TFLTensor *inputTensor;
@property (nonatomic,strong) TFLTensor *regressorsTensor;
@property (nonatomic,strong) TFLTensor *classificatorsTensor;
@property (nonatomic,strong) NSArray *mAnchors;

@end

@implementation BlazeFace

- (void)initSync:(int)threadCount {
    Detections *detection = [[Detections alloc] init];
    NSString *modelPath = [[NSBundle  mainBundle] pathForResource:BF_MODEL_NAME ofType:@"tflite"];
    NSAssert(modelPath!=nil, @"hand tracking model is not exists!");
    TFLInterpreterOptions *options = [[TFLInterpreterOptions alloc] init];
    [options setNumberOfThreads:threadCount];
    
    self.mAnchors = [BlazeFaceUtil genAnchor];
    
    //初始化TFLite
    NSError *error = nil;
    self.interpreter = [[TFLInterpreter alloc] initWithModelPath:modelPath options:options error:&error];
    NSAssert(error == nil, @"error:%@",[error localizedDescription]);
    
    
    //初始化input&output
    //申请内存
    error = nil;
    [self.interpreter allocateTensorsWithError:&error];
    NSAssert(error == nil, @"error:%@",[error localizedDescription]);
    
    //设置input&output
    self.inputTensor = [self.interpreter inputTensorAtIndex:0 error:&error];
    NSAssert(error == nil, @"error:%@",[error localizedDescription]);
    self.regressorsTensor = [self.interpreter outputTensorAtIndex:0 error:&error];
    NSAssert(error == nil, @"error:%@",[error localizedDescription]);
    self.classificatorsTensor = [self.interpreter outputTensorAtIndex:1 error:&error];
    NSAssert(error == nil, @"error:%@",[error localizedDescription]);
    
    //检查一下模型的输入端，是否是Float32？现在处理的输入端数据，是通过:
    //1.128*128
    //2.uint8
    NSAssert([self.inputTensor dataType] == TFLTensorDataTypeFloat32, @"hand tracking inputTensor is not TFLTensorDataTypeUInt8!!");
    
    //检查输入部分
    NSArray *inputShapes = [self.inputTensor shapeWithError:&error];
    NSAssert(inputShapes != nil, @"hand tracking input Tensor's shape is nil!");
    //[batch_size,width,height,RGB888]
    //[1,128,128,3]
    NSAssert(([[inputShapes objectAtIndex:0] intValue] == 1) &&
             [[inputShapes objectAtIndex:1] intValue] == BF_INPUT_WIDTH &&
             [[inputShapes objectAtIndex:2] intValue] == BF_INPUT_HEIGHT &&
             [[inputShapes objectAtIndex:3] intValue] == BF_INPUT_CHANNEL_NUM, @"hand tracking input tensor is not correct!");
}

- (void)closeSync {
    
}

- (Face *)detector:(CVPixelBufferRef)image size:(CGSize)size {
    NSAssert(image!=nil, @"image is nil!!!");
    Face *face = nil;
    NSError *err = nil;
    BOOL result = NO;
    NSData *data = [VImageUtils copyDataFromPixelBuffer:image];
    NSParameterAssert(data);
    
    result = [self.inputTensor copyData:data error:&err];
    NSAssert(result, @"error:%@",[err localizedDescription]);
    
    result = [self.interpreter invokeWithError:&err];
    NSAssert(result, @"error:%@",[err localizedDescription]);
    
    NSData *rawBoxesData = [self.regressorsTensor dataWithError:&err];
    NSAssert(err==nil, @"error:%@",[err localizedDescription]);
    float (*rawBoxesPoint)[BF_NUM_CLASSES][BF_NUM_BOXES][BF_NUM_COORDS];
    rawBoxesPoint = (float(*)[BF_NUM_CLASSES][BF_NUM_BOXES][BF_NUM_COORDS])rawBoxesData.bytes;
    
    NSData *rawScoresData = [self.classificatorsTensor dataWithError:&err];
    NSAssert(err==nil, @"error:%@",[err localizedDescription]);
    float (*rawScoresPoint)[BF_NUM_CLASSES][BF_NUM_BOXES][1];
    rawScoresPoint = (float(*)[BF_NUM_CLASSES][BF_NUM_BOXES][1])rawScoresData.bytes;
    
    int img_height = size.height;
    int img_width = size.width;
    
    float *rawBoxes = malloc(BF_NUM_BOXES * BF_NUM_COORDS * sizeof(float));
    memset(rawBoxes, 0, BF_NUM_BOXES * BF_NUM_COORDS * sizeof(float));
    [BlazeFaceUtil rawBoxWithReshape:(float *)rawBoxesPoint ret:rawBoxes];
    
    float *rawScores = malloc(BF_NUM_BOXES * sizeof(float));
    memset(rawScores, 0, sizeof(float) * BF_NUM_BOXES);
    [BlazeFaceUtil rawScoresWithReshape:(float *)rawScoresPoint ret:rawScores];
    
    CGRect rect = CGRectZero;
    Detections *newDetection;
    NSArray *detections = [BlazeFaceUtil processCPU:rawBoxes raw_scores:rawScores anchors_:self.mAnchors];
    if(!detections || [detections count] == 0){
        goto onExits;
    }
    newDetection = [BlazeFaceUtil origNms:detections threshold:0.3f];
    
    rect.origin.x = img_width * newDetection.xmin;
    rect.origin.y = img_height * newDetection.ymin;
    rect.size.width = img_width * newDetection.width;
    rect.size.height = img_height * newDetection.height;
    face = [[Face alloc] init];
    face.detectionRect = rect;
    
onExits:
    if(rawBoxes){
        free(rawBoxes);
    }
    if(rawScores){
        free(rawScores);
    }
    
    return face;
}

@end
