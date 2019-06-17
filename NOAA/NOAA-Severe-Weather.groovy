/**
 *
 * Hubitat Import URL: https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/NOAA/NOAA-Severe-Weather.groovy
 *
 *  ****************  NOAA Weather Alerts App  ****************
 *
 *  Design Usage:
 *  This app is designed to notify you of severe and extreme weather alerts to a specified TTS device.  This is only for US based users.  
 *  Willing to collaborate with others to create world-wide solution.
 *
 *  Copyright 2018 Aaron Ward
 *
 *  Special thanks to csteele for all his help and education of Groovy.  Another special thanks to contributions from bptworld!
 *  
 *  This App is free and was designed for my needs originally but has grown for most needs too.
 *
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *              Donations are always appreciated: https://www.paypal.me/aaronmward
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  Changes:
 *   2.2.9 - fixed some confusing wording, customized look and feel to match NOAA color theme, added ability to set log debug disable timeout, added donations link
 *   2.2.8 - added customization for introduction to alert for TTS devices to cleanup PushOver and NOAA Tile notifications, catch potential API service availability issues
 *   2.2.7 - added weather.gov URI that is built based on user options in app
 *   2.2.6 - added ability to see weather.gov API current response with app settings in place
 *   2.2.5 - fixed new introduced looping error due to code changes, reduced info message from two lines of logs to a one to save log retention
 *   2.2.4 - fixed installation issue and fixed comparison of alert sent dates issue
 *   2.2.3 - fixed repeat every 5 minutes issue
 *   2.2.2 - added global volume/restore for all TTS devices, enabled ability for multiple different TTS device types, added support for Google Devices modified GUI to accomodate.
 *   2.2.1 - fixed custom coordinates not being displayed
 *   2.2.0 - fixed repeat issues - can be tested with issuing a test alert
 *   2.1.9 - support for updated NOAA Tile Driver
 *   2.1.8 - fixed NOAA Dashboard Tile to reset after user predefined time, fixed bug with repeating the alert
 *   2.1.7 - moved options around into appropriate categories to support simple installation, added warning and URL to weather.gov for advanced configuration testing, removed required fields for simple install of NOAA
 *   2.1.6 - minor code cleanup for install, modified test alert to use custom formatting for testing
 *   2.1.5 - added minor severity for those who want weather forecasting and ALL weather alerts 
 *   2.1.4 - added alert certainty to support Hydrologic alerts, changed alert severity and urgency to multi-select, cleaned up code a bit, reset tile alerts if no alerts in feed
 *   2.1.3 - added automatic volume restoral for EchoSpeaks devices
 *   2.1.2 - added ability to restore volume for EchoSpeaks devices
 *   2.1.1 - fixed Weather Alert Event Codes
 *   2.1.0 - fixed test alert to reset dashboard tile
 *   2.0.9 - added new Dashboard Tile ability (thanks to BPTWorld!)
 *   2.0.8 - added ability to turn on switch if a weather alert occurs
 *   2.0.7 - removed 30 minute option for polling
 *   2.0.6 - added ability to alert only on specific weather alert code events
 *   2.0.5 - added ability to use custom coordinates
 *   2.0.4 - removed requirements of forcing selections in TTS, enabled the option to just use PushOver and not use TTS 
 *   2.0.3 - added ability to have both Music/Speech TTS and Echo Speaks devices for notification services
 *   2.0.2 - fixed {alertevent} replacement, modified URI string to only look for actual alerts (NOAA was notifying test events due to lack of URI refinement) 
 *   2.0.1 - fixed testing alertmsg
 *   2.0.0 - alertmsg building components, added customization of weather announcement along with variable inputs, option for immediate or expected notifications, cleaned up UI 
 *   1.1.4 - fixed poll ffrequency case statement
 *   1.1.3 - added poll frequency configuration
 *   1.1.2 - fixed repeat errors 
 *   1.1.1 - changed API feed for more detailed weather alerts
 *   1.1.0 - fixed PushOver testing to work correctly if pushover not being used, fixed UI elements for test, auto turn-off test mode after initiated, fixed check for TTS
 *   1.0.9 - added more logic on restriction options, fixed PushOver character limitation
 *   1.0.8 - fixed repeat in # minutes errors and execution
 *   1.0.7 - added ability to decide weather alert severity to check for
 *   1.0.6 - added testing option, repeat alert after # of minutes
 *   1.0.5 - fixed error with checking both mode and switch restrictions.
 *   1.0.4 - fixed mode restriction wording, fixed auto log off issue, added disable by switch option
 *   1.0.3 - added restrictions based on modes, pushover notification support and logEnable for only 15 min
 *   1.0.2 - added standard logEnable logic for 30 min disable, latitude and longitude from Hub Location, announcement intro customization, random bug fixes
 *   1.0.1 - misc bug fixes
 *   1.0.0 - initial code concept
**/

