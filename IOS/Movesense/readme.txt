
Quick guide for the MDS-library usage 


1. Project

  MDS-library has just two files MDS.h (Objective-C) and libmds.a that you need to copy to your project
  and you sould also link your binary with following libraries:

    libmds.a
    libz.tbd
    libc++.tbd
    CoreFoundation
    CoreBluetooth

  The default Xcode Build Settings are mostly suitable.



2. Initialization/Release

  MDS-library is initialized simply by

    MDSWrapper* mds = [[MDSWrapper alloc] init];

  When you are done with the library or at the latest before your application exits call

    deactivate

  as not to do so will most likely lead a crash of the MDS-library when the application exits. 



3. Connectivity

3.1 Listening device connections/disconnections

  Before making any connection requests you should setup a listener to get notifications of connected and disconnected devices:

	[mds doSubscribe:@"MDS/ConnectedDevices" contract:@{}
        response:^(MDSResponse *response) {
            if (response.statusCode == 200)
            {
                NSLog(@"Listening connected devices: %@", [response description]);
            }
            else
            {
                NSLog(@"Failed to start listening connected devices: %@", [response description]);
            }
        }
        onEvent:^(MDSEvent *event) {
            if ([event.bodyDictionary[@"Method"] isEqualToString:@"POST"])
            {
                NSLog(@"Device connected: %@", [event description]); // serial number can be catched in here
            }
            else if ([event.bodyDictionary[@"Method"] isEqualToString:@"DEL"])
            {
                NSLog(@"Device disconnected: %@", [event description]);                        
            }
        }];

  
  In response-block you should check from statusCode-field whether the subscription request was successful or not (MDS-library uses HTTP status 
  codes flexibly (https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html).

  onEvent-block will receive notifications related to ConnectedDevice-resource of the MDS. Method "POST" means that a device 
  is added (i.e. connected) to the system and "DEL" means it is removed (i.e. disconnected). When "POST" event is received its 
  bodyDictionary contains three items "Serial", "Connection" and "DeviceInfo" which will give further information about the device 
  connected. "Serial" is important as it will be used when accessing resources of the device. "DEL" event bodyDictionary will contain 
  just "Serial" of the disconnected device.

  If/when you want to stop listening those events you should call doUnsubscribe like this

    [mds doUnsubscribe:@"MDS/ConnectedDevices"];


3.2 Device discovery

  At the moment discovering suitable BLE devices is responsibility of the client application so you must setup your own instance of 
  CBCentralManager (https://developer.apple.com/documentation/corebluetooth/cbcentralmanager) to scan peripherals (scanForPeripheralsWithServices) 
  supporting serviceUUID 61353090-8231-49cc-b57a-886370740041 (Movesense BLE service UUID). Note that this instance should be used only 
  for scanning the Movesense supported devices not to connect or cancelConnection with them.


3.3 Connecting

  When you have found the device and its UUID call 

    connectPeripheralWithUUID

  to start connection request. Call will return immediately but only after you have received "connected" notification for your device (from 
  the listener we setup in the previous chapter) connection is established throughout the system. Note that connectPeripheralWithUUID call 
  will not timeout so to explicitly cancel pending connection request call 

    disconnectPeripheralWithUUID.

  disconnectPeripheralWithUUID returns Boolean value where 0 means that either the passed UUID was unknown or the device was already 
  disconnected. Value 1 just means that cancelling or disconnection request in OS level is done (but not completed). Disconnection is 
  completed when "disconnected" ("DEL") notification is received assuming that the device was connected throughut the system in the first place. 

  MDS-library will take care of reconnections e.g. when the BLE connection is lost and recovered again, but if you want to disable this 
  behaviour you can call

    disableAutoReconnectForDeviceWithSerial.

  Note that for this call you should pass serial of the device instead of UUID. This call should be issued after you have received 
  the "connected" event as otherwise the connection attempt might fail before it is completed throughout the system and currently 
  application have no way to know it. Also note that this method should be redone after every connectPeripheralWithUUID call as it will re-enable 
  the autoreconnect feature.



4. Communicating with the device

4.1 Requesting data

  To get resource value, data structure or whatever is specified in its YAML MDS-library provides

    doGet

  for it. Simple example about how to get the time from the device is below

    [mds doGet:@"<serialnumber of the device>/Time" contract:{}
        completion:^(MDSResponse* response) {
            NSLog(@"Got response: %@", response);

            if (response.statusCode == 200)
            {
                NSLog(@"OK");
                time = response.bodyDictionary["Content"];
            }
            else
            {
                NSLog(@"Failed");
            }
        }]; 

   
  Once again MDSResponse statusCode tells whether the request succeeded or not and the bodyDictionary contains the actual data received 
  assuming that the request was successful. In responses coming from device the bodyDictionary contains an item called "Content" 
  which has the actual data.


4.2 Setting data

  Setting data to the device is done either with

    doPut

  or
  
    doPost

  and both are quite similar to doGet but you must fill the data into the contract-parameter (NSDictionary) like it is described in 
  the YAML of the resource.
 

4.3 Subscriptions

  To subscribe notifications from the resource as described in its YAML MDS-library provides

    doSubscribe 

  method (familiar from the setting up connected devices listener). With a device it can be used like this

	[mds doSubscribe:@"<serialnumber of the device>/resourcepath" contract:<parameters of subscription as NSDictionary>
        response:^(MDSResponse* response) {
            NSLog(@"Got response: %@", response);
            
        }
        onEvent:^(MDSEvent* event) {
            NSLog(@"Got event: %@", event);
    	}];

  Note that the resourcepaths used in here should NOT contain the "/Subscription" (as seen in YAML documentation) part i.e the resourcepath 
  to subscribe e.g. device time is just "Time" not "Time/Subscription".

  
  To stop listening notifications call doUnsubscribe for the same uri/path
  
    [mds doUnsubscribe:@"<serialnumber of the device>/resourcepath"];


  To update parameters of the existing subscription use

    doUpdateSubscription

  like this

    mds doUpdateSubscription:@"<path of the existing subscription>" contract:<new parameters as NSDictionary>
        completion:^(MDSResponse* response) {
            NSLog(@"Got response: %@", response);

            if (response.statusCode == 200)
            {
                NSLog(@"OK");
            }
            else
            {
                NSLog(@"Failed"); // e.g. there were no existing subscription for the give path
            }
        }]; 


4.4 Delete

  To delete a resource 

    doDelete

  which once again behaves same way as methods above.


4.5 ByteStreams

  Currently MDS-library does not support binary data transfer as such thus all requests to the ByteStream type of resources 
  return the data (i.e. content of the ByteStream) in Base64 encoded format and it is up to an application to decode it back 
  to the binary format.



5 Communicating with the MDS

  MDS-library itself also offers some services/resources that be used from the application. Common thing of all these internal 
  resources is that in the full resource path serial is replaced with "MDS" meaning the request is targeted to the MDS itself 
  not to a external device.

  Note! Inconviniently responses from these MDS internal resources do NOT have "Content" root item like responses from the device do!


5.1 ConnectedDevices
 
  "MDS/ConnectedDevices" resource can be used to listen notifications about connected and disconnected devices 
  but also getting list of currently connected devices

    [mds doGet:@"MDS/ConnectedDevices"] contract:{}
        completion:^(MDSResponse* response) {
            NSLog(@"Got response: %@", response);

            if (response.statusCode == 200)
            {
                dictionaryAboutCurrentlyConnectedDevices = response.bodyDictionary;
            }
            else
            {
                NSLog(@"Failed");
            }
        }]; 


5.2 Logbook

  Movesense device has a resource to read entries/logs stored in it, but it returns the data in specific binary format which requires 
  its own decoder to convert it thus the MDS provides "MDS/Logbook/..." convinience resource(s) to return logbook entries in JSON format.

  To read a log from the device you first need its id which can be retrieved directly from the device by using its "/Mem/Logbook/Entries"
  resource or from "MDS/Logbook/<your serial>/Entries" resource of the MDS. Both of these resources return a list of entries containing id, 
  modification date and size of the log. Only difference is that MDS-resource returns all entries with one query and the device returns 
  longer lists in several pieces as specified in its YAML.

  With the id you can query the content (in JSON) of the log from "MDS/Logbook/<serial>/ById/<id>/Data" resource.

  [mds doGet:@"MDS/Logbook/<your serial here >/ById/<id of the log>/Data"] contract:{}
        completion:^(MDSResponse* response) {
            NSLog(@"Got response: %@", response);

            if (response.statusCode == 200)
            {
                loggedData = response.bodyDictionary;
            }
            else
            {
                NSLog(@"Failed");
            }
        }]; 

  Note! MDS-resource(s) reads the information from the device thus it have to be connected while doing these requests.


5.3 Version

  "MDS/Version" resource can used to get version information of the MDS-library itself.



6 Runtime created files

  MDS-library creates/writes two type of files during runtime into Documents folder of the application.

    mdcache_<serial>  contains cached information about resource metadata read from the device to reduce amount of traffic over the air. 
    suuntoapp.log     debug information mainly useful MDS-library developers


