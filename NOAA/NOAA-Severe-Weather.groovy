/*  **************** NOAA Weather Alerts ****************
 *
 *  Hubitat Import URL: https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/NOAA/NOAA-Severe-Weather.groovy
 *
 *  Copyright 2019 Aaron Ward
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
 * Last Update: 6/11/2020 : 6:30PM
 */

String version() { return "3.0.017" }

definition(
    name:"NOAA Weather Alerts",
    namespace: "aaronward",
    author: "Aaron Ward",
    description: "NOAA Weather Alerts Application ",
    category: "Weather",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	documentationLink: "https://github.com/PrayerfulDrop/Hubitat/blob/master/NOAA/README.md",
    singleInstance: true,
    oauth: false,
    pausable: true)

preferences { 
    page name: "mainPage", title: "", install: true, uninstall: false
    page name: "NotificationPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
    page name: "ConfigPage", title: "", install: false, uninstall: false, nextPage: "mainPage"
    page name: "AdvConfigPage", title: "", install: false, uninstall: false, nextPage: "mainPage"
    page name: "RestrictionsPage", title: "", install: false, uninstall: false, nextPage: "mainPage"
    page name: "SettingsPage", title: "", install: false, uninstall: true, nextPage: "mainPage"
    
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        installCheck()
        if(state.appInstalled == 'COMPLETE') {
            section(UIsupport("logo","")) {
                if(pushovertts || musicmode || speechmode || echoSpeaks2) href(name: "NotificationPage", title: "${UIsupport("configured","")} Setup Notification Device(s)", required: false, page: "NotificationPage", description: "Select TTS and PushOver Devices")
                else href(name: "NotificationPage", title: "${UIsupport("attention","")} Setup Notification Device(s)", required: false, page: "NotificationPage", description: "Select TTS and PushOver Devices")
			    
                if(whatAlertSeverity || whatPoll || alertCustomMsg) href(name: "ConfigPage", title: "${UIsupport("configured","")} Weather Alert Settings", required: false, page: "ConfigPage", description: "Change default settings for weather alerts to monitor")
                else  href(name: "ConfigPage", title: "${UIsupport("attention","")} Weather Alert Settings", required: false, page: "ConfigPage", description: "Change default settings for weather alerts to monitor")
			    
                if(myWeatherAlert || whatAlertUrgency || whatAlertCertainty) href(name: "AdvConfigPage", title: "${UIsupport("configured","")} Advanced Alert Settings", required: false, page: "AdvConfigPage", description: "Add additional detailed weather settings to monitor")
                else href(name: "AdvConfigPage", title: "Advanced Alert Settings", required: false, page: "AdvConfigPage", description: "Add additional detailed weather settings to monitor")
			    
                if(modeYes || switchYes || modeSeverityYes || pushoverttsalways) href(name: "RestrictionsPage", title: "${UIsupport("configured","")} Restrictions", required: false, page: "RestrictionsPage", description: "Setup restriction options")
                else href(name: "RestrictionsPage", title: "Restrictions", required: false, page: "RestrictionsPage", description: "Setup restriction options")
                href(name: "SettingsPage", title: "Settings", required: false, page: "SettingsPage", description: "Modify NOAA Weather Alerts Application Settings")
                paragraph UIsupport("line","")
                paragraph UIsupport("footer","")
            }                
        }        
    }
}

def NotificationPage() {
    if(atomicState.eventTypes==null || atomicState.eventTypes=="") buildEventsList() 
    dynamicPage(name: "NotificationPage") {
        section(UIsupport("logo","")) {
            paragraph UIsupport("header", " Setup Notification Device(s)")
            paragraph "Select a communication(s) method, notification and restore volume levels, use a switch with advanced settings."
				// PushOver Devices
			    input "pushovertts", "bool", title: "Use 'Pushover' device(s)?", required: false, defaultValue: false, submitOnChange: true 
			    if(pushovertts == true){ input "pushoverdevice", "capability.notification", title: "PushOver Device", required: true, multiple: true}
			     
				// Music Speakers (Sonos, etc)
				input(name: "musicmode", type: "bool", defaultValue: "false", title: "Use Music Speaker(s) for TTS?", description: "Music Speaker(s)?", submitOnChange: true)
				if (musicmode) input "musicspeaker", "capability.musicPlayer", title: "Choose speaker(s)", required: false, multiple: true, submitOnChange: true
				
				// Speech Speakers
				input(name: "speechmode", type: "bool", defaultValue: "false", title: "Use Speech Speaker(s) for TTS? (Google, Alexa TTS, etc)", description: "Speech Speaker(s)?", submitOnChange: true)
   	            if (speechmode) input "speechspeaker", "capability.speechSynthesis", title: "Choose speaker(s)", required: false, multiple: true, submitOnChange: true
						
				// Echo Speaks devices
				input (name: "echoSpeaks2", type: "bool", defaultValue: "false", title: "Use Echo Speaks device(s) for TTS?", description: "Echo Speaks device?", submitOnChange: true)
					 if(echoSpeaks2 == true) input "echospeaker", "capability.musicPlayer", title: "Choose Echo Speaks Device(s)", required: false, multiple: true, submitOnChange: true 
				
				// Master Volume settings
				input "speakervolume", "number", title: "Notification Volume Level:", description: "0-100%", required: false, defaultValue: "75", submitOnChange: true
				input "speakervolRestore", "number", title: "Restore Volume Level:", description: "0-100", required: false, defaultValue: "60", submitOnChange: true
				
				// Switch to set when alert active
                input (name: "UsealertSwitch", type: "bool", title: "Use a switch to turn ON with Alert?", required: false, defaultValue: false, submitOnChange: true)
                if(UsealertSwitch) {
				    input (name: "alertSwitch", type: "capability.switch", title: "Select a switch to turn ON with Alert?", multiple: false, required: false, defaultValue: false, submitOnChange: true) 
                    input (name:"alertSwitchOff", type: "bool", title: "Turn off switch when all Alerts expire?", required: false, defaultValue: false, submitOnChange: true)
                    input (name:"alertSwitchWeatherType", type: "bool", title: "Turn off switch if certain weather alert types expire?", required: false, defaultValue: false, submitOnChange: true)
                    if(alertSwitchWeatherType) input "alertSwitchWeatherTypeWatch", "enum", title: "Watch for a specific Weather event(s)?", required: false, multiple: true, submitOnChange: true, options: atomicState.eventTypes
                }
        }
    }
}

def ConfigPage() {
    dynamicPage(name: "ConfigPage") {
        section(UIsupport("logo","")) {
            paragraph UIsupport("header", " Alert Settings")
            paragraph "Configure NOAA to look for specific alert severities, how often to poll for weather information, repeat you alert, use custom coordinates and customize the alert message sent to notification device(s)."
			input name: "whatAlertSeverity", type: "enum", title: "Weather Severity to monitor: ", 
				options: [
						"minor": "Minor",
						"moderate": "Moderate", 
						"severe": "Severe", 
						"extreme": "Extreme"], required: true, multiple: true, defaultValue: "Severe"
			input name: "whatPoll", type: "enum", title: "Poll Frequency: ", options: ["1": "1 Minute", "5": "5 Minutes", "10": "10 Minutes", "15": "15 Minutes"], required: true, multiple: false, defaultValue: "1 Minute"
			input "repeatYes", "bool", title: "Repeat Alert?", require: false, defaultValue: false, submitOnChange: true
			if(repeatYes) {
                input name:"repeatTimes", type: "text", title: "Number of times to repeat the alert?", require: false, defaultValue: "1", submitOnChange:true
                input name:"repeatMinutes", type: "text", title: "Number of minutes between each repeating alert?", require: false, defaultValue: "15", submitOnChange:true 
            }
			input name: "useCustomCords", type: "bool", title: "Use Custom Coordinates?", require: false, defaultValue: false, submitOnChange: true
			if(useCustomCords) {
			    paragraph "Below coordinates are acquired from your Hubitat Hub.  Enter your custom coordinates:"
				input name:"customlatitude", type:"text", title: "Latitude coordinate:", require: false, defaultValue: "${location.latitude}", submitOnChange: true
				input name:"customlongitude", type:"text", title: "Longitude coordinate:", require: false, defaultValue: "${location.longitude}", submitOnChange: true
			}
			input name:"useAlertIntro", type: "bool", title: "Use a pre-notification message for TTS device(s)?", require: false, defaultValue: false, submitOnChange: true
			if(useAlertIntro) input name:"AlertIntro", type: "text", title: "Alert pre-notification message:", require: false, defaultValue:"Attention, Attention"              
            input name: "alertCustomMsg", type: "text", title: "Custom Alert Message (use customization instructions):", require: false, defaultValue: "{alertseverity} Weather Alert for the following counties: {alertarea} {alertdescription} This is the end of this Weather Announcement.", submitOnChange: true
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
    }
}

def AdvConfigPage() {
    dynamicPage(name: "AdvConfigPage") {
        section(UIsupport("logo","")) {
            paragraph UIsupport("header", " Advanced Alert Settings")
            paragraph "Use with caution as below settings may cause undesired results.  Only select what you would like to refine in your alerts.  Reference <a href='https://www.weather.gov/documentation/services-web-api?prevfmt=application%2Fcap%2Bxml/default/get_alerts#/default/get_alerts' target='_blank'>Weather.gov API</a> and use the API response test button below to determine your desired results."
			input "myWeatherAlert", "enum", title: "Watch for a specific Weather event(s)?", required: false, multiple: true, submitOnChange: true, options: atomicState.eventTypes 
			input name: "whatAlertUrgency", type: "enum", title: "Watch for a specific Alert Urgency: ", multiple: true, submitOnChange: true, 
						options: [
								"immediate": "Immediate", 
								"expected": "Expected",
								"future": "Future"
                        ]
				
			input name: "whatAlertCertainty", type: "enum", title: "Watch for a specific Alert Certainty: ", required: false, multiple: true, submitOnChange: true,
						options: [
								"possible": "Possible",
								"likely": "Likely",
								"observed": "Observed"
                        ]
			}
    }
}

def RestrictionsPage() {
    dynamicPage(name: "RestrictionsPage") {
        section(UIsupport("logo","")) {
            paragraph UIsupport("header", " Restrictions")
            paragraph "Restrict notifications based on modes or a switch.  Override restrictions if the alert is a certain severity or weather type.  For notifications that are restricted, if a PushOver device is enabled alerts can still be sent but not over TTS."
 			input "modesYes", "bool", title: "Enable restriction of notifications by current mode(s)?", required: true, defaultValue: false, submitOnChange: true	
            if(modesYes) input(name:"modes", type: "mode", title: "Restrict notifications when current mode is:", multiple: true, required: false, submitOnChange: true)
            input "switchYes", "bool", title: "Restrict notifications using a switch?", required: true, defaultValue: false, submitOnChange: true
            if(switchYes) input "restrictbySwitch", "capability.switch", title: "Use a switch to restrict notfications?", required: false, multiple: false, defaultValue: null, submitOnChange: true
            paragraph "<hr><br>Below settings will ignore restrictions above based on either weather severity type or weather type."
            input "modeSeverityYes", "bool", title: "Ignore restrictions for certain severity types?", required: false, defaultValue: false, submitOnChange: true	          
            if(modeSeverityYes) input name: "modeSeverity", type: "enum", title: "Severity option(s) that will ignore restrictions: ", 
		        options: [
						"Minor": "Minor",
						"Moderate": "Moderate", 
						"Severe": "Severe", 
						"Extreme": "Extreme"], required: true, multiple: true, defaultValue: "Severe"             
     
            input "modeWeatherType", "bool", title: "Ignore restrictions for certain weather types?", required: false, defaultValue: false, submitOnChange: true
            
            if(modeWeatherType) { input name: "WeatherType", type: "enum", title: "Select weather type to ignore restrictions: ", required: true, multiple:true, submitOnChange: true, options: atomicState.eventTypes
                                }
            paragraph "<hr>"
            if(pushovertts) input "pushoverttsalways", "bool", title: "Enable Pushover notifications even when restricted?", required: false, defaultValue: false, submitOnChange: true
                }
    }
}

def SettingsPage() {
    dynamicPage(name: "SettingsPage") {
        section(UIsupport("logo","")) {
            paragraph UIsupport("header", " Settings")
            paragraph "Enable logging, run a test alert, if errors reset the applications state settings and test your weather alert configurations."
 				input "logEnable", "bool", title: "Enable Debug Logging?", required: false, defaultValue: false, submitOnChange: true
                if(logEnable) input "logMinutes", "text", title: "Log for the following number of minutes (0=logs always on):", required: false, defaultValue:15, submitOnChange: true 
				input "runTest", "bool", title: "Run a test Alert?", required: false, defaultValue: false, submitOnChange: true
                if(runTest) {
					app?.updateSetting("runTest",[value:"false",type:"bool"])
                    atomicState.testmsg = true
                    runtestAlert()
				}           
                input "init", "bool", title: "Reset current application state?", required: false, defaultValue: false, submitOnChange: true
                if(init) {
                    app?.updateSetting("init",[value:"false",type:"bool"])
                    unschedule()
                    atomicState.alertAnnounced = false
                    atomicState.ListofAlerts = ""
                    state.repeat = false
                    log.warn "NOAA Weather Alerts application state has been reset."
                    initialize()
                }
                input "debug", "bool", title: "Debug alert configuration - if expired alerts are available, use those alerts? (only enable this with the test config option below)", required: false, defaultValue: false, submitOnChange: true
				input "getAPI", "bool", title: "Test alert configuration and display weather.gov API response?", required: false, defaultValue: false, submitOnChange: true
				if(getAPI) {
                    getAlertMsg()
					app?.updateSetting("getAPI",[value:"false",type:"bool"])
                    app?.updateSetting("debug",[value:"false",type:"bool"])
					
					if(atomicState.ListofAlerts) {                    
                        def restrictionSwitch = (switchYes && restrictbySwitch !=null && restrictbySwitch.currentState("switch").value == "on") ? true : false
                        def restrictionMode = (modesYes && modes !=null && modes.contains(location.mode)) ? true : false
                        def restrictionSeverity = (modeSeverityYes && modeSeverity !=null) ? true : false
                        def restrictionWeatherType = (modeWeatherType && WeatherType !=null) ? true : false
                        def alertSwitchReset = (alertSwitchWeatherType && alertSwitchWeatherTypeWatch != null && alertSwitchWeatherTypeWatch.contains(atomicState.ListofAlerts[0].alertevent)) ? true : false
                        def testresult = (!(result || result2) || result3 || result4) ? true : false
					    def date = new Date()
					    sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a")
                        def testConfig = ""
                        temp = "Current poll of weather API at: ${sdf.format(date)}<br/><br/>URI: <a href='${state.wxURI}' target=_blank>${state.wxURI}</a><br><br>AlertMSG Built based on configuration:<br><br>${alertCustomMsg}<br><br>"
                        temp += "<table border=0><tr colspan=2><td>Current Restriction Settings:</td></tr>"
                        temp += "<tr><td>Switch:</td><td>${restrictionSwitch}</td></tr>"
                        temp += "<tr><td>Mode:</td><td>${restrictionMode}</td></tr>"
                        temp += "<tr><td>Severity:</td><td>${restrictionSeverity}</td></tr>"
                        temp += "<tr><td>Weather Type:</td><td>${restrictionWeatherType}</td></tr></table></br>"
                        paragraph temp
                        for(y=0;y<atomicState.ListofAlerts.size();y++) {
                            testalertmsg = ""
                            restrictionSeverity = (modeSeverityYes && modeSeverity !=null && modeSeverity.contains(atomicState.ListofAlerts[y].alertseverity)) ? true : false
                            restrictionWeatherType = (modeWeatherType && WeatherType !=null && WeatherType.contains(atomicState.ListofAlerts[y].alertevent)) ? true : false
                            if((restrictionSwitch == false || restrictionMode == false) && (restrictionSeverity == true || restrictionWeatherType == true)) {
                                if(pushovertts) testalertmsg = "alert would be announced on TTS and PushOver device(s)."
                                else testalertmsg = "alert would be announced only on TTS device(s)."
                            } else {
                                if(pushovertts && pushoverttsalways) testalertmsg = "alert would be announced only on PushOver device(s).  Alert does not meet other restriction requirements for TTS."
                                else testalertmsg = "alert would not be announced.  Alert does not meet restiction requirements."
                            }
                            testConfig +="<table border=1px><tr><td colspan='2'>Alert ${y+1}/${atomicState.ListofAlerts.size()} - ${testalertmsg}</td></tr>"
                            testConfig += "<tr><td>Field Name</td><td>Value</td></tr><tr><td>Severity</td><td>${atomicState.ListofAlerts[y].alertseverity}</td></tr>"
                            testConfig += "<tr><td>Area</td><td>${atomicState.ListofAlerts[y].alertarea}</td></tr>"
                            testConfig += "<tr><td>Sent</td><td>${atomicState.ListofAlerts[y].alertsent}</td></tr>"
                            testConfig += "<tr><td>Effective</td><td>${atomicState.ListofAlerts[y].alerteffective}</td></tr>"
                            testConfig += "<tr><td>Expires</td><td>${atomicState.ListofAlerts[y].alertexpires}</td></tr>"
                            testConfig += "<tr><td>Status</td><td>${atomicState.ListofAlerts[y].alertstatus}</td></tr>"
                            testConfig += "<tr><td>Message Type</td><td>${atomicState.ListofAlerts[y].alertmessagetype}</td></tr>"
                            testConfig += "<tr><td>Category</td><td>${atomicState.ListofAlerts[y].alertcategory}</td></tr>"
                            testConfig += "<tr><td>Certainty</td><td>${atomicState.ListofAlerts[y].alertcertainty}</td></tr>"
                            testConfig += "<tr><td>Urgency</td><td>${atomicState.ListofAlerts[y].alerturgency}</td></tr>"
                            testConfig += "<tr><td>Sender Name</td><td>${atomicState.ListofAlerts[y].alertsendername}</td></tr>"
                            testConfig += "<tr><td>Event Type</td><td>${atomicState.ListofAlerts[y].alertevent}</td></tr>"
                            testConfig += "<tr><td>Headline</td><td>${atomicState.ListofAlerts[y].alertheadline}</td></tr>"
                            testConfig += "<tr><td>Description</td><td>${atomicState.ListofAlerts[y].alertdescription}</td></tr>"
                            testConfig += "<tr><td>Instruction</td><td>${atomicState.ListofAlerts[y].alertinstruction}</td></tr></table>"
                        }
                        paragraph testConfig
                    }
					else paragraph "There are no reported weather alerts in your area, the weather alerts available have expired, the api.weather.gov api is not available, or you need to change NOAA Weather Alert options to acquire desired results.<br><br>Current URI: <a href='${state.wxURI}' target=_blank>${state.wxURI}</a>"
				}
			}
		}
}            

// Main Application Routines
def main() {
    // Get the alert message
	getAlertMsg()	
    if(atomicState.ListofAlerts) {
	    if(atomicState.alertAnnounced) { 
		    if(logEnable) log.info "No new alerts.  Waiting ${whatPoll.toInteger()} minute(s) before next poll..."
        } else {
            if(pushovertts || musicmode || speechmode || echoSpeaks2) {
                 atomicState.alertAnnounced = true
                 alertNow(atomicState.ListofAlerts[0].alertmsg, false)
                if(repeatYes && atomicState.ListofAlerts[0].alertrepeat == false) {
                    state.repeatmsg = atomicState.ListofAlerts[0].alertmsg
                    repeatNow()
                } else state.repeatmsg = null
            }   
	    }
    } else if(logEnable) log.info "No new alerts.  Waiting ${whatPoll.toInteger()} minute(s) before next poll..."
}

def alertNow(alertmsg, repeatCheck){
	// check restrictions based on Modes and Switches
    def restrictionSwitch = (switchYes && restrictbySwitch !=null && restrictbySwitch.currentState("switch").value == "on") ? true : false
    def restrictionMode = (modesYes && modes !=null && modes.contains(location.mode)) ? true : false
    def restrictionSeverity = (modeSeverityYes && modeSeverity !=null && modeSeverity.contains(atomicState.ListofAlerts[0].alertseverity)) ? true : false
    def restrictionWeatherType = (modeWeatherType && WeatherType !=null && WeatherType.contains(atomicState.ListofAlerts[0].alertevent)) ? true : false
    def alertSwitchReset = (alertSwitchWeatherType && alertSwitchWeatherTypeWatch != null && alertSwitchWeatherTypeWatch.contains(atomicState.ListofAlerts[0].alertevent)) ? true : false
    if(logEnable) log.debug "Restrictions on?  Modes: ${result2}, Switch: ${result}, Severity Override: ${result3}, Weather Type Override: ${result4}"
   
    // no restrictions
    if((restrictionSwitch == false || restrictionMode == false) && (restrictionSeverity == true || restrictionWeatherType == true)) {
        log.info "Sending alert: ${alertmsg}"
        pushNow(alertmsg, repeatCheck)
	    if(alertSwitch == true) alertSwitch.on()
	    talkNow(alertmsg, repeatCheck)  
     } else {
            if(pushoverttsalways == true) {	
                log.info "Restrictions are enabled but PushoverTTS enabled.  Waiting ${whatPoll.toInteger()} minutes before next poll..."
                pushNow(alertmsg, repeatCheck) 
            }
            if(alertSwitchReset == true) alertSwitch.off()
            else log.info "Restrictions are enabled!  Waiting ${whatPoll.toInteger()} minutes before next poll..."
    }
}

def repeatNow(){
    if(state.repeat) {
        alertNow(state.repeatmsg, true)
        state.count = state.count + 1
     } else state.repeat = true
     if(logEnable) log.debug "Repeating alert in ${repeatMinutes} minute(s).  This is ${state.count}/${repeatTimes} repeated alert(s). Repeat State: ${state.repeat}"
    
	 if(state.num > 0){
	    state.num = state.num - 1
        runIn(repeatMinutes.toInteger()*60,repeatNow)
     } else { 
        if(logEnable) log.debug "Finished repeating alerts."
        state.count = 0
        state.num = repeatTimes.toInteger()
        state.repeat = false 
        state.repeatmsg = ""
     }
}

def getAlertMsg() {
    def ListofAlerts = []
    def result = getResponseURL()
    if(result) {
        def date = new Date()
        String timestamp = date.format("yyyy-MM-dd'T'HH:mm:ssXXX")

        for(i=0; i<result.data.features.size();i++) { 
            debug=true
            alertmsg=[]
            
            //alert expiration
            if(result.data.features[i].properties.ends) alertexpires = result.data.features[i].properties.ends
            else alertexpires = result.data.features[i].properties.expires

            //if alert has expired ignore alert
            if((alertexpires.compareTo(timestamp)>=0) || app?.debug) {
                
            //if specific weatheralerts is chosen
                if(myWeatherAlert==null || myWeatherAlert=="") msg = buildAlertMsg(result.data.features[i])   
                else if(myWeatherAlert.contains(result.data.features[i].properties.event)) msg = buildAlertMsg(result.data.features[i])
                
                if(msg!=null && atomicState.ListofAlerts) {
                    if(!(atomicState.ListofAlerts.alertid.contains(result.data.features[i].properties.id))) { IsnewList = true
                    if(logEnable) log.debug "${result.data.features[i].properties.id} is new in ListofAlerts: ${IsnewList}" }
                } //end of listofalerts
                if(msg!=null) ListofAlerts << msg
            } // end alertexpires
        } //end of for statement

        if(ListofAlerts==null || IsnewList) atomicState.alertAnnounced = false
        if(ListofAlerts==null && alertSwitch) alertSwitch.off()
        atomicState.ListofAlerts = ListofAlerts        
    }
    

} 

def buildAlertMsg(result) {
    //build new entry for map
    if(result.properties.ends) alertexpires = result.properties.ends
    else alertexpires = result.properties.expires
    alertarea = (result.properties.areaDesc)
    alertarea = alertRemoveStates(alertarea)
    alertarea = alertFormatArea(alertarea)
    alertheadline = result.properties.headline
    alertheadline = alertFormatStates(alertheadline)
    alertheadline = alertRemoveTimeZone(alertheadline)
    alertheadline = alertFormatText(alertheadline)
    alertdescription = result.properties.description
    alertdescription = alertFormatStates(alertdescription)
    alertdescription = alertRemoveTimeZone(alertdescription)
    alertdescription = alertFormatText(alertdescription)
    if(result.properties.instruction==null) alertinstruction = alertdescription
    else { 
         alertinstruction = result.properties.instruction
         alertinstruction = alertFormatStates(alertinstruction)
         alertinstruction = alertRemoveTimeZone(alertinstruction)
         alertinstruction = alertFormatText(alertinstruction)
    }
    alertmsg = alertCustomMsg
    try {alertmsg = alertmsg.replace("{alertarea}","${alertarea}") }
      catch (any) {}
    try {alertmsg = alertmsg.replace("{alertseverity}","${result.properties.severity}") }
      catch (any) {}
    try {alertmsg = alertmsg.replace("{alertcertainty}","${result.properties.certainty}") }
      catch (any) {}
    try {alertmsg = alertmsg.replace("{alerturgency}","${result.properties.urgency}") }
      catch (any) {}
    try {alertmsg = alertmsg.replace("{alertheadline}","${alertheadline}") }
      catch (any) {}
    try {alertmsg = alertmsg.replace("{alertdescription}","${alertdescription}") }
      catch (any) {}
    try {alertmsg = alertmsg.replace("{alertinstruction}","${alertinstruction}") }
      catch (any) {}
    try {alertmsg = alertmsg.replace("{alertevent}","${result.properties.event}") }
      catch (any) {}					
    try {alertmsg = alertmsg.replaceAll("\n"," ") }
      catch (any) {}
    try {alertmsg = alertmsg.trim().replaceAll("[ ]{2,}", ", ") }
      catch (any) {}
    alertmsg = alertmsg.replaceAll("\\s+", " ")

    return [alertid:result.properties.id, alertseverity:result.properties.severity, alertarea:alertarea, alertsent:result.properties.sent, alerteffective:result.properties.effective, alertexpires:alertexpires, alertstatus:result.properties.status, alertmessagetype:result.properties.messageType, alertcategory:result.properties.category, alertcertainty:result.properties.certainty, alerturgency:result.properties.urgency, alertsendername:result.properties.senderName, alertheadline:alertheadline, alertdescription:alertdescription, alertinstruction:alertinstruction, alertevent:result.properties.event, alertmsg:alertmsg]
}

def alertFormatStates(msg) {
    msg = msg.replaceAll("/AL/","Alabama")
    msg = msg.replaceAll("/AK/","Alaska")
    msg = msg.replaceAll("/AZ/","Arizona")
    msg = msg.replaceAll("/AR/","Arkansas")
    msg = msg.replaceAll("/CA/","California") 
    msg = msg.replaceAll("/CO/","Colorado")
    msg = msg.replaceAll("/CT/","Connecticut")
    msg = msg.replaceAll("/DE/","Deleware")
    msg = msg.replaceAll("/FL/","Florida")
    msg = msg.replaceAll("/GA/","Georgia")
    msg = msg.replaceAll("/HI/","Hawaii")
    msg = msg.replaceAll("/ID/","Idaho")
    msg = msg.replaceAll("/IL/","Illinois")
    msg = msg.replaceAll("/IN/","Indiana")
    msg = msg.replaceAll("/IA/","Iowa")
    msg = msg.replaceAll("/KS/","Kansas")
    msg = msg.replaceAll("/KY/","Kentucky")
    msg = msg.replaceAll("/LA/","Louisiana")
    msg = msg.replaceAll("/ME/","Maine")
    msg = msg.replaceAll("/MA/","Massachusetts")
    msg = msg.replaceAll("/MD/","Maryland")
    msg = msg.replaceAll("/MI/","Michigan")
    msg = msg.replaceAll("/MN/","Minnesota")
    msg = msg.replaceAll("/MS/","Mississippi")
    msg = msg.replaceAll("/MO/","Missouri")
    msg = msg.replaceAll("/MT/","Montana")
    msg = msg.replaceAll("/NE/","Nebraska")
    msg = msg.replaceAll("/NV/","Nevada")
    msg = msg.replaceAll("/NH/","New Hampshire")
    msg = msg.replaceAll("/NJ/","New Jersey")
    msg = msg.replaceAll("/NM/","New Mexico")
    msg = msg.replaceAll("/NY/","New York")
    msg = msg.replaceAll("/NC/","North Carolina")
    msg = msg.replaceAll("/ND/","North Dakota")
    msg = msg.replaceAll("/OH/","Ohio")
    msg = msg.replaceAll("/OK/","Oklahoma")
    msg = msg.replaceAll("/OR/","Oregon")
    msg = msg.replaceAll("/PA/","Pennsylvania")
    msg = msg.replaceAll("/RI/","Rhode Island")
    msg = msg.replaceAll("/SC/","South Carolina")
    msg = msg.replaceAll("/SD/","South Dakota")
    msg = msg.replaceAll("/TN/","Tennessee")
    msg = msg.replaceAll("/TX/","Texas")
    msg = msg.replaceAll("/UT/","Utah")
    msg = msg.replaceAll("/VT/","Vermont")
    msg = msg.replaceAll("/VA/","Virginia")
    msg = msg.replaceAll("/WA/","Washington")
    msg = msg.replaceAll("/WV/","West Virginia")
    msg = msg.replaceAll("/WI/","Wisconsin")
    msg = msg.replaceAll("/WY/","Wyoming")
    return msg
}

def alertRemoveTimeZone(msg) {
    // Remove Timezones
    return msg.replaceAll(/(AST|EST|EDT|CST|CDT|MST|MDT|PST|PDT|AKST|AKDT|HST|HAST|HADT)/,"")
}

def alertRemoveStates(msg) {
    return msg.replaceAll(/(AL|AK|AZ|AR|CA|CO|CT|DE|FL|GA|HI|ID|IL|IA|IN|KS|KY|LA|ME|MA|MD|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|WY)/, "")
}

def alertFormatText(msg) {
    msg = msg.replaceAll(/NWS/,"the National Weather Service of") 
    msg = msg.replaceAll(/(WHAT|WHEN|IMPACTS|IMPACT|WHERE|INCLUDES|HAZARDS|INCLUDE|HAZARD|TEMPERATURE|SOURCE)/, "")
    msg = msg.replaceAll(/\.{2,}/, "")
    msg = msg.replaceAll(/\*/, "")
    msg = msg.replaceAll(/MPH/, "miles per hour")
    msg = msg.replaceAll("","")
    msg = msg.replaceAll("\n"," ")
    msg = msg.replaceAll("\\s+", " ")
    msg = msg.replaceAll(/(?:(\d{2})(\d{2}))|(?:(\d(?!\d{3}))(\d{2}))(?=\s?(?i:am|pm))/,'$1$3:$2$4')
    return msg
}
    
def alertFormatArea(msg) {
    msg.replaceAll(/NWS/,"the National Weather Service of")
    msg = msg.replaceAll(", ","")
    msg = msg.replaceAll(",","")
    msg = msg.replaceAll(";",",")                    
    msg = msg.replaceAll("\n"," ")
    msg = msg.replaceAll("\\s+", " ")  
    msg = msg.replaceAll("/","")
    StringBuffer buffer = new StringBuffer(msg)
    msg = buffer.reverse().toString().replaceFirst(",","dna ")
    msg = new StringBuffer(msg).reverse().toString()
    msg = msg + "."
    return msg   
}   

//Test Alert Section
def runtestAlert() {
    if(logEnable) log.debug "Initiating a test alert."
    state.repeatmsg=buildTestAlert()
    alertNow(state.repeatmsg, false)
    if(repeatYes==true) repeatNow()
}

def buildTestAlert() {
    alertmsg = alertCustomMsg
	try { alertmsg = alertmsg.replace("{alertarea}","Springfield County.") }
	catch (any) {}
	try { alertmsg = alertmsg.replace("{alertseverity}","Severe") }
	catch (any) {}
	try { alertmsg = alertmsg.replace("{alertcertainty}","Likely") }
	catch (any) {}
	try { alertmsg = alertmsg.replace("{alerturgency}","Immediate") }
	catch (any) {}
	try { alertmsg = alertmsg.replace("{alertheadline}","The founder, Jebediah Springfield has spotted a cloud above the nuclear power plant towers.") }
	catch (any) {}
	try { alertmsg = alertmsg.replace("{alertdescription}","The founder, Jebediah Springfield has spotted a cloud above the nuclear power plant towers.  Expect heavy polution, possible fish with three eyes, and a Simpson asleep at the console.  Also a notorius yellow haired boy is terrorizing animals with spit wads.  Be on the look out for suspicious activity.") }
	catch (any) {}
	try { alertmsg = alertmsg.replace("{alertinstruction}","Expect heavy polution, possible fish with three eyes, and a Simpson asleep at the console.") }
	catch (any) {}
	try { alertmsg = alertmsg.replace("{alertevent}","Nuclear Power Plant Warning") }
	catch (any) {}
	return alertmsg
}

// Common Notifcation Routines
def talkNow(alertmsg, repeatCheck) {			
    if(repeatCheck) {
        if(useAlertIntro) alertmsg = "Repeating previous alert,, ${AlertIntro}" + alertmsg
        else alertmsg = "Repeating previous alert,," + alertmsg
    } else if(useAlertIntro) alertmsg = "${AlertIntro}, " + alertmsg
        
	speechDuration = Math.max(Math.round(alertmsg.length()/12),2)+3		
	atomicState.speechDuration2 = speechDuration * 1000
	
  	if(musicmode) { 
    	try {
	    	musicspeaker.playTextAndRestore(alertmsg.toLowerCase(), speakervolume)
            if(logEnable) log.debug "Sending alert to Music Speaker(s)."
		}
	    catch (any) { log.warn "Music Player device(s) has not been selected or not supported." }
  	}   
	
	if(echoSpeaks2) {
		try {
			echospeaker.setVolumeSpeakAndRestore(speakervolume, alertmsg.toLowerCase())
            if(logEnable) log.debug "Sending alert to Echo Speaks device(s)."
		}
		catch (any) { log.warn "Echo Speaks device(s) has not been selected or are not supported." }
	}
	
	if(speechmode) { 
		try {
            speechspeaker.initialize() 
            if(logEnable) log.debug "Initializing Speech Speaker"
            pauseExecution(2500)
        }
        catch (any) { if(logEnable) log.debug "Speech device doesn't support initialize command" }
        try { 
            speechspeaker.setVolume(speakervolume)
            if(logEnable) log.debug "Setting Speech Speaker to volume level: ${speakervolume}"
			pauseExecution(2000)
        }
        catch (any) { if(logEnable) log.debug "Speech speaker doesn't support volume level command" }
                
		if(logEnable) log.debug "Sending alert to Speech Speaker(s)"
        alertmsg = alertmsg.toLowerCase()
        try { speechspeaker.speak(alertmsg) }
        catch (any) { log.warn "Speech or Echo Speaks device(s) has not been selected or not supported." }
            
        try {
		    if(speakervolRestore) {
				pauseExecution(atomicState.speechDuration2)
				speechspeaker.setVolume(speakervolRestore)	
                if(logEnable) log.debug "Restoring Speech Speaker to volume level: ${speakervolRestore}"
            }
        }
            catch (any) { if (logEnable) log.debug "Speech speaker doesn't support restore volume command" }
	}
}

def pushNow(alertmsg, repeatCheck) {
	if (pushovertts) {
        fullalert = []
	    if(logEnable) log.debug "Sending Pushover message."
        if(repeatCheck) {
            if(repeatTimes.toInteger()>1) alertmsg = "[Alert Repeat ${state.count}/${repeatTimes}] " + alertmsg
            else alertmsg = "[Alert Repeat] " + alertmsg
        }

	    def m = alertmsg =~ /(.|[\r\n]){1,1023}\W/
	   	while(m.find()) {
            fullalert << m.group()
	    }

		for(x=0;x<fullalert.size();x++) {
            if(fullalert.size()>1) pushoverdevice.deviceNotification("(${x+1}/${fullalert.size()}) ${fullalert[x]}")
            else pushoverdevice.deviceNotification("${fullalert[x]}")
            pauseExecution(1000)
        } 
	}
}


def getTile() {
    try {
        msg = []
        if(atomicState.testmsg) {
            msg << [alertmsg:state.repeatmsg]
            atomicState.testmsg = false
        } else {
            if(atomicState.ListofAlerts) {
                for(x=0;x<atomicState.ListofAlerts.size();x++) {
                    msg << [alertmsg:atomicState.ListofAlerts[x].alertmsg]
                }   
            }
        }
        return msg
    }
    catch (e) {}
}

def buildEventsList() {
    def results = getResponseEvents()
    if(results) {
        atomicState.eventTypes = results.data.eventTypes
        if(logEnable) log.debug "Acquired current events list from api.weather.gov"
    }
    schedule("00 00 01 ? * *", buildEventsList)
}

// Device creation and status updhandlers
def createChildDevices() {
    try {
        if (!getChildDevice("NOAA")) {
            if (logEnable) log.info "Creating device: NOAA Tile"
            addChildDevice("aaronward", "NOAA Tile", "NOAA", 1234, ["name": "NOAA Tile", isComponent: false])
        }
    }
    catch (e) { log.error "Couldn't create child device. ${e}" }
}

def cleanupChildDevices() {
    try {
	    for(device in getChildDevices()) deleteChildDevice(device.deviceNetworkId)
    }
    catch (e) { log.error "Couldn't clean up child devices." }
}

// Application Support Routines
def getResponseURL() {
	// Determine if custom coordinates have been selected
	if(useCustomCords) {
		latitude = "${customlatitude}"
		longitude = "${customlongitude}"
	} else {
		latitude = "${location.latitude}"
		longitude = "${location.longitude}"
	}
    
	def wxURI = "https://api.weather.gov/alerts?point=${latitude}%2C${longitude}&status=actual&message_type=alert"    
    def result = null
    
	// Build out the API options
	if(whatAlertUrgency != null) wxURI = wxURI + "&urgency=${whatAlertUrgency.join(",")}"
    
    if(whatAlertSeverity != null) wxURI = wxURI + "&severity=${whatAlertSeverity.join(",")}"
	else wxURI = wxURI + "&severity=severe"
    
	if(whatAlertCertainty !=null) wxURI = wxURI + "&certainty=${whatAlertCertainty.join(",")}"
    
	state.wxURI = wxURI
    if(logEnable) log.debug "URI: <a href='${wxURI}' target=_blank>${wxURI}</a>"
    

    if(logEnable) log.debug "Connecting to weather.gov service."
    def requestParams =	[ 
        uri: wxURI,
        requestContentType: "application/json",
        contentType: "application/json"
    ]
	
	try {
        httpGet(requestParams)	{ response -> result = response }
    }
    catch (e) { if(logEnable) log.warn "The API Weather.gov did not return a response." }
    return result
}

def getResponseEvents() {
    wxURI = "https://api.weather.gov/alerts/types"
    def requestParams =	[ 
        uri: wxURI,
        requestContentType: "application/json",
        contentType: "application/json"
    ]
	
	try {
        httpGet(requestParams)	{ response -> result = response }
    }
    catch (e) { if(logEnable) log.warn "The API Weather.gov did not return a response." }
    return result    
}

def checkState() {
    atomicState.testmsg = false
    if(whatPoll==null) whatPoll = 5
    if(logEnable==null) logEnable = false
    if(logMinutes==null) logMinutes = 15
    if(whatAlertSeverity==null) whatAlertSeverity = "Severe"
    if(alertCustomMsg==null) alertCustomMsg = "{alertseverity} Weather Alert for the following counties: {alertarea} {alertdescription} This is the end of this Weather Announcement."
    
    if(repeatTimes==null || repeatMinutes==null) {
        if(repeatTimes==null) state.num = 1 
        else { state.num = repeatTimes.toInteger()
               state.repeatTime = repeatTimes.toInteger()
        }
        if(repeatMinutes==null)state.frequency = 15
        else state.frequency = repeatMinutes.toInteger() 
    } else {
        state.num = repeatTimes.toInteger()
        state.frequency = repeatMinutes.toInteger()
    }
    state.count = 0
    if(!state.repeat) state.repeatmsg = ""
}

def installCheck(){     
	state.appInstalled = app.getInstallationState() 
	if(state.appInstalled != 'COMPLETE'){
		section{paragraph "Please hit 'Done' to install ${app.label} "}
  	}
}

def initialize() {
    buildEventsList()
    checkState()    
    unschedule()
    createChildDevices()
    state.repeat = false
    if(logEnable && logMinutes.toInteger() != 0) {
        if(logMinutes.toInteger() !=0) log.warn "Debug messages set to automatically disable in ${logMinutes} minute(s)."
        runIn((logMinutes.toInteger() * 60),logsOff)
    }
    else if(logEnable && logMinutes.toInteger() == 0) log.warn "Debug logs set to not automatically disable."  
         else log.info "Debug logs disabled."

    switch(whatPoll.toInteger()) {
	    case 1: 
			runEvery1Minute(main)
			break
		case 5: 
			runEvery5Minutes(main)
			break
		case 10: 
			runEvery10Minutes(main)
			break
		case 15: 
			runEvery15Minutes(main)
			break
		default: 
			runEvery5Minutes(main)
			break
	}
    main()
}

def installed() {
    if(logEnable) log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    if(logEnable) log.debug "Updated with settings: ${settings}"
    initialize()
}

def uninstalled() {
    cleanupChildDevices()
}


def UIsupport(type, txt) {
    switch(type) {
        case "logo": 
            return "<table border=0><thead><tr><th><img border=0 style='max-width:100px' src='https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/NOAA/Support/NOAA.png'></th><th style='padding:10px' align=left><font style='font-size:34px;color:#1A77C9;font-weight: bold'>NOAA Weather Alerts</font><br><font style='font-size:14px;font-weight: none'>This application provides customized Weather Alert announcements.</font></tr></thead></table><br><hr style='margin-top:-15px;background-color:#1A77C9; height: 1px; border: 0;'></hr>"
            break
        case "line": 
            return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
            break
        case "header": 
            return "<div style='color:#ffffff;font-weight: bold;background-color:#1A7BC7;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${txt}</div>"
            break
        case "footer":
            return "<div style='color:#1A77C9;text-align:center'>App/Driver v${version()}<br>Developed by: Aaron Ward<br><a href='https://paypal.me/aaronmward?locale.x=en_US' target='_blank'><img src='https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/support/images/paypal.jpg' border='0' alt='PayPal Logo'></a></div>"
            break
        case "configured":
            return "<img border=0 style='max-width:15px' src='https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/support/images/Checked.svg'>"
            break
        case "attention":
            return "<img border=0 style='max-width:15px' src='https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/support/images/Attention.svg'>"
            break
    }
}

def logsOff(){
    log.warn "Debug logging disabled."
    app?.updateSetting("logEnable",[value:"false",type:"bool"])
}

import groovy.transform.Field
import groovy.json.*
import java.util.regex.*
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import groovy.time.*