import groovy.json.*
import java.util.regex.*
import java.text.SimpleDateFormat
	
def version(){"v2.2.9"}

definition(
    name:"NOAA Weather Alerts",
    namespace: "aaronward",
    author: "Aaron Ward",
    description: "NOAA Weather Alerts Application ",
    category: "Weather",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
    )

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
} 

def mainPage() {
    dynamicPage(name: "mainPage") {
    	//installCheck()
		//if(state.appInstalled == 'COMPLETE'){
			section(getFormat("title", "${getImage("Blank")}" + " ${app.label}")) {
				paragraph "<div style='color:#1A77C9'>This application provides customized Weather Alerts.</div>"
			}
			section(getFormat("header-green", " General")) {
       			label title: "Custom Application Name (for multiple instances of NOAA):", required: false
			}
			section(getFormat("header-green", " Notification Devices")) {
				// PushOver Devices
			    input "pushovertts", "bool", title: "Use 'Pushover' device(s)?", required: false, defaultValue: false, submitOnChange: true 
			    if(pushovertts == true){ input "pushoverdevice", "capability.notification", title: "PushOver Device", required: true, multiple: true}
			     
				// Music Speakers (Sonos, etc)
				input(name: "musicmode", type: "bool", defaultValue: "false", title: "Use Music Speaker(s) for TTS?", description: "Music Speaker(s)?", submitOnChange: true)
				if (musicmode) input "musicspeaker", "capability.musicPlayer", title: "Choose speaker(s)", required: false, multiple: true, submitOnChange: true
				
				// Speech Speakers
				input(name: "speechmode", type: "bool", defaultValue: "false", title: "Use Google or Speech Speaker(s) for TTS?", description: "Speech Speaker(s)?", submitOnChange: true)
   	            if (speechmode) input "speechspeaker", "capability.speechSynthesis", title: "Choose speaker(s)", required: false, multiple: true, submitOnChange: true
						
				// Echo Speaks devices
				input (name: "echoSpeaks2", type: "bool", defaultValue: "false", title: "Use Echo Speaks device(s) for TTS?", description: "Echo Speaks device?", submitOnChange: true)
					 if(echoSpeaks2 == true) input "echospeaker", "capability.musicPlayer", title: "Choose Echo Speaks Device(s)", required: false, multiple: true, submitOnChange: true 
				
				// Master Volume settings
				input "speakervolume", "number", title: "Notification Volume Level:", description: "0-100%", required: false, defaultValue: "75", submitOnChange: true
				input "speakervolRestore", "number", title: "Restore Volume Level:", description: "0-100", required: false, submitOnChange: true
				
				// Switch to set when alert active
				input (name: "alertSwitch", type: "capability.switch", title: "Switch to turn ON with Alert? (optional)", required: false, defaultValue: false, submitOnChange: true)

			}
			section(getFormat("header-green", " Configuration")) {
				input name: "whatAlertSeverity", type: "enum", title: "Weather Severity to monitor: ", 
					options: [
						"minor": "Minor",
						"moderate": "Moderate", 
						"severe": "Severe", 
						"extreme": "Extreme"], required: true, multiple: true, defaultValue: "Severe"
				input name: "whatPoll", type: "enum", title: "Poll Frequency: ", options: ["1": "1 Minute", "5": "5 Minutes", "10": "10 Minutes", "15": "15 Minutes"], required: true, multiple: false, defaultValue: "5 Minutes"
				input "repeatYes", "bool", title: "Repeat Alert after certain amount of minutes?", require: false, defaultValue: false, submitOnChange: true
				if(repeatYes){ input name:"repeatMinutes", type: "text", title: "Number of minutes before repeating the alert?", require: false, defaultValue: "30" }
				input name: "useCustomCords", type: "bool", title: "Use Custom Coordinates?", require: false, defaultValue: false, submitOnChange: true
				if(useCustomCords) {
					paragraph "Below coordinates are acquired from your Hubitat Hub.  Enter your custom coordinates:"
					input name:"customlatitude", type:"text", title: "Latitude coordinate:", require: false, defaultValue: "${location.latitude}", submitOnChange: true
					input name:"customlongitude", type:"text", title: "Longitude coordinate:", require: false, defaultValue: "${location.longitude}", submitOnChange: true
				}
				input name:"useAlertIntro", type: "bool", title: "Use a pre-notification message to TTS device(s)?", require: false, defaultValue: false, submitOnChange: true
				if(useAlertIntro) input name:"AlertIntro", type: "text", title: "Alert pre-notification message:", require: false, defaultValue:"Attention, Attention."              
                input name: "alertCustomMsg", type: "text", title: "Custom Alert Message (use customization instructions):", require: false, defaultValue: "{alertseverity} Weather Alert for the following counties: {alertarea} {alertheadline} {alertinstruction} This is the end of this Weather Announcement.", submitOnChange: true
            }	
        	   section("Alert Message Customization Instructions:", hideable: true, hidden: true) {
        	    	paragraph "<b>Alert message variables:</b>"
			    	paragraph "{alertseverity} = alertseverity"
    				paragraph "{alertcertainty} = alert certainty of occuring"
    				paragraph "{alerturgency} = alert urgency"
    				paragraph "{alertevent} = alert event type"
	    			paragraph "{alertheadline} = alert headline"
		    		paragraph "{alertdescription} = alert description"
			    	paragraph "{alertinstruction} = alert instructions"
				    paragraph "{alertarea} = counties, cities or area"	
				    paragraph " "
				    paragraph "<b>Example:</b>{alertseverity} weather alert. Certainty is {alertcertainty}. Urgency is {alerturgency}. {alertheadline}. {alertinstruction}. This is the end of the weather announcement."
			    }

			section(getFormat("header-green", " Dashboard Tile")) {}
			section("Instructions for Dashboard Tile:", hideable: true, hidden: true) {
				paragraph "<b>Instructions for adding NOAA Weather Alerts to Hubitat Dashboards:</b><br>"
				paragraph " -Install NOAA Tile Device driver<br>- Create a new Virtual Device and use the NOAA Tile Driver  'NOAA Tile'<br>- Select the new Virtual Device Below<br><br>"
				paragraph "<b>Add the NOAA Tile device to your dashboard with the following options:</b><br>"
				paragraph "- Pick a Device: NOAA Tile <br>- Pick a template: attribute<br>- Options - Select Attribute: Alerts"
			}
			section() {
			input(name: "noaaTileDevice", type: "capability.actuator", title: "NOAA Tile Device to send alerts to:", submitOnChange: true, required: false, multiple: false)
			input name:"noaaTileReset", type: "text", title: "Reset NOAA Tile on dashboard after how many minutes?", require: false, defaultValue: "30", submitOnChange: true
			}
			section(getFormat("header-green", " Advanced Configuration")) {
			paragraph "Use with caution as below settings may cause undesired results.  Reference <a href='https://www.weather.gov/documentation/services-web-api?prevfmt=application%2Fcap%2Bxml/default/get_alerts#/default/get_alerts' target='_blank'>Weather.gov API</a> and use the API response test button below to determine your desired results."
				input "myWeatherAlert", "enum", title: "Watch for a specific Weather event(s)?", required: false, multiple: true, submitOnChange: true,
                            options: [
							"BZW":	"Blizzard Warning",
                            "CFA":	"Coastal Flood Watch",
                            "CFW":	"Coastal Flood Warning",
                            "DSW":	"Dust Storm Warning",
                            "EWW":	"Extreme Wind Warning",
                            "FFA":	"Flash Flood Watch",
                            "FFW":	"Flash Flood Warning",
                            "FLA":	"Flood Watch",
                            "FLW":	"Flood Warning",
                            "HWA":	"High Wind Watch",
                            "HWW":	"High Wind Warning",
                            "HUA":	"Hurricane Watch",
	                        "HUW":	"Hurricane Warning",
                            "SVA":	"Severe Thunderstorm Watch",
                            "SVR":	"Severe Thunderstorm Warning",
                            "SQW":	"Snow Squall Warning",
                            "SMW":	"Special Marine Warning",
                            "SSA":	"Storm Surge Watch",
                            "SSW":	"Storm Surge Warning",
                            "TOA":	"Tornado Watch",
                            "TOR":	"Tornado Warning",
                            "TRA":	"Tropical Storm Watch",
                            "HUA":	"Tropical Storm Warning",   
							"TSA":	"Tsunami Watch",
                            "TSW":	"Tsunami Warning",
                            "WSA":	"Winter Storm Watch",
                            "WSW":	"Winter Storm Warning"
                            ]
				input name: "whatAlertUrgency", type: "enum", title: "Watch for a specific Alert Urgency: ", multiple: true, submitOnChange: true, 
							options: [
								"immediate": "Immediate", 
								"expected": "Expected",
								"future": "Future"]
				
				input name: "whatAlertCertainty", type: "enum", title: "Watch for a specific Alert Certainty: ", required: false, multiple: true, submitOnChange: true,
							options: [
								"possible": "Possible",
								"likely": "Likely",
								"observed": "Observed"]
			}
			section(getFormat("header-green", " Restrictions")) {
				input "modesYes", "bool", title: "Enable restriction of notifications by current mode(s)?", required: true, defaultValue: false, submitOnChange: true	
				if(modesYes){	
				    input(name:"modes", type: "mode", title: "Restrict notifications when current mode is:", multiple: true, required: false, submitOnChange: true)
				}
				if(!modesYes){
			          input "restrictbySwitch", "capability.switch", title: "Use a switch to restrict notfications:", required: false, multiple: false, defaultValue: null, submitOnChange: true
				}
			}
			section(getFormat("header-green", " Logging and Testing")) {
				input "runTest", "bool", title: "Run a test Alert?", required: false, defaultValue: false, submitOnChange: true
				if(runTest) {
					app?.updateSetting("runTest",[value:"false",type:"bool"])
					if (logEnable) log.debug "Initiating a test alert."
					testalertmsg=buildTestAlert()
					alertNow(testalertmsg)
					if(repeatYes && state.alertRepeat) {
					if (logEnable) log.debug "Scheduling a repeat alert in ${repeatMinutes} minutes."
						runIn((60*repeatMinutes.toInteger()),repeattestAlert)
						state.alertrepeat = false
					}
				}
 				input "logEnable", "bool", title: "Enable Debug Logging?", required: false, defaultValue: true, submitOnChange: true
                if(logEnable) input "logMinutes", "text", title: "Log for the following number of minutes (0=logs always on):", required: false, defaultValue:15, submitOnChange: true                
				input "getAPI", "bool", title: "Test above configuration and display current weather.gov API response?", required: false, defaultValue: false, submitOnChange: true
				if(getAPI) {
					app?.updateSetting("getAPI",[value:"false",type:"bool"])
					getAlertMsg()
					def date = new Date()
					sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a")
					if(state.alertJSON) {paragraph "Current poll of weather API at: ${sdf.format(date)}<br/><br/>URI: <a href='${state.wxURI}' target=_blank>${state.wxURI}</a><br><br><table border=1px><tr><th>Field Name</th><th>Value</th></tr><tr><td>Severity</td><td>${state.alertseverity}</td></tr><tr><td>Area</td><td>${state.alertarea}</td></tr><tr><td>Sent</td><td>${state.alertsent}</td></tr><tr><td>Effective</td><td>${state.alerteffective}</td></tr><tr><td>Expires</td><td>${state.alertexpires}</td></tr><tr><td>Status</td><td>${state.alertstatus}</td></tr><tr><td>Message Type</td><td>${state.alertmessagetype}</td></tr><tr><td>Category</td><td>${state.alertcategory}</td></tr><tr><td>Certainty</td><td>${state.alertcertainty}</td></tr><tr><td>Urgency</td><td>${state.alerturgency}</td></tr><tr><td>Sender Name</td><td>${state.alertsendername}</td></tr><tr><td>Event Type</td><td>${state.alertevent}</td></tr><tr><td>Headline</td><td>${state.alertheadline}</td></tr><tr><td>Description</td><td>${state.alertdescription}</td></tr><tr><td>Instruction</td><td>${state.alertinstruction}</td></tr></table>"}
					else { paragraph "No JSON feed currently available for your coordinates.  Either there is no weather alerts in your area or you need to change options above to acquire desired results."}
				}
				paragraph getFormat("line")
				paragraph "<div style='color:#1A77C9;text-align:center'>Developed by: Aaron Ward<br/>${version()}<br><br><a href='https://paypal.me/aaronmward?locale.x=en_US' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br><br>Donations always appreciated!</div>"
			}       
		}
	}
