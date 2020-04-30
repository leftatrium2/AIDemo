Pod::Spec.new do |s|
  s.name             = 'AILib'
  s.version          = '0.0.1'
  s.summary          = 'AILib'
  s.homepage         = 'no'
  s.license          =  'MIT'
  s.author           = { 'leftatrium' => 'leftatrium@vip.qq.com' }
  s.source           = { :git => '', :tag => s.version.to_s }
  
  s.ios.deployment_target = '9.0'
  s.source_files = 'Classes/**/*.{h,m}'
  s.resource = 'Classes/**/*.bundle'
  s.requires_arc = true
  s.static_framework = true
  s.frameworks = 'UIKit', 'Foundation'
  
  
  EOS
end
