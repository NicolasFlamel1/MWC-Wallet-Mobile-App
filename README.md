# MWC Wallet Mobile App

### Description
Mobile app version of the MimbleWimble Coin web wallet, [mwcwallet.com](https://mwcwallet.com), for Android.

### Installing
This app can be installed on Android by downloading it from its [releases](https://github.com/NicolasFlamel1/MWC-Wallet-Mobile-App/releases) and installing it onto your Android device.

### Building
Install [Android Studio](https://developer.android.com/studio) and run the following command to install the dependencies required to build this app.
```
sudo apt install php php-intl php-mbstring unzip wget grep sed coreutils findutils
```
Then run the following commands to build this app where `BUILD_TOOLS` is set to the location of the Android SDK build tools that you want to use, `ANDROID_JAR` is set to the location of the Android Java library that you want to use, and `JBR_BIN` is set to the location of an Android JetBrains Runtime.
```
wget "https://github.com/NicolasFlamel1/MWC-Wallet-Mobile-App/archive/refs/heads/master.zip"
unzip "./master"
cd "./MWC-Wallet-Mobile-App-master"
cd "./Android" && BUILD_TOOLS=~/Android/Sdk/build-tools/36.0.0 ANDROID_JAR=~/Android/Sdk/platforms/android-35/android.jar JBR_BIN=~/android-studio/jbr/bin "./build.sh"
```
This will create the Android app, `MWC-Wallet-Mobile-App-master/MWC Wallet Android App.apk`.