//}

def installed() {
    if (logEnable) log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    if (logEnable) log.debug "Updated with settings: ${settings}"
    unsubscribe()
    if (logEnable && logMinutes.toInteger() != 0) {
        if(logMinutes.toInteger() !=0) log.warn "Debug messages set to automatically disable in ${logMinutes} minute(s)."
        runIn((logMinutes.toInteger() * 60),logsOff)
    }
    else { log.warn "Debug logs set to not automatically disable." }
    initialize()
	switch (whatPoll.toInteger()) {
		case 1: 
			runEvery1Minute(refresh)
			break
		case 5: 
			runEvery5Minutes(refresh)
			break
		case 10: 
			runEvery10Minutes(refresh)
			break
		case 15: 
			runEvery15Minutes(refresh)
			break
		default: 
			runEvery5Minutes(refresh)
			break
	}
}

def initialize() {
	runIn(5, refresh)
	state.alertRepeat = true
	}

def installCheck(){         
    state.appInstalled = app.getInstallationState() 
    if(state.appInstalled != 'COMPLETE'){
    	section{paragraph "Please hit 'Done' to install '${app.label}'"}
    }
    else{
		if (logEnable) log.debug "${app.label} has been updated."
    }
}

def getImage(type) {
    def loc = "<img src='https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/NOAA/Support/NOAA.png'>"
}

