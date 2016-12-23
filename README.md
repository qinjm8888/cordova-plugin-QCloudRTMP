ionic2  腾讯云视频服务RTMP 插件

插件安装
$ ionic plugin add https://github.com/qinjm8888/cordova-plugin-QCloudRTMP.git

插件中提供一个方法 
QCloudRTMP.play(param1, param2, function(msg){},function(msg) {});

param1:类型  1：播放直播   2：推流
param2：url  播放地址/推流地址
function1：成功回调
function2：失败回调