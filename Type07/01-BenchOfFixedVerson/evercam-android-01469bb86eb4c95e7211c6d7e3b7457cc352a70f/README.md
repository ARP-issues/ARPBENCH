# Evercam for Android [![Build Status][travis-image]][travis-url] [![API][api-image]][api-url]  
[![Ready][waffle-image]][waffle-url] [![Join the chat][gitter-image]][gitter-url]

Evercam allows connect your own IP cameras, public webcams and any Android devices that you use as an IP camera. It connects you to Evercam dashboard so that you can see all your cameras on your desktop as well. 

| Name   | Evercam for Android|
| --- | --- |
| Owner   | [@liutingdu](https://github.com/liutingdu)   |
| Version  | 1.6.5 |
| Evercam API Version  | 1.0  |
| Licence | [AGPL](https://tldrlegal.com/license/gnu-affero-general-public-license-v3-%28agpl-3.0%29) |

## Features

* Support of a huge range of different camera types - business & residential CCTV, home & business WiFi security cameras, public webcams, Android devices.
* Add, edit and remove cameras from your account
* Scan the local network to find cameras to add
* Pre-populates camera details based on vendor
* Portrait and Landscape viewing
* Save snapshots from any camera and share them with your friends & family 
* Homescreen shortcut for single camera live view

## Published App
[![Google Play](http://developer.android.com/images/brand/en_generic_rgb_wo_45.png)](https://play.google.com/store/apps/details?id=io.evercam.androidapp&hl=en)

## Build

1. Checkout from Git:
    ```git clone https://github.com/evercam/evercam-play-android.git```
2. Download [GStreamer 1.6.1 for Android](http://gstreamer.freedesktop.org/data/pkg/android/1.6.1/gstreamer-1.0-android-arm-1.6.1.tar.bz2) and extract it.
3. Edit evercamPlay/src/main/jni/Android.mk with the Gstreamer path: ```GSTREAMER_ROOT_ANDROID := /path/to/GStreamer/gstreamer-1.0-android-arm-1.6.1```
4. Compile GStreamer - ```cd evercamPlay/src/main``` and then ```/path/to/ndk/android-ndk-r10e/ndk-build```
5. Open the project in Android Studio and run

## Help make it better

The entire Evercam codebase is open source, see details: http://www.evercam.io/open-source

If you have experience with Android SDK and IP cameras, we look forward to your pull requests!

For any bugs and discussions, please use [Github Issues](https://github.com/evercam/evercam-play-android/issues).

Any questions or suggestions around Evercam, drop us a line: http://www.evercam.io/contact

[gitter-url]: https://gitter.im/evercam/evercam-play-android?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[gitter-image]: https://badges.gitter.im/Join%20Chat.svg

[waffle-url]: https://waffle.io/evercam/evercam-play-android
[waffle-image]: https://badge.waffle.io/evercam/evercam-play-android.png?label=ready&title=Ready

[travis-url]: https://travis-ci.org/evercam/evercam-play-android
[travis-image]: https://travis-ci.org/evercam/evercam-play-android.svg?branch=master

[api-url]: https://android-arsenal.com/api?level=14
[api-image]: https://img.shields.io/badge/API-14%2B-blue.svg?style=flat
