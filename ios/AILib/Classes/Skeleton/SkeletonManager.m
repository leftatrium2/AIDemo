#import "SkeletonManager.h"
#import "Skeleton.h"

@interface SkeletonManager()

@property (nonatomic,strong) Skeleton *skeleton;

@end

static SkeletonManager *manager = nil;
@implementation SkeletonManager

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
        _skeleton = [[Skeleton alloc] init];
    }
    return self;
}

- (void) initNow {
    [self.skeleton initSync:2];
}

- (void) close {
    [self.skeleton closeSync];
}

- (NSArray *) classifyFrame:(UIImage *) image {
    return [self.skeleton classifyFrame:image];
}

@end
