#import <AVFoundation/AVFoundation.h>
#import <Accelerate/Accelerate.h>
#import "AIConstants.h"
#import "PoseEstimationControllerViewController.h"
#import "PoseNet.h"
#import "PoseNetManager.h"
#import "SkeletonManager.h"
#import "Skeleton.h"
#import "VImageUtils.h"

@interface PoseEstimationControllerViewController () <AVCaptureVideoDataOutputSampleBufferDelegate>

@property (nonatomic,strong) UIImageView *imageView;
@property (nonatomic,strong) UIView *preView;
@property (nonatomic,strong) UITextView *textView;

@end

@implementation PoseEstimationControllerViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)loadView {
    [super loadView];
    NSLog(@"%f",[VImageUtils sigmoid:0.1f]);
    
    //模型库初始化
    [[PoseNetManager sharedManager] initNow];
    [[SkeletonManager sharedManager] initNow];
    
    self.preView = [[UIView alloc] initWithFrame:self.view.bounds];
    [self.preView setBackgroundColor:[UIColor redColor]];
    [self.view addSubview:self.preView];
    
    int x = self.view.bounds.size.width - SK_INPUT_WIDTH;
    int y = self.view.bounds.size.height - SK_INPUT_HEIGHT;
    CGRect rect = CGRectMake(x, y, SK_INPUT_WIDTH, SK_INPUT_HEIGHT);
    self.imageView = [[UIImageView alloc] initWithFrame:rect];
    [self.imageView setBackgroundColor:[UIColor blueColor]];
    [self.view addSubview:self.imageView];
    
    NSString *text = @"初始化";
    float fontSize = 18.f;
    CGSize size = [text sizeWithFont:[UIFont systemFontOfSize:fontSize]];
    self.textView = [[UITextView alloc] initWithFrame:CGRectMake(0, 20, self.view.bounds.size.width, size.height+10)];
    self.textView.font = [UIFont systemFontOfSize:fontSize];
    [self.textView setText:text];
    [self.textView setTextAlignment: NSTextAlignmentCenter];
    [self.textView setScrollEnabled:YES];
    [self.view addSubview:self.textView];
    
    [self setupCamera];
}

- (void)dealloc {
    [[PoseNetManager sharedManager] close];
    [[SkeletonManager sharedManager] close];
}

//初始化相机
- (void) setupCamera {
    //设置分辨率
    AVCaptureSession *session = [[AVCaptureSession alloc] init];
    if([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone)
    {
        //按照最低分辨率取值 iphone&ipod
        [session setSessionPreset:AVCaptureSessionPreset640x480];
    }else{
        //ipad
        if([session canSetSessionPreset:AVCaptureSessionPreset640x480]){
            [session setSessionPreset:AVCaptureSessionPreset640x480];
        }else{
            [session setSessionPreset:AVCaptureSessionPresetPhoto];
        }
    }
    
    //开启前摄像头
    AVCaptureDevice *device = nil;
    if (@available(iOS 10.0, *)) {
        device = [AVCaptureDevice defaultDeviceWithDeviceType:AVCaptureDeviceTypeBuiltInWideAngleCamera mediaType:AVMediaTypeVideo position:AVCaptureDevicePositionFront];
    } else {
        device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    }
    NSError *error = nil;
    AVCaptureDeviceInput *deviceInput = [AVCaptureDeviceInput deviceInputWithDevice:device error:&error];
    if ([session canAddInput:deviceInput]){
        [session addInput:deviceInput];
    }
    
    //创建预览
    AVCaptureVideoDataOutput* videoOutput = [[AVCaptureVideoDataOutput alloc] init];
    videoOutput.alwaysDiscardsLateVideoFrames = YES;
    videoOutput.videoSettings = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:kCVPixelFormatType_32BGRA] forKey:(id)kCVPixelBufferPixelFormatTypeKey];//设置像素格式
    if ([session canAddOutput:videoOutput])
        [session addOutput:videoOutput];
    AVCaptureConnection *captureConnection = [videoOutput connectionWithMediaType:AVMediaTypeVideo];
    if([captureConnection isVideoOrientationSupported]) {
        captureConnection.videoOrientation = [self getCaptureVideoOrientation];
    }
    
    dispatch_queue_t queue = dispatch_queue_create("myQueue", NULL);
    [videoOutput setSampleBufferDelegate:self queue:queue];
    
    
    AVCaptureVideoPreviewLayer* preLayer = [AVCaptureVideoPreviewLayer layerWithSession: session];//相机拍摄预览图层
    CGRect rect = CGRectMake(0, 0, self.view.bounds.size.width,self.view.bounds.size.height);
    preLayer.frame = [VImageUtils cropRect:rect];
    preLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    [self.preView.layer addSublayer:preLayer];
    
    
    [session startRunning];
    if (error) {
        UIAlertController *alertController = [UIAlertController alertControllerWithTitle:[NSString stringWithFormat:@"打开摄像头错误,%d",(int)[error code]] message:[error localizedDescription] preferredStyle:UIAlertControllerStyleAlert];
        [alertController addAction:[UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:nil]];
        [self presentViewController:alertController animated:YES completion:nil];
    }
}

