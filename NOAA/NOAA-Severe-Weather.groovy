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
 *
 *  Changes:
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
 *   1.0.0 - Initial code concept
**/

import groovy.json.*
import java.util.regex.*

	
def version(){"v2.1.9"}

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
    	installCheck()
		if(state.appInstalled == 'COMPLETE'){
			section(getFormat("title", "${getImage("Blank")}" + " ${app.label}")) {
				paragraph "<div style='color:#1A77C9'>This application supplies Severe Weather alert TTS notifications.</div>"
			}
			section(getFormat("header-green", " General")) {
       			label title: "Enter a name for application:", required: false
			}
			section(getFormat("header-green", " Notification Devices")) {
			    input "pushovertts", "bool", title: "Send a 'Pushover' message for NOAA Weather Alerts?", required: true, defaultValue: false, submitOnChange: true 
			    if(pushovertts == true){ input "pushoverdevice", "capability.notification", title: "PushOver Device", required: true, multiple: true}
				paragraph "Configure your TTS devices:"
			      input "speechMode", "enum", required: false, title: "Select Speaker Type:", submitOnChange: true,  options: ["Music Player", "Speech Synth"] 
				if (speechMode == "Music Player"){
           	   		      input "speaker1", "capability.musicPlayer", title: "Choose speaker(s)", required: false, multiple: true, submitOnChange: true
						  input "volume1", "number", title: "Speaker volume", description: "0-100%", required: false, defaultValue: "75"
          	            }   
        	            if (speechMode == "Speech Synth"){
         	            	input "speaker1", "capability.speechSynthesis", title: "Choose speaker(s)", required: false, multiple: true
          	            }
				input (name: "echoSpeaks2", type: "bool", defaultValue: "false", title: "Use Echo Speaks device(s) with TTS?", description: "Echo speaks device?", submitOnChange: true)
					 if(echoSpeaks2 == true){ 
						 input "echospeaker", "capability.musicPlayer", title: "Choose Echo Speaks device(s)", required: false, multiple: true
					 	 input "echospeaksvolume", "number", title: "Echo Speaks volume", description: "0-100%", required: false, defaultValue: "75"}
				input (name: "alertSwitch", type: "capability.switch", title: "Switch to turn ON with Alert? (optional)", required: false, defaultValue: false)

			}
			section(getFormat("header-green", " Configuration")) {
				input name: "whatAlertSeverity", type: "enum", title: "Choose Weather Severity to monitor: ", 
					options: [
						"minor": "Minor",
						"moderate": "Moderate", 
						"severe": "Severe", 
						"extreme": "Extreme"], required: true, multiple: true, defaultValue: "Severe"
				input name: "whatPoll", type: "enum", title: "Choose poll frequency: ", options: ["1": "1 Minute", "5": "5 Minutes", "10": "10 Minutes", "15": "15 Minutes"], required: true, multiple: false, defaultValue: "5 Minutes"
				input "repeatYes", "bool", title: "Repeat alerts after certain amount of minutes?", require: false, defaultValue: false, submitOnChange: true
				if(repeatYes){ input name:"repeatMinutes", type: "text", title: "Number of minutes before repeating the alert?", require: false, defaultValue: "30" }
				input name: "useCustomCords", type: "bool", title: "Use Custom Coordinates?", require: false, defaultValue: false, submitOnChange: true
				input (name: "alertCustomMsg", type: "text", title: "Alert Message:", require: false, defaultValue: "Attention, Attention. {alertseverity} Weather Alert for the following counties: {alertarea} {alertheadline} {alertinstruction} This is the end of this Weather Announcement.")
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
				paragraph "{alertarea} = counties or area being affected"	
				paragraph " "
				paragraph "<b>Example:</b> Attention, Attention. {alertseverity} weather alert. Certainty is {alertcertainty}. Urgency is {alerturgency}. {alertheadline}. {alertinstruction}. This is the end of the weather announcement."
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
			input name:"noaaTileReset", type: "text", title: "Number of minutes before resetting the NOAA Tile Dashboard?", require: false, defaultValue: "30"
			}
			section(getFormat("header-green", " Advanced Configuration")) {
			paragraph "Use with caution as below settings may cause undesired results.  Reference <a href='https://www.weather.gov/documentation/services-web-api?prevfmt=application%2Fcap%2Bxml/default/get_alerts#/default/get_alerts' target='_blank'>Weather.gov API</a> and test your configuration first."
				input "myWeatherAlert", "enum", title: "Watch only for a specific Weather event?", required: false, multiple: true,
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
				input name: "whatAlertUrgency", type: "enum", title: "Choose Alerts Urgency: ", multiple: true, 
							options: [
								"immediate": "Immediate", 
								"expected": "Expected",
								"future": "Future"]
				
				input name: "whatAlertCertainty", type: "enum", title: "Choose Alerts Certainty: ", required: false, multiple: true,
							options: [
								"possible": "Possible",
								"likely": "Likely",
								"observed": "Observed"]

				if(useCustomCords) {
					paragraph "Below coordinates are acquired from your Hubitat:"
					input name:"customlatitude", type:"text", title: "Latitude coordinate:", require: false, defaultValue: "${location.latitude}"
					input name:"customlongitude", type:"text", title: "Longitude coordinate:", require: false, defaultValue: "${location.longitude}"
				}
			}
			section(getFormat("header-green", " Restrictions")) {
				input "modesYes", "bool", title: "Enable restriction by current mode(s)?", required: true, defaultValue: false, submitOnChange: true	
				if(modesYes){	
				    input(name:"modes", type: "mode", title: "Restrict actions when current mode is:", multiple: true, required: false)
				}
				if(!modesYes){
			          input "restrictbySwitch", "capability.switch", title: "Or use a switch to restrict:", required: false, multiple: false, defaultValue: null
				}
			}
			section(getFormat("header-green", " Logging and Testing")) {
				input "runTest", "bool", title: "Run a test Alert?", required: false, defaultValue: false, submitOnChange: true
				if(runTest) {
					app?.updateSetting("runTest",[value:"false",type:"bool"])
					if (logEnable) log.debug "Initiating a test alert."
					testalert=buildTestAlert()
					alertNow(testalert)
				}
 				input "logEnable", "bool", title: "Enable Debug Logging?", required: false, defaultValue: true
				paragraph getFormat("line")
				paragraph "<div style='color:#1A77C9;text-align:center'>Developed by: Aaron Ward<br/>${version()}</div>"
			}       
		}
	}
}

def installed() {
    if (logEnable) log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    if (logEnable) log.debug "Updated with settings: ${settings}"
    unsubscribe()
    if (logEnable) runIn(900,logsOff)
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
	state.alertrepeat = true
}

def installCheck(){         
    state.appInstalled = app.getInstallationState() 
    if(state.appInstalled != 'COMPLETE'){
    	section{paragraph "Please hit 'Done' to install '${app.label}'"}
    }
    else{
		if (logEnable) log.debug "${app.label} is Installed Correctly"
    }
}

def getImage(type) {
    def loc = "<img src='https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/NOAA/Support/NOAA.png'>"
}

def getFormat(type, myText=""){
    if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
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
			buildAlertMsg()	
			if(state.alertarea) {
				if (logEnable) log.debug "AlertSent: '${state.alertsent}  Pastalert: '${state.pastalert}'"
				if(state.alertsent != state.pastalert){
					// play TTS and send PushOver
					alertNow(state.alertmsg)
					// determine if alert needs to be repeated after # of minutes
					if(repeatYes && state.alertrepeat) {
						runIn((60*repeatMinutes.toInteger()),repeatAlert)
						state.alertrepeat = false
					}
					// set the pastalert to the current alertsent timestamp
					state.pastalert = state.alertsent
					log.info "Speaking: ${state.alertmsg}"
					if (logEnable) log.debug "AlertSent: '${state.alertsent}  Pastalert: '${state.pastalert}'"
				} 
				else  log.info "No new alerts."
			} 
		else log.info "No new alerts."	
		log.info "Waiting ${whatPoll.toInteger()} minutes before next poll..."
	}
    else log.info "Restrictions are enabled!  Waiting ${whatPoll.toInteger()} minutes before next poll..."
}

def buildAlertMsg() {
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

	//What default weather.gov API looks like: wxURI = "https://api.weather.gov/alerts?point=${state.latitude}%2C${state.longitude}&status=actual&message_type=alert&urgency=${AlertUrgency}&severity=${whatAlertSeverity}&code=${myCodes}"
		
		if (logEnable) log.debug "URI: ${wxURI}"
		def requestParams =
			[
				uri:  wxURI,
				requestContentType: "application/json",
				contentType: "application/json"
			]
		httpGet(requestParams)	{	  response ->
			if (response?.status == 200){
				if(response.data.features){
				
				// build out variables from JSON feed
					alertseverity = response.data.features[0].properties.severity
					alertarea = response.data.features[0].properties.areaDesc
						alertarea = alertarea.replaceAll(";",",")
						alertarea = alertarea.replaceAll("\n"," ")
						StringBuffer buffer = new StringBuffer(alertarea)
						alertarea = buffer.reverse().toString().replaceFirst(",","dna ")
					state.alertarea = new StringBuffer(alertarea).reverse().toString()
					state.alertsent = response.data.features[0].properties.sent
					alerteffective = response.data.features[0].properties.effective
					alertexpires = response.data.features[0].properties.expires
					alertstatus = response.data.features[0].properties.status
					alertmessagetype = response.data.features[0].properties.messageType
					alertcategory = response.data.features[0].properties.category
					alertseverity = response.data.features[0].properties.severity
					alertcertainty = response.data.features[0].properties.certainty
					state.alerturgency = response.data.features[0].properties.urgency
					alertsendername = response.data.features[0].properties.senderName
					alertheadline = response.data.features[0].properties.headline
					alertdescription = response.data.features[0].properties.description
					if(response.data.features[0].properties.instruction) { alertinstruction = response.data.features[0].properties.instruction }
				else {alertinstruction = response.data.features[0].properties.description }
					alertevent = response.data.features[0].properties.event
			
				// build the alertmsg
					alertmsg = alertCustomMsg
					try {alertmsg = alertmsg.replace("{alertarea}","${state.alertarea}") }
						 catch (any) {}
					try {alertmsg = alertmsg.replace("{alertseverity}","${alertseverity}") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertcertainty}","${alertcertainty}") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alerturgency}","${state.alerturgency}") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertheadline}","${alertheadline}") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertdescription}","${alertdescription}") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertinstruction}","${alertinstruction}") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertevent}","${alertevent}") }
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
			}
			else log.warn "${response?.status}"
		}
}

def buildTestAlert() {
					alertmsg = alertCustomMsg
					try {alertmsg = alertmsg.replace("{alertarea}","Springfield County") }
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
		
  		if (speechMode == "Music Player"){ 
				if (logEnable) log.debug "Sending alert to Music Player devices."
				try {speaker1.playTextAndRestore(alertmsg)}
				catch (any) {log.warn "Music Player device(s) has not been selected."}
  		}   
	
		if(echoSpeaks2) {
			if (logEnable) log.debug "Sending alert to Echo Speaks devices."
			try {
				echospeaker.setVolumeSpeakAndRestore(echospeaksvolume, alertmsg)
				}
			catch (any) {log.warn "Echo Speaks device(s) has not been selected."}
		}
	
		if (speechMode == "Speech Synth"){ 
			if (logEnable) log.debug "Sending alert to Speech devices"
			try {speaker1.speak(alertmsg)}
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
		state.msg = "${alertmsg}"
		if(logEnable) log.debug "Sending to NOAA Tile - msg: ${state.msg}"
		noaaTileDevice.sendNoaaTile(state.msg)
		if(resetAlert == "true") {
			if (logEnable) log.debug "Resetting NOAA Tile in ${noaaTileReset} minutes."
			runIn((60*noaaTileReset.toInteger()),tileReset)
		}
	}
}

def repeatAlert() {
	if (logEnable) log.debug "Repeating alert."
	talkNow(state.alertmsg, true)
	pushNow(state.alertmsg)
	state.alertrepeat = true
}

def alertNow(alertmsg){
		//talkNow(alertmsg)
		//pushNow(alertmsg)
		if(alertSwitch) { alertSwitch.on() }
		tileNow(alertmsg, "true") 

}
