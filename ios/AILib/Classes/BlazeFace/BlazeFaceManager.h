//
//  面部侦测管理器
//

#import <UIKit/UIKit.h>
#import "Face.h"

NS_ASSUME_NONNULL_BEGIN

@interface BlazeFaceManager : NSObject
+ (instancetype) sharedManager;
- (BOOL) isOk;
- (void) initAsync;
- (void) initNow;
- (void) close;
- (Face *)detector:(CVPixelBufferRef)image size:(CGSize) size ;
@end

NS_ASSUME_NONNULL_END
