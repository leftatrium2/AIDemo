#import "HandTrackingManager.h"
#import "HandTrakcing.h"
#import "AIConstants.h"

@interface HandTrackingManager()

@property (nonatomic,strong) SNHandTrakcing *handTracking;
@property (nonatomic,assign) BOOL isInited;

@end

static HandTrackingManager *manager = nil;

@implementation HandTrackingManager

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
        _handTracking = [[SNHandTrakcing alloc] init];
        _isInited = NO;
    }
    return self;
}

- (BOOL)isOk {
    return self.isInited;
}

- (void)initAsync {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        [self initNow];
    });
}

- (void)initNow {
    [self.handTracking initSync:POSE_THREAD_COUNT];
    self.isInited = YES;
}

- (void)close {
    [self.handTracking closeSync];
    self.isInited = NO;
}

- (Hand *)tracking:(CVPixelBufferRef)image {
    if(self.isInited){
        return [self.handTracking tracking:image];
    }
    return nil;
}

@end
