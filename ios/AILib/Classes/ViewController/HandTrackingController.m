#import "HandTrackingController.h"
#import "HandTrackingManager.h"
#import "HandTrackingLayer.h"
#import "CameraUtils.h"
#import "AIConstants.h"
#import "Hand.h"
#import "VImageUtils.h"

@interface HandTrackingController () <SNCameraUtilsDelegate>

@property (nonatomic,strong) HandTrackingLayer *handTrackingLayer;
@property (nonatomic,strong) UIView *preView;
@property (nonatomic,strong) CameraUtils *cameralUtils;

@end

@implementation HandTrackingController

- (void)loadView {
    [super loadView];
    
    [self initView];
    [self initModel];
}

- (void) initView {
    self.preView = [[UIView alloc] initWithFrame:self.view.bounds];
    [self.view addSubview:self.preView];
    
    self.handTrackingLayer = [[HandTrackingLayer alloc] init];
    UIColor *color = [UIColor colorWithRed:1.f green:0.f blue:0.f alpha:0.23f];
    [self.handTrackingLayer setBackgroundColor:color.CGColor];
    [self.handTrackingLayer setFrame:self.view.bounds];
    
    self.cameralUtils = [[CameraUtils alloc] init];
    [self.cameralUtils setDelegate:self];
    [self.cameralUtils initWithPreview:self.preView poseLayer:self.handTrackingLayer];
}

- (void) initModel {
    [[HandTrackingManager sharedManager] initAsync];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)viewDidAppear:(BOOL)animated {
    [self.cameralUtils start];
}

- (void)viewWillDisappear:(BOOL)animated {
    [self.cameralUtils stop];
}

- (void)captureOutput:(AVCaptureOutput *)output didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer fromConnection:(AVCaptureConnection *)connection {
    if(![self.cameralUtils isOK]){
        //摄像头初始化失败
        NSLog(@"摄像头初始化失败！！！");
        return;
    }
    if(![[HandTrackingManager sharedManager] isOk]){
        NSLog(@"模型初始化未完成！！！");
        return;
    }
    
    //初始化变量部分
    CVPixelBufferRef pixelBufferRef = CMSampleBufferGetImageBuffer(sampleBuffer);
    CVPixelBufferRef cropPixelBufferRef = NULL;
    Hand *result = NULL;
    CGColorSpaceRef cSpace = CGColorSpaceCreateDeviceRGB();
    if(pixelBufferRef == NULL) {
        goto onExit;
    }
    //剪切以及缩放到制定宽高
    cropPixelBufferRef = [VImageUtils cropAndResize:pixelBufferRef toSize:CGSizeMake(HT_INPUT_WIDTH, HT_INPUT_HEIGHT)];
    if(cropPixelBufferRef == NULL) {
        goto onExit;
    }
    result = [[HandTrackingManager sharedManager] tracking:cropPixelBufferRef];
    //绘图到当前屏幕
    [self.handTrackingLayer setResult:result];
    
onExit:
    if(cropPixelBufferRef != NULL) {
        CVPixelBufferRelease(cropPixelBufferRef);
    }
    CGColorSpaceRelease(cSpace);
}

@end
