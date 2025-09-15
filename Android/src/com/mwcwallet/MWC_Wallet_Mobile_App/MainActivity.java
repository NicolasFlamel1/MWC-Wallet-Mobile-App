// Package
package com.mwcwallet.MWC_Wallet_Mobile_App;


// Imports
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.graphics.Insets;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Pair;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebMessage;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.StringBuilder;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;


// Classes

// Main activity class
public final class MainActivity extends Activity {

	// USB device array list item class
	private final class UsbDeviceArrayListItem extends Object {
	
		// Constructor
		public UsbDeviceArrayListItem(final UsbDevice usbDevice) {
		
			// Set USB device to USB device
			this.usbDevice = usbDevice;
		}
		
		// To string
		@Override public final String toString() {
		
			// Return USB device's product name
			final String productName = usbDevice.getProductName();
			return (productName == null) ? "" : productName;
		}
		
		// Get USB device
		public UsbDevice getUsbDevice() {
		
			// Return USB device
			return usbDevice;
		}
		
		// USB device
		private UsbDevice usbDevice;
	}
	
	// Bluetooth device array list item class
	private final class BluetoothDeviceArrayListItem extends Object {
	
		// Constructor
		public BluetoothDeviceArrayListItem(final BluetoothDevice bluetoothDevice) {
		
			// Set Bluetooth device to Bluetooth device
			this.bluetoothDevice = bluetoothDevice;
		}
		
		// To string
		@Override public final String toString() {
		
			// Return Bluetooth device's name
			final String name = bluetoothDevice.getName();
			return (name == null) ? "" : name;
		}
		
		// Get Bluetooth device
		public BluetoothDevice getBluetoothDevice() {
		
			// Return Bluetooth device
			return bluetoothDevice;
		}
		
		// Bluetooth device
		private BluetoothDevice bluetoothDevice;
	}
	
	// On create
	@Override protected final void onCreate(final Bundle savedInstanceState) {
	
		// Call parent function
		super.onCreate(savedInstanceState);
		
		// Check if device doesn't have a WebView implementation
		if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WEBVIEW)) {
		
			// Close app
			finishAndRemoveTask();
			
			// Return
			return;
		}
		
		// Set current values to nothing
		currentPermissionRequest = null;
		currentFileChooserCallback = null;
		currentNotification = null;
		currentRequestUsbDeviceId = null;
		currentRequestBluetoothDeviceId = null;
		
		// Create notification channel
		final NotificationChannel notificationChannel = new NotificationChannel(getPackageName(), getString(R.string.ApplicationLabel), NotificationManager.IMPORTANCE_MAX);
		notificationChannel.setShowBadge(false);
		
		// Create or update notification channel
		notificationIndex = 0;
		final NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notificationManager.createNotificationChannel(notificationChannel);
		
		// Register receiver for locale changed action
		registerReceiver(new BroadcastReceiver() {
		
			// On receive
			@Override public final void onReceive(final Context context, final Intent intent) {
			
				// Check if intent and its action exist
				if(intent != null && intent.getAction() != null) {
				
					// Check action
					switch(intent.getAction()) {
					
						// Locale changed action
						case Intent.ACTION_LOCALE_CHANGED:
						
							// Update notification channel's name
							notificationChannel.setName(getString(R.string.ApplicationLabel));
							
							// Update notification channel
							notificationManager.createNotificationChannel(notificationChannel);
							
							// Break
							break;
					}
				}
			}
			
		}, new IntentFilter(Intent.ACTION_LOCALE_CHANGED), RECEIVER_NOT_EXPORTED);
		
		// Create web view
		webView = new WebView(this);
		
		// Check if device supports being a USB host
		final boolean deviceSupportsUsbHost = getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST);
		if(deviceSupportsUsbHost) {
		
			// Register receiver for USB device permission and detached actions
			final IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(ACTION_USB_DEVICE_PERMISSION);
			intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
			
			registerReceiver(new BroadcastReceiver() {
			
				// On receive
				@Override public final void onReceive(final Context context, final Intent intent) {
				
					// Check if intent and its action exist
					if(intent != null && intent.getAction() != null) {
					
						// Check action
						switch(intent.getAction()) {
						
							// USB device Permission action
							case ACTION_USB_DEVICE_PERMISSION:
							
								// Check if current request USB device ID exists
								if(currentRequestUsbDeviceId != null) {
								
									// Check if permission was granted
									if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
									
										// Check if API level is at least Tiramisu
										UsbDevice usbDevice;
										if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
										
											// Get USB device
											usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);
										}
										
										// Otherwise
										else {
										
											// Get USB device
											@SuppressWarnings("deprecation")
											final UsbDevice temp = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
											usbDevice = temp;
										}
										
										// Check if USB device exists and is allowed
										if(usbDevice != null && isUsbDeviceAllowed(usbDevice)) {
										
											// Send request USB device response message to web view
											webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(currentRequestUsbDeviceId) + ", \"Data\": {\"ID\": " + JSONObject.quote(Integer.toString(usbDevice.getDeviceId())) + ", \"Manufacturer Name\": " + JSONObject.quote((usbDevice.getManufacturerName() == null) ? "" : usbDevice.getManufacturerName()) + ", \"Product Name\": " + JSONObject.quote((usbDevice.getProductName() == null) ? "" : usbDevice.getProductName()) + ", \"Vendor ID\": " + usbDevice.getVendorId() + ", \"Product ID\": " + usbDevice.getProductId() + "}}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
										
										// Otherwise
										else {
										
											// Send request USB device response message to web view
											webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(currentRequestUsbDeviceId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
									}
									
									// Otherwise
									else {
									
										// Send request USB device response message to web view
										webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(currentRequestUsbDeviceId) + ", \"Error\": \"Permission denied\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
									}
									
									// Set current request USB device ID to nothing
									currentRequestUsbDeviceId = null;
								}
								
								// Break
								break;
								
							// USB device detached action
							case UsbManager.ACTION_USB_DEVICE_DETACHED:
							
								// Check if API level is at least Tiramisu
								UsbDevice usbDevice;
								if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
								
									// Get USB device
									usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);
								}
								
								// Otherwise
								else {
								
									// Get USB device
									@SuppressWarnings("deprecation")
									final UsbDevice temp = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
									usbDevice = temp;
								}
								
								// Check if USB device exists and is allowed
								if(usbDevice != null && isUsbDeviceAllowed(usbDevice)) {
								
									// Send USB device disconnect event message to web view
									webView.postWebMessage(new WebMessage("{\"Event\": \"USB Device Disconnected\", \"Data\": " + JSONObject.quote(Integer.toString(usbDevice.getDeviceId())) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
								
								// Break
								break;
						}
					}
				}
				
			}, intentFilter, RECEIVER_NOT_EXPORTED);
		}
		
		// Configure web view
		setContentView(webView);
		final WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setDomStorageEnabled(true);
		webSettings.setAllowFileAccess(false);
		webSettings.setAllowContentAccess(false);
		webSettings.setDisplayZoomControls(false);
		webSettings.setSupportZoom(false);
		webSettings.setSupportMultipleWindows(true);
		webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		webSettings.setMediaPlaybackRequiresUserGesture(false);
		webView.setWebContentsDebuggingEnabled((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
		webView.clearCache(true);
		webView.setBackgroundColor(Color.TRANSPARENT);
		
		// Check if API level is at least Vanilla Ice Cream
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
		
			// Web view on apply window insets
			webView.setOnApplyWindowInsetsListener((final View view, final WindowInsets insets) -> {
			
				// Check if view and insets exist
				if(view != null && insets != null) {
				
					// Check if getting system bar insets was successful
					final Insets systemBarInsets = insets.getInsets(WindowInsets.Type.systemBars());
					if(systemBarInsets != null) {
					
						// Check if getting view's layout parameters was successful
						final MarginLayoutParams layoutParameters = (MarginLayoutParams)view.getLayoutParams();
						if(layoutParameters != null) {
						
							// Set layout parameter's to use system bar insets as margins
							layoutParameters.setMargins(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, systemBarInsets.bottom);
							
							// Set view's layout parameters
							view.setLayoutParams(layoutParameters);
						}
					}
				}
				
				// Return consumed
				return WindowInsets.CONSUMED;
			});
		}
		
		// Add mobile app JavaScript interface
		final MainActivity self = this;
		final boolean deviceSupportsCameras = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
		final boolean deviceSupportsBluetooth = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
		final Map<String, byte[]> postData = new HashMap<String, byte[]>();
		final Map<String, Pair<UsbDevice, UsbDeviceConnection>> openedUsbDevices = new HashMap<String, Pair<UsbDevice, UsbDeviceConnection>>();
		openedBluetoothDevices = new HashMap<String, Pair<String, BluetoothGatt>>();
		doneLoading = false;
		backButtonAllowed = false;
		webView.addJavascriptInterface(new Object() {
		
			// Is splash screen showing
			@JavascriptInterface public boolean isSplashScreenShowing() {
			
				// Use done loading exclusivley
				synchronized(self) {
				
					// Return if not done loading
					return !doneLoading;
				}
			}
			
			// Hide splash screen
			@JavascriptInterface public void hideSplashScreen() {
			
				// Use done loading exclusivley
				synchronized(self) {
				
					// Set done loading to true
					doneLoading = true;
				}
			}
			
			// Allow back button
			@JavascriptInterface public void allowBackButton() {
			
				// Use back button allowed exclusivley
				synchronized(self) {
				
					// Set back button allowd to true
					backButtonAllowed = true;
				}
			}
			
			// Prevent back button
			@JavascriptInterface public void preventBackButton() {
			
				// Use back button allowed exclusivley
				synchronized(self) {
				
					// Set back button allowd to false
					backButtonAllowed = false;
				}
			}
			
			// Get language
			@JavascriptInterface static public String getLanguage() {
			
				// Return language
				final Locale defaultLocale = Locale.getDefault();
				return (defaultLocale != null && defaultLocale.toLanguageTag() != null) ? defaultLocale.toLanguageTag() : "";
			}
			
			// Set post data
			@JavascriptInterface public void setPostData(final String fragment, final byte[] data) {
			
				// Check if fragment and data exist
				if(fragment != null && data != null) {
				
					// Use post data exclusivley
					synchronized(postData) {
					
						// Add post data to list
						postData.put(fragment, data);
					}
				}
			}
			
			// Save file
			@JavascriptInterface public void saveFile(final String name, final byte[] contents) {
			
				// Check if name and contents exist
				if(name != null && contents != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Create content values for file
						final ContentValues contentValues = new ContentValues();
						contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
						contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
						contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
						
						// Try
						try {
						
							// Check if creating file was successful
							final ContentResolver contentResolver = getContentResolver();
							final Uri uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
							if(uri != null) {
							
								// Try
								try {
								
									// Check if getting cursor for file's name was successful
									String actualFileName = null;
									final Cursor cursor = contentResolver.query(uri, new String[] {MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
									if(cursor != null) {
									
										// Try
										try {
										
											// Check if moving cursor was successful
											if(cursor.moveToFirst()) {
											
												// Set actual file name to file's name
												actualFileName = cursor.getString(0);
											}
										}
										
										// Finally
										finally {
										
											// Close cursor
											cursor.close();
										}
										
										// Check if actual file name exists
										if(actualFileName != null) {
									
											// Check if getting file's output stream was successful
											final OutputStream outputStream = contentResolver.openOutputStream(uri);
											if(outputStream != null) {
											
												// Try
												try {
												
													// Write contents to file
													outputStream.write(contents);
												}
												
												// Finally
												finally {
												
													// Close file
													outputStream.close();
												}
												
												// Set current notification to show that saving the file was successful
												currentNotification = new Notification.Builder(self, getPackageName()).setSmallIcon(R.drawable.logo).setContentTitle(getString(R.string.FileSavedSuccessLabel)).setContentText(String.format(getString(R.string.FileSavedSuccessDescription), actualFileName)).setContentIntent(PendingIntent.getActivity(self, 0, Intent.createChooser(new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uri).setType("application/octet-stream").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), getString(R.string.FileSenderLabel)), PendingIntent.FLAG_IMMUTABLE)).setAutoCancel(true).addAction(new Notification.Action.Builder(Icon.createWithResource(self, R.drawable.logo), getString(R.string.OpenDownloadsLabel), PendingIntent.getActivity(self, 0, new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)).setAllowGeneratedReplies(false).build()).setAllowSystemGeneratedContextualActions(false).build();
												
												// Set current notification is for success to true
												currentNotificationIsForSuccess = true;
												
												// Request post notifications permission
												requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS_PERMISSION);
												
												// Return
												return;
											}
										}
									}
									
									// Try
									try {
									
										// Delete file
										contentResolver.delete(uri, null);
									}
									
									// Catch errors
									catch(final Throwable error) {
									
									}
								}
								
								// Catch errors
								catch(final Throwable error) {
								
									// Try
									try {
									
										// Delete file
										contentResolver.delete(uri, null);
									}
									
									// Catch errors
									catch(final Throwable otherError) {
									
									}
								}
							}
						}
						
						// Catch errors
						catch(final Throwable error) {
						
						}
						
						// Set current notification to show that saving the file failed
						currentNotification = new Notification.Builder(self, getPackageName()).setSmallIcon(R.drawable.logo).setContentTitle(getString(R.string.FileSavedFailLabel)).setContentText(getString(R.string.FileSavedFailDescription)).setAllowSystemGeneratedContextualActions(false).build();
						
						// Set current notification is for success to false
						currentNotificationIsForSuccess = false;
						
						// Request post notifications permission
						requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS_PERMISSION);
					});
				}
			}
			
			// Device has camera capabilities
			@JavascriptInterface public boolean deviceHasCameraCapabilities() {
			
				// Return if device supports cameras
				return deviceSupportsCameras;
			}
			
			// Device has USB host capabilities
			@JavascriptInterface public boolean deviceHasUsbHostCapabilities() {
			
				// Return if device supports being a USB host
				return deviceSupportsUsbHost;
			}
			
			// Get USB devices
			@JavascriptInterface public void getUsbDevices(final String requestId) {
			
				// Check if request ID exists
				if(requestId != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports being a USB host
						if(deviceSupportsUsbHost) {
						
							// Check if getting connected USB devices was successful
							final HashMap<String, UsbDevice> connectedUsbDevices = ((UsbManager)getSystemService(USB_SERVICE)).getDeviceList();
							if(connectedUsbDevices != null) {
							
								// Try
								try {
								
									// Go through all connected USB devices
									final JSONArray usbDevices = new JSONArray();
									for(final UsbDevice usbDevice : connectedUsbDevices.values()) {
									
										// Check if USB device exists and is allowed
										if(usbDevice != null && isUsbDeviceAllowed(usbDevice)) {
										
											// Add USB device to list
											usbDevices.put(new JSONObject() {{
											
												put("ID", Integer.toString(usbDevice.getDeviceId()));
												put("Manufacturer Name", (usbDevice.getManufacturerName() == null) ? "" : usbDevice.getManufacturerName());
												put("Product Name", (usbDevice.getProductName() == null) ? "" : usbDevice.getProductName());
												put("Vendor ID", usbDevice.getVendorId());
												put("Product ID", usbDevice.getProductId());
											}});
										}
									}
								
									// Send get USB devices response message to web view
									webView.postWebMessage(new WebMessage((new JSONObject() {{
									
										// Get USB devices response
										put("USB Request ID", requestId);
										put("Data", usbDevices);
										
									}}).toString()), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
								
								// Catch errors
								catch(final Throwable error) {
								
									// Send USB devices response message to web view
									webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
							}
							
							// Otherwise
							else {
							
								// Send USB devices response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send USB devices response message to web view
							webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"USB host support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
			// Request USB device
			@JavascriptInterface public void requestUsbDevice(final String requestId) {
			
				// Check if request ID exists
				if(requestId != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports being a USB host
						if(deviceSupportsUsbHost) {
						
							// Check if getting connected USB devices was successful
							final UsbManager usbManager = (UsbManager)getSystemService(USB_SERVICE);
							final HashMap<String, UsbDevice> connectedUsbDevices = usbManager.getDeviceList();
							if(connectedUsbDevices != null) {
							
								// Create USB devices
								final ArrayList<UsbDeviceArrayListItem> usbDevices = new ArrayList<UsbDeviceArrayListItem>();
								
								// Go through all connected USB devices
								for(final UsbDevice usbDevice : connectedUsbDevices.values()) {
								
									// Check if USB device exists and is allowed
									if(usbDevice != null && isUsbDeviceAllowed(usbDevice)) {
									
										// Add USB device to list
										usbDevices.add(new UsbDeviceArrayListItem(usbDevice));
									}
								}
								
								// Check if no applicable USB devices exist
								if(usbDevices.isEmpty()) {
								
									// Send request USB device response message to web view
									webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No device found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
								
								// Otherwise
								else {
								
									// Create alert dialog
									final AlertDialog.Builder alertDialog = new AlertDialog.Builder(self);
									alertDialog.setTitle(R.string.UsbDeviceChooserLabel);
									
									// Set alert's on cancel listener
									alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
									
										// On cancel
										@Override public final void onCancel(final DialogInterface dialog) {
										
											// Send request USB device response message to web view
											webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No device selected\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
									});
									
									// Set alert dialog's adapter
									final ArrayAdapter<UsbDeviceArrayListItem> arrayAdapter = new ArrayAdapter<UsbDeviceArrayListItem>(self, android.R.layout.select_dialog_singlechoice, usbDevices);
									alertDialog.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
									
										// On click
										@Override public final void onClick(final DialogInterface dialog, final int which) {
										
											// Set current request USB device ID to the request ID
											currentRequestUsbDeviceId = requestId;
										
											// Request permission to access the USB device
											usbManager.requestPermission(arrayAdapter.getItem(which).getUsbDevice(), PendingIntent.getBroadcast(self, 0, new Intent(ACTION_USB_DEVICE_PERMISSION).setPackage(getPackageName()), PendingIntent.FLAG_MUTABLE));
										}
									});
									
									// Show alert dialog
									alertDialog.show();
								}
							}
							
							// Otherwise
							else {
							
								// Send request USB device response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send request USB device response message to web view
							webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"USB host support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
			// Open USB device
			@JavascriptInterface public void openUsbDevice(final String requestId, final String deviceId) {
			
				// Check if request ID and device ID exist
				if(requestId != null && deviceId != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports being a USB host
						if(deviceSupportsUsbHost) {
						
							// Check if getting connected USB devices was successful
							final UsbManager usbManager = (UsbManager)getSystemService(USB_SERVICE);
							final HashMap<String, UsbDevice> connectedUsbDevices = usbManager.getDeviceList();
							if(connectedUsbDevices != null) {
							
								// Go through all connected USB devices
								for(final UsbDevice usbDevice : connectedUsbDevices.values()) {
								
									// Check if USB device exists and has the correct device ID
									if(usbDevice != null && Integer.toString(usbDevice.getDeviceId()).equals(deviceId)) {
									
										// Check if app has permission to access the USB device and the USB device isn't already open and is allowed
										if(usbManager.hasPermission(usbDevice) && !openedUsbDevices.containsKey(deviceId) && isUsbDeviceAllowed(usbDevice)) {
										
											// Check if opening the USB device was successful
											final UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(usbDevice);
											if(usbDeviceConnection != null) {
											
												// Add opened USB device to list
												openedUsbDevices.put(deviceId, new Pair<>(usbDevice, usbDeviceConnection));
												
												// Send open USB device response message to web view
												webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
												
												// Return
												return;
											}
										}
										
										// Otherwise check if add doesn't have permission to access USB device
										else if(!usbManager.hasPermission(usbDevice)) {
										
											// Send open USB device response message to web view
											webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Permission required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
											
											// Return
											return;
										}
										
										// Otherwise check if USB device is already open
										else if(openedUsbDevices.containsKey(deviceId)) {
										
											// Send open USB device response message to web view
											webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device already opened\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
											
											// Return
											return;
										}
										
										// Break
										break;
									}
								}
							}
							
							// Send open USB device response message to web view
							webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
						
						// Otherwise
						else {
						
							// Send open USB device response message to web view
							webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"USB host support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
			// Close USB device
			@JavascriptInterface public void closeUsbDevice(final String requestId, final String deviceId) {
			
				// Check if request ID and device ID exist
				if(requestId != null && deviceId != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports being a USB host
						if(deviceSupportsUsbHost) {
						
							// Check if USB device is open
							if(openedUsbDevices.containsKey(deviceId)) {
							
								// Get USB device connection
								final UsbDeviceConnection usbDeviceConnection = openedUsbDevices.get(deviceId).second;
								
								// Use USB connection exclusivley
								synchronized(usbDeviceConnection) {
							
									// Close USB device connection
									usbDeviceConnection.close();
								}
								
								// Remove opened USB device from list
								openedUsbDevices.remove(deviceId);
								
								// Send close USB device response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
							
							// Otherwise
							else {
							
								// Send close USB device response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not open\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send close USB device response message to web view
							webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"USB host support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
			// Select USB device configuration
			@JavascriptInterface public void selectUsbDeviceConfiguration(final String requestId, final String deviceId, final int configurationId) {
			
				// Check if request ID and device ID exist
				if(requestId != null && deviceId != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports being a USB host
						if(deviceSupportsUsbHost) {
						
							// Check if USB device is open
							if(openedUsbDevices.containsKey(deviceId)) {
							
								// Get USB device
								final UsbDevice usbDevice = openedUsbDevices.get(deviceId).first;
								
								// Go through all of the USB device's configurations
								for(int i = 0; i < usbDevice.getConfigurationCount(); ++i) {

									// Get USB configuration
									final UsbConfiguration usbConfiguration = usbDevice.getConfiguration(i);
									
									// Check if USB configuration is correct
									if(usbConfiguration != null && usbConfiguration.getId() == configurationId) {
									
										// Go through all of the USB configuration's interfaces
										final JSONArray usbInterfaces = new JSONArray();
										for(int j = 0; j < usbConfiguration.getInterfaceCount(); ++j) {
										
											// Check if USB interface exists
											final UsbInterface usbInterface = usbConfiguration.getInterface(j);
											if(usbInterface != null) {
											
												// Add USB interface's class to list
												usbInterfaces.put(usbInterface.getInterfaceClass());
											}
										}
										
										// Try
										try {
										
											// Send select USB device configuration response message to web view
											webView.postWebMessage(new WebMessage((new JSONObject() {{
											
												// Select USB device configuration response
												put("USB Request ID", requestId);
												put("Data", usbInterfaces);
												
											}}).toString()), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
										
										// Catch errors
										catch(final Throwable error) {
										
											// Send select USB device configuration response message to web view
											webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
										
										// Return
										return;
									}
								}
								
								// Send select USB device configuration response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No configuration found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
							
							// Otherwise
							else {
							
								// Send select USB device configuration response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not open\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send select USB device configuration response message to web view
							webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"USB host support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
			// Claim USB device interface
			@JavascriptInterface public void claimUsbDeviceInterface(final String requestId, final String deviceId, final int configurationId, final int interfaceNumber) {
			
				// Check if request ID and device ID exist and interface number is valid
				if(requestId != null && deviceId != null && interfaceNumber >= 0) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports being a USB host
						if(deviceSupportsUsbHost) {
						
							// Check if USB device is open
							if(openedUsbDevices.containsKey(deviceId)) {
							
								// Get USB device and its connection
								final UsbDevice usbDevice = openedUsbDevices.get(deviceId).first;
								final UsbDeviceConnection usbDeviceConnection = openedUsbDevices.get(deviceId).second;
								
								// Go through all of the USB device's configurations
								for(int i = 0; i < usbDevice.getConfigurationCount(); ++i) {

									// Get USB configuration
									final UsbConfiguration usbConfiguration = usbDevice.getConfiguration(i);
									
									// Check if USB configuration is correct
									if(usbConfiguration != null && usbConfiguration.getId() == configurationId) {
									
										// Check if USB configuration's interface exists
										if(interfaceNumber < usbConfiguration.getInterfaceCount()) {
										
											// Check if getting USB interface was successful
											final UsbInterface usbInterface = usbConfiguration.getInterface(interfaceNumber);
											if(usbInterface != null) {
											
												// Use USB connection exclusivley
												boolean result;
												synchronized(usbDeviceConnection) {
												
													// Claim the USB interface
													result = usbDeviceConnection.claimInterface(usbInterface, true);
												}
												
												// Check if claiming the USB interface was successful
												if(result) {
												
													// Send claim USB device interface response message to web view
													webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
												}
												
												// Otherwise
												else {
												
													// Send claim USB device interface response message to web view
													webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Connection error\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
												}
												
												// Return
												return;
											}
										}
										
										// Send claim USB device interface response message to web view
										webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No interface found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										
										// Return
										return;
									}
								}
								
								// Send claim USB device interface response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No configuration found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
							
							// Otherwise
							else {
							
								// Send claim USB device interface response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not open\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send claim USB device interface response message to web view
							webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"USB host support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
			// Release USB device interface
			@JavascriptInterface public void releaseUsbDeviceInterface(final String requestId, final String deviceId, final int configurationId, final int interfaceNumber) {
			
				// Check if request ID and device ID exist and interface number is valid
				if(requestId != null && deviceId != null && interfaceNumber >= 0) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports being a USB host
						if(deviceSupportsUsbHost) {
						
							// Check if USB device is open
							if(openedUsbDevices.containsKey(deviceId)) {
							
								// Get USB device and its connection
								final UsbDevice usbDevice = openedUsbDevices.get(deviceId).first;
								final UsbDeviceConnection usbDeviceConnection = openedUsbDevices.get(deviceId).second;
								
								// Go through all of the USB device's configurations
								for(int i = 0; i < usbDevice.getConfigurationCount(); ++i) {

									// Get USB configuration
									final UsbConfiguration usbConfiguration = usbDevice.getConfiguration(i);
									
									// Check if USB configuration is correct
									if(usbConfiguration != null && usbConfiguration.getId() == configurationId) {
									
										// Check if USB configuration's interface exists
										if(interfaceNumber < usbConfiguration.getInterfaceCount()) {
										
											// Check if getting USB interface was successful
											final UsbInterface usbInterface = usbConfiguration.getInterface(interfaceNumber);
											if(usbInterface != null) {
											
												// Use USB connection exclusivley
												boolean result;
												synchronized(usbDeviceConnection) {
												
													// Release the USB interface
													result = usbDeviceConnection.releaseInterface(usbInterface);
												}
												
												// Check if releasing the USB interface was successful
												if(result) {
												
													// Send release USB device interface response message to web view
													webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
												}
												
												// Otherwise
												else {
												
													// Send release USB device interface response message to web view
													webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Connection error\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
												}
												
												// Return
												return;
											}
										}
										
										// Send release USB device interface response message to web view
										webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No interface found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										
										// Return
										return;
									}
								}
								
								// Send release USB device interface response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No configuration found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
							
							// Otherwise
							else {
							
								// Send release USB device interface response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not open\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send release USB device interface response message to web view
							webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"USB host support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
			// Transfer USB device out
			@JavascriptInterface public void transferUsbDeviceOut(final String requestId, final String deviceId, final int configurationId, final int endpointNumber, final byte[] data) {
			
				// Check if request ID, device ID, and data exist
				if(requestId != null && deviceId != null && data != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports being a USB host
						if(deviceSupportsUsbHost) {
						
							// Check if USB device is open
							if(openedUsbDevices.containsKey(deviceId)) {
							
								// Get USB device and its connection
								final UsbDevice usbDevice = openedUsbDevices.get(deviceId).first;
								final UsbDeviceConnection usbDeviceConnection = openedUsbDevices.get(deviceId).second;
								
								// Go through all of the USB device's configurations
								for(int i = 0; i < usbDevice.getConfigurationCount(); ++i) {

									// Get USB configuration
									final UsbConfiguration usbConfiguration = usbDevice.getConfiguration(i);
									
									// Check if USB configuration is correct
									if(usbConfiguration != null && usbConfiguration.getId() == configurationId) {
									
										// Go through all of the USB configuration's interfaces
										for(int j = 0; j < usbConfiguration.getInterfaceCount(); ++j) {
										
											// Check if getting USB interface was successful
											final UsbInterface usbInterface = usbConfiguration.getInterface(j);
											if(usbInterface != null) {
											
												// Go through all of the USB interface's endpoints
												for(int k = 0; k < usbInterface.getEndpointCount(); ++k) {

													// Get USB endpoint
													final UsbEndpoint usbEndpoint = usbInterface.getEndpoint(k);
													
													// Check if USB endpoint is correct and is for sending data to the USB device
													if(usbEndpoint != null && usbEndpoint.getEndpointNumber() == endpointNumber && usbEndpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
													
														// Create thread
														new Thread(() -> {
														
															// Use USB connection exclusivley
															boolean result;
															synchronized(usbDeviceConnection) {
															
																// Send data to the USB device
																result = usbDeviceConnection.bulkTransfer(usbEndpoint, data, data.length, 0) == data.length;
															}
															
															// Run on the UI thread
															runOnUiThread(() -> {
															
																// Check if sending data to the USB device was successful
																if(result) {
															
																	// Send transfer USB device out response message to web view
																	webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																}
																
																// Otherwise
																else {
																
																	// Send transfer USB device out response message to web view
																	webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Connection error\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																}
															});
															
														}).start();
														
														// Return
														return;
													}
												}
											}
										}
										
										// Send transfer USB device out response message to web view
										webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No endpoint found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										
										// Return
										return;
									}
								}
								
								// Send transfer USB device out response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No configuration found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
							
							// Otherwise
							else {
							
								// Send transfer USB device out response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not open\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send transfer USB device out response message to web view
							webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"USB host support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
			// Transfer USB device in
			@JavascriptInterface public void transferUsbDeviceIn(final String requestId, final String deviceId, final int configurationId, final int endpointNumber, final int length) {
			
				// Check if request ID and device ID exist and length is valid
				if(requestId != null && deviceId != null && length >= 0) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports being a USB host
						if(deviceSupportsUsbHost) {
						
							// Check if USB device is open
							if(openedUsbDevices.containsKey(deviceId)) {
							
								// Get USB device and its connection
								final UsbDevice usbDevice = openedUsbDevices.get(deviceId).first;
								final UsbDeviceConnection usbDeviceConnection = openedUsbDevices.get(deviceId).second;
								
								// Go through all of the USB device's configurations
								for(int i = 0; i < usbDevice.getConfigurationCount(); ++i) {

									// Get USB configuration
									final UsbConfiguration usbConfiguration = usbDevice.getConfiguration(i);
									
									// Check if USB configuration is correct
									if(usbConfiguration != null && usbConfiguration.getId() == configurationId) {
									
										// Go through all of the USB configuration's interfaces
										for(int j = 0; j < usbConfiguration.getInterfaceCount(); ++j) {
										
											// Check if getting USB interface was successful
											final UsbInterface usbInterface = usbConfiguration.getInterface(j);
											if(usbInterface != null) {
											
												// Go through all of the USB interface's endpoints
												for(int k = 0; k < usbInterface.getEndpointCount(); ++k) {

													// Get USB endpoint
													final UsbEndpoint usbEndpoint = usbInterface.getEndpoint(k);
													
													// Check if USB endpoint is correct and is for receiving data from the USB device
													if(usbEndpoint != null && usbEndpoint.getEndpointNumber() == endpointNumber && usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
													
														// Create thread
														new Thread(() -> {
														
															// Use USB connection exclusivley
															boolean result;
															final byte[] data = new byte[length];
															synchronized(usbDeviceConnection) {
															
																// Receive data from the USB device
																result = usbDeviceConnection.bulkTransfer(usbEndpoint, data, length, 0) == length;
															}
															
															// Run on the UI thread
															runOnUiThread(() -> {
															
																// Check if receiving data from the USB device was successful
																if(result) {
																
																	// Send transfer USB device in response message to web view
																	webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Data\": " + JSONObject.quote(MainActivity.toHexString(data)) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																}
																
																// Otherwise
																else {
																
																	// Send transfer USB device in response message to web view
																	webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Connection error\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																}
															});
															
														}).start();
														
														// Return
														return;
													}
												}
											}
										}
										
										// Send transfer USB device in response message to web view
										webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No endpoint found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										
										// Return
										return;
									}
								}
								
								// Send transfer USB device in response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No configuration found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
							
							// Otherwise
							else {
							
								// Send transfer USB device in response message to web view
								webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not open\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send transfer USB device in response message to web view
							webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"USB host support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
			// Device has Bluetooth capabilities
			@JavascriptInterface public boolean deviceHasBluetoothCapabilities() {
			
				// Return if device supports Bluetooth
				return deviceSupportsBluetooth;
			}
			
			// Request Bluetooth device
			@JavascriptInterface public void requestBluetoothDevice(final String requestId) {
			
				// Check if request ID exists
				if(requestId != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports Bluetooth
						if(deviceSupportsBluetooth) {
						
							// Check if device supports Bluetooth
							final BluetoothAdapter bluetoothAdapter = ((BluetoothManager)getSystemService(BLUETOOTH_SERVICE)).getAdapter();
							if(bluetoothAdapter != null) {
							
								// Check if Bluetooth is enabled
								if(bluetoothAdapter.isEnabled()) {
								
									// Set current request Bluetooth device ID to the request ID
									currentRequestBluetoothDeviceId = requestId;
									
									// Check if API level is at least S
									if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
									
										// Request Bluetooth connect permission
										requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT_PERMISSION);
									}
									
									// Otherwise
									else {
									
										// Request Bluetooth permission
										requestPermissions(new String[]{Manifest.permission.BLUETOOTH}, REQUEST_BLUETOOTH_PERMISSION);
									}
								}
								
								// Otherwise
								else {
								
									// Send Bluetooth devices response message to web view
									webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Bluetooth is off\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
								
								// Return
								return;
							}
						}
						
						// Send Bluetooth devices response message to web view
						webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Bluetooth support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
					});
				}
			}
			
			// Connect Bluetooth device
			@JavascriptInterface public void connectBluetoothDevice(final String requestId, final String deviceId) {
			
				// Check if request ID and device ID exist
				if(requestId != null && deviceId != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports Bluetooth
						if(deviceSupportsBluetooth) {
						
							// Check if device supports Bluetooth
							final BluetoothAdapter bluetoothAdapter = ((BluetoothManager)getSystemService(BLUETOOTH_SERVICE)).getAdapter();
							if(bluetoothAdapter != null) {
							
								// Check if Bluetooth is enabled
								if(bluetoothAdapter.isEnabled()) {
								
									// Check if Bluetooth device is open
									if(openedBluetoothDevices.containsKey(deviceId)) {
									
										// Check if Bluetooth device isn't already connected
										if(openedBluetoothDevices.get(deviceId).second == null) {
										
											// Check if getting the Bluetooth device was successful
											final BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceId);
											if(bluetoothDevice != null) {
											
												// Connect to the GATT server hosted by the Bluetooth device
												openedBluetoothDevices.put(deviceId, new Pair<>(requestId, bluetoothDevice.connectGatt(self, false, new BluetoothGattCallback() {

													// On connection change
													@Override public final void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
													
														// Check if GATT exists
														if(gatt != null) {
														
															// Run on the UI thread
															runOnUiThread(() -> {
														
																// Check if operation was successful
																if(status == BluetoothGatt.GATT_SUCCESS) {
																
																	// Check new state
																	switch(newState) {
																	
																		// Connected state
																		case BluetoothProfile.STATE_CONNECTED:
																		
																			// Check if Bluetooth device is open
																			if(openedBluetoothDevices.containsKey(deviceId)) {
																			
																				// Check if Bluetooth device has a pending request and its to connect to the Bluetooth device
																				final String currentRequestId = openedBluetoothDevices.get(deviceId).first;
																				if(currentRequestId != null && currentRequestId.equals(requestId)) {
																				
																					// Set that Bluetooth device doesn't have a pending request
																					openedBluetoothDevices.put(deviceId, new Pair<>(null, openedBluetoothDevices.get(deviceId).second));
																				
																					// Send connect Bluetooth device response message to web view
																					webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																				}
																			}
																			
																			// Break
																			break;
																			
																		// Disconnected state
																		case BluetoothProfile.STATE_DISCONNECTED:
																		
																			// Check if Bluetooth device is open
																			if(openedBluetoothDevices.containsKey(deviceId)) {
																			
																				// Check if Bluetooth device has a pending request
																				final String currentRequestId = openedBluetoothDevices.get(deviceId).first;
																				if(currentRequestId != null) {
																				
																					// Send response message to web view
																					webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestId) + ", \"Error\": \"Connection error\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																				}
																				
																				// Close GATT
																				gatt.close();
																				
																				// Remove opened Bluetooth device from list
																				openedBluetoothDevices.remove(deviceId);
																				
																				// Send Bluetooth device disconnect event message to web view
																				webView.postWebMessage(new WebMessage("{\"Event\": \"Bluetooth Device Disconnected\", \"Data\": " + JSONObject.quote(deviceId) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																			}
																			
																			// Break
																			break;
																	}
																}
																
																// Otherwise
																else {
																
																	// Check if Bluetooth device is open
																	if(openedBluetoothDevices.containsKey(deviceId)) {
																	
																		// Check if Bluetooth device has a pending request
																		final String currentRequestId = openedBluetoothDevices.get(deviceId).first;
																		if(currentRequestId != null) {
																		
																			// Send response message to web view
																			webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestId) + ", \"Error\": \"Connection error\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																		}
																		
																		// Close GATT
																		gatt.close();
																		
																		// Remove opened Bluetooth device from list
																		openedBluetoothDevices.remove(deviceId);
																		
																		// Send Bluetooth device disconnect event message to web view
																		webView.postWebMessage(new WebMessage("{\"Event\": \"Bluetooth Device Disconnected\", \"Data\": " + JSONObject.quote(deviceId) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																	}
																}
															});
														}
													}
													
													// On services discovered
													@Override public final void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
													
														// Check if GATT exists
														if(gatt != null) {
														
															// Run on the UI thread
															runOnUiThread(() -> {
															
																// Check if operation was successful
																if(status == BluetoothGatt.GATT_SUCCESS) {
																
																	// Check if GATT's services exist
																	final List<BluetoothGattService> gattServices = gatt.getServices();
																	if(gattServices != null) {
																	
																		// Try
																		try {
																		
																			// Go through all of the GATT's services
																			final JSONArray services = new JSONArray();
																			for(final BluetoothGattService gattService : gattServices) {
																			
																				// Check if GATT service and its UUID exist
																				if(gattService != null && gattService.getUuid() != null) {
																				
																					// Add service to list
																					services.put(gattService.getUuid().toString());
																				}
																			}
																			
																			// Check if Bluetooth device is open
																			if(openedBluetoothDevices.containsKey(deviceId)) {
																			
																				// Check if Bluetooth device has a pending request
																				final String currentRequestId = openedBluetoothDevices.get(deviceId).first;
																				if(currentRequestId != null) {
																				
																					// Set that Bluetooth device doesn't have a pending request
																					openedBluetoothDevices.put(deviceId, new Pair<>(null, openedBluetoothDevices.get(deviceId).second));
																				
																					// Send get Bluetooth devices services response message to web view
																					webView.postWebMessage(new WebMessage((new JSONObject() {{
																					
																						// Get Bluetooth devices services response
																						put("Bluetooth Request ID", currentRequestId);
																						put("Data", services);
																						
																					}}).toString()), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																				}
																			}
																		}
																		
																		// Catch errors
																		catch(final Throwable error) {
																		
																			// Check if Bluetooth device is open
																			if(openedBluetoothDevices.containsKey(deviceId)) {
																			
																				// Check if Bluetooth device has a pending request
																				final String currentRequestId = openedBluetoothDevices.get(deviceId).first;
																				if(currentRequestId != null) {
																				
																					// Set that Bluetooth device doesn't have a pending request
																					openedBluetoothDevices.put(deviceId, new Pair<>(null, openedBluetoothDevices.get(deviceId).second));
																				
																					// Send get Bluetooth devices services response message to web view
																					webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																				}
																			}
																		}
																	}
																	
																	// Otherwise
																	else {
																	
																		// Check if Bluetooth device is open
																		if(openedBluetoothDevices.containsKey(deviceId)) {
																		
																			// Check if Bluetooth device has a pending request
																			final String currentRequestId = openedBluetoothDevices.get(deviceId).first;
																			if(currentRequestId != null) {
																			
																				// Set that Bluetooth device doesn't have a pending request
																				openedBluetoothDevices.put(deviceId, new Pair<>(null, openedBluetoothDevices.get(deviceId).second));
																			
																				// Send get Bluetooth devices services response message to web view
																				webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																			}
																		}
																	}
																}
																
																// Otherwise
																else {
																
																	// Check if Bluetooth device is open
																	if(openedBluetoothDevices.containsKey(deviceId)) {
																	
																		// Check if Bluetooth device has a pending request
																		final String currentRequestId = openedBluetoothDevices.get(deviceId).first;
																		if(currentRequestId != null) {
																		
																			// Set that Bluetooth device doesn't have a pending request
																			openedBluetoothDevices.put(deviceId, new Pair<>(null, openedBluetoothDevices.get(deviceId).second));
																		
																			// Send get Bluetooth devices services response message to web view
																			webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestId) + ", \"Error\": \"Connection error\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																		}
																	}
																}
															});
														}
													}
													
													// On descriptor write
													@Override public final void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
													
														// Run on the UI thread
														runOnUiThread(() -> {
														
															// Check if operation was successful
															if(status == BluetoothGatt.GATT_SUCCESS) {
															
																// Check if Bluetooth device is open
																if(openedBluetoothDevices.containsKey(deviceId)) {
																
																	// Check if Bluetooth device has a pending request
																	final String currentRequestId = openedBluetoothDevices.get(deviceId).first;
																	if(currentRequestId != null) {
																	
																		// Set that Bluetooth device doesn't have a pending request
																		openedBluetoothDevices.put(deviceId, new Pair<>(null, openedBluetoothDevices.get(deviceId).second));
																		
																		// Send Bluetooth device characteristic notifications response message to web view
																		webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestId) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																	}
																}
															}
															
															// Otherwise
															else {
															
																// Check if Bluetooth device is open
																if(openedBluetoothDevices.containsKey(deviceId)) {
																
																	// Check if Bluetooth device has a pending request
																	final String currentRequestId = openedBluetoothDevices.get(deviceId).first;
																	if(currentRequestId != null) {
																	
																		// Set that Bluetooth device doesn't have a pending request
																		openedBluetoothDevices.put(deviceId, new Pair<>(null, openedBluetoothDevices.get(deviceId).second));
																	
																		// Send Bluetooth device characteristic notifications response message to web view
																		webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestId) + ", \"Error\": \"Connection error\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																	}
																}
															}
														});
													}
													
													// On characteristic write
													@Override public final void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
													
														// Run on the UI thread
														runOnUiThread(() -> {
														
															// Check if operation was successful
															if(status == BluetoothGatt.GATT_SUCCESS) {
															
																// Check if Bluetooth device is open
																if(openedBluetoothDevices.containsKey(deviceId)) {
																
																	// Check if Bluetooth device has a pending request
																	final String currentRequestId = openedBluetoothDevices.get(deviceId).first;
																	if(currentRequestId != null) {
																	
																		// Set that Bluetooth device doesn't have a pending request
																		openedBluetoothDevices.put(deviceId, new Pair<>(null, openedBluetoothDevices.get(deviceId).second));
																	
																		// Send write Bluetooth device characteristic response message to web view
																		webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestId) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																	}
																}
															}
															
															// Otherwise
															else {
															
																// Check if Bluetooth device is open
																if(openedBluetoothDevices.containsKey(deviceId)) {
																
																	// Check if Bluetooth device has a pending request
																	final String currentRequestId = openedBluetoothDevices.get(deviceId).first;
																	if(currentRequestId != null) {
																	
																		// Set that Bluetooth device doesn't have a pending request
																		openedBluetoothDevices.put(deviceId, new Pair<>(null, openedBluetoothDevices.get(deviceId).second));
																	
																		// Send write Bluetooth device characteristic response message to web view
																		webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestId) + ", \"Error\": \"Connection error\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																	}
																}
															}
														});
													}
													
													// On characteristic changed
													@SuppressWarnings("deprecation")
													@Override public final void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
													
														// Check if characteristic exist
														if(characteristic != null) {
														
															// Check if characteristic's value exists
															@SuppressWarnings("deprecation")
															final byte[] characteristicValue = characteristic.getValue();
															if(characteristicValue != null) {
															
																// Get characteristic's value as a hex string
																final String value = MainActivity.toHexString(characteristicValue);
																
																// Run on the UI thread
																runOnUiThread(() -> {
																
																	// Check if Bluetooth device is open and UUIDs exist
																	if(openedBluetoothDevices.containsKey(deviceId) && characteristic.getService() != null && characteristic.getService().getUuid() != null && characteristic.getUuid() != null) {
																	
																		// Send Bluetooth device characteristic changed event message to web view
																		webView.postWebMessage(new WebMessage("{\"Event\": \"Bluetooth Device Characteristic Changed\", \"Data\": {\"ID\": " + JSONObject.quote(deviceId) + ", \"Service\": " + JSONObject.quote(characteristic.getService().getUuid().toString()) + ", \"Characteristic\": " + JSONObject.quote(characteristic.getUuid().toString()) + ", \"Value\": " + JSONObject.quote(value) + "}}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																	}
																});
															}
														}
													}
													
													// On characteristic changed
													@Override public final void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte[] value) {
													
														// Check if characteristic and value exist
														if(characteristic != null && value != null) {
														
															// Run on the UI thread
															runOnUiThread(() -> {
															
																// Check if Bluetooth device is open and UUIDs exist
																if(openedBluetoothDevices.containsKey(deviceId) && characteristic.getService() != null && characteristic.getService().getUuid() != null && characteristic.getUuid() != null) {
																
																	// Send Bluetooth device characteristic changed event message to web view
																	webView.postWebMessage(new WebMessage("{\"Event\": \"Bluetooth Device Characteristic Changed\", \"Data\": {\"ID\": " + JSONObject.quote(deviceId) + ", \"Service\": " + JSONObject.quote(characteristic.getService().getUuid().toString()) + ", \"Characteristic\": " + JSONObject.quote(characteristic.getUuid().toString()) + ", \"Value\": " + JSONObject.quote(MainActivity.toHexString(value)) + "}}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																}
															});
														}
													}
												})));
											}
											
											// Otherwise
											else {
											
												// Send connect Bluetooth device response message to web view
												webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
											}
										}
										
										// Otherwise
										else {
										
											// Send connect Bluetooth device response message to web view
											webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device already connected\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
									}
									
									// Otherwise
									else {
									
										// Send connect Bluetooth device response message to web view
										webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not open\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
									}
								}
								
								// Otherwise
								else {
								
									// Send Bluetooth devices response message to web view
									webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Bluetooth is off\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
								
								// Return
								return;
							}
						}
						
						// Send connect Bluetooth device response message to web view
						webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Bluetooth support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
					});
				}
			}
			
			// Disconnect Bluetooth device
			@JavascriptInterface public void disconnectBluetoothDevice(final String deviceId) {
			
				// Check if device ID exists
				if(deviceId != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports Bluetooth
						if(deviceSupportsBluetooth) {
						
							// Check if Bluetooth device is open
							if(openedBluetoothDevices.containsKey(deviceId)) {
							
								// Check if Bluetooth device has a pending request
								final String currentRequestId = openedBluetoothDevices.get(deviceId).first;
								if(currentRequestId != null) {
								
									// Send response message to web view
									webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestId) + ", \"Error\": \"Connection error\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
								
								// Get Bluetooth GATT
								final BluetoothGatt bluetoothGatt = openedBluetoothDevices.get(deviceId).second;
								
								// Check if Bluetooth GATT is connected
								if(bluetoothGatt != null) {
								
									// Disconnect Bluetooth GATT
									bluetoothGatt.disconnect();
									
									// Close Bluetooth GATT
									bluetoothGatt.close();
								}
								
								// Remove opened Bluetooth device from list
								openedBluetoothDevices.remove(deviceId);
								
								// Send Bluetooth device disconnect event message to web view
								webView.postWebMessage(new WebMessage("{\"Event\": \"Bluetooth Device Disconnected\", \"Data\": " + JSONObject.quote(deviceId) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
					});
				}
			}
			
			// Get Bluetooth device services
			@JavascriptInterface public void getBluetoothDeviceServices(final String requestId, final String deviceId) {
			
				// Check if request ID and device ID exist
				if(requestId != null && deviceId != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports Bluetooth
						if(deviceSupportsBluetooth) {
						
							// Check if Bluetooth device is open
							if(openedBluetoothDevices.containsKey(deviceId)) {
							
								// Get Bluetooth GATT
								final BluetoothGatt bluetoothGatt = openedBluetoothDevices.get(deviceId).second;
								
								// Check if Bluetooth GATT is connected
								if(bluetoothGatt != null) {
								
									// Check if Bluetooth device doesn't have a pending request
									if(openedBluetoothDevices.get(deviceId).first == null) {
									
										// Check if starting discovering Bluetooth GATT's services was successful
										if(bluetoothGatt.discoverServices()) {
										
											// Set that Bluetooth device has a pending request
											openedBluetoothDevices.put(deviceId, new Pair<>(requestId, bluetoothGatt));
											
											// Return
											return;
										}
									}
									
									// Send get Bluetooth devices services response message to web view
									webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
								
								// Otherwise
								else {
								
									// Send get Bluetooth devices services response message to web view
									webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not connected\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
							}
							
							// Otherwise
							else {
							
								// Send get Bluetooth devices services response message to web view
								webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not open\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send get Bluetooth devices services response message to web view
							webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Bluetooth support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
			// Get Bluetooth device characteristic
			@JavascriptInterface public void getBluetoothDeviceCharacteristic(final String requestId, final String deviceId, final String serviceUuid, final String characteristicUuid) {
			
				// Check if request ID, device ID, service UUID, and characteristic UUID exist
				if(requestId != null && deviceId != null && serviceUuid != null && characteristicUuid != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports Bluetooth
						if(deviceSupportsBluetooth) {
						
							// Check if Bluetooth device is open
							if(openedBluetoothDevices.containsKey(deviceId)) {
							
								// Get Bluetooth GATT
								final BluetoothGatt bluetoothGatt = openedBluetoothDevices.get(deviceId).second;
								
								// Check if Bluetooth GATT is connected
								if(bluetoothGatt != null) {
								
									// Check if Bluetooth GATT's services exist
									final List<BluetoothGattService> gattServices = bluetoothGatt.getServices();
									if(gattServices != null) {
									
										// Check if Bluetooth GATT's services were discovered
										if(!gattServices.isEmpty()) {
										
											// Go through all of the Bluetooth GATT's services
											for(final BluetoothGattService gattService : gattServices) {
											
												// Check if GATT service and its UUID exist
												if(gattService != null && gattService.getUuid() != null) {
												
													// Check if GATT service is correct
													if(gattService.getUuid().toString().equals(serviceUuid)) {
													
														// Check if GATT service's characteristics exist
														final List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
														if(gattCharacteristics != null) {
														
															// Go through all of the GATT service's characteristics
															for(final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
															
																// Check if GATT characteristic and its UUID exist
																if(gattCharacteristic != null && gattCharacteristic.getUuid() != null) {
																
																	// Check if GATT characteristic is correct
																	if(gattCharacteristic.getUuid().toString().equals(characteristicUuid)) {
																	
																		// Send get Bluetooth device characteristic response message to web view
																		webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																		
																		// Return
																		return;
																	}
																}
															}
														}
														
														// Send get Bluetooth device characteristic response message to web view
														webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No characteristic found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
														
														// Return
														return;
													}
												}
											}
											
											// Send get Bluetooth device characteristic response message to web view
											webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No service found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
										
										// Otherwise
										else {
										
											// Send get Bluetooth device characteristic response message to web view
											webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Services not discovered\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
									}
									
									// Otherwise
									else {
									
										// Send get Bluetooth device characteristic response message to web view
										webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
									}
								}
								
								// Otherwise
								else {
								
									// Send get Bluetooth device characteristic response message to web view
									webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not connected\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
							}
							
							// Otherwise
							else {
							
								// Send get Bluetooth device characteristic response message to web view
								webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not open\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send get Bluetooth device characteristic response message to web view
							webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Bluetooth support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
			// Start Bluetooth device characteristic notifications
			@JavascriptInterface public void startBluetoothDeviceCharacteristicNotifications(final String requestId, final String deviceId, final String serviceUuid, final String characteristicUuid) {
			
				// Check if request ID, device ID, service UUID, and characteristic UUID exist
				if(requestId != null && deviceId != null && serviceUuid != null && characteristicUuid != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports Bluetooth
						if(deviceSupportsBluetooth) {
						
							// Check if Bluetooth device is open
							if(openedBluetoothDevices.containsKey(deviceId)) {
							
								// Get Bluetooth GATT
								final BluetoothGatt bluetoothGatt = openedBluetoothDevices.get(deviceId).second;
								
								// Check if Bluetooth GATT is connected
								if(bluetoothGatt != null) {
								
									// Check if Bluetooth GATT's services exist
									final List<BluetoothGattService> gattServices = bluetoothGatt.getServices();
									if(gattServices != null) {
									
										// Check if Bluetooth GATT's services were discovered
										if(!gattServices.isEmpty()) {
										
											// Go through all of the Bluetooth GATT's services
											for(final BluetoothGattService gattService : gattServices) {
											
												// Check if GATT service and its UUID exist
												if(gattService != null && gattService.getUuid() != null) {
												
													// Check if GATT service is correct
													if(gattService.getUuid().toString().equals(serviceUuid)) {
													
														// Check if GATT service's characteristics exist
														final List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
														if(gattCharacteristics != null) {
														
															// Go through all of the GATT service's characteristics
															for(final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
															
																// Check if GATT characteristic and its UUID exist
																if(gattCharacteristic != null && gattCharacteristic.getUuid() != null) {
																
																	// Check if GATT characteristic is correct
																	if(gattCharacteristic.getUuid().toString().equals(characteristicUuid)) {
																	
																		// Check if getting GATT characteristic's descriptor was successful
																		final BluetoothGattDescriptor gattDescriptor = gattCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID);
																		if(gattDescriptor != null) {
																		
																			// Check if enabling notifications on the GATT characteristic was successful
																			if(bluetoothGatt.setCharacteristicNotification(gattCharacteristic, true)) {
																			
																				// Check if API level is at least Tiramisu
																				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
																				
																					// Check if Bluetooth device doesn't have a pending request
																					if(openedBluetoothDevices.get(deviceId).first == null) {
																					
																						// Check if enabling notification on the GATT descriptor was successful
																						if(bluetoothGatt.writeDescriptor(gattDescriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS) {
																						
																							// Set that Bluetooth device has a pending request
																							openedBluetoothDevices.put(deviceId, new Pair<>(requestId, bluetoothGatt));
																							
																							// Return
																							return;
																						}
																					}
																					
																					// Send start Bluetooth device characteristic notifications response message to web view
																					webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																				}
																				
																				// Otherwise
																				else {
																				
																					// Check if Bluetooth device doesn't have a pending request
																					if(openedBluetoothDevices.get(deviceId).first == null) {
																					
																						// Check if setting GATT descriptor's value to enable notification was successful
																						@SuppressWarnings("deprecation")
																						final boolean tempOne = gattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
																						if(tempOne) {
																						
																							// Check if enabling notification on the GATT descriptor was successful
																							@SuppressWarnings("deprecation")
																							final boolean tempTwo = bluetoothGatt.writeDescriptor(gattDescriptor);
																							if(tempTwo) {
																							
																								// Set that Bluetooth device has a pending request
																								openedBluetoothDevices.put(deviceId, new Pair<>(requestId, bluetoothGatt));
																								
																								// Return
																								return;
																							}
																						}
																					}
																					
																					// Send start Bluetooth device characteristic notifications response message to web view
																					webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																				}
																			}
																			
																			// Otherwise
																			else {
																			
																				// Send start Bluetooth device characteristic notifications response message to web view
																				webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																			}
																		}
																		
																		// Otherwise
																		else {
																		
																			// Send start Bluetooth device characteristic notifications response message to web view
																			webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No descriptor found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																		}
																		
																		// Return
																		return;
																	}
																}
															}
														}
														
														// Send start Bluetooth device characteristic notifications response message to web view
														webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No characteristic found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
														
														// Return
														return;
													}
												}
											}
											
											// Send start Bluetooth device characteristic notifications response message to web view
											webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No service found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
										
										// Otherwise
										else {
										
											// Send start Bluetooth device characteristic notifications response message to web view
											webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Services not discovered\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
									}
									
									// Otherwise
									else {
									
										// Send start Bluetooth device characteristic notifications response message to web view
										webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
									}
								}
								
								// Otherwise
								else {
								
									// Send start Bluetooth device characteristic notifications response message to web view
									webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not connected\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
							}
							
							// Otherwise
							else {
							
								// Send start Bluetooth device characteristic notifications response message to web view
								webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not open\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send start Bluetooth device characteristic notifications response message to web view
							webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Bluetooth support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
			// Stop Bluetooth device characteristic notifications
			@JavascriptInterface public void stopBluetoothDeviceCharacteristicNotifications(final String requestId, final String deviceId, final String serviceUuid, final String characteristicUuid) {
			
				// Check if request ID, device ID, service UUID, and characteristic UUID exist
				if(requestId != null && deviceId != null && serviceUuid != null && characteristicUuid != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports Bluetooth
						if(deviceSupportsBluetooth) {
						
							// Check if Bluetooth device is open
							if(openedBluetoothDevices.containsKey(deviceId)) {
							
								// Get Bluetooth GATT
								final BluetoothGatt bluetoothGatt = openedBluetoothDevices.get(deviceId).second;
								
								// Check if Bluetooth GATT is connected
								if(bluetoothGatt != null) {
								
									// Check if Bluetooth GATT's services exist
									final List<BluetoothGattService> gattServices = bluetoothGatt.getServices();
									if(gattServices != null) {
									
										// Check if Bluetooth GATT's services were discovered
										if(!gattServices.isEmpty()) {
										
											// Go through all of the Bluetooth GATT's services
											for(final BluetoothGattService gattService : gattServices) {
											
												// Check if GATT service and its UUID exist
												if(gattService != null && gattService.getUuid() != null) {
												
													// Check if GATT service is correct
													if(gattService.getUuid().toString().equals(serviceUuid)) {
													
														// Check if GATT service's characteristics exist
														final List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
														if(gattCharacteristics != null) {
														
															// Go through all of the GATT service's characteristics
															for(final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
															
																// Check if GATT characteristic and its UUID exist
																if(gattCharacteristic != null && gattCharacteristic.getUuid() != null) {
																
																	// Check if GATT characteristic is correct
																	if(gattCharacteristic.getUuid().toString().equals(characteristicUuid)) {
																	
																		// Check if getting GATT characteristic's descriptor was successful
																		final BluetoothGattDescriptor gattDescriptor = gattCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID);
																		if(gattDescriptor != null) {
																		
																			// Check if disabling notifications on the GATT characteristic was successful
																			if(bluetoothGatt.setCharacteristicNotification(gattCharacteristic, false)) {
																			
																				// Check if API level is at least Tiramisu
																				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
																				
																					// Check if Bluetooth device doesn't have a pending request
																					if(openedBluetoothDevices.get(deviceId).first == null) {
																					
																						// Check if disabling notification on the GATT descriptor was successful
																						if(bluetoothGatt.writeDescriptor(gattDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS) {
																						
																							// Set that Bluetooth device has a pending request
																							openedBluetoothDevices.put(deviceId, new Pair<>(requestId, bluetoothGatt));
																							
																							// Return
																							return;
																						}
																					}
																					
																					// Send stop Bluetooth device characteristic notifications response message to web view
																					webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																				}
																				
																				// Otherwise
																				else {
																				
																					// Check if Bluetooth device doesn't have a pending request
																					if(openedBluetoothDevices.get(deviceId).first == null) {
																					
																						// Check if setting GATT descriptor's value to disable notification was successful
																						@SuppressWarnings("deprecation")
																						final boolean tempOne = gattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
																						if(tempOne) {
																						
																							// Check if disabling notification on the GATT descriptor was successful
																							@SuppressWarnings("deprecation")
																							final boolean tempTwo = bluetoothGatt.writeDescriptor(gattDescriptor);
																							if(tempTwo) {
																							
																								// Set that Bluetooth device has a pending request
																								openedBluetoothDevices.put(deviceId, new Pair<>(requestId, bluetoothGatt));
																								
																								// Return
																								return;
																							}
																						}
																					}
																					
																					// Send stop Bluetooth device characteristic notifications response message to web view
																					webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																				}
																			}
																			
																			// Otherwise
																			else {
																			
																				// Send stop Bluetooth device characteristic notifications response message to web view
																				webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																			}
																		}
																		
																		// Otherwise
																		else {
																		
																			// Send stop Bluetooth device characteristic notifications response message to web view
																			webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No descriptor found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																		}
																		
																		// Return
																		return;
																	}
																}
															}
														}
														
														// Send stop Bluetooth device characteristic notifications response message to web view
														webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No characteristic found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
														
														// Return
														return;
													}
												}
											}
											
											// Send stop Bluetooth device characteristic notifications response message to web view
											webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No service found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
										
										// Otherwise
										else {
										
											// Send stop Bluetooth device characteristic notifications response message to web view
											webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Services not discovered\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
									}
									
									// Otherwise
									else {
									
										// Send stop Bluetooth device characteristic notifications response message to web view
										webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
									}
								}
								
								// Otherwise
								else {
								
									// Send stop Bluetooth device characteristic notifications response message to web view
									webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not connected\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
							}
							
							// Otherwise
							else {
							
								// Send stop Bluetooth device characteristic notifications response message to web view
								webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not open\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send stop Bluetooth device characteristic notifications response message to web view
							webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Bluetooth support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
			// Write Bluetooth device characteristic
			@JavascriptInterface public void writeBluetoothDeviceCharacteristic(final String requestId, final String deviceId, final String serviceUuid, final String characteristicUuid, final byte[] data) {
			
				// Check if request ID, device ID, service UUID, characteristic UUID, and data exist
				if(requestId != null && deviceId != null && serviceUuid != null && characteristicUuid != null && data != null) {
				
					// Run on the UI thread
					runOnUiThread(() -> {
					
						// Check if device supports Bluetooth
						if(deviceSupportsBluetooth) {
						
							// Check if Bluetooth device is open
							if(openedBluetoothDevices.containsKey(deviceId)) {
							
								// Get Bluetooth GATT
								final BluetoothGatt bluetoothGatt = openedBluetoothDevices.get(deviceId).second;
								
								// Check if Bluetooth GATT is connected
								if(bluetoothGatt != null) {
								
									// Check if Bluetooth GATT's services exist
									final List<BluetoothGattService> gattServices = bluetoothGatt.getServices();
									if(gattServices != null) {
									
										// Check if Bluetooth GATT's services were discovered
										if(!gattServices.isEmpty()) {
										
											// Go through all of the Bluetooth GATT's services
											for(final BluetoothGattService gattService : gattServices) {
											
												// Check if GATT service and its UUID exist
												if(gattService != null && gattService.getUuid() != null) {
												
													// Check if GATT service is correct
													if(gattService.getUuid().toString().equals(serviceUuid)) {
													
														// Check if GATT service's characteristics exist
														final List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
														if(gattCharacteristics != null) {
														
															// Go through all of the GATT service's characteristics
															for(final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
															
																// Check if GATT characteristic and its UUID exist
																if(gattCharacteristic != null && gattCharacteristic.getUuid() != null) {
																
																	// Check if GATT characteristic is correct
																	if(gattCharacteristic.getUuid().toString().equals(characteristicUuid)) {
																	
																		// Check if API level is at least Tiramisu
																		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
																		
																			// Check if Bluetooth device doesn't have a pending request
																			if(openedBluetoothDevices.get(deviceId).first == null) {
																			
																				// Check if writing data to the GATT characteristic was successful
																				if(bluetoothGatt.writeCharacteristic(gattCharacteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS) {
																				
																					// Set that Bluetooth device has a pending request
																					openedBluetoothDevices.put(deviceId, new Pair<>(requestId, bluetoothGatt));
																					
																					// Return
																					return;
																				}
																			}
																			
																			// Send write Bluetooth device characteristic response message to web view
																			webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																		}
																		
																		// Otherwise
																		else {
																		
																			// Check if Bluetooth device doesn't have a pending request
																			if(openedBluetoothDevices.get(deviceId).first == null) {
																			
																				// Set GATT characteristic's write type
																				gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
																				
																				// Check if setting GATT characteristic's value was successful
																				@SuppressWarnings("deprecation")
																				final boolean tempOne = gattCharacteristic.setValue(data);
																				if(tempOne) {
																				
																					// Check if writing data to the GATT characteristic was successful
																					@SuppressWarnings("deprecation")
																					final boolean tempTwo = bluetoothGatt.writeCharacteristic(gattCharacteristic);
																					if(tempTwo) {
																					
																						// Set that Bluetooth device has a pending request
																						openedBluetoothDevices.put(deviceId, new Pair<>(requestId, bluetoothGatt));
																						
																						// Return
																						return;
																					}
																				}
																			}
																			
																			// Send write Bluetooth device characteristic response message to web view
																			webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
																		}
																		
																		// Return
																		return;
																	}
																}
															}
														}
														
														// Send write Bluetooth device characteristic response message to web view
														webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No characteristic found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
														
														// Return
														return;
													}
												}
											}
											
											// Send write Bluetooth device characteristic response message to web view
											webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No service found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
										
										// Otherwise
										else {
										
											// Send write Bluetooth device characteristic response message to web view
											webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Services not discovered\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
									}
									
									// Otherwise
									else {
									
										// Send write Bluetooth device characteristic response message to web view
										webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
									}
								}
								
								// Otherwise
								else {
								
									// Send write Bluetooth device characteristic response message to web view
									webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not connected\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
							}
							
							// Otherwise
							else {
							
								// Send write Bluetooth device characteristic response message to web view
								webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device not open\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send write Bluetooth device characteristic response message to web view
							webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Bluetooth support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					});
				}
			}
			
		}, "MobileApp");
		
		// Set web view's client
		final AssetManager assetManager = getAssets();
		webView.setWebViewClient(new WebViewClient() {
		
			// Should intercept request
			@Override public final WebResourceResponse shouldInterceptRequest(final WebView view, final WebResourceRequest request) {
			
				// Check if request exists
				if(request != null) {
				
					// Check if request's URL and method exist
					final Uri uri = request.getUrl();
					if(uri != null && request.getMethod() != null) {
					
						// Check if request is a GET request for an asset
						final String authority = uri.getAuthority();
						if(request.getMethod().equals("GET") && authority != null && authority.equals(ASSET_URI_AUTHORITY)) {
						
							// Try
							try {
							
								// Check if URL doesn't have a path
								final String path = uri.getPath();
								if(path == null || path.isEmpty()) {
								
									// Return not found response
									return new WebResourceResponse(null, null, 404, "Not Found", null, null);
								}
								
								// Otherwise
								else {
								
									// Use asset manager exclusivley
									InputStream inputStream;
									synchronized(assetManager) {
									
										// Get input stream for the asset
										inputStream = assetManager.open(path.equals("/") ? "index.html" : path.substring("/".length()));
									}
									
									// Return requested asset
									return new WebResourceResponse(MainActivity.getMineType(path), MainActivity.getEncoding(path), inputStream);
								}
							}
							
							// Catch errors
							catch(final Throwable error) {
							
								// Return not found response
								return new WebResourceResponse(null, null, 404, "Not Found", null, null);
							}
						}
						
						// Otherwise check if request is an OPTIONS request
						else if(request.getMethod().equals("OPTIONS")) {
						
							// Return allowed CORS response
							return new WebResourceResponse(null, null, 200, "OK", new HashMap<String, String>() {{
							
								// CORS and connection headers
								put("Access-Control-Allow-Origin", "*");
								put("Access-Control-Allow-Methods", "*");
								put("Access-Control-Allow-Headers", "*");
								put("Access-Control-Allow-Private-Network", "true");
								put("Connection", "close");
								
							}}, null);
						}
						
						// Otherwise
						else {
						
							// Set request's data to nothing
							byte[] requestData = null;
							
							// Check if request is a POST request and it has a fragment
							final String fragment = uri.getFragment();
							if(request.getMethod().equals("POST") && fragment != null) {
							
								// Use post data exclusivley
								synchronized(postData) {
								
									// Check if request has data
									if(postData.containsKey(fragment)) {
									
										// Get request's data
										requestData = postData.get(fragment);
										
										// Remove request's post data from list
										postData.remove(fragment);
									}
								}
							}
							
							// Check if request isn't a POST request or it has a data
							if(!request.getMethod().equals("POST") || requestData != null) {
							
								// Try
								try {
								
									// Create connection to server
									final HttpURLConnection connection = (HttpURLConnection)(new java.net.URI(uri.toString()).toURL().openConnection());
									
									// Check if request headers exist
									final Map<String, String> requestHeaders = request.getRequestHeaders();
									if(requestHeaders != null) {
									
										// Go through all request headers
										for(final Map.Entry<String, String> header : requestHeaders.entrySet()) {
										
											// Check if header isn't a connection or accept encoding header and its value exists
											final String key = header.getKey();
											final String value = header.getValue();
											if(key != null && !key.equalsIgnoreCase("Connection") && !key.equalsIgnoreCase("Accept-Encoding") && value != null) {
											
												// Set connection to use header
												connection.setRequestProperty(key, value);
											}
										}
									}
									
									// Configure connection
									connection.setRequestProperty("Connection", "close");
									connection.setUseCaches(false);
									connection.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
									connection.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
									
									// Check if request is a POST request
									if(request.getMethod().equals("POST")) {
									
										// Get request's data
										connection.setDoOutput(true);
										connection.setFixedLengthStreamingMode(requestData.length);
										
										// Get connection's output stream
										final OutputStream outputStream = connection.getOutputStream();
										
										// Try
										try {
										
											// Send request's data to the output stream
											outputStream.write(requestData);
										}
										
										// Finally
										finally {
										
											// Close output stream
											outputStream.close();
										}
									}
									
									// Create headers
									final Map<String, String> headers = new HashMap<String, String>();
									
									// Check if response headers exist
									final Map<String, List<String>> responseHeaders = connection.getHeaderFields();
									if(responseHeaders != null) {
									
										// Go through all response headers
										for(final Map.Entry<String, List<String>> header : responseHeaders.entrySet()) {
										
											// Check if header isn't a CORS or connection header and its values exists
											final String key = header.getKey();
											final List<String> values = header.getValue();
											if(key != null && !key.equalsIgnoreCase("Access-Control-Allow-Origin") && !key.equalsIgnoreCase("Access-Control-Allow-Methods") && !key.equalsIgnoreCase("Access-Control-Allow-Headers") && !key.equalsIgnoreCase("Access-Control-Allow-Private-Network") && !key.equalsIgnoreCase("Access-Control-Allow-Credentials") && !key.equalsIgnoreCase("Access-Control-Expose-Headers") && !key.equalsIgnoreCase("Access-Control-Max-Age") && !key.equalsIgnoreCase("Connection") && values != null) {
											
												// Set values exist to true
												boolean valuesExist = true;
												
												// Go through all values
												for(final String value : values) {
												
													// Check if value doesn't exist
													if(value == null) {
													
														// Set values exist to false
														valuesExist = false;
														
														// Break
														break;
													}
												}
												
												// Check if values exist
												if(valuesExist) {
												
													// Append header to list
													headers.put(key, String.join(", ", values));
												}
											}
										}
									}
									
									// Append allowed CORS and connection headers to list
									headers.put("Access-Control-Allow-Origin", "*");
									headers.put("Connection", "close");
									
									// Return response with new headers
									return new WebResourceResponse(null, null, connection.getResponseCode(), connection.getResponseMessage(), headers, connection.getInputStream());
								}
								
								// Catch errors
								catch(final Throwable error) {
								
									// Return nothing
									return null;
								}
							}
							
							// Otherwise
							else {
							
								// Return calling parent function
								return super.shouldInterceptRequest(view, request);
							}
						}
					}
					
					// Otherwise
					else {
					
						// Return calling parent function
						return super.shouldInterceptRequest(view, request);
					}
				}
				
				// Otherwise
				else {
				
					// Return calling parent function
					return super.shouldInterceptRequest(view, request);
				}
			}
		});
		
		// Set web view's Chrome client
		webView.setWebChromeClient(new WebChromeClient() {
		
			// On console message
			@Override public final boolean onConsoleMessage(final ConsoleMessage consoleMessage) {
			
				// Check if console message exists and debugging
				if(consoleMessage != null && (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
				
					// Log message
					Log.v(getPackageName(), ((consoleMessage.sourceId() == null) ? "" : consoleMessage.sourceId()) + ":" + consoleMessage.lineNumber() + " - " + ((consoleMessage.message() == null) ? "" : consoleMessage.message()));
					
					// Return true
					return true;
				}
				
				// Otherwise
				else {
				
					// Return calling parent function
					return super.onConsoleMessage(consoleMessage);
				}
			}
			
			// On permission request
			@Override public final void onPermissionRequest(final PermissionRequest request) {
			
				// Check if request and its resources exist
				if(request != null && request.getResources() != null) {
				
					// Go through all requested resources
					for(final String resource : request.getResources()) {
					
						// Check if resource is video capture
						if(resource != null && resource.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
						
							// Check if device supports cameras
							if(deviceSupportsCameras) {
							
								// Set current permission request
								currentPermissionRequest = request;
								
								// Request camera permission
								requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
								
								// Return
								return;
							}
						}
					}
				}
				
				// Call parent function
				super.onPermissionRequest(request);
			}
			
			// On show file chooser
			@Override public final boolean onShowFileChooser(final WebView webView, final ValueCallback<Uri[]> filePathCallback, final WebChromeClient.FileChooserParams fileChooserParams) {
			
				// Check if file path callback exists
				if(filePathCallback != null) {
				
					// Set current file chooser callback
					currentFileChooserCallback = filePathCallback;
					
					// Show file chooser
					final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
					intent.addCategory(Intent.CATEGORY_OPENABLE);
					intent.setType("*/*");
					
					startActivityForResult(Intent.createChooser(intent, getString(R.string.FileChooserLabel)), REQUEST_FILE_SELECTION);
					
					// Return true
					return true;
				}
				
				// Otherwise
				else {
				
					// Return calling parent function
					return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
				}
			}
			
			// On create window
			@Override public final boolean onCreateWindow(final WebView view, final boolean dialog, final boolean userGesture, final Message resultMsg) {
			
				// Check if view exists
				if(view != null) {
				
					// Check if getting clicked element was successful
					final WebView.HitTestResult hitTestResult = view.getHitTestResult();
					if(hitTestResult != null) {
					
						// Check if clicked element's href exists
						final String href = hitTestResult.getExtra();
						if(href != null) {
					
							// Check clicked element's type
							switch(hitTestResult.getType()) {
							
								// Link type
								case WebView.HitTestResult.SRC_ANCHOR_TYPE:
								
									// Open request using a web browser
									startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(href)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
									
									// Break
									break;
									
								// Email type
								case WebView.HitTestResult.EMAIL_TYPE:
								
									// Open request using an email client
									startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + href)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
									
									// Break
									break;
							}
						}
					}
				}
				
				// Return false
				return false;
			}
			
			// On close window
			@Override public final void onCloseWindow(final WebView window) {
			
				// Call parent function
				super.onCloseWindow(window);
				
				// Close app
				finishAndRemoveTask();
			}
		});
		
		// Create URI builder
		final Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.scheme(ASSET_URI_SCHEME).authority(ASSET_URI_AUTHORITY).appendPath("index.html");
		
		// Check if intent and its action exist
		final Intent intent = getIntent();
		if(intent != null && intent.getAction() != null) {
		
			// Check action
			switch(intent.getAction()) {
			
				// View action
				case Intent.ACTION_VIEW:
			
					// Check if transaction's URI and its scheme exists
					final Uri uri = intent.getData();
					if(uri != null && uri.getScheme() != null) {
					
						// Check transaction's scheme
						switch(uri.getScheme()) {
						
							// MWC HTTP or HTTPS
							case "web+mwchttp":
							case "web+mwchttps":
							
								// Append query parameters to URI builder
								uriBuilder.appendQueryParameter("Override Wallet Type", "MimbleWimble Coin").appendQueryParameter("Override Network Type", "Mainnet").appendQueryParameter("Request", "Start Transaction").appendQueryParameter("Recipient Address", uri.toString());
								
								// Break
								break;
								
							// MWC floonet HTTP or HTTPS
							case "web+mwcfloonethttp":
							case "web+mwcfloonethttps":
							
								// Append query parameters to URI builder
								uriBuilder.appendQueryParameter("Override Wallet Type", "MimbleWimble Coin").appendQueryParameter("Override Network Type", "Floonet").appendQueryParameter("Request", "Start Transaction").appendQueryParameter("Recipient Address", uri.toString());
								
								// Break
								break;
								
							// GRIN HTTP or HTTPS
							case "web+grinhttp":
							case "web+grinhttps":
							
								// Append query parameters to URI builder
								uriBuilder.appendQueryParameter("Override Wallet Type", "Grin").appendQueryParameter("Override Network Type", "Mainnet").appendQueryParameter("Request", "Start Transaction").appendQueryParameter("Recipient Address", uri.toString());
								
								// Break
								break;
								
							// GRIN testnet HTTP or HTTPS
							case "web+grintestnethttp":
							case "web+grintestnethttps":
							
								// Append query parameters to URI builder
								uriBuilder.appendQueryParameter("Override Wallet Type", "Grin").appendQueryParameter("Override Network Type", "Testnet").appendQueryParameter("Request", "Start Transaction").appendQueryParameter("Recipient Address", uri.toString());
								
								// Break
								break;
								
							// EPIC HTTP or HTTPS
							case "web+epichttp":
							case "web+epichttps":
							
								// Append query parameters to URI builder
								uriBuilder.appendQueryParameter("Override Wallet Type", "Epic Cash").appendQueryParameter("Override Network Type", "Mainnet").appendQueryParameter("Request", "Start Transaction").appendQueryParameter("Recipient Address", uri.toString());
								
								// Break
								break;
								
							// EPIC floonet HTTP or HTTPS
							case "web+epicfloonethttp":
							case "web+epicfloonethttps":
							
								// Append query parameters to URI builder
								uriBuilder.appendQueryParameter("Override Wallet Type", "Epic Cash").appendQueryParameter("Override Network Type", "Floonet").appendQueryParameter("Request", "Start Transaction").appendQueryParameter("Recipient Address", uri.toString());
								
								// Break
								break;
								
							// Default
							default:
							
								// Close app
								finishAndRemoveTask();
								
								// Return
								return;
						}
					}
					
					// Otherwise
					else {
					
						// Close app
						finishAndRemoveTask();
						
						// Return
						return;
					}
					
					// Break
					break;
			}
		}
		
		// Load URI
		webView.loadUrl(uriBuilder.build().toString());
		
		// Check if content exists
		final View content = findViewById(android.R.id.content);
		if(content != null) {
		
			// Add content on pre-draw listener
			content.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			
				// On pre-draw
				@Override public final boolean onPreDraw() {
				
					// Use done loading exclusivley
					boolean isDoneLoading;
					synchronized(self) {
					
						// Set is done loading to if done loading
						isDoneLoading = doneLoading;
					}
					
					// Check if is done loading
					if(isDoneLoading) {
					
						// Remove content pre-draw listener
						content.getViewTreeObserver().removeOnPreDrawListener(this);
						
						// Request internet permission
						requestPermissions(new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET_PERMISSION);
					}
					
					// Return if is done loading
					return isDoneLoading;
				}
			});
		}
	}
	
	// On new intent
	@Override protected final void onNewIntent(final Intent intent) {
	
		// Check if intent and its action exist
		if(intent != null && intent.getAction() != null) {
		
			// Check action
			switch(intent.getAction()) {
			
				// View action
				case Intent.ACTION_VIEW:
				
					// Check if transaction's URI and its scheme exists
					final Uri uri = intent.getData();
					if(uri != null && uri.getScheme() != null) {
					
						// Create message
						final JSONObject message = new JSONObject();
						
						// Try
						try {
						
							// Add start transaction to recipient address to message
							message.put("Request", "Start Transaction");
							message.put("Recipient Address", uri.toString());
							
							// Check transaction's scheme
							switch(uri.getScheme()) {
							
								// MWC HTTP or HTTPS
								case "web+mwchttp":
								case "web+mwchttps":
								
									// Add wallet and network type to message
									message.put("Wallet Type", "MimbleWimble Coin");
									message.put("Network Type", "Mainnet");
									
									// Break
									break;
									
								// MWC floonet HTTP or HTTPS
								case "web+mwcfloonethttp":
								case "web+mwcfloonethttps":
								
									// Add wallet and network type to message
									message.put("Wallet Type", "MimbleWimble Coin");
									message.put("Network Type", "Floonet");
									
									// Break
									break;
									
								// GRIN HTTP or HTTPS
								case "web+grinhttp":
								case "web+grinhttps":
								
									// Add wallet and network type to message
									message.put("Wallet Type", "Grin");
									message.put("Network Type", "Mainnet");
									
									// Break
									break;
									
								// GRIN testnet HTTP or HTTPS
								case "web+grintestnethttp":
								case "web+grintestnethttps":
								
									// Add wallet and network type to message
									message.put("Wallet Type", "Grin");
									message.put("Network Type", "Testnet");
									
									// Break
									break;
									
								// EPIC HTTP or HTTPS
								case "web+epichttp":
								case "web+epichttps":
								
									// Add wallet and network type to message
									message.put("Wallet Type", "Epic Cash");
									message.put("Network Type", "Mainnet");
									
									// Break
									break;
									
								// EPIC floonet HTTP or HTTPS
								case "web+epicfloonethttp":
								case "web+epicfloonethttps":
								
									// Add wallet and network type to message
									message.put("Wallet Type", "Epic Cash");
									message.put("Network Type", "Floonet");
									
									// Break
									break;
									
								// Default
								default:
								
									// Move to background
									moveTaskToBack(true);
									
									// Return
									return;
							}
						}
						
						// Catch errors
						catch(final Throwable error) {
						
							// Move to background
							moveTaskToBack(true);
							
							// Return
							return;
						}
						
						// Send message to web view
						webView.postWebMessage(new WebMessage(message.toString()), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
					}
					
					// Otherwise
					else {
					
						// Move to background
						moveTaskToBack(true);
					}
					
					// Break
					break;
					
				// USB device attached action
				case UsbManager.ACTION_USB_DEVICE_ATTACHED:
				
					// Check if device supports being a USB host
					if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
					
						// Check if API level is at least Tiramisu
						UsbDevice usbDevice;
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
						
							// Get USB device
							usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);
						}
						
						// Otherwise
						else {
						
							// Get USB device
							@SuppressWarnings("deprecation")
							final UsbDevice temp = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
							usbDevice = temp;
						}
						
						// Check if USB device exists and is allowed
						if(usbDevice != null && isUsbDeviceAllowed(usbDevice)) {
						
							// Send USB device connect event message to web view
							webView.postWebMessage(new WebMessage("{\"Event\": \"USB Device Connected\", \"Data\": {\"ID\": " + JSONObject.quote(Integer.toString(usbDevice.getDeviceId())) + ", \"Manufacturer Name\": " + JSONObject.quote((usbDevice.getManufacturerName() == null) ? "" : usbDevice.getManufacturerName()) + ", \"Product Name\": " + JSONObject.quote((usbDevice.getProductName() == null) ? "" : usbDevice.getProductName()) + ", \"Vendor ID\": " + usbDevice.getVendorId() + ", \"Product ID\": " + usbDevice.getProductId() + "}}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					}
					
					// Break
					break;
			}
		}
	}
	
	// On request permissions result
	@Override public final void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
	
		// Check request code
		switch(requestCode) {
		
			// Request camera permission
			case REQUEST_CAMERA_PERMISSION:
			
				// Check if current permission request exists
				if(currentPermissionRequest != null) {
				
					// Check if permission is granted
					if(grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					
						// Grant current permission request
						currentPermissionRequest.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
					}
					
					// Otherwise
					else {
					
						// Deny current permission request
						currentPermissionRequest.deny();
					}
					
					// Set current permission request to nothing
					currentPermissionRequest = null;
				}
				
				// Break
				break;
				
			// Request post notifications permission
			case REQUEST_POST_NOTIFICATIONS_PERMISSION:
			
				// Check if current notification exists
				if(currentNotification != null) {
				
					// Check if notifications are enabled and API level is at least Tiramisu and permission is granted or API level is less than Tiramisu
					final NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
					if(notificationManager.areNotificationsEnabled() && ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)) {
					
						// Show current notification
						notificationManager.notify(notificationIndex++, currentNotification);
					
						// Check if notification index overflowed
						if(notificationIndex < 0) {
						
							// Reset notification index
							notificationIndex = 0;
						}
						
						// Set current notification to nothing
						currentNotification = null;
					}
					
					// Otherwise
					else {
					
						// Create alert dialog
						final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this).setTitle(currentNotification.extras.getCharSequence(Notification.EXTRA_TITLE)).setMessage(currentNotification.extras.getCharSequence(Notification.EXTRA_TEXT)).setNegativeButton(R.string.OkLabel, null);
						
						// Check if current notification is for success
						if(currentNotificationIsForSuccess) {
						
							// Check if getting current notification's content intent was successful
							final PendingIntent contentIntent = currentNotification.contentIntent;
							if(contentIntent != null) {
							
								// Set alert dialog's negative button
								alertDialog.setNegativeButton(R.string.FileSenderLabel, new DialogInterface.OnClickListener() {
								
									// On click
									@Override public final void onClick(final DialogInterface dialog, final int which) {
									
										// Try
										try {
										
											// Share file
											contentIntent.send();
										}
										
										// Catch errors
										catch(final Throwable error) {
										
										}
									}
								});
							}
							
							// Set alert dialog's positive button
							alertDialog.setPositiveButton(R.string.OpenDownloadsLabel, new DialogInterface.OnClickListener() {
							
								// On click
								@Override public final void onClick(final DialogInterface dialog, final int which) {
								
									// Open downloads
									startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
								}
							});
						}
						
						// Set current notification to nothing
						currentNotification = null;
						
						// Show alert dialog
						alertDialog.show();
					}
				}
				
				// Break
				break;
				
			// Request internet permission
			case REQUEST_INTERNET_PERMISSION:
			
				// break
				break;
				
			// Request Bluetooth connect permission or request bluetooth permission
			case REQUEST_BLUETOOTH_CONNECT_PERMISSION:
			case REQUEST_BLUETOOTH_PERMISSION:
				
				// Check if current request bluetooth device ID exists
				if(currentRequestBluetoothDeviceId != null) {
				
					// Check if permission is granted
					if(grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					
						// Check if device supports Bluetooth
						final BluetoothAdapter bluetoothAdapter = ((BluetoothManager)getSystemService(BLUETOOTH_SERVICE)).getAdapter();
						if(bluetoothAdapter != null) {
						
							// Check if Bluetooth is enabled
							if(bluetoothAdapter.isEnabled()) {
							
								// Check if getting paired Bluetooth devices was successful
								final Set<BluetoothDevice> pairedBluetoothDevices = bluetoothAdapter.getBondedDevices();
								if(pairedBluetoothDevices != null) {
							
									// Create Bluetooth devices
									final ArrayList<BluetoothDeviceArrayListItem> bluetoothDevices = new ArrayList<BluetoothDeviceArrayListItem>();
									
									// Go through all paired Bluetooth devices
									for(final BluetoothDevice bluetoothDevice : pairedBluetoothDevices) {
									
										// Check if Bluetooth device and its address exist
										if(bluetoothDevice != null && bluetoothDevice.getAddress() != null) {
										
											// Add Bluetooth device to list
											bluetoothDevices.add(new BluetoothDeviceArrayListItem(bluetoothDevice));
										}
									}
									
									// Check if no applicable Bluetooth devices are paired
									if(bluetoothDevices.isEmpty()) {
									
										// Send Bluetooth devices response message to web view
										webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestBluetoothDeviceId) + ", \"Error\": \"No device found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
									}
									
									// Otherwise
									else {
									
										// Create alert dialog
										final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
										alertDialog.setTitle(R.string.BluetoothDeviceChooserLabel);
										
										// Get request ID
										final String requestId = currentRequestBluetoothDeviceId;
										
										// Set alert's on cancel listener
										alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
										
											// On cancel
											@Override public final void onCancel(final DialogInterface dialog) {
											
												// Send Bluetooth devices response message to web view
												webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No device selected\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
											}
										});
										
										// Set alert dialog's adapter
										final ArrayAdapter<BluetoothDeviceArrayListItem> arrayAdapter = new ArrayAdapter<BluetoothDeviceArrayListItem>(this, android.R.layout.select_dialog_singlechoice, bluetoothDevices);
										alertDialog.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
										
											// On click
											@Override public final void onClick(final DialogInterface dialog, final int which) {
											
												// Get selected Bluetooth device
												final BluetoothDevice bluetoothDevice = arrayAdapter.getItem(which).getBluetoothDevice();
												
												// Check if Bluetooth device isn't already opened
												final String bluetoothDeviceAddress = bluetoothDevice.getAddress();
												if(!openedBluetoothDevices.containsKey(bluetoothDeviceAddress)) {
												
													// Add opened Bluetooth device to list
													openedBluetoothDevices.put(bluetoothDeviceAddress, new Pair<>(null, null));
													
													// Send Bluetooth devices response message to web view
													webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Data\": {\"ID\": " + JSONObject.quote(bluetoothDeviceAddress) + ", \"Connected\": false}}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
												}
												
												// Otherwise
												else {
												
													// Send Bluetooth devices response message to web view
													webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(requestId) + ", \"Data\": {\"ID\": " + JSONObject.quote(bluetoothDeviceAddress) + ", \"Connected\": " + ((openedBluetoothDevices.get(bluetoothDeviceAddress).second == null) ? "false" : "true") + "}}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
												}
											}
										});
										
										// Set current request Bluetooth device ID to nothing
										currentRequestBluetoothDeviceId = null;
										
										// Show alert dialog
										alertDialog.show();
										
										// Return
										return;
									}
								}
								
								// Otherwise
								else {
								
									// Send Bluetooth devices response message to web view
									webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestBluetoothDeviceId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
								}
							}
							
							// Otherwise
							else {
							
								// Send Bluetooth devices response message to web view
								webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestBluetoothDeviceId) + ", \"Error\": \"Bluetooth is off\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
							}
						}
						
						// Otherwise
						else {
						
							// Send Bluetooth devices response message to web view
							webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestBluetoothDeviceId) + ", \"Error\": \"Bluetooth support required\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
						}
					}
					
					// Otherwise
					else {
					
						// Send Bluetooth devices response message to web view
						webView.postWebMessage(new WebMessage("{\"Bluetooth Request ID\": " + JSONObject.quote(currentRequestBluetoothDeviceId) + ", \"Error\": \"Permission denied\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
					}
					
					// Set current request Bluetooth device ID to nothing
					currentRequestBluetoothDeviceId = null;
				}
				
				// Break
				break;
				
			// Default
			default:
			
				// Call parent function
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
				
				// Break
				break;
		}
	}
	
	// On activity result
	@Override protected final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
	
		// Check request code
		switch(requestCode) {
		
			// Request file selection
			case REQUEST_FILE_SELECTION:
			
				// Check if current file choose callback exists
				if(currentFileChooserCallback != null) {
				
					// Run current file chooser callback with the result
					currentFileChooserCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
					
					// Set current file choose callback to nothing
					currentFileChooserCallback = null;
				}
				
				// Break
				break;
				
			// Default
			default:
			
				// Call parent function
				super.onActivityResult(requestCode, resultCode, data);
				
				// Break
				break;
		}
	}
	
	// On key down
	@Override public final boolean onKeyDown(final int keyCode, final KeyEvent event) {
	
		// Use back button allowed exclusivley
		boolean canGoBack;
		synchronized(this) {
		
			// Set can go back to if the back button is allowed
			canGoBack = backButtonAllowed;
		}
		
		// Check if back button was pressed and can go back
		if(keyCode == KeyEvent.KEYCODE_BACK && canGoBack) {
		
			// Click current section's navigation back button
			webView.evaluateJavascript("$(\"div.unlocked div.sections > div > div:not(.hide) div.navigation button.back:not(.hide)\").trigger(\"click\");", null);
			
			// Return true
			return true;
		}
		
		// Otherwise
		else {
		
			// Return calling parent function
			return super.onKeyDown(keyCode, event);
		}
	}
	
	// Is USB device allowed
	public boolean isUsbDeviceAllowed(final UsbDevice usbDevice) {
	
		// Check if USB device exists
		if(usbDevice != null) {
		
			// Try
			try {
			
				// Get USB device filter from resources
				final XmlResourceParser usbDeviceFilter = getResources().getXml(R.xml.usb_device_filter);
				
				// Try
				try {
				
					// Go through all entries in the filter
					for(int eventType = usbDeviceFilter.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = usbDeviceFilter.next()) {
					
						// Check if entry is a USB device
						final String name = usbDeviceFilter.getName();
						if(name != null && name.equals("usb-device") && usbDeviceFilter.getEventType() == XmlPullParser.START_TAG) {
						
							// Set has values to true
							boolean hasValues = true;
							
							// Go through all values for the entry
							for(int i = 0; i < usbDeviceFilter.getAttributeCount(); ++i) {
							
								// Check value's name
								switch(usbDeviceFilter.getAttributeName(i)) {
								
									// Vendor ID
									case "vendor-id":
									
										// Check if the USB device's vendor ID doesn't match the entry's vendor ID
										if(usbDevice.getVendorId() != Integer.parseInt(usbDeviceFilter.getAttributeValue(i), 16)) {
										
											// Set has values to false
											hasValues = false;
										}
										
										// Break
										break;
										
									// Product ID
									case "product-id":
									
										// Check if the USB device's product ID doesn't match the entry's product ID
										if(usbDevice.getProductId() != Integer.parseInt(usbDeviceFilter.getAttributeValue(i), 16)) {
										
											// Set has values to false
											hasValues = false;
										}
										
										// Break
										break;
								}
							}
							
							// Check if the USB device has all the entry's values
							if(hasValues) {
							
								// Return true
								return true;
							}
						}
					}
				}
				
				// Finally
				finally {
				
					// Close USB device filter
					usbDeviceFilter.close();
				}
			}
			
			// Catch errors
			catch(final Throwable error) {
			
			}
		}
		
		// Return false
		return false;
	}
	
	// Get mime type
	static public String getMineType(final String file) {
	
		// Check if file exists
		if(file != null) {
		
			// Check file's extension
			final int startOfExtension = file.lastIndexOf(".");
			switch((startOfExtension == -1) ? "" : file.substring(startOfExtension + ".".length())) {
			
				// Xml
				case "xml":
				
					// Return mime type
					return "text/xml";
					
				// Css
				case "css":
				
					// Return mime type
					return "text/css";
					
				// Js
				case "js":
				
					// Return mime type
					return "application/javascript";
					
				// Json
				case "json":
				
					// Return mime type
					return "application/json";
					
				// Webmanifest
				case "webmanifest":
				
					// Return mime type
					return "application/manifest+json";
					
				// Wasm
				case "wasm":
				
					// Return mime type
					return "application/wasm";
					
				// Svg
				case "svg":
				
					// Return mime type
					return "image/svg+xml";
					
				// Ico
				case "ico":
				
					// Return mime type
					return "image/x-icon";
					
				// Png
				case "png":
				
					// Return mime type
					return "image/png";
					
				// Woff
				case "woff":
				
					// Return mime type
					return "font/woff";
					
				// Woff2
				case "woff2":
				
					// Return mime type
					return "font/woff2";
					
				// Txt, vert, or frag
				case "txt":
				case "vert":
				case "frag":
				
					// Return mime type
					return "text/plain";
					
				// Default
				default:
				
					// Return mime type
					return "text/html";
			}
		}
		
		// Otherwise
		else {
		
			// Return mime type
			return "text/html";
		}
	}
	
	// Get encoding
	static public String getEncoding(final String file) {
	
		// Check if file exists
		if(file != null) {
		
			final int startOfExtension = file.lastIndexOf(".");
			switch((startOfExtension == -1) ? "" : file.substring(startOfExtension + ".".length())) {
			
				// Wasm, ico, png, woff, or woff2
				case "wasm":
				case "ico":
				case "png":
				case "woff":
				case "woff2":
				
					// Return no encoding
					return null;
					
				// Default
				default:
				
					// Return encoding
					return "UTF-8";
			}
		}
		
		// Otherwise
		else {
		
			// Return encoding
			return "UTF-8";
		}
	}
	
	// To hex string
	static public String toHexString(final byte[] bytes) {
	
		// Check if bytes exist
		if(bytes != null) {
		
			// Create hex string
			final StringBuilder hexString = new StringBuilder(bytes.length * 2);
			
			// Go through all bytes
			for(final byte currentByte : bytes) {
			
				// Append current byte to the hex string
				hexString.append(String.format("%02x", currentByte));
			}
			
			// Return hex string
			return hexString.toString();
		}
		
		// Otherwise
		else {
		
			// Return empty string
			return "";
		}
	}
	
	// Action USB device permission
	static private final String ACTION_USB_DEVICE_PERMISSION = "com.mwcwallet.MWC_Wallet_Mobile_App.USB_DEVICE_PERMISSION";
	
	// Asset URI scheme
	static private final String ASSET_URI_SCHEME = "https";
	
	// Asset URI authority
	static private final String ASSET_URI_AUTHORITY = "mwcwallet.com";
	
	// Client characteristic configuration descriptor UUID
	static public final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	
	// Connect timeout milliseconds
	static public int CONNECT_TIMEOUT_MILLISECONDS = 30 * 1000;
	
	// Read timeout milliseconds
	static public int READ_TIMEOUT_MILLISECONDS = 5 * 60 * 1000;
	
	// Request camera permission
	static private final int REQUEST_CAMERA_PERMISSION = 1;
	
	// Request ost notifications permission
	static private final int REQUEST_POST_NOTIFICATIONS_PERMISSION = 2;
	
	// Request internet permission
	static private final int REQUEST_INTERNET_PERMISSION = 3;
	
	// Request Bluetooth connect permission
	static private final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 4;
	
	// Request Bluetooth permission
	static private final int REQUEST_BLUETOOTH_PERMISSION = 5;
	
	// Request file selection
	static private final int REQUEST_FILE_SELECTION = 1;
	
	// Notification index
	private int notificationIndex;
	
	// Done loading
	public boolean doneLoading;
	
	// Back button allowed
	public boolean backButtonAllowed;
	
	// Web view
	public WebView webView;
	
	// Current permission request
	public PermissionRequest currentPermissionRequest;
	
	// Current file chooser callback
	public ValueCallback<Uri[]> currentFileChooserCallback;
	
	// Current notification
	public Notification currentNotification;
	
	// Current notification is for success
	public boolean currentNotificationIsForSuccess;
	
	// Current request USB device ID
	public String currentRequestUsbDeviceId;
	
	// Current request Bluetooth device ID
	public String currentRequestBluetoothDeviceId;
	
	// Opened Bluetooth devices
	public Map<String, Pair<String, BluetoothGatt>> openedBluetoothDevices;
}
