Kiosk App
=========
A simple template for basic Android kiosk apps


Use case
--------
You want to use an Android device for a task such as recording survey responses, or in a kiosk-type deployment. But you don't want people to be able to escape from the app you are using for this purpose.

There are lots of examples of how to do this, but none of them seem to give a basic working method where the only possible interaction is the app you want to use. This repository is a simple template to help get started with these types of application.


Setup
-----
Edit the template's code to add your own questions or other functionality, making sure to preserve the call to `startKioskMode()` at startup so that your app requests kiosk mode.

When you first deploy your app, you need to grant device ownership to the `SurveyDeviceAdminReceiver` component. This is a one-time process, and the permission persists across device reboots. For the template's package and class name, run the following command while connected to your device:

`adb shell dpm set-device-owner ac.robinson.kiosk/.SurveyDeviceAdminReceiver`

Note that once you have run this command, the next time you start your app you will not be able to exit from it without restarting the device or using `adb`

This permission can only be granted to one app/component at once, so you may need to remove the previous permission if you change the package or class name for your own app. Do this via:

`adb shell dpm remove-active-admin ac.robinson.kiosk/.SurveyDeviceAdminReceiver`


Caveats
-------
It is important to note that unrestricted use of the permissions and capabilities you can grant using this mode can make it so that you are unable to exit or update your app without factory resetting the device. This is especially the case if you choose to, for example, launch your app on device startup. The template here is setup so that this will not be the case, and you will be able to develop and debug without problems, but for more information see [full details and additional options](https://snow.dog/blog/kiosk-mode-android).


Credits
-------
The kiosk mode method used in this sample is based on the excellent guide at https://snow.dog/blog/kiosk-mode-android, which also goes into further detail about other options and important factors to be aware of.


License
-------
MIT
