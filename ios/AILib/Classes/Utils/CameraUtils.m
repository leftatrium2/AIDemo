#import "CameraUtils.h"

@interface CameraUtils()<AVCaptureVideoDataOutputSampleBufferDelegate>

@property (nonatomic,strong) id<SNCameraUtilsDelegate> dele;
@property (nonatomic,strong) UIView *preView;
@property (nonatomic,strong) CALayer *poseLayer;
@property (nonatomic,assign) BOOL isInited;
@property (nonatomic,strong) AVCaptureSession *session;

@end

@implementation CameraUtils

- (void) initWithPreview:(UIView *) preView poseLayer:(CALayer *) poseLayer {
    self.preView = preView;
    self.poseLayer = poseLayer;
    self.isInited = NO;
    [self setupCamera];
}

- (BOOL) isOK {
    return self.isInited;
}

- (void) setupCamera {
    //设置分辨率
    self.session = [[AVCaptureSession alloc] init];
    if([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone)
    {
        //按照最低分辨率取值 iphone&ipod
        [self.session setSessionPreset:AVCaptureSessionPreset640x480];
    }else{
        //ipad
        if([self.session canSetSessionPreset:AVCaptureSessionPreset640x480]){
            [self.session setSessionPreset:AVCaptureSessionPreset640x480];
        }else{
            [self.session setSessionPreset:AVCaptureSessionPresetPhoto];
        }
    }
    
    //开启前摄像头
    AVCaptureDevice *device = nil;
    if (@available(iOS 10.0, *)) {
        device = [AVCaptureDevice defaultDeviceWithDeviceType:AVCaptureDeviceTypeBuiltInWideAngleCamera mediaType:AVMediaTypeVideo position:AVCaptureDevicePositionFront];
//        if (@available(iOS 10.2, *)) {
//            device = [AVCaptureDevice defaultDeviceWithDeviceType:AVCaptureDeviceTypeBuiltInDualCamera mediaType:AVMediaTypeVideo position:AVCaptureDevicePositionFront];
//        } else {
//            device = [AVCaptureDevice defaultDeviceWithDeviceType:AVCaptureDeviceTypeBuiltInWideAngleCamera mediaType:AVMediaTypeVideo position:AVCaptureDevicePositionFront];
//        }
    } else {
        device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    }
    NSError *error = nil;
    AVCaptureDeviceInput *deviceInput = [AVCaptureDeviceInput deviceInputWithDevice:device error:&error];
    if(error!=nil){
        self.isInited = NO;
        NSLog(@"error:%@",[error localizedDescription]);
    }
    if ([self.session canAddInput:deviceInput]){
        [self.session addInput:deviceInput];
    }
    
    //创建预览
    AVCaptureVideoDataOutput* videoOutput = [[AVCaptureVideoDataOutput alloc] init];
    videoOutput.alwaysDiscardsLateVideoFrames = YES;
    videoOutput.videoSettings = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:kCVPixelFormatType_32BGRA] forKey:(id)kCVPixelBufferPixelFormatTypeKey];//设置像素格式
    if ([self.session canAddOutput:videoOutput])
        [self.session addOutput:videoOutput];
    AVCaptureConnection *captureConnection = [videoOutput connectionWithMediaType:AVMediaTypeVideo];
    if([captureConnection isVideoOrientationSupported]) {
        captureConnection.videoOrientation = [self getCaptureVideoOrientation];
    }
    if(captureConnection.supportsVideoMirroring){
        captureConnection.videoMirrored = YES;
    }
    
    dispatch_queue_t queue = dispatch_queue_create("myQueue", NULL);
    [videoOutput setSampleBufferDelegate:self queue:queue];
    
    
    AVCaptureVideoPreviewLayer* preLayer = [AVCaptureVideoPreviewLayer layerWithSession: self.session];//相机拍摄预览图层
    preLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    preLayer.frame = [self.preView bounds];
    [self.preView.layer addSublayer:preLayer];
    [self.preView.layer insertSublayer:self.poseLayer above:preLayer];
    
    self.isInited = YES;
}

- (void) setDelegate:(id<SNCameraUtilsDelegate>) dele {
    if(dele) {
        self.dele = dele;
    }
}

- (void) start {
    if(self.session){
        if(!self.session.isRunning){
            [self.session startRunning];
        }
    }
}

- (void)stop {
    if(self.session){
        if(self.session.isRunning){
            [self.session stopRunning];
        }
    }
}

- (void)captureOutput:(AVCaptureOutput *)output didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer fromConnection:(AVCaptureConnection *)connection {
    if(self.dele) {
        [self.dele captureOutput:output didOutputSampleBuffer:sampleBuffer fromConnection:connection];
    }
}

- (void)captureOutput:(AVCaptureOutput *)output didDropSampleBuffer:(CMSampleBufferRef)sampleBuffer fromConnection:(AVCaptureConnection *)connection {
    
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
