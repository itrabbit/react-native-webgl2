require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name                = package['name']
  s.version             = package['version']
  s.summary             = package['description']
  s.homepage            = 'https://github.com/itrabbit/react-native-webgl2'
  s.license             = package['license']
  s.author              = package['author']
  s.source              = { :git => 'https://github.com/itrabbit/react-native-webgl2.git', :tag => "v#{package['version']}" }
  s.requires_arc        = true
  s.platform            = :ios, '10.3'
  s.pod_target_xcconfig = { 'ENABLE_BITCODE' => 'YES' }
  s.source_files        = 'ios/*.{h,m}','cpp/*.{h,c,cpp,mm}'
  s.framework           = 'OpenGLES'
  s.dependency          'React'
end