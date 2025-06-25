# Copy files
rm -rf "./build"
mkdir -p "./build/assets" "./build/gen" "./build/apk"
wget "https://github.com/NicolasFlamel1/mwcwallet.com/archive/refs/heads/master.zip"
unzip "./master"
cp -r "./mwcwallet.com-master/public_html/." "./build/assets/"
chmod 777 -R "./build/"
cp -r "./res" "./build"

# Create locales
php "./res/values/strings.xml" > "./build/res/values/strings.xml"
for FILE in ./mwcwallet.com-master/public_html/languages/*.php; do
	LANGUAGE=$(grep -Po '(?<=\$availableLanguages\[")[^"]+(?="\])' $FILE)
	EXTENSION_LOCALE_CODE=$(grep -Po '(?<="Extension Locale Code" => ")[^"]+(?=")' $FILE | sed -e 's/-/-r/g')
	if [[ -n $EXTENSION_LOCALE_CODE ]]; then
		mkdir "./build/res/values-$EXTENSION_LOCALE_CODE"
		HTTP_ACCEPT_LANGUAGE="$LANGUAGE" php "./res/values/strings.xml" > "./build/res/values-$EXTENSION_LOCALE_CODE/strings.xml"
	fi
done
for FILE in ./mwcwallet.com-master/public_html/languages/*.php; do
	LANGUAGE=$(grep -Po '(?<=\$availableLanguages\[")[^"]+(?="\])' $FILE)
	LANGUAGE_IDENTIFIER=$(grep -Po '(?<=\$availableLanguages\[")[^"]+(?="\])' $FILE | sed -e 's/-/-r/g')
	rm -rf "./build/res/values-$LANGUAGE_IDENTIFIER"
	mkdir "./build/res/values-$LANGUAGE_IDENTIFIER"
	HTTP_ACCEPT_LANGUAGE="$LANGUAGE" php "./res/values/strings.xml" > "./build/res/values-$LANGUAGE_IDENTIFIER/strings.xml"
done

# Get version
VERSION=$(grep -Po "(?<=VERSION_NUMBER = \").*(?=\";)" "./mwcwallet.com-master/public_html/backend/common.php")

# Remove unused files
rm -r "./build/assets/backend"
rm -r "./build/assets/errors"
rm -r "./build/assets/languages"
rm "./build/assets/browserconfig.xml"
rm "./build/assets/connection_test.html"
rm "./build/assets/robots.txt"
rm "./build/assets/site.webmanifest"
rm "./build/assets/sitemap.xml"
rm "./build/assets/privacy_policy.txt"
rm "./build/assets/.user.ini"
rm "./build/assets/scripts/service_worker.js"

# Compile index.html
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/index.html" > "./build/assets/index.html"
sed -i "/<link .* rel=\"manifest\".*>/d" "./build/assets/index.html"
sed -i "/<meta name=\"msapplication-config\".*>/d" "./build/assets/index.html"
sed -i "/<meta name=\"msapplication-starturl\".*>/d" "./build/assets/index.html"
sed -i "/<link rel=\"alternate\".*>/d" "./build/assets/index.html"
sed -i "/<link rel=\"canonical\".*>/d" "./build/assets/index.html"

# Use index.js
cp "./index.js" "./build/assets/"
sed -i "0,/--><script/s//--><script src=\".\/index.js\" type=\"application\/javascript\" charset=\"UTF-8\" onerror=\"processLoadingError(this, true);\"><\/script><!--\n\t--><script/" "./build/assets/index.html"

# Compile styles
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/fonts/btc/btc.css" > "./build/assets/fonts/btc/btc.css"
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/fonts/eth/eth.css" > "./build/assets/fonts/eth/eth.css"
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/fonts/font_awesome/font_awesome.css" > "./build/assets/fonts/font_awesome/font_awesome.css"
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/fonts/grin/grin.css" > "./build/assets/fonts/grin/grin.css"
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/fonts/epic/epic.css" > "./build/assets/fonts/epic/epic.css"
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/fonts/mwc/mwc.css" > "./build/assets/fonts/mwc/mwc.css"
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/fonts/open_sans/open_sans.css" > "./build/assets/fonts/open_sans/open_sans.css"
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/styles/section.css" > "./build/assets/styles/section.css"
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/styles/unlocked.css" > "./build/assets/styles/unlocked.css"

# Compile scripts
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/scripts/camera_worker.js" > "./build/assets/scripts/camera_worker.js"
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/scripts/output_worker.js" > "./build/assets/scripts/output_worker.js"
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/scripts/slate_worker.js" > "./build/assets/scripts/slate_worker.js"
SERVER_NAME="mwcwallet.com" HTTPS="on" NO_FILE_VERSIONS="" NO_FILE_CHECKSUMS="" NO_MINIFIED_FILES="" HTTPS_SERVER_ADDRESS="https://mwcwallet.com" TOR_SERVER_ADDRESS="http://mwcwalletmiq3gdkmfbqlytxunvlxyli4m6zrqozk7xjc353ewqb6bad.onion" php "./mwcwallet.com-master/public_html/scripts/languages.js" > "./build/assets/scripts/languages.js"

# Prepare app
sed -i "s/android:versionCode=\".*\" android:versionName=\".*\"/android:versionCode=\"$(($(grep -Po "(?<=android:versionCode=\").*?(?=\")" "./AndroidManifest.xml") + 1))\" android:versionName=\"$VERSION\"/" "./AndroidManifest.xml"

# Build app
"$BUILD_TOOLS/aapt" package -m -J "./build/gen" -M "./AndroidManifest.xml" -I "$ANDROID_JAR" -S "./build/res"
"$JBR_BIN/javac" -classpath "$ANDROID_JAR" -d "./build/obj" $(find "./build/gen/" -name "*.java") "./src/com/mwcwallet/MainActivity.java"
PATH="$JBR_BIN":$PATH "$BUILD_TOOLS/d8" --min-api $(grep -Po "(?<=minSdkVersion=\").*?(?=\")" "./AndroidManifest.xml") --release --lib "$ANDROID_JAR" --output "./build/apk" $(find "./build/obj/" -name "*.class")
"$BUILD_TOOLS/aapt" package -M "./AndroidManifest.xml" -I "$ANDROID_JAR" -S "./build/res" -A "./build/assets" -F "./build/MWC Wallet_unsigned.apk" "./build/apk"

# Check if lint is provided
if [[ -v LINT ]]; then

	# Lint project
	PATH="$JBR_BIN":$PATH "$LINT" -Wall --ignore HardcodedDebugMode --ignore UnknownNullness --ignore SetJavaScriptEnabled --sources "./src" --sources "./build/gen" --resources "./build/res" --classpath "./build/obj" --libraries "$ANDROID_JAR" "./"
fi

# Sign app
"$BUILD_TOOLS/zipalign" -p 4 "./build/MWC Wallet_unsigned.apk" "./build/MWC Wallet_aligned.apk"
if [[ ! -f "./keystore.jks" ]]; then
	"$JBR_BIN/keytool" -genkeypair -keystore "./keystore.jks" -keyalg RSA -storepass android -dname CN= -validity 36500
fi
PATH="$JBR_BIN":$PATH "$BUILD_TOOLS/apksigner" sign --ks "./keystore.jks" --ks-pass pass:android --out "../MWC Wallet Android App v$VERSION.apk" "./build/MWC Wallet_aligned.apk"

# Cleanup
rm -rf "./master.zip" "./mwcwallet.com-master" "./build"

# Check if adb is provided
if [[ -v ADB ]]; then

	# Install and run app
	"$ADB" install "../MWC Wallet Android App v$VERSION.apk"
	"$ADB" shell am start -n "com.mwcwallet/.MainActivity"
	"$ADB" logcat -c
	"$ADB" logcat -s "com.mwcwallet"
fi