- (long) getInterval {
    NSDate *date = [NSDate date];
    return (long)([date timeIntervalSince1970] * 1000);
}

//捕捉一个视频帧
- (void)captureOutput:(AVCaptureOutput *)output didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer fromConnection:(AVCaptureConnection *)connection
{
//    long begin1 = [self getInterval];
    // 为媒体数据设置一个CMSampleBuffer的Core Video图像缓存对象
    CVPixelBufferRef pixelBufferRef = CMSampleBufferGetImageBuffer(sampleBuffer);
    CVPixelBufferRef cropPixelBufferRef = NULL;
//    CIImage *mirrorImage;
//    CIImage *outputImage;
//    CIContext *context;
    CGColorSpaceRef cSpace = CGColorSpaceCreateDeviceRGB();
    if(pixelBufferRef == NULL) {
        goto onExit;
    }
    cropPixelBufferRef = [VImageUtils cropAndResize:pixelBufferRef toSize:CGSizeMake(INPUT_WIDTH, INPUT_HEIGHT)];
    if(cropPixelBufferRef == NULL) {
        goto onExit;
    }
//    mirrorImage = [CIImage imageWithCVImageBuffer:cropPixelBufferRef];
//    CVPixelBufferRelease(cropPixelBufferRef);
//    CGAffineTransform transform = CGAffineTransformIdentity;
//    transform = CGAffineTransformScale(transform, -1, 1);
//    outputImage = [mirrorImage imageByApplyingTransform:transform];
//    context = [CIContext contextWithOptions:nil];
//    [context render:outputImage toCVPixelBuffer:cropPixelBufferRef bounds:mirrorImage.extent colorSpace:cSpace];
    
    
//    long begin2 = [self getInterval];
//    NSLog(@"step1:%ld",(begin2-begin1));
    [self runModel:cropPixelBufferRef];
//    NSLog(@"====\n");
onExit:
    if(cropPixelBufferRef != NULL) {
        CVPixelBufferRelease(cropPixelBufferRef);
    }
    CGColorSpaceRelease(cSpace);
}

//2.识别当前帧，输出SNPoseResult
- (void) runModel:(CVPixelBufferRef) ref {
    //1.通过model识别为SNPoseResult结构
//    long begin1 = [self getInterval];
    SNPerson *result = [[PoseNetManager sharedManager] estimateSinglePose:ref];
//    dispatch_async(dispatch_get_main_queue(), ^{
//        [self.imageView setPerson:result];
//    });
//    long begin2 = [self getInterval];
//    NSLog(@"step2:%ld",(begin2-begin1));
    if(result){
        //2. 将当前结果，画出一张bitmap出来，尺寸：96*96
        UIImage *skeletonImg = [[PoseNetManager sharedManager] drawSkeleton:result size:CGSizeMake(SK_INPUT_WIDTH, SK_INPUT_HEIGHT)];
        //3.通过mobilenet模型识别骨架图
        NSArray *result = [[SkeletonManager sharedManager] classifyFrame:skeletonImg];
        NSMutableString *str = [NSMutableString stringWithCapacity:1];
        int index = -1;
        float temp = 0.f;
        if(result){
            for(int i=0;i<[result count];i++){
                float val = [[result objectAtIndex:i] floatValue];
                if(val > temp) {
                    index = i;
                    temp = val;
                }
                [str appendFormat:@"姿势：%@ 准确率：%f\n",[self getValidPose:i],val];
            }
        }
        [str appendFormat:@"当前姿势为：%@",[self getValidPose:index]];

        dispatch_async(dispatch_get_main_queue(), ^{
            [self.textView setText:[str copy]];
            [self.imageView setImage:skeletonImg];
        });
    }
}

- (NSString *) getValidPose:(int) index {
    NSString *resultStr = @" 未知 ";
    switch (index) {
        case 0:
            resultStr = @" 大 ";
            break;
        case 1:
            resultStr = @" 举 ";
            break;
        case 2:
            resultStr = @" 站 ";
            break;
        default:
            break;
    }
    return resultStr;
}

- (AVCaptureVideoOrientation) getCaptureVideoOrientation {
    AVCaptureVideoOrientation result;
    
    UIDeviceOrientation deviceOrientation = [UIDevice currentDevice].orientation;
    switch (deviceOrientation) {
        case UIDeviceOrientationPortrait:
        case UIDeviceOrientationFaceUp:
        case UIDeviceOrientationFaceDown:
            result = AVCaptureVideoOrientationPortrait;
            break;
        case UIDeviceOrientationPortraitUpsideDown:
            //如果这里设置成AVCaptureVideoOrientationPortraitUpsideDown，则视频方向和拍摄时的方向是相反的。
            result = AVCaptureVideoOrientationPortrait;
            break;
        case UIDeviceOrientationLandscapeLeft:
            result = AVCaptureVideoOrientationLandscapeRight;
            break;
        case UIDeviceOrientationLandscapeRight:
            result = AVCaptureVideoOrientationLandscapeLeft;
            break;
        default:
            result = AVCaptureVideoOrientationPortrait;
            break;
    }
    
    return result;
}

@end
