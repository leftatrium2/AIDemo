//
//  面部侦测-数据结构
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

typedef enum _FacePart {
    NOSE,
    MOUTH,
    LEFT_EYE,
    RIGHT_EYE,
    LEFT_EAR,
    RIGHT_EAR,
} SNFacePart;

@interface FaceKeyPoint : NSObject
@property (nonatomic,assign) int facePart;
@property (nonatomic,assign) CGPoint position;
@property (nonatomic,assign) float score;
@end

@interface Face : NSObject
@property (nonatomic,strong) NSMutableArray *keyPoints;
@property (nonatomic,assign) CGRect detectionRect;
@property (nonatomic,assign) float scores;
@end

#pragma mark 一些辅助数据结构，不要直接调用

@interface Anchor : NSObject

@property (nonatomic,assign) float h;
@property (nonatomic,assign) float w;
@property (nonatomic,assign) float x_center;
@property (nonatomic,assign) float y_center;

@end

@interface Detections : NSObject

@property (nonatomic,assign) float class_id;
@property (nonatomic,assign) float height;
@property (nonatomic,assign) float width;
@property (nonatomic,assign) float score;
@property (nonatomic,assign) float xmin;
@property (nonatomic,assign) float ymin;

@end

NS_ASSUME_NONNULL_END
