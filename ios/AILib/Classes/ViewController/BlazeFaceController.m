#import "BlazeFaceController.h"
#import "BlazeFaceLayer.h"
#import "BlazeFace.h"
#import "BlazeFaceManager.h"
#import "CameraUtils.h"
#import "VImageUtils.h"
#import "AIConstants.h"

@interface BlazeFaceController () <SNCameraUtilsDelegate>

@property (nonatomic,strong) BlazeFaceLayer *blazeFaceLayer;
@property (nonatomic,strong) UIView *preView;
@property (nonatomic,strong) CameraUtils *cameralUtils;
@property (nonatomic,assign) CGSize preViewSize;

@end

@implementation BlazeFaceController

- (void)loadView {
    [super loadView];
    [self initView];
    [self initModel];
}

- (void)viewDidLoad {
    [super viewDidLoad];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self.cameralUtils start];
}

- (void)viewWillDisappear:(BOOL)animated {
    [self.cameralUtils stop];
    [super viewWillDisappear:animated];
}

- (void) initView {
    self.preView = [[UIView alloc] initWithFrame:self.view.bounds];
    [self.view addSubview:self.preView];
    
    self.blazeFaceLayer = [[BlazeFaceLayer alloc] init];
    UIColor *color = [UIColor colorWithRed:1.f green:0.f blue:0.f alpha:0.23f];
    [self.blazeFaceLayer setBackgroundColor:color.CGColor];
    [self.blazeFaceLayer setFrame:self.view.bounds];
    
    self.cameralUtils = [[CameraUtils alloc] init];
    [self.cameralUtils setDelegate:self];
    [self.cameralUtils initWithPreview:self.preView poseLayer:self.blazeFaceLayer];
    self.preViewSize = self.blazeFaceLayer.frame.size;
}

- (void) initModel {
    [[BlazeFaceManager sharedManager] initAsync];
}

- (void)captureOutput:(AVCaptureOutput *)output didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer fromConnection:(AVCaptureConnection *)connection {
    if(![self.cameralUtils isOK]){
        //摄像头初始化失败
        NSLog(@"摄像头初始化失败！！！");
        return;
    }
    if(![[BlazeFaceManager sharedManager] isOk]){
        NSLog(@"模型初始化未完成！！！");
        return;
    }
    //初始化变量部分
    CVPixelBufferRef pixelBufferRef = CMSampleBufferGetImageBuffer(sampleBuffer);
    CVPixelBufferRef cropPixelBufferRef = NULL;
    Face *result = NULL;
    CGColorSpaceRef cSpace = CGColorSpaceCreateDeviceRGB();
    if(pixelBufferRef == NULL) {
        goto onExit;
    }
    //剪切以及缩放到制定宽高
    cropPixelBufferRef = [VImageUtils cropAndResize:pixelBufferRef toSize:CGSizeMake(BF_INPUT_WIDTH, BF_INPUT_HEIGHT)];
    if(cropPixelBufferRef == NULL) {
        goto onExit;
    }
    result = [[BlazeFaceManager sharedManager] detector:cropPixelBufferRef size:self.preViewSize];
    //绘图到当前屏幕
    [self.blazeFaceLayer setResult:result];
    
    
onExit:
    if(cropPixelBufferRef != NULL) {
        CVPixelBufferRelease(cropPixelBufferRef);
    }
    CGColorSpaceRelease(cSpace);
}

@end
