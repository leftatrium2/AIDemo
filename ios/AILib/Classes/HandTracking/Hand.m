#import "Hand.h"

@interface Hand()

@property (nonatomic,strong) NSMutableArray *points;

@end

@implementation Hand

- (instancetype)init {
    self = [super init];
    if(self){
        _points = [NSMutableArray array];
    }
    return self;
}

- (NSArray *)getPoint {
    return self.points;
}

- (void)add:(CGPoint)point {
    [self.points addObject:[NSValue valueWithCGPoint:point]];
}

@end
