#import "PoseNetManager.h"
#import "AIConstants.h"

@interface PoseNetManager()

@property (nonatomic,strong) SNPoseNet *poseNet;
@property (nonatomic,assign) BOOL isInited;

@end

static PoseNetManager *manager = nil;

@implementation PoseNetManager

+ (instancetype) sharedManager {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        manager = [[self alloc] init];
    });
    return manager;
}

- (instancetype)init {
    self = [super init];
    if(self){
        _poseNet = [[SNPoseNet alloc] init];
        _isInited = NO;
    }
    return self;
}

- (BOOL) isOk {
    return self.isInited;
}

- (void)initAsync {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        [self initNow];
    });
}

- (void) initNow {
    [self.poseNet initSync:POSE_THREAD_COUNT];
    self.isInited = YES;
}

- (void) close {
    [self.poseNet closeSync];
}

- (SNPerson *) estimateSinglePose:(CVPixelBufferRef) ref {
    if(self.poseNet){
        return [self.poseNet estimateSinglePose:ref];
    }
    return nil;
}

- (UIImage *) drawSkeleton:(SNPerson *) result size:(CGSize) size {
    if(result){
        return [self.poseNet drawSkeleton:result size:size];
    }
    return nil;
}

@end
