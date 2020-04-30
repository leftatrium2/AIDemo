#import "Skeleton.h"
#import "VImageUtils.h"

@interface Skeleton()
{
    BOOL isInited;
}
@property (nonatomic,strong) TFLInterpreter *interpreter;
@property (nonatomic,strong) TFLTensor *inputTensor;
@property (nonatomic,strong) TFLTensor *outputTensor;
@end

@implementation Skeleton

- (instancetype)init {
    self = [super init];
    if(self){
        isInited = NO;
    }
    return self;
}

- (void) initSync:(int) threadCount {
    NSString *modelPath = [[NSBundle  mainBundle] pathForResource:SKELETON_MODEL_NAME ofType:@"tflite"];
    NSAssert(modelPath!=nil, @"skeleton model is not exists!");
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
    //1.96*96
    //2.uint8
    NSAssert([self.inputTensor dataType] == TFLTensorDataTypeFloat32, @"skeleon inputTensor is not TFLTensorDataTypeUInt8!!");
    
    //检查输入部分
    NSArray *inputShapes = [self.inputTensor shapeWithError:&error];
    NSAssert(inputShapes != nil, @"skeleon input Tensor's shape is nil!");
    //[batch_size,width,height,RGB888]
    //[1,224,224,3]
    NSAssert(([[inputShapes objectAtIndex:0] intValue] == 1) &&
             [[inputShapes objectAtIndex:1] intValue] == SK_INPUT_WIDTH &&
             [[inputShapes objectAtIndex:2] intValue] == SK_INPUT_HEIGHT &&
             [[inputShapes objectAtIndex:3] intValue] == INPUT_POSENET_CHANNEL_NUM, @"skeleon input tensor is not correct!");
    
    isInited = YES;
}

- (void) closeSync {
}


- (NSArray *) classifyFrame:(UIImage *) image {
    NSAssert(image!=nil, @"image is nil!!!");
    if(!isInited){
        return nil;
    }
    return nil;
    NSError *err = nil;
    BOOL result = NO;
    NSData *data = [VImageUtils copyDataFromUIImage:image];
    NSParameterAssert(data);
    
    result = [self.inputTensor copyData:data error:&err];
    NSAssert(result, @"error:%@",[err localizedDescription]);


    result = [self.interpreter invokeWithError:&err];
    NSAssert(result, @"error:%@",[err localizedDescription]);

    return [self postProcess];
}

- (NSArray *) postProcess {
    NSError *err = nil;
    NSArray *shapes = [self.outputTensor shapeWithError:&err];
    NSAssert(err == nil, @"error:%@",[err localizedDescription]);
    //拿到识别模型的个数
    int sk_output_model_num = [[shapes objectAtIndex:1] intValue];
    
    float (*labelProbArrayPoint)[1][sk_output_model_num];
    float filterLabelProbArray[SK_FILTER_STAGES][sk_output_model_num];
    memset(&filterLabelProbArray, 0, SK_FILTER_STAGES * sk_output_model_num * sizeof(float));
    float labelProbArray[1][sk_output_model_num];
    memset(&labelProbArray, 0, sk_output_model_num * sizeof(float));
    NSData *data = [self.outputTensor dataWithError:&err];
    NSAssert(err == nil, @"error:%@",[err localizedDescription]);
    
    //把结果数据拷贝到新的结构里面
    labelProbArrayPoint = (float (*)[1][sk_output_model_num])data.bytes;
    for(int i=0;i<sk_output_model_num;i++) {
        labelProbArray[0][i] = (*labelProbArrayPoint)[0][i];
    }
    //将结果做平滑处理
    for(int j=0;j<sk_output_model_num;j++) {
        filterLabelProbArray[0][j] += SK_FILTER_FACTOR * ((labelProbArray[0][j]) - filterLabelProbArray[0][j]);
    }
    for(int i=0;i<SK_FILTER_STAGES;i++) {
        for(int j=0;j<sk_output_model_num;j++) {
            filterLabelProbArray[i][j] += SK_FILTER_FACTOR * ((labelProbArray[i-1][j]) - filterLabelProbArray[i][j]);
        }
    }
    for(int j=0;j<sk_output_model_num;j++) {
        labelProbArray[0][j] = filterLabelProbArray[SK_FILTER_STAGES-1][j];
    }
    //开始处理最后的识别结果
    NSMutableArray *arr = [NSMutableArray arrayWithCapacity:sk_output_model_num];
    for(int i=0;i<sk_output_model_num;i++) {
        [arr addObject:[NSNumber numberWithFloat:labelProbArray[0][i]]];
    }
    
    return arr;
}

@end
