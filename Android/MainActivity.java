// Package
package com.mwcwallet;


// Imports
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.StringBuilder;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;


// Classes

// Main activity class
public final class MainActivity extends Activity {

	// USB device array list item class
	private class UsbDeviceArrayListItem extends Object {
	
		// Constructor
		private UsbDeviceArrayListItem(final UsbDevice usbDevice) {
		
			// Set USB device to USB device
			this.usbDevice = usbDevice;
		}
		
		// To string
		@Override public final String toString() {
		
			// Return USB device's product name
			return usbDevice.getProductName();
		}
		
		// Get USB device
		private UsbDevice getUsbDevice() {
		
			// Return USB device
			return usbDevice;
		}
		
		// USB device
		private UsbDevice usbDevice;
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
		currentSaveFileName = null;
		currentSaveFileContents = null;
		currentPermissionRequest = null;
		currentFileChooserCallback = null;
		currentNotification = null;
		currentRequestUsbDeviceId = null;
		
		// Register notification channel
		notificationIndex = 0;
		((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel(getPackageName(), getString(R.string.ApplicationLabel), NotificationManager.IMPORTANCE_MAX));
		
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
											UsbDevice temp = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
											usbDevice = temp;
										}
										
										// Check if USB device exists and is allowed
										if(usbDevice != null && isUsbDeviceAllowed(usbDevice)) {
										
											// Send request USB device response message to web view
											webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(currentRequestUsbDeviceId) + ", \"Data\": {\"ID\": " + JSONObject.quote(Integer.toString(usbDevice.getDeviceId())) + ", \"Manufacturer Name\": " + JSONObject.quote(usbDevice.getManufacturerName()) + ", \"Product Name\": " + JSONObject.quote(usbDevice.getProductName()) + ", \"Vendor ID\": " + usbDevice.getVendorId() + ", \"Product ID\": " + usbDevice.getProductId() + "}}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
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
									UsbDevice temp = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
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
				
			}, intentFilter);
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
		
		// Add mobile app JavaScript interface
		final Map<String, byte[]> postData = new HashMap<String, byte[]>();
		final MainActivity self = this;
		final boolean deviceSupportsCameras = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
		final Map<String, Pair<UsbDevice, UsbDeviceConnection>> openedUsbDevices = new HashMap<String, Pair<UsbDevice, UsbDeviceConnection>>();
		webView.addJavascriptInterface(new Object() {
		
			// Get language
			@JavascriptInterface static public String getLanguage() {
			
				// Return language
				return Locale.getDefault().toLanguageTag();
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
					
						// Set current save file name and contents
						currentSaveFileName = name;
						currentSaveFileContents = contents;
						
						// Request write file permission
						requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_FILE_WRITE_PERMISSION);
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
						
							// Try
							try {
							
								// Go through all USB devices
								final JSONArray usbDevices = new JSONArray();
								for(final UsbDevice usbDevice : ((UsbManager)getSystemService(USB_SERVICE)).getDeviceList().values()) {
								
									// Check if USB device exists and is allowed
									if(usbDevice != null && isUsbDeviceAllowed(usbDevice)) {
									
										// Add USB device to list
										usbDevices.put(new JSONObject() {{
										
											put("ID", Integer.toString(usbDevice.getDeviceId()));
											put("Manufacturer Name", usbDevice.getManufacturerName());
											put("Product Name", usbDevice.getProductName());
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
							catch(final Exception exception) {
							
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
						
							// Create USB devices
							final ArrayList<UsbDeviceArrayListItem> usbDevices = new ArrayList<UsbDeviceArrayListItem>();
							
							// Go through all USB devices
							final UsbManager usbManager = (UsbManager)getSystemService(USB_SERVICE);
							for(final UsbDevice usbDevice : usbManager.getDeviceList().values()) {
							
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
									@Override public void onCancel(final DialogInterface dialog) {
									
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
						
							// Go through all USB devices
							final UsbManager usbManager = (UsbManager)getSystemService(USB_SERVICE);
							for(final UsbDevice usbDevice : usbManager.getDeviceList().values()) {
							
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
										webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Device already open\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										
										// Return
										return;
									}
									
									// Break
									break;
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
									if(usbConfiguration.getId() == configurationId) {
									
										// Go through all of the USB configuration's interfaces
										final JSONArray usbInterfaces = new JSONArray();
										for(int j = 0; j < usbConfiguration.getInterfaceCount(); ++j) {
										
											// Add USB interface's class to list
											usbInterfaces.put(usbConfiguration.getInterface(j).getInterfaceClass());
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
										catch(final Exception exception) {
										
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
			
				// Check if request ID and device ID exist
				if(requestId != null && deviceId != null) {
				
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
									if(usbConfiguration.getId() == configurationId) {
									
										// Check if USB configuration's interface exists
										if(interfaceNumber < usbConfiguration.getInterfaceCount()) {
										
											// Get USB interface
											final UsbInterface usbInterface = usbConfiguration.getInterface(interfaceNumber);
											
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
												webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
											}
										}
										
										// Otherwise
										else {
										
											// Send claim USB device interface response message to web view
											webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No interface found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
										
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
			
				// Check if request ID and device ID exist
				if(requestId != null && deviceId != null) {
				
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
									if(usbConfiguration.getId() == configurationId) {
									
										// Check if USB configuration's interface exists
										if(interfaceNumber < usbConfiguration.getInterfaceCount()) {
										
											// Get USB interface
											final UsbInterface usbInterface = usbConfiguration.getInterface(interfaceNumber);
											
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
												webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
											}
										}
										
										// Otherwise
										else {
										
											// Send release USB device interface response message to web view
											webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"No interface found\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
										}
										
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
			
				// Check if request ID and device ID exist
				if(requestId != null && deviceId != null) {
				
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
									if(usbConfiguration.getId() == configurationId) {
									
										// Go through all of the USB configuration's interfaces
										for(int j = 0; j < usbConfiguration.getInterfaceCount(); ++j) {
										
											// Get USB interface
											final UsbInterface usbInterface = usbConfiguration.getInterface(j);
											
											// Go through all of the USB interface's endpoints
											for(int k = 0; k < usbInterface.getEndpointCount(); ++k) {

												// Get USB endpoint
												final UsbEndpoint usbEndpoint = usbInterface.getEndpoint(k);
												
												// Check if USB endpoint is correct and is for sending data to the USB device
												if(usbEndpoint.getEndpointNumber() == endpointNumber && usbEndpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
												
													// Create thread
													new Thread(() -> {
													
														// Use USB connection exclusivley
														boolean result;
														synchronized(usbDeviceConnection) {
														
															// Send data to the USB device
															result = usbDeviceConnection.bulkTransfer(usbEndpoint, data, data.length, 0) == data.length;
														}
														
														// Check if sending data to the USB device was successful
														if(result) {
														
															// Run on the UI thread
															runOnUiThread(() -> {
															
																// Send transfer USB device out response message to web view
																webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
															});
														}
														
														// Otherwise
														else {
														
															// Run on the UI thread
															runOnUiThread(() -> {
															
																// Send transfer USB device out response message to web view
																webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
															});
														}
														
													}).start();
													
													// Return
													return;
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
			
				// Check if request ID and device ID exist
				if(requestId != null && deviceId != null) {
				
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
									if(usbConfiguration.getId() == configurationId) {
									
										// Go through all of the USB configuration's interfaces
										for(int j = 0; j < usbConfiguration.getInterfaceCount(); ++j) {
										
											// Get USB interface
											final UsbInterface usbInterface = usbConfiguration.getInterface(j);
											
											// Go through all of the USB interface's endpoints
											for(int k = 0; k < usbInterface.getEndpointCount(); ++k) {

												// Get USB endpoint
												final UsbEndpoint usbEndpoint = usbInterface.getEndpoint(k);
												
												// Check if USB endpoint is correct and is for receiving data from the USB device
												if(usbEndpoint.getEndpointNumber() == endpointNumber && usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
												
													// Create thread
													new Thread(() -> {
													
														// Use USB connection exclusivley
														boolean result;
														final byte[] data = new byte[length];
														synchronized(usbDeviceConnection) {
														
															// Receive data from the USB device
															result = usbDeviceConnection.bulkTransfer(usbEndpoint, data, length, 0) == length;
														}
														
														// Check if receiving data from the USB device was successful
														if(result) {
														
															// Run on the UI thread
															runOnUiThread(() -> {
														
																// Send transfer USB device in response message to web view
																webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Data\": " + JSONObject.quote(toHexString(data)) + "}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
															});
														}
														
														// Otherwise
														else {
														
															// Run on the UI thread
															runOnUiThread(() -> {
															
																// Send transfer USB device in response message to web view
																webView.postWebMessage(new WebMessage("{\"USB Request ID\": " + JSONObject.quote(requestId) + ", \"Error\": \"Error occurred\"}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
															});
														}
														
													}).start();
													
													// Return
													return;
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
									return new WebResourceResponse(getMineType(path), getEncoding(path), inputStream);
								}
							}
							
							// Catch errors
							catch(final Exception exception) {
							
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
										
										// Send request's data
										final OutputStream outputStream = connection.getOutputStream();
										outputStream.write(requestData);
										outputStream.close();
									}
									
									// Go through all response headers
									final Map<String, String> headers = new HashMap<String, String>();
									for(final Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
									
										// Check if header isn't a CORS or connection header
										final String key = header.getKey();
										if(key != null && !key.equalsIgnoreCase("Access-Control-Allow-Origin") && !key.equalsIgnoreCase("Access-Control-Allow-Methods") && !key.equalsIgnoreCase("Access-Control-Allow-Headers") && !key.equalsIgnoreCase("Connection")) {
										
											// Append header to list
											headers.put(key, String.join(", ", header.getValue()));
										}
									}
									
									// Append allowed CORS and connection headers to list
									headers.put("Access-Control-Allow-Origin", "*");
									headers.put("Access-Control-Allow-Methods", "*");
									headers.put("Access-Control-Allow-Headers", "*");
									headers.put("Connection", "close");
									
									// Return response with new headers
									return new WebResourceResponse(null, null, connection.getResponseCode(), connection.getResponseMessage(), headers, connection.getInputStream());
								}
								
								// Catch errors
								catch(final Exception exception) {
								
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
				
					// Check if an anchor tag was clicked
					final WebView.HitTestResult hitTestResult = view.getHitTestResult();
					if(hitTestResult != null && hitTestResult.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
					
						// Open request using a web browser
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(hitTestResult.getExtra())));
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
			
					// Check if transaction's URI exists
					final Uri uri = intent.getData();
					if(uri != null) {
					
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
						}
					}
					
					// Break
					break;
			}
		}
		
		// Load URI
		webView.loadUrl(uriBuilder.build().toString());
		
		// Wait while the splash screen is shown
		doneLoading = false;
		new Handler(Looper.getMainLooper()).postDelayed(() -> {
		
			// Set done loading to true
			doneLoading = true;
			
		}, SPLASH_SCREEN_DURATION_MILLISECONDS);
		
		// Add on pre-draw listener
		final View content = findViewById(android.R.id.content);
		content.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
		
			// On pre-draw
			@Override public final boolean onPreDraw() {
			
				// Check if done loading
				if(doneLoading) {
				
					// Remove pre-draw listener
					content.getViewTreeObserver().removeOnPreDrawListener(this);
					
					// Request internet permission
					requestPermissions(new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET_PERMISSION);
				}
				
				// Return if done loading
				return doneLoading;
			}
		});
	}
	
	// On new intent
	@Override protected final void onNewIntent(final Intent intent) {
	
		// Check if intent and its action exist
		if(intent != null && intent.getAction() != null) {
		
			// Check action
			switch(intent.getAction()) {
			
				// View action
				case Intent.ACTION_VIEW:
				
					// Check if transaction's URI exists
					final Uri uri = intent.getData();
					if(uri != null) {
					
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
								
									// Return
									return;
							}
						}
						
						// Catch errors
						catch(final Exception exception) {
						
							// Return
							return;
						}
						
						// Send message to web view
						webView.postWebMessage(new WebMessage(message.toString()), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
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
							UsbDevice temp = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
							usbDevice = temp;
						}
						
						// Check if USB device exists and is allowed
						if(usbDevice != null && isUsbDeviceAllowed(usbDevice)) {
						
							// Send USB device connect event message to web view
							webView.postWebMessage(new WebMessage("{\"Event\": \"USB Device Connected\", \"Data\": {\"ID\": " + JSONObject.quote(Integer.toString(usbDevice.getDeviceId())) + ", \"Manufacturer Name\": " + JSONObject.quote(usbDevice.getManufacturerName()) + ", \"Product Name\": " + JSONObject.quote(usbDevice.getProductName()) + ", \"Vendor ID\": " + usbDevice.getVendorId() + ", \"Product ID\": " + usbDevice.getProductId() + "}}"), Uri.parse(ASSET_URI_SCHEME + "://" + ASSET_URI_AUTHORITY));
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
				
			// Request file write permission
			case REQUEST_FILE_WRITE_PERMISSION:
			
				// Check if current save file name and contents exist
				if(currentSaveFileName != null && currentSaveFileContents != null) {
				
					// Try
					try {
					
						// Create current save file
						final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), currentSaveFileName);
						
						// Write current save file contents to file
						final FileOutputStream fileOutputStream = new FileOutputStream(file);
						fileOutputStream.write(currentSaveFileContents);
						fileOutputStream.close();
						
						// Set current notification to show that saving the file was successful
						currentNotification = new Notification.Builder(this, getPackageName()).setSmallIcon(R.mipmap.app_icon).setContentTitle(getString(R.string.FileSavedSuccessLabel)).setContentText(String.format(getString(R.string.FileSavedSuccessDescription), currentSaveFileName)).setContentIntent(PendingIntent.getActivity(this, 0, new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), PendingIntent.FLAG_IMMUTABLE)).setAutoCancel(true).build();
					}
					
					// Catch errors
					catch(final Exception exception) {
					
						// Set current notification to show that saving the file failed
						currentNotification = new Notification.Builder(this, getPackageName()).setSmallIcon(R.mipmap.app_icon).setContentTitle(getString(R.string.FileSavedFailLabel)).setContentText(getString(R.string.FileSavedFailDescription)).build();
					}
					
					// Set current save file name and contents to nothing
					currentSaveFileName = null;
					currentSaveFileContents = null;
					
					// Request post notifications permission
					requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS_PERMISSION);
				}
				
				// Break
				break;
				
			// Request post notifications permission
			case REQUEST_POST_NOTIFICATIONS_PERMISSION:
			
				// Check if current notification exists
				if(currentNotification != null) {
				
					// Try
					try {
					
						// Show current notification
						((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(notificationIndex++, currentNotification);
					}
					
					// Catch errors
					catch(final Exception exception) {
					
					}
					
					// Finally
					finally {
					
						// Set current notification to nothing
						currentNotification = null;
						
						// Check if notification index overflowed
						if(notificationIndex < 0) {
						
							// Reset notification index
							notificationIndex = 0;
						}
					}
				}
				
				// Break
				break;
				
			// Request internet permission
			case REQUEST_INTERNET_PERMISSION:
			
				// break
				break;
				
			// Request Bluetooth connect permission
			case REQUEST_BLUETOOTH_CONNECT_PERMISSION:
			
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
	
	// Is USB device allowed
	private boolean isUsbDeviceAllowed(final UsbDevice usbDevice) {
	
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
		
		// Catch errors
		catch(final Exception exception) {
		
		}
		
		// Finally
		finally {
		
			// Close USB device filter
			usbDeviceFilter.close();
		}
		
		// Return false
		return false;
	}
	
	// Get mime type
	static private String getMineType(final String file) {
	
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
	
	// Get encoding
	static private String getEncoding(final String file) {
	
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
	
	// To hex string
	static private String toHexString(final byte[] bytes) {
	
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
	
	// Action USB device permission
	static private final String ACTION_USB_DEVICE_PERMISSION = "com.mwcwallet.USB_DEVICE_PERMISSION";
	
	// Splash screen duration milliseconds
	static private int SPLASH_SCREEN_DURATION_MILLISECONDS = 600;
	
	// Asset URI scheme
	static private final String ASSET_URI_SCHEME = "https";
	
	// Asset URI authority
	static private final String ASSET_URI_AUTHORITY = "mwcwallet.com";
	
	// Connect timeout milliseconds
	static private int CONNECT_TIMEOUT_MILLISECONDS = 30 * 1000;
	
	// Read timeout milliseconds
	static private int READ_TIMEOUT_MILLISECONDS = 5 * 60 * 1000;
	
	// Request camera permission
	static private final int REQUEST_CAMERA_PERMISSION = 1;
	
	// Request file write permission
	static private final int REQUEST_FILE_WRITE_PERMISSION = 2;
	
	// Request ost notifications permission
	static private final int REQUEST_POST_NOTIFICATIONS_PERMISSION = 3;
	
	// Request internet permission
	static private final int REQUEST_INTERNET_PERMISSION = 4;
	
	// Request Bluetooth connect permission
	static private final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 5;
	
	// Request file selection
	static private final int REQUEST_FILE_SELECTION = 1;
	
	// Notification index
	private int notificationIndex;
	
	// Web view
	private WebView webView;
	
	// Done loading
	private boolean doneLoading;
	
	// Current save file name
	private String currentSaveFileName;
	
	// Current save file contents
	private byte[] currentSaveFileContents;
	
	// Current permission request
	private PermissionRequest currentPermissionRequest;
	
	// Current file chooser callback
	private ValueCallback<Uri[]> currentFileChooserCallback;
	
	// Current notification
	private Notification currentNotification;
	
	// Current request USB device ID
	private String currentRequestUsbDeviceId;
}