def getFormat(type, myText=""){
    if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#1A7BC7;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}


def logsOff(){
    log.warn "Debug logging disabled."
    app?.updateSetting("logEnable",[value:"false",type:"bool"])
}


def refresh() {
	// check restrictions based on Modes and Switches
    def result = (!modesYes && restrictbySwitch !=null && restrictbySwitch.currentState("switch").value == "on") ? true : false
    def result2 =    ( modesYes && modes            !=null && modes.contains(location.mode))                         ? true : false
	if (logEnable) log.debug "Restrictions on?  Modes: $result2, Switch: $result"

	if( ! (result || result2) ) {
			// Get the alert message
			getAlertMsg()	
			if (logEnable) log.debug "Polled weather API: Alert Sent Timestamp: '${state.alertsent}'   Past Alert Timestamp: '${state.pastalert}'"
			if(state.alertsent.equals(state.pastalert)) { 
				log.info "No new alerts.  Waiting ${whatPoll.toInteger()} minutes before next poll..."
			} else {
				if(state.alertsent!=null){
				// play TTS and send PushOver
				buildAlertMsg()
				log.info "Sending alert: ${state.alertmsg}"
				alertNow(state.alertmsg)
				// set the pastalert to the current alertsent timestamp
				state.pastalert = state.alertsent
				// determine if alert needs to be repeated after # of minutes
				if(repeatYes==true && state.alertRepeat==true) {
				if (logEnable) log.debug "Scheduling a repeat alert in ${repeatMinutes} minutes."
					runIn((60*repeatMinutes.toInteger()),repeatAlert)
					state.alertrepeat = false
				}
				}
			} 
	}
    else log.info "Restrictions are enabled!  Waiting ${whatPoll.toInteger()} minutes before next poll..."
}


def getAlertMsg() {
	if (logEnable) log.debug "Connecting to weather.gov service."
	// Determine if custom coordinates have been selected
	if(useCustomCords) {
		state.latitude = "${customlatitude}"
		state.longitude = "${customlongitude}"
	} else {
		state.latitude = "${location.latitude}"
		state.longitude = "${location.longitude}"
	}
	wxURI = "https://api.weather.gov/alerts?point=${state.latitude}%2C${state.longitude}&status=actual&message_type=alert"
	
	// Build out the API options
	if(whatAlertUrgency != null) {
		wxURI = wxURI + "&urgency=${whatAlertUrgency.join(",")}"
	}
	if(whatAlertSeverity != null) {
		wxURI = wxURI + "&severity=${whatAlertSeverity.join(",")}"
	} else {
		wxURI = wxURI + "&severity=severe"
	}
	if(whatAlertCertainty !=null) {
		wxURI = wxURI + "&certainty=${whatAlertCertainty.join(",")}"
	}
	if(myWeatherAlert != null) {
		wxURI = wxURI + "&code=${myWeatherAlert.join(",")}"
	}
	state.wxURI = wxURI

	//What default weather.gov API looks like: wxURI = "https://api.weather.gov/alerts?point=${state.latitude}%2C${state.longitude}&status=actual&message_type=alert&urgency=${AlertUrgency}&severity=${whatAlertSeverity}&code=${myCodes}"
		
		if (logEnable) log.debug "URI: ${wxURI}"
		def requestParams =
			[
				uri:  wxURI,
				requestContentType: "application/json",
				contentType: "application/json"
			]
	
	try {
		httpGet(requestParams)	{	  response ->
			if (response?.status == 200){
				if(response.data.features){
					if (logEnable) log.debug "Building alert variables."
				// build out variables from JSON feed
					state.alertJSON = true
					state.alertseverity = response.data.features[0].properties.severity
					alertarea = response.data.features[0].properties.areaDesc
						alertarea = alertarea.replaceAll(";",",")
						alertarea = alertarea.replaceAll("\n"," ")
						StringBuffer buffer = new StringBuffer(alertarea)
						alertarea = buffer.reverse().toString().replaceFirst(",","dna ")
					state.alertarea = new StringBuffer(alertarea).reverse().toString()
					state.alertsent = response.data.features[0].properties.sent
					state.alerteffective = response.data.features[0].properties.effective
					state.alertexpires = response.data.features[0].properties.expires
					state.alertstatus = response.data.features[0].properties.status
					state.alertmessagetype = response.data.features[0].properties.messageType
					state.alertcategory = response.data.features[0].properties.category
					state.alertcertainty = response.data.features[0].properties.certainty
					state.alerturgency = response.data.features[0].properties.urgency
					state.alertsendername = response.data.features[0].properties.senderName
					state.alertheadline = response.data.features[0].properties.headline
					state.alertdescription = response.data.features[0].properties.description
					if(response.data.features[0].properties.instruction) { state.alertinstruction = response.data.features[0].properties.instruction }
					else {state.alertinstruction = response.data.features[0].properties.description }
					state.alertevent = response.data.features[0].properties.event
				}  else { state.alertJSON = false }
			}
			else log.warn "${response?.status}"
		}
	}
	catch (any) { log.warn "Weather.gov API did not respond to JSON request."}
}
			
def buildAlertMsg() {
		if (logEnable) log.debug "Building alert message."
				// build the alertmsg
					alertmsg = alertCustomMsg
					try {alertmsg = alertmsg.replace("{alertarea}","${state.alertarea}") }
						 catch (any) {}
					try {alertmsg = alertmsg.replace("{alertseverity}","${state.alertseverity}") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertcertainty}","${state.alertcertainty}") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alerturgency}","${state.alerturgency}") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertheadline}","${state.alertheadline}") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertdescription}","${state.alertdescription}") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertinstruction}","${state.alertinstruction}") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertevent}","${state.alertevent}") }
						catch (any) {}					
					try {alertmsg = alertmsg.replace(" CST","") }
						catch (any) {}
					try {alertmsg = alertmsg.replace(" CDT","") }
						catch (any) {}
					try {alertmsg = alertmsg.replace(" MDT","") }
						catch (any) {}
					try {alertmsg = alertmsg.replace(" MST","") }
						catch (any) {}
					try {alertmsg = alertmsg.replace(" PST","") }
						catch (any) {}
					try {alertmsg = alertmsg.replace(" PDT","") }
						catch (any) {}
					try {alertmsg = alertmsg.replace(" EST","") }
						catch (any) {}
					try {alertmsg = alertmsg.replace(" EDT","") }
						catch (any) {}
					try {alertmsg = alertmsg.replace(" NWS "," the National Weather Service ") }
						catch (any) {}
					try {alertmsg = alertmsg.replaceAll("\n"," ") }
						catch (any) {}
					state.alertmsg = alertmsg
					if (logEnable) log.debug "alertMsg built: ${state.alertmsg}"
				} 



