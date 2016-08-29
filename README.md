# AndroidN-ify
A Xposed module which allows you to use features introduced in Android N on devices running Lollipop and Marshmallow!


### Translation help

I'll try to move the translations to Crowdin, untl then use this little guide to translate:

The original English strings can be found here: https://github.com/wasdennnoch/AndroidN-ify/blob/master/app/src/main/res/values/strings.xml

If you are familiar with creating pull requests, create a new values folder with the appended country code (for example: `values-de` for German strings), create a `strings.xml` file in there, copy the original strings and translate.
Alternatively you can import the project to Android Studio and use it to translate.
If you don't know how to create pull requests you can also send me the translated file.
While translating **make sure to keep variables like `%1$s`**, otherwise I get build errors.


### Builds

If you always want to be up to date with the latest code changes: You can find automated builds here: https://ci.paphonb.xyz/jenkins/job/AndroidN-ify/. Note that they may be buggy or crash as they are built every time a commit gets pushed.


### Links

Support thread: http://forum.xda-developers.com/xposed/modules/xposed-android-n-ify-features-t3345091

Module repository: http://repo.xposed.info/module/tk.wasdennnoch.androidn_ify


### License

```
Copyright 2016 MrWasdennnoch@xda

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
