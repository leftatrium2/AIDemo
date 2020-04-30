#import <TFLTensorFlowLite/TFLTensorFlowLite.h>
#import "HandTrakcing.h"
#import "AIConstants.h"
#import "VImageUtils.h"

/**
 *  地址：https://ai.googleblog.com/2019/08/on-device-real-time-hand-tracking-with.html
 * [  1 256 256   3]
 * [{
 * 'name': 'ld_21_2d',
 * 'index': 893,
 * 'shape': array([ 1, 42], dtype=int32),
 * 'dtype': <class 'numpy.float32'>,
 * 'quantization': (0.0, 0)
 * }, {
 * 'name': 'output_handflag',
 * 'index': 894,
 * 'shape': array([1, 1], dtype=int32),
 * 'dtype': <class 'numpy.float32'>,
 * 'quantization': (0.0, 0)
 * }]
 */
@interface SNHandTrakcing()

@property (nonatomic,strong) TFLInterpreter *interpreter;
@property (nonatomic,strong) TFLTensor *inputTensor;
@property (nonatomic,strong) TFLTensor *outputTensor;

@end

@implementation SNHandTrakcing

- (void)initSync:(int)threadCount
{
    NSString *modelPath = [[NSBundle  mainBundle] pathForResource:HT_MODEL_NAME ofType:@"tflite"];
    NSAssert(modelPath!=nil, @"hand tracking model is not exists!");
    TFLInterpreterOptions *options = [[TFLInterpreterOptions alloc] init];
    [options setNumberOfThreads:threadCount];
    
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
    self.outputTensor = [self.interpreter outputTensorAtIndex:0 error:&error];
    NSAssert(error == nil, @"error:%@",[error localizedDescription]);
    
    //检查一下模型的输入端，是否是Float32？现在处理的输入端数据，是通过:
    //1.256*256
    //2.uint8
    NSAssert([self.inputTensor dataType] == TFLTensorDataTypeFloat32, @"hand tracking inputTensor is not TFLTensorDataTypeUInt8!!");
    
    //检查输入部分
    NSArray *inputShapes = [self.inputTensor shapeWithError:&error];
    NSAssert(inputShapes != nil, @"hand tracking input Tensor's shape is nil!");
    //[batch_size,width,height,RGB888]
    //[1,256,256,3]
    NSAssert(([[inputShapes objectAtIndex:0] intValue] == 1) &&
             [[inputShapes objectAtIndex:1] intValue] == HT_INPUT_WIDTH &&
             [[inputShapes objectAtIndex:2] intValue] == HT_INPUT_HEIGHT &&
             [[inputShapes objectAtIndex:3] intValue] == HT_INPUT_CHANNEL_NUM, @"hand tracking input tensor is not correct!");
}

- (void)closeSync {
}

- (Hand *) tracking:(CVPixelBufferRef) image {
    NSAssert(image!=nil, @"image is nil!!!");
    
    NSError *err = nil;
    BOOL result = NO;
    NSData *data = [VImageUtils copyDataFromPixelBuffer:image];
    NSParameterAssert(data);
    
    result = [self.inputTensor copyData:data error:&err];
    NSAssert(result, @"error:%@",[err localizedDescription]);
    
    result = [self.interpreter invokeWithError:&err];
    NSAssert(result, @"error:%@",[err localizedDescription]);
    
    return [self postProcess];
}

- (Hand *) postProcess {
    NSError *err = nil;
    Hand *hand = [[Hand alloc] init];
    NSArray *shapes = [self.outputTensor shapeWithError:&err];
    NSAssert(err == nil, @"error:%@",[err localizedDescription]);
    
    int output_model_num = [[shapes objectAtIndex:1] intValue];
    
    float (*landMarksPoint)[1][output_model_num];
    float landMarks[1][output_model_num];
    memset(&landMarks, 0, sizeof(float) * 1 * output_model_num);
    NSData *data = [self.outputTensor dataWithError:&err];
    NSAssert(err == nil, @"error:%@",[err localizedDescription]);
    //把结果数据拷贝到新的结构里面
    landMarksPoint = (float (*)[1][output_model_num])data.bytes;
    for(int i=0;i<output_model_num;i++) {
        landMarks[0][i] = (*landMarksPoint)[0][i];
    }
    //将得到的坐标点放到SNHand
    for(int i=0;i<output_model_num;i = i + 2) {
        CGPoint point = CGPointZero;
        point.x = landMarks[0][i];
        point.y = landMarks[0][i+1];
        [hand add:point];
    }
    
    return hand;
}

@end
