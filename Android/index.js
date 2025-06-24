// Use strict
"use strict";


// Main function

// Check if this is a mobile app
if(typeof MobileApp !== "undefined") {

	// Document click event
	document.addEventListener("click", function(event) {
	
		// Check if a link to download a file was clicked
		if(event["target"]["tagName"] === "A" && event["target"].getAttribute("download") !== null) {
		
			// Prevent default
			event.preventDefault();
			
			// Create request to download the file
			var request = new XMLHttpRequest();
			request.open("GET", event["target"].getAttribute("href"), true);
			
			// Request on ready state change
			request["onreadystatechange"] = function() {
			
				// Check if request completed successfully
				if(request["readyState"] === 4 && request["status"] === 200) {
				
					// Save file using mobile app
					MobileApp.saveFile(event["target"].getAttribute("download"), (new TextEncoder()).encode(request.responseText));
				}
			};
			
			// Perform request
			request.send();
		}
	});
	
	// Check if XMLHttpRequest is supported
	if(typeof XMLHttpRequest !== "undefined") {
	
		// Maximum request index
		var MAXIMUM_REQUEST_INDEX = 10000;
		
		// Initialize request index
		var requestIndex = 0;
		
		// Override XMLHttpRequest's open method
		XMLHttpRequest["prototype"].mobileAppOriginalOpen = XMLHttpRequest["prototype"].open;
		XMLHttpRequest["prototype"].open = function(method, url, async, user, password) {
		
			// Check if request is a POST request
			if(method === "POST") {
			
				// Try
				try {
				
					// Parse URL
					var parsedUrl = new URL(url);
				}
				
				// Catch errors
				catch(error) {
				
					// Perform original open with the URL's fragment removed
					this.mobileAppOriginalOpen(method, url.toString().split("#", 1)[0], async, user, password);
					
					// Return
					return;
				}
				
				// Set parsed URL's fragment to the request index
				this.mobileAppFragment = (requestIndex++ % MAXIMUM_REQUEST_INDEX).toFixed();
				parsedUrl["hash"] = "#" + this.mobileAppFragment;
				
				// Perform original open with the parsed URL
				this.mobileAppOriginalOpen(method, parsedUrl, async, user, password);
			}
			
			// Otherwise
			else {
			
				// Perform original open
				this.mobileAppOriginalOpen(method, url, async, user, password);
			}
		};
		
		// Override XMLHttpRequest's send method
		XMLHttpRequest["prototype"].mobileAppOriginalSend = XMLHttpRequest["prototype"].send;
		XMLHttpRequest["prototype"].send = function(body) {
		
			// Check if request had its fragment set
			if(typeof this.mobileAppFragment !== "undefined") {
			
				// Set mobile app's post data
				MobileApp.setPostData(this.mobileAppFragment, (new TextEncoder()).encode((body === null) ? "" : body));
			}
			
			// Perform original send
			this.mobileAppOriginalSend(body);
		};
	}
	
	// Check if WebUSB isn't supported by the browser and the device supports being a USB host
	if("usb" in navigator === false && MobileApp.deviceHasUsbHostCapabilities() === true) {
		
		// USB device class
		class UsbDevice {
		
			// Public
			
				// Constructor
				constructor(getCurrentRequestId, id, manufacturerName, productName, vendorId, productId) {
				
					// Set get current request ID to get current request ID
					this.getCurrentRequestId = getCurrentRequestId;
					
					// Set ID to ID
					this.id = id;
					
					// Set manufacturer name to manufacturer name
					this.manufacturerName = manufacturerName;
					
					// Set product name to product name
					this.productName = productName;
					
					// Set vendor ID to vendor ID
					this.vendorId = vendorId;
					
					// Set product ID to product ID
					this.productId = productId;
					
					// Set opened to false
					this.opened = false;
					
					// Set configuration ID to zero
					this.configurationId = 0;
					
					// Set configurations to nothing
					this.configurations = [];
				}
				
				// Open
				open() {
				
					// Set self
					var self = this;
					
					// Return promise
					return new Promise(function(resolve, reject) {
					
						// Get current request ID
						var currentRequestId = self.getCurrentRequestId();
						
						// On message event
						var onMessageEvent = function(event) {
						
							// Try
							try {
							
								// Parse message as JSON
								var response = JSON.parse(event["data"]);
							}
							
							// Catch errors
							catch(error) {
							
								// Return
								return;
							}
							
							// Check if message is a response for this request
							if(typeof response === "object" && response !== null && "USB Request ID" in response === true && response["USB Request ID"] === currentRequestId.toFixed()) {
							
								// Remove this window message event
								window.removeEventListener("message", onMessageEvent);
								
								// Check if response doesn't have an error
								if("Error" in response === false) {
								
									// Set opened to true
									self.opened = true;
									
									// Resolve
									resolve();
								}
								
								// Otherwise
								else {
								
									// Reject
									reject();
								}
							}
						};
						
						// Window message event
						window.addEventListener("message", onMessageEvent);
						
						// Open USB device using mobile app
						MobileApp.openUsbDevice(currentRequestId.toFixed(), self.id);
					});
				}
				
				// Close
				close() {
				
					// Set self
					var self = this;
					
					// Return promise
					return new Promise(function(resolve, reject) {
					
						// Get current request ID
						var currentRequestId = self.getCurrentRequestId();
						
						// On message event
						var onMessageEvent = function(event) {
						
							// Try
							try {
							
								// Parse message as JSON
								var response = JSON.parse(event["data"]);
							}
							
							// Catch errors
							catch(error) {
							
								// Return
								return;
							}
							
							// Check if message is a response for this request
							if(typeof response === "object" && response !== null && "USB Request ID" in response === true && response["USB Request ID"] === currentRequestId.toFixed()) {
							
								// Remove this window message event
								window.removeEventListener("message", onMessageEvent);
								
								// Check if response doesn't have an error
								if("Error" in response === false) {
								
									// Set opened to false
									self.opened = false;
									
									// Resolve
									resolve();
								}
								
								// Otherwise
								else {
								
									// Reject
									reject();
								}
							}
						};
						
						// Window message event
						window.addEventListener("message", onMessageEvent);
						
						// Close USB device using mobile app
						MobileApp.closeUsbDevice(currentRequestId.toFixed(), self.id);
					});
				}
				
				// Reset
				reset() {
				
					// Return promise
					return new Promise(function(resolve, reject) {
					
						// Resolve
						resolve();
					});
				}
				
				// Select configuration
				selectConfiguration(configurationId) {
				
					// Set self
					var self = this;
					
					// Return promise
					return new Promise(function(resolve, reject) {
					
						// Get current request ID
						var currentRequestId = self.getCurrentRequestId();
						
						// On message event
						var onMessageEvent = function(event) {
						
							// Try
							try {
							
								// Parse message as JSON
								var response = JSON.parse(event["data"]);
							}
							
							// Catch errors
							catch(error) {
							
								// Return
								return;
							}
							
							// Check if message is a response for this request
							if(typeof response === "object" && response !== null && "USB Request ID" in response === true && response["USB Request ID"] === currentRequestId.toFixed()) {
							
								// Remove this window message event
								window.removeEventListener("message", onMessageEvent);
								
								// Check if response's data is valid
								if("Data" in response === true && Array.isArray(response["Data"]) === true) {
								
									// Go through all interfaces in the response
									for(var i = 0; i < response["Data"]["length"]; ++i) {
									
										// Check if interface's class is invalid
										if(typeof response["Data"][i] !== "number") {
										
											// Reject
											reject();
											
											// Return
											return;
										}
									}
									
									// Set configuration ID to configuration ID
									self.configurationId = configurationId;
									
									// Set configurations
									self.configurations = [
										{
									
											// Interfaces
											"interfaces": response["Data"].map(function(currentInterface, index) {
											
												// Return interface
												return {
												
													// Interface number
													"interfaceNumber": index,
													
													// Alternates
													"alternates": [
														{
														
															// Interface class
															"interfaceClass": currentInterface
														}
													]
												};
											})
										}
									];
									
									// Resolve
									resolve();
								}
								
								// Otherwise
								else {
								
									// Reject
									reject();
								}
							}
						};
						
						// Window message event
						window.addEventListener("message", onMessageEvent);
						
						// Select USB device configuration using mobile app
						MobileApp.selectUsbDeviceConfiguration(currentRequestId.toFixed(), self.id, configurationId);
					});
				}
				
				// Claim interface
				claimInterface(interfaceNumber) {
				
					// Set self
					var self = this;
					
					// Return promise
					return new Promise(function(resolve, reject) {
					
						// Get current request ID
						var currentRequestId = self.getCurrentRequestId();
						
						// On message event
						var onMessageEvent = function(event) {
						
							// Try
							try {
							
								// Parse message as JSON
								var response = JSON.parse(event["data"]);
							}
							
							// Catch errors
							catch(error) {
							
								// Return
								return;
							}
							
							// Check if message is a response for this request
							if(typeof response === "object" && response !== null && "USB Request ID" in response === true && response["USB Request ID"] === currentRequestId.toFixed()) {
							
								// Remove this window message event
								window.removeEventListener("message", onMessageEvent);
								
								// Check if response doesn't have an error
								if("Error" in response === false) {
								
									// Resolve
									resolve();
								}
								
								// Otherwise
								else {
								
									// Reject
									reject();
								}
							}
						};
						
						// Window message event
						window.addEventListener("message", onMessageEvent);
						
						// Claim USB device interface using mobile app
						MobileApp.claimUsbDeviceInterface(currentRequestId.toFixed(), self.id, self.configurationId, interfaceNumber);
					});
				}
				
				// Release interface
				releaseInterface(interfaceNumber) {
				
					// Set self
					var self = this;
					
					// Return promise
					return new Promise(function(resolve, reject) {
					
						// Get current request ID
						var currentRequestId = self.getCurrentRequestId();
						
						// On message event
						var onMessageEvent = function(event) {
						
							// Try
							try {
							
								// Parse message as JSON
								var response = JSON.parse(event["data"]);
							}
							
							// Catch errors
							catch(error) {
							
								// Return
								return;
							}
							
							// Check if message is a response for this request
							if(typeof response === "object" && response !== null && "USB Request ID" in response === true && response["USB Request ID"] === currentRequestId.toFixed()) {
							
								// Remove this window message event
								window.removeEventListener("message", onMessageEvent);
								
								// Check if response doesn't have an error
								if("Error" in response === false) {
								
									// Resolve
									resolve();
								}
								
								// Otherwise
								else {
								
									// Reject
									reject();
								}
							}
						};
						
						// Window message event
						window.addEventListener("message", onMessageEvent);
						
						// Release USB device interface using mobile app
						MobileApp.releaseUsbDeviceInterface(currentRequestId.toFixed(), self.id, self.configurationId, interfaceNumber);
					});
				}
				
				// Transfer out
				transferOut(endpointNumber, data) {
				
					// Set self
					var self = this;
					
					// Return promise
					return new Promise(function(resolve, reject) {
					
						// Get current request ID
						var currentRequestId = self.getCurrentRequestId();
						
						// On message event
						var onMessageEvent = function(event) {
						
							// Try
							try {
							
								// Parse message as JSON
								var response = JSON.parse(event["data"]);
							}
							
							// Catch errors
							catch(error) {
							
								// Return
								return;
							}
							
							// Check if message is a response for this request
							if(typeof response === "object" && response !== null && "USB Request ID" in response === true && response["USB Request ID"] === currentRequestId.toFixed()) {
							
								// Remove this window message event
								window.removeEventListener("message", onMessageEvent);
								
								// Check if response doesn't have an error
								if("Error" in response === false) {
								
									// Resolve
									resolve();
								}
								
								// Otherwise
								else {
								
									// Reject
									reject();
								}
							}
						};
						
						// Window message event
						window.addEventListener("message", onMessageEvent);
						
						// Transfer USB device out using mobile app
						MobileApp.transferUsbDeviceOut(currentRequestId.toFixed(), self.id, self.configurationId, endpointNumber, data);
					});
				}
				
				// Transfer in
				transferIn(endpointNumber, length) {
				
					// Set self
					var self = this;
					
					// Return promise
					return new Promise(function(resolve, reject) {
					
						// Get current request ID
						var currentRequestId = self.getCurrentRequestId();
						
						// On message event
						var onMessageEvent = function(event) {
						
							// Try
							try {
							
								// Parse message as JSON
								var response = JSON.parse(event["data"]);
							}
							
							// Catch errors
							catch(error) {
							
								// Return
								return;
							}
							
							// Check if message is a response for this request
							if(typeof response === "object" && response !== null && "USB Request ID" in response === true && response["USB Request ID"] === currentRequestId.toFixed()) {
							
								// Remove this window message event
								window.removeEventListener("message", onMessageEvent);
								
								// Check if response's data is valid
								if("Data" in response === true && typeof response["Data"] === "string" && /^(?:[0-9A-F]{2})+$/iu.test(response["Data"]) === true) {
								
									// Resolve
									resolve({
									
										// Data
										"data": {
										
											// Buffer
											"buffer": new Uint8Array(response["Data"].match(/[0-9A-F]{2}/iug).map(function(hexCharacters) {
									
												// Return hex characters as a byte
												return parseInt(hexCharacters, 16);
											}))
										}
									});
								}
								
								// Otherwise
								else {
								
									// Reject
									reject();
								}
							}
						};
						
						// Window message event
						window.addEventListener("message", onMessageEvent);
						
						// Transfer USB device int using mobile app
						MobileApp.transferUsbDeviceIn(currentRequestId.toFixed(), self.id, self.configurationId, endpointNumber, length);
					});
				}
		}
		
		// USB connection event class
		class USBConnectionEvent extends Event {
		
			// Public
			
				// Constructor
				constructor(type, options) {
				
					// Delegate constructor
					super(type, options);
					
					// Set device to option's device
					this.device = options["device"];
				}
		}
		
		// WebUSB class
		class WebUsb extends EventTarget {
		
			// Public
			
				// Constructor
				constructor() {
				
					// Delegate constructor
					super();
					
					// Initialize USB devices
					this.usbDevices = {};
					
					// Set request ID to zero
					this.requestId = 0;
					
					// Set self
					var self = this;
					
					// Window message event
					window.addEventListener("message", function(event) {
					
						// Try
						try {
						
							// Parse message as JSON
							event = JSON.parse(event["data"]);
						}
						
						// Catch errors
						catch(error) {
						
							// Return
							return;
						}
						
						// Check if message could be a USB event
						if(typeof event === "object" && event !== null && "Event" in event === true && typeof event["Event"] === "string" && "Data" in event === true) {
						
							// Check event
							switch(event["Event"]) {
							
								// USB device connected event
								case "USB Device Connected":
								
									// Check if data is valid
									if(typeof event["Data"] === "object" && event["Data"] !== null && "ID" in event["Data"] === true && typeof event["Data"]["ID"] === "string" && "Manufacturer Name" in event["Data"] === true && typeof event["Data"]["Manufacturer Name"] === "string" && "Product Name" in event["Data"] === true && typeof event["Data"]["Product Name"] === "string" && "Vendor ID" in event["Data"] === true && typeof event["Data"]["Vendor ID"] === "number" && "Product ID" in event["Data"] === true && typeof event["Data"]["Product ID"] === "number") {
									
										// Check if USB device doesn't exist in the list
										if(event["Data"]["ID"] in self.usbDevices === false) {
										
											// Add USB device to list
											self.usbDevices[event["Data"]["ID"]] = new UsbDevice(self.getCurrentRequestId, event["Data"]["ID"], event["Data"]["Manufacturer Name"], event["Data"]["Product Name"], event["Data"]["Vendor ID"], event["Data"]["Product ID"]);
										}
										
										// Trigger USB connect event
										self.dispatchEvent(new USBConnectionEvent("connect", {
										
											// Device
											"device": self.usbDevices[event["Data"]["ID"]]
										}));
									}
									
									// Break
									break;
									
								// USB device disconnected event
								case "USB Device Disconnected":
								
									// Check if data is valid
									if(typeof event["Data"] === "string") {
									
										// Check if USB device exists in the list
										if(event["Data"] in self.usbDevices === true) {
										
											// Trigger USB disconnect event
											self.dispatchEvent(new USBConnectionEvent("disconnect", {
											
												// Device
												"device": self.usbDevices[event["Data"]]
											}));
											
											// Remove USB device from list
											delete self.usbDevices[event["Data"]];
										}
									}
									
									// Break
									break;
							}
						}
					});
				}
				
				// Get devices
				getDevices() {
				
					// Set self
					var self = this;
					
					// Return promise
					return new Promise(function(resolve, reject) {
					
						// Get current request ID
						var currentRequestId = self.getCurrentRequestId();
						
						// On message event
						var onMessageEvent = function(event) {
						
							// Try
							try {
							
								// Parse message as JSON
								var response = JSON.parse(event["data"]);
							}
							
							// Catch errors
							catch(error) {
							
								// Return
								return;
							}
							
							// Check if message is a response for this request
							if(typeof response === "object" && response !== null && "USB Request ID" in response === true && response["USB Request ID"] === currentRequestId.toFixed()) {
							
								// Remove this window message event
								window.removeEventListener("message", onMessageEvent);
								
								// Check if response's data is valid
								if("Data" in response === true && Array.isArray(response["Data"]) === true) {
								
									// Go through all USB devices in the response
									for(var i = 0; i < response["Data"]["length"]; ++i) {
									
										// Check if USB device is valid and it doesn't exist in the list
										if(typeof response["Data"][i] === "object" && response["Data"][i] !== null && "ID" in response["Data"][i] === true && typeof response["Data"][i]["ID"] === "string" && "Manufacturer Name" in response["Data"][i] === true && typeof response["Data"][i]["Manufacturer Name"] === "string" && "Product Name" in response["Data"][i] === true && typeof response["Data"][i]["Product Name"] === "string" && "Vendor ID" in response["Data"][i] === true && typeof response["Data"][i]["Vendor ID"] === "number" && "Product ID" in response["Data"][i] === true && typeof response["Data"][i]["Product ID"] === "number" && response["Data"][i]["ID"] in self.usbDevices === false) {
										
											// Add USB device to list
											self.usbDevices[response["Data"][i]["ID"]] = new UsbDevice(self.getCurrentRequestId, response["Data"][i]["ID"], response["Data"][i]["Manufacturer Name"], response["Data"][i]["Product Name"], response["Data"][i]["Vendor ID"], response["Data"][i]["Product ID"]);
										}
									}
									
									// Create USB devices
									var usbDevices = [];
									
									// Go through all USB devices
									for(var id in self.usbDevices) {
												
										if(self.usbDevices.hasOwnProperty(id) === true) {
										
											// Append USB device to list
											usbDevices.push(self.usbDevices[id]);
										}
									}
									
									// Resolve USB devices
									resolve(usbDevices);
								}
								
								// Otherwise
								else {
								
									// Reject
									reject();
								}
							}
						};
						
						// Window message event
						window.addEventListener("message", onMessageEvent);
						
						// Get USB devices using mobile app
						MobileApp.getUsbDevices(currentRequestId.toFixed());
					});
				}
				
				// Request device
				requestDevice(filters) {
				
					// Set self
					var self = this;
					
					// Return promise
					return new Promise(function(resolve, reject) {
					
						// Get current request ID
						var currentRequestId = self.getCurrentRequestId();
						
						// On message event
						var onMessageEvent = function(event) {
						
							// Try
							try {
							
								// Parse message as JSON
								var response = JSON.parse(event["data"]);
							}
							
							// Catch errors
							catch(error) {
							
								// Return
								return;
							}
							
							// Check if message is a response for this request
							if(typeof response === "object" && response !== null && "USB Request ID" in response === true && response["USB Request ID"] === currentRequestId.toFixed()) {
							
								// Remove this window message event
								window.removeEventListener("message", onMessageEvent);
								
								// Check if response's data is valid
								if("Data" in response === true && typeof response["Data"] === "object" && response["Data"] !== null && "ID" in response["Data"] === true && typeof response["Data"]["ID"] === "string" && "Manufacturer Name" in response["Data"] === true && typeof response["Data"]["Manufacturer Name"] === "string" && "Product Name" in response["Data"] === true && typeof response["Data"]["Product Name"] === "string" && "Vendor ID" in response["Data"] === true && typeof response["Data"]["Vendor ID"] === "number" && "Product ID" in response["Data"] === true && typeof response["Data"]["Product ID"] === "number") {
								
									// Check if USB device in the response doesn't exist in the list
									if(response["Data"]["ID"] in self.usbDevices === false) {
									
										// Add USB device to list
										self.usbDevices[response["Data"]["ID"]] = new UsbDevice(self.getCurrentRequestId, response["Data"]["ID"], response["Data"]["Manufacturer Name"], response["Data"]["Product Name"], response["Data"]["Vendor ID"], response["Data"]["Product ID"]);
									}
									
									// Resolve USB device
									resolve(self.usbDevices[response["Data"]["ID"]]);
								}
								
								// Otherwise check if response's error is valid
								else if("Error" in response === true && typeof response["Error"] === "string") {
								
									// Check error
									switch(response["Error"]) {
									
										// No device found
										case "No device found":
										
											// Reject error
											reject(new DOMException("", "NoChoiceError"));
											
											// Break
											break;
											
										// No device selected
										case "No device selected":
										
											// Reject error
											reject(new DOMException("", "NotFoundError"));
											
											// Break
											break;
											
										// Default
										default:
										
											// Reject
											reject();
											
											// break
											break;
									}
								}
								
								// Otherwise
								else {
								
									// Reject
									reject();
								}
							}
						};
						
						// Window message event
						window.addEventListener("message", onMessageEvent);
						
						// Request USB device using mobile app
						MobileApp.requestUsbDevice(currentRequestId.toFixed());
					});
				}
				
			// Private
			
				// Get current request ID
				getCurrentRequestId() {
				
					// Get current request ID
					var currentRequestId = this.requestId++;
					
					// Check if current request ID is at the max safe integer
					if(currentRequestId === Number.MAX_SAFE_INTEGER) {
					
						// Reset request ID
						this.requestId = 0;
					}
					
					// Return current request ID
					return currentRequestId;
				}
		}
		
		// Use mobile app's WebUSB implementation
		navigator["usb"] = new WebUsb();
	}
}
