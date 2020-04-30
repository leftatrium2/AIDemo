#import "Face.h"


@implementation Face

- (instancetype)init {
    self = [super init];
    if(self){
        _keyPoints = [NSMutableArray array];
        _detectionRect = CGRectZero;
        _scores = 0.f;
    }
    return self;
}

@end

@implementation FaceKeyPoint

- (instancetype)init {
    self = [super init];
    if(self){
        _facePart = NOSE;
        _position = CGPointZero;
        _score = 0.f;
    }
    return self;
}

@end

#pragma mark 一些辅助数据结构，不要直接调用

@implementation Anchor

- (instancetype)init {
    self = [super init];
    if(self){
        _h = 0.f;
        _w = 0.f;
        _x_center = 0.f;
        _y_center = 0.f;
    }
    return self;
}

@end

@implementation Detections

- (instancetype)init {
    self = [super init];
    if(self){
        _class_id = 0.f;
        _height =0.f;
        _width = 0.f;
        _score = 0.f;
        _xmin = 0.f;
        _ymin = 0.f;
    }
    return self;
}

@end
