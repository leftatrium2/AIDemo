#import "BlazeFaceManager.h"
#import "BlazeFace.h"
#import "AIConstants.h"

@interface BlazeFaceManager ()

@property (nonatomic,strong) BlazeFace *blazeFace;
@property (nonatomic,assign) BOOL isInited;

@end

static BlazeFaceManager *manager = nil;

@implementation BlazeFaceManager

+ (instancetype)sharedManager {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        manager = [[self alloc] init];
    });
    return manager;
}

- (instancetype)init {
    self = [super init];
    if(self){
        _blazeFace = [[BlazeFace alloc] init];
        _isInited = NO;
    }
    return self;
}

- (BOOL)isOk {
    return self.isInited;
}

- (void)initNow {
    [self.blazeFace initSync:POSE_THREAD_COUNT];
    self.isInited = YES;
}

- (void)initAsync {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        [self initNow];
    });
}

- (void)close {
    [self.blazeFace closeSync];
    self.isInited = NO;
}

- (Face *)detector:(CVPixelBufferRef)image size:(CGSize) size {
    if(self.isInited){
        return [self.blazeFace detector:image size:size];
    }
    return nil;
}

@end
