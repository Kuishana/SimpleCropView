# SimpleCropView
一个简单的Android图像裁剪组件，简单到就一个[Kotlin](http://kotlinlang.org/)文件，200多行代码，并支持超大图片高清预览和裁切。
## 示例
先来截两个小鬼，呃。。。一个美女的头像！！！

![截取头像](https://github.com/Kuishana/SimpleCropView/blob/master/demo11.gif "示例1")


再来裁剪一幅大图，一幅9054x5945的世界地图！！！

![裁剪图片](https://github.com/Kuishana/SimpleCropView/blob/master/demo22.gif "示例2")
## 下载
```
dependencies {
    ...
    implementation 'com.kuishana:simplecropview:0.0.1'
}
```
## 使用
```xml
<com.kuishana.simplecropview.library.SimpleCropView
        android:id="@+id/simpleCropView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:simpleCropViewBackgroundColorX="#eeeeee"
        app:simpleCropViewMarkColor="#80000000"
        app:simpleCropViewMaxOutPutSize="256" />
```
## License

    Copyright 2018 Kuishana

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.