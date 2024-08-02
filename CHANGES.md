## Version 3.26-beta1 ##

### Whats new: ###
- TBD

### Known Bugs: ###
- TBD

### Notes: ###

## Version 3.15.0 ##

### Whats new: ###
- Feature: Improved SBEM-to-JSON conversion with reduced memory consumption
####
- Bugfix: Fix for dynamic libc++_shared.so (iOS)
####
- Technical: Changed version numbering to follow Suunto

### Known Bugs: ###


### Notes: ###
- See [README.md](./README.md) and included [samples](./android/samples/) and [iOS showcase app][samples](./IOS/MovesenseShowcase/) to see how to use different features.

## Version 1.68.0 ##

### Whats new: ###
- Feature: Support fast Logbook data fetch (some sensor models, up-to 20x faster).
####
- Bugfix: Fix memory leaks
####
- Technical: Remove unused curl library
- Technical: Remove unnecessary logging to avoid creating huge log files, e.g. in SOF firmware update

### Known Bugs: ###


### Notes: ###
- See [README.md](./README.md) and included [samples](./android/samples/) and [iOS showcase app][samples](./IOS/MovesenseShowcase/) to see how to use different features.

## Version 1.63.0 ##

### Whats new: ###
- Feature: ToFile to Logbook-resource path (i.e. //MDS/Logbook/{serial}/byId/{logId}/ToFile) to write json directly to a file
- Feature: Update SBEM library reference (support for enum as value in Json)
- Feature(iOS): Use dynamically negotiated BLE MTU

- Bugfix(iOS): Fix debug console log level handling
- Bugfix(iOS): Restore devices in connecting-state when the app wakes up from background
- Bugfix(iOS): Fix NSDictionary* to NSMutableDictionary* assignment in forwardMDSResourceRequestCallbackOfType
- Bugfix(Android): Consider 100 CONTINUE response as success
- Bugfix(Android): RxJava2 default error handler missing TP[99266]
 
- Technical: Updated Whiteboard to latest release
- Technical(iOS): Protect peripherals-dictionary with synchronized block
- Technical(iOS): Mark the didFailToConnectWithError callback parameter nullable
- Technical(iOS): Add UUID to connection failure method of MDSConnectivityServiceDelegate
- Technical(iOS): Propagate missing service and read characteristic errors to delegate callback
- Technical(iOS): Updated DFU library in Showcase App (fixes build on XCode 13)
- Technical: Propagate more detailed error message to the client (inside header of the response-callback)
 
### Known Bugs: ###


### Notes: ###
- See [README.md](./README.md) and included [samples](./android/samples/) and [iOS showcase app][samples](./IOS/MovesenseShowcase/) to see how to use different features.


## Version 1.44.0 ##

### Whats new: ###
- Bugfix: Use Logbook descriptor cache (clear when the device disconnects)
- Bugfix(Android): Reconnect doesn't work if multiple connections (Issue #88)
- Bugfix(Android): Remove connection monitor on disconnect
- Bugfix(Android): Disconnect doesn't give callback if reconnected at some point
- Bugfix: Fix eventlistener subscriptions for overlapping paths (e.g. previously subscribing both MDS/resource/subresource and MDS/resource didn't work correctly)

- Technical(Android): Migrate to androidx, upgrade gradle and dependencies
- Technical(iOS): Increase MTU size


### Known Bugs: ###
- TBD

### Notes: ###
- See [README.md](./README.md) or included [Android samples](./android/samples/) to see how to take rxjava2 in use

## Version 1.39.0

### Whats new:
- Feature: New "ToFile" Logbook-resource parameter for writing converted json directly to the file (instead of returning it in the response)
- Technical(Android): Upgrade to rxjava2 and rxandroidble2. This solves issue #82 and should fix MissingBackpressureExceptions
- Technical: Refactored Logbook-resource sbem to json conversion for lower memory consumption

### Known Bugs:
- TBD

### Notes:
- See [README.md](./README.md) or included [Android samples](./android/samples/) to see how to take rxjava2 in use

## (Oct 30th 2019) Update to mobile libraries  

### Android and iOS (MDS Version 1.38.0, Whiteboard 4.1.2, libsbem 1.9)

### Whats new:

- Feature(iOS): BLE Connectivity improvements and dynamic maximum transmit payload length (MTU) for the latest generation
- Feature: Support for registered 16bit BLE service UUID

- Bugfix(iOS): Some smaller memory leak fixes
- Bugfix(iOS): Fix memory leak on event/notification send from the MDS to the client/app
- Bugfix(iOS): Do not attempt reconnection if write is not permitted
- Bugfix: Disable descriptors cache 

- Technical: Whiteboard 4.1.2
- Technical(iOS): BLE Start connection timer after the state is restored
- Technical(iOS): Handle removed service (didMofifyServices) correctly
- Technical: Increased timeouts for Logbook-resource
- Technical: Minor improvements for handling of multiple simultaneous connections 


### Known Bugs:

### Notes:


## (Apr 10th 2019) Update to mobile libraries  

### Android and iOS (MDS Version 1.34.1, Whiteboard 4.1.1, libsbem 1.9)

### Whats new:
- Feature: Path variable resource id cache (faster Logbook GET)
- Feature(iOS): store debug logs etc. files to application support directory

- Bugfix: multiple device connection that got broken with WB 4.1.0 integration
- Bugfix: Fix metadata cache handling
- Bugfix(iOS): BLE: Fix reconnection failure after pairing is removed on device
- Bugfix(iOS): BLE Fix multiple device connection issue (skisense)

- Technical: Whiteboard 4.1.1
- Technical: libsbem 1.9 (Fix compiling with VS 2013, fix JSON to SBEM binary conversion for unit tests)
- Technical: update to libsbem 1.8 (fix JSON to SBEM binary conversion for unit tests)
- Technical(Android): 64bit Android support (Issue #79)
- Technical(Android): Add status code to MdsException
- Technical(Android): Add response handler parameter to subscriptions
- Technical(Android): Add x86 build
- Technical(iOS): Add device UUID to the disconnected event
- Technical(iOS): BLE Connectivity updates

### Known Bugs:

### Notes:

## (Oct 23rd 2018) Update to mobile libraries  

### Android and iOS (MDS 1.28.1, Whiteboard 3.16.3, libsbem 1.7)

###Whats new:
- Bugfix(Android): BleManager - MissingBackpressureException (#75)
- Bugfix(Android): Match MAC on disconnect / Reconnection fails when multiple sensors are used (#74)
- Bugfix(Android): Propagate RxBle errors to onConnectError (#75)
- Technical: Whiteboard updated to v 3.16.3


## (Oct 3rd 2018) Update to mobile libraries

Movesense mobile libraries (MDS) for both iOS and Android have been updated. See below for list of changes per platform.

### Android (MDS 1.25.0, Whiteboard 3.16.0, libsbem 1.7) ###  

  - Technical: Logbook json conversion bugfixes and improvements
  - Technical: ResourceId caching for non pathmeter resources (less traffic to get and release remote resource ids)
  - Technical: Don't log email addresses (GDPR Compliancy)
  - Technical: Light in-memory cache Logbook/Data resource
  - Technical(iOS): Enable use of an external CBCentralManager to be provided to WBCentral
  - Technical(iOS): Added device discovery control to iOS MDSWrapper API
  - Technical(iOS): Forward debug logging to iOS system log
  - Technical(iOS): External device UUID list in init() for connecting only interested devices
  - Technical(iOS): Add public MDS log level API for iOS (sets logging level for suuntoapp.log output)
  - Bugfix: Descriptor cleanup crash when device has been updated
  - Bugfix: Missing DEL event in disconnect
  - Bugfix: MDS shutdown crash in ResourceClient destruction
  - Bugfix: Protect callback thread joining in ResourceClient
  - Bugfix: Add missing UUID-field to GET ConnectedDevices response
  - Bugfix: Manually POSTing serial devices
  - Bugfix: Fix only partially downloaded Logbook entries
  - Bugfix(Android): Concurrent modification exception
  - Bugfix(iOS): Added logic for re-pairing a device even it has not been removed from iOS BT paired list.
  - Bugfix(iOS): Modified reconnection flow to fix the sensor connectivity issues
  - Bugfix(iOS): iOS BLE-plugin parallel connections
  - Bugfix(iOS): WBCentral restorestate handling fixies
  - Bugfix(iOS): Pass BLE received data forward immediately without dispatch queue
  - Bugfix(iOS): Add more checks to prevent crash if the response contains invalid utf-8 characters 

### Android (MDS 1.13.0, Whiteboard 3.12.0, libsbem 1.3) ###  

Changes compared to previous library version (1.10.0) on Android:

  - Technical: Reduce default WB retries from 20 to 3
  - Technical: SDS version resource for whiteboard devices
  - Technical: DeviceConnector to support WB devices without InetGW
  - Technical: Allow NoType to be used in Bypass subscription
  - Technical: Serialize BusyState subscription
  - Technical: Disable BusyState subscription by default
  - Technical: Added response code to MdsResponseListener
  - Technical: Device busy subscription to ResourceClient
  - Bugfix: Connect without serial in device discovery
  - Bugfix: Add public MDS log level API for Android
  - Bugfix: Synchronize mIBleConnectionMonitorArrayList usage in BleManager
  - Bugfix: Fix IllegalStateException where RxBleConnection subscription and internal devicesMap are not in sync
  - Bugfix: Prevent NPE when callback is null
  - Bugfix: Fix simultaneous connection issue with builds having busy subscription enabled
  - Bugfix: Send unsubscriptions to providers in application when subscribed device disconnects

### iOS (MDS 1.13.0, Whiteboard 3.12.0, libsbem 1.3) ###

Changes compared to previous library version (1.7.0) on iOS:

  - Technical: Reduce default WB retries from 20 to 3
  - Technical: SDS version resource for whiteboard devices
  - Technical: DeviceConnector to support WB devices without InetGW
  - Technical: Allow NoType to be used in Bypass subscription
  - Technical: Serialize BusyState subscription
  - Technical: Disable BusyState subscription by default
  - Technical: Device busy subscription to ResourceClient
  - Technical: iOS MDSWrapper to set resource request method as SUBSCRIBE/UNSUBSCRIBE instead of POST/DEL
  - Technical: iOS MDSWrapper improvements
  - WB resource registeration
  - Support ByteStream type without base64 encoding
  - Technical: Release metadata hash resource if retrieved during WB operation
  - Bugfix: Connect without serial in device discovery
  - Bugfix: Fix simultaneous connection issue with builds having busy subscription enabled
  - Bugfix: Send unsubscriptions to providers in application when subscribed device disconnects
  - Bugfix: Fix possible unsubscription race condition crash at device disconnect
  - Bugfix: Fix issue where disconnect during handshake causes device to appear connected

---

# Release notes for earlier releases #

## More samples release ##

### Whats new: ###
   **NOTE**: This is an Android samples only release, library version and Showcase app (sampleapp) stay the same)
   - Improved ConnectivityAPISample
   - Added SensorSample
   - Added ECGSample


## Android Library Version: 1.6.1 ##
## iOS Library Version: 1.3.1 ##
## Application Version: 1.3 ##

### Whats new: ###
   **NOTE**: This is an Android only release (i.e. the changes are only about the Android version)
   - Improve DFU update process ( fix issue with device disconnected error)
   - Add apk with Android sample App in directory android/samples/sampleapp/
   - Fix 404 error after changing device name
   - Add support for dfu manufacture data ( fix issue with unknown macAddress when Movesense has manufacture data writen)
   - Fix typo in Magnetic Field
   - Fix null serial and sw version after device name change
   - Add information about connected device into main screen after connection
   - Fix dfu incrementation with
   - Fix connection issue with fast stop scanning click before connection was established
   
## Android Library Version: 1.6.0 ##
## iOS Library Version: 1.3.1 ##
## Application Version: 1.2 ##

### Whats new: ###
   **NOTE**: This is an Android only release (i.e. the changes are only about the Android version)
   - Moved the Android samples under separate folder
   - The .aar package can now be found in folder *./Android/Movesense/*
   - New simple *Connectivity API* for Android MDS library
   - New sample for Android: "ConnectivityAPISample"
   - NOTE: The *sampleapp* still uses the old connectivity

   (No known bugs since half of the samples use still the old method)

## Library Version: 1.3.1 ##
## Application Version: 1.2 ##

### Whats new: ###

   - Improvment UI design
   - DFU is moved to the main screen
   - Added MultiConnection
   - Added Version Library and Application on the main view
   - Toolbars changing name to current view

### Known Bugs: ###

   - Movesense not reconnecting with app after bluetooth off / on.
   - Nexus 6 can't connect with Movesense ( Android platform BUG )

## Library Version: 1.3.1 ##
## Application Version: 1.1 ##

### Whats new: ###

   - Fixed: Memory leaks for longer sensor subscriptions ( BackPressureException ) 
   - Recovery for non-intentionall disconnect ( Movesense out-of-range / disconnect / connection lost )
   - Validation for DFU file ( sometimes file was corrupted by Google Drive )
   - Subscription is not stopped when devices is going to sleep.
   - Allow connection for new and old software ( changes in JSON response)
   - More Unit Tests for library.
   
### Known Bugs: ###

   - When DFU update will fail at some point then may have problem with connection because Movesense is still in DFU mode ( solution -> DFU will be moved to main view in next Application version )
   - Movesense sometimes can't reconnect to Application.
   - Movesense not reconnecting with app after bluetooth off / on.
