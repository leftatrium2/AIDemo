#import "MainViewController.h"
#import "PoseController.h"
#import "PoseEstimationControllerViewController.h"
#import "BlazeFaceController.h"
#import "GpuImageController.h"
#import "HandTrackingController.h"

#define BTN1_TAG 1
#define BTN2_TAG 2
#define BTN3_TAG 3
#define BTN4_TAG 4
#define BTN5_TAG 5

@interface MainViewController ()

@property (nonatomic,strong) UIButton *btn1;
@property (nonatomic,strong) UIButton *btn2;
@property (nonatomic,strong) UIButton *btn3;
@property (nonatomic,strong) UIButton *btn4;
@property (nonatomic,strong) UIButton *btn5;

@end

@implementation MainViewController

- (void)loadView {
    [super loadView];
    
    self.btn1 = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [self.btn1 setBackgroundColor:[UIColor redColor]];
    [self.btn1 setTitle:@"姿势识别" forState:UIControlStateNormal];
    CGRect rect = CGRectMake(20.f, 70.f, self.view.bounds.size.width-40.f, 40.f);
    self.btn1.frame = rect;
    [self.view addSubview:self.btn1];
    [self.btn1 addTarget:self action:@selector(onClick:) forControlEvents:UIControlEventTouchUpInside];
    [self.btn1 setTag:BTN1_TAG];
    
    _btn2 = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [self.btn2 setTitle:@"姿势骨架图" forState:UIControlStateNormal];
    [self.btn2 setBackgroundColor:[UIColor redColor]];
    rect = CGRectMake(20.f, rect.origin.y + rect.size.height + 10.f, self.view.bounds.size.width-40.f, 40.f);
    self.btn2.frame = rect;
    [self.view addSubview:self.btn2];
    [self.btn2 addTarget:self action:@selector(onClick:) forControlEvents:UIControlEventTouchUpInside];
    [self.btn2 setTag:BTN2_TAG];
    
    _btn3 = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [self.btn3 setTitle:@"面部识别" forState:UIControlStateNormal];
    [self.btn3 setBackgroundColor:[UIColor redColor]];
    rect = CGRectMake(20.f, rect.origin.y + rect.size.height + 10.f, self.view.bounds.size.width-40.f, 40.f);
    self.btn3.frame = rect;
    [self.view addSubview:self.btn3];
    [self.btn3 addTarget:self action:@selector(onClick:) forControlEvents:UIControlEventTouchUpInside];
    [self.btn3 setTag:BTN3_TAG];

    _btn5 = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [self.btn5 setTitle:@"手指骨架图" forState:UIControlStateNormal];
    [self.btn5 setBackgroundColor:[UIColor redColor]];
    rect = CGRectMake(20.f, rect.origin.y + rect.size.height + 10.f, self.view.bounds.size.width-40.f, 40.f);
    self.btn5.frame = rect;
    [self.view addSubview:self.btn5];
    [self.btn5 addTarget:self action:@selector(onClick:) forControlEvents:UIControlEventTouchUpInside];
    [self.btn5 setTag:BTN5_TAG];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void) onClick:(UIButton *) btn{
    if(btn.tag == BTN1_TAG){
        PoseEstimationControllerViewController *controller = [[PoseEstimationControllerViewController alloc] init];
//        [self presentViewController:controller animated:YES completion:nil];
        
        [self.navigationController pushViewController:controller animated:YES];
    }else if(btn.tag == BTN2_TAG){
        PoseController *controller = [[PoseController alloc] init];
        [self.navigationController pushViewController:controller animated:YES];
    }else if(btn.tag == BTN3_TAG){
        BlazeFaceController *controller = [[BlazeFaceController alloc] init];
        [self.navigationController pushViewController:controller animated:YES];
    }else if(btn.tag == BTN5_TAG){
        HandTrackingController *controller = [[HandTrackingController alloc] init];
        [self.navigationController pushViewController:controller animated:YES];
    }
}

@end