def buildTestAlert() {
					alertmsg = alertCustomMsg
					try {alertmsg = alertmsg.replace("{alertarea}","Springfield County.") }
						 catch (any) {}
					try {alertmsg = alertmsg.replace("{alertseverity}","Severe") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertcertainty}","Likely") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alerturgency}","Immediate") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertheadline}","The founder, Jebediah Springfield has spotted a cloud above the nuclear power plant towers.") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertdescription}","The founder, Jebediah Springfield has spotted a cloud above the nuclear power plant towers.  Expect heavy polution, possible fish with three eyes, and a Simpson asleep at the console.  Also a notorius yellow haired boy is terrorizing animals with spit wads.  Be on the look out for suspicious activity.") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertinstruction}","Expect heavy polution, possible fish with three eyes, and a Simpson asleep at the console.") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertevent}","Nuclear Power Plant Warning") }
						catch (any) {}
					return alertmsg
}

def talkNow(alertmsg) {								
	if(useAlertIntro) alertmsg = "${AlertIntro}" + alertmsg
		speechDuration = Math.max(Math.round(alertmsg.length()/12),2)+3		
		atomicState.speechDuration2 = speechDuration * 1000
	
  		if (musicmode){ 
				if (logEnable) log.debug "Sending alert to Music Speaker(s)."
				try {
					musicspeaker.playTextAndRestore(alertmsg, speakervolume)
				}
				catch (any) {log.warn "Music Player device(s) has not been selected."}
  		}   
	
		if(echoSpeaks2) {
			if (logEnable) log.debug "Sending alert to Echo Speaks device(s)."
			try {
				echospeaker.setVolumeSpeakAndRestore(speakervolume, alertmsg)
				}
			catch (any) {log.warn "Echo Speaks device(s) has not been selected."}
		}
	
		if (speechmode){ 
			if (logEnable) log.debug "Sending alert to Google and Speech Speaker(s)"
			try {
				if (logEnable) log.debug "Setting Speech Speaker to volume level: ${speakervolume}"
				speechspeaker.setVolume(speakervolume)
				pauseExecution(1000)
				speechspeaker.speak(alertmsg)
				if (speakervolRestore) {
					pauseExecution(atomicState.speechDuration2)
					if (logEnable) log.debug "Restoring Speech Speaker to volume level: ${speakervolRestore}"
					speechspeaker.setVolume(speakervolRestore)	
				}
			}
			catch (any) {log.warn "Speech device(s) has not been selected."}
		}
	
}

