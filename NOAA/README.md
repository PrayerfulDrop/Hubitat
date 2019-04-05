Instructions:
After adding the groovy code to your Apps Code in Hubitat go into your Apps and add a New User App. After the app installs select your PushOver and/or TTS/Echo Speaks devices and then hit done. NOAA Weather Alerts is setup and should begin announcing any new weather alerts for your area.

For those who would like to customize your NOAA Weather Alerts below is a description of what each option does:

Alert Message - allows you to customize the alert that is announced. You can use {variables} listed in the Customization Instructions to create your own custom message.

<b>Restrictions</b> - you can either restrict by mode or by switch

Weather Severity - You have choices of Moderate, Severe and Extreme. The more restrictive the severity to fewer announcements.

Alerts Urgency - Immediate is just alerts that are for immediate notifications. Expected is forecasted alerts.

Poll Frequency - change how often NOAA pulls for weather alerts

Repeat alerts - repeat a given alert in a certain # of minutes from original announcement

Custom Coordinates - allows for custom latitude/longitude to acquire weather alerts from another location within the USA

Watch a Specific Weather Alert - allows for reduction of alerts to be monitored to just those alerts you are wanting to be announced

Run a test Alert - test NOAA and your configuration

Enabel Debug Logging - if you are having issues and/or need support enable debug logging. Debug logging will automatically shutoff after 15 minutes to reduce the amount of logs being produced by NOAA
