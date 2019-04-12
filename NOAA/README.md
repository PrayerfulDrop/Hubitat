<h1>NOAA Weather Alerts</h1>

NOAA Weather Alerts provides Severe and Extreme weather notifications for USA based users through both a TTS device, Echo Speaks devices, PushOver Notifications and/or Dashboard tiles.

<b>Instructions:</b>
After adding the groovy code to your "Apps Code" and "Drivers Code" in Hubitat go into your Apps and add a New User App. After the app installs select your PushOver and/or TTS/Echo Speaks devices and then hit done. NOAA Weather Alerts is setup and should begin announcing any new weather alerts for your area.

For those who would like to customize your NOAA Weather Alerts below is a description of what each option does:

<b>Alert Message</b> - allows you to customize the alert that is announced. You can use {variables} listed in the Customization Instructions to create your own custom message.

<b>Restrictions</b> - you can either restrict by mode or by switch

<b>Weather Severity</b> - You have choices of Moderate, Severe and Extreme. The more restrictive the severity to fewer announcements.

<b>Alerts Urgency</b> - Immediate is just alerts that are for immediate notifications. Expected is forecasted alerts.

<b>Poll Frequency</b> - change how often NOAA pulls for weather alerts

<b>Repeat alerts</b> - repeat a given alert in a certain # of minutes from original announcement

<b>Custom Coordinates</b> - allows for custom latitude/longitude to acquire weather alerts from another location within the USA

<b>Weather Severity</b> - Allows to choose what severity to watch for: Moderate, Severe and Extreme

<b>Watch a Specific Weather Alert</b> - allows for reduction of alerts to be monitored to just those alerts you are wanting to be announced

<b>Alert Certainty</b> - allows to specify for Possible, Likely and Observed

<b>Alert Urgency</b> - allows to specify Immediate, Expected and Future 

<b>Run a test Alert</b> - test NOAA and your configuration

<b>Enabel Debug Logging</b> - if you are having issues and/or need support enable debug logging. Debug logging will automatically shutoff after 15 minutes to reduce the amount of logs being produced by NOAA