def pushNow(alertmsg) {
	if (pushovertts) {
	if (logEnable) log.debug "Sending Pushover message."
	def m = alertmsg =~ /(.|[\r\n]){1,1023}\W/
	def n = alertmsg =~ /(.|[\r\n]){1,1023}\W/
	def index = 0
	def index2 = 1
		while (m.find()) {
		   index = index +1
		}

		while(n.find()) {
			fullMsg1 = n.group()
			pushoverdevice.deviceNotification("(${index2}/${index}) ${fullMsg1}")
			index2 = index2 +1
			pauseExecution(1000)
        } 
	}
}

def tileReset() {
			if (logEnable) log.debug "NOAA Tile has been reset."
			tileNow("No weather alerts to report.","false")
}

def tileNow(alertmsg, resetAlert) {
	if(noaaTileDevice) {
		if(logEnable) log.debug "Sending to NOAA Tile - msg: ${alertmsg}"
		noaaTileDevice.sendNoaaTile(alertmsg)
		if(resetAlert == "true") {
			if (logEnable) log.debug "Resetting NOAA Tile in ${noaaTileReset} minutes."
			runIn((60*noaaTileReset.toInteger()),tileReset)
		}
	}
}

def repeatAlert() {
	if (logEnable) log.debug "Repeating alert."
	alertNow(state.alertmsg)
	state.alertRepeat = true
}

def repeattestAlert() {
	if (logEnable) log.debug "Repeating alert."
	alertNow(state.testalertmsg)
	state.alertRepeat = true
}

def alertNow(alertmsg){
		pushNow(alertmsg)
		if(alertSwitch) { alertSwitch.on() }
		tileNow(alertmsg, "true") 
		talkNow(alertmsg)
}
