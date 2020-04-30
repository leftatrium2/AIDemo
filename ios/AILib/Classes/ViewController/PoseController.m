#import "PoseController.h"
#import "CameraUtils.h"
#import "PoseLayer.h"
#import "PoseNetManager.h"
#import "VImageUtils.h"

@interface PoseController () <SNCameraUtilsDelegate>

@property (nonatomic,strong) PoseLayer *poseLayer;
@property (nonatomic,strong) UIView *preView;
@property (nonatomic,strong) CameraUtils *cameralUtils;

@end

@implementation PoseController

- (void)loadView {
    [super loadView];
    [self initView];
    [self initModel];
}

- (void) initView {
    self.preView = [[UIView alloc] initWithFrame:self.view.bounds];
    [self.view addSubview:self.preView];
    
    self.poseLayer = [[PoseLayer alloc] init];
    UIColor *color = [UIColor colorWithRed:1.f green:0.f blue:0.f alpha:0.23f];
    [self.poseLayer setBackgroundColor:color.CGColor];
    [self.poseLayer setFrame:self.view.bounds];
    
    self.cameralUtils = [[CameraUtils alloc] init];
    [self.cameralUtils setDelegate:self];
    [self.cameralUtils initWithPreview:self.preView poseLayer:self.poseLayer];
}

- (void) initModel {
    [[PoseNetManager sharedManager] initAsync];
}

- (void)viewDidLoad {
    [super viewDidLoad];
}

- (void)viewWillAppear:(BOOL)animated {
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
    if(![[PoseNetManager sharedManager] isOk]){
        NSLog(@"模型初始化未完成！！！");
        return;
    }
    //初始化变量部分
    CVPixelBufferRef pixelBufferRef = CMSampleBufferGetImageBuffer(sampleBuffer);
    CVPixelBufferRef cropPixelBufferRef = NULL;
    SNPerson *result = NULL;
    CGColorSpaceRef cSpace = CGColorSpaceCreateDeviceRGB();
    if(pixelBufferRef == NULL) {
        goto onExit;
    }
    //剪切以及缩放到制定宽高
    cropPixelBufferRef = [VImageUtils cropAndResize:pixelBufferRef toSize:CGSizeMake(INPUT_WIDTH, INPUT_HEIGHT)];
    if(cropPixelBufferRef == NULL) {
        goto onExit;
    }
    //进入模型识别
    result = [[PoseNetManager sharedManager] estimateSinglePose:cropPixelBufferRef];
    //绘图到当前屏幕
    [self.poseLayer setResult:result];
    
onExit:
    if(cropPixelBufferRef != NULL) {
        CVPixelBufferRelease(cropPixelBufferRef);
    }
    CGColorSpaceRelease(cSpace);
}

@end
