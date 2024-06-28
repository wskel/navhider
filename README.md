# NavHider

NavHider is a simple Android app that allows users to hide the navigation pill / gesture hint on
devices running One UI 6.1 (and above?). The ability to hide the navigation pill / gesture hint was
removed from the native Samsung settings app in One UI 6.1, which
included [Circle to Search](https://blog.google/products/search/google-circle-to-search-android/).

## ADB Method

If you don't want to install an app, you can achieve the same effect using ADB:

```
adb shell settings put global navigation_bar_gesture_hint 0
```

To re-enable the gesture hint:

```
adb shell settings put global navigation_bar_gesture_hint 1
```

## Using the app

### Prerequisites

- An Android device running One UI 6.1
- USB debugging enabled on the device
- ADB (Android Debug Bridge) installed on a computer (if you don't have this, you can
  install [Android Studio](https://developer.android.com/studio) or
  the [SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools))

### Installation Steps

1. Download the NavHider APK from the [Releases](https://github.com/wskel/NavHider/releases) page.

2. Install the APK on the device.

3. Open a terminal or command prompt on the connected computer.

4. Connect the device to the computer by USB.

5. Grant
   the [necessary permission](https://developer.android.com/reference/android/Manifest.permission#WRITE_SECURE_SETTINGS)
   by running:

```
adb shell pm grant dev.wskel.navhider android.permission.WRITE_SECURE_SETTINGS
```

6. Launch the app on the device.

7. Use the toggle switch in the app to hide or show the navigation pill / gesture hint.

## Note

The app requires the `WRITE_SECURE_SETTINGS` permission in order to modify the global system
setting `navigation_bar_gesture_hint`. The ADB command is required because `WRITE_SECURE_SETTINGS`
is intended for system applications and can't be granted through the normal permission system. Read
more [here](https://developer.android.com/reference/android/Manifest.permission#WRITE_SECURE_SETTINGS)
and [here](https://stackoverflow.com/questions/19538809/how-to-set-the-permission-write-secure-settings-in-android).