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
 *  Special thanks to bptorld, Cobra and csteele for use of their code or assistance in refining code.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
	
def version(){"v1.0.8"}

definition(
    name:"NOAA Weather Alerts",
    namespace: "Aaron Ward",
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

def installed() {
    if (logEnable) log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    if (logEnable) log.debug "Updated with settings: ${settings}"
    unsubscribe()
    if (logEnable) runIn(900,logsOff)
    initialize()
	log.info "Checking for Severe Weather"
	runEvery5Minutes(refresh)
	runIn(5, refresh)

}

def initialize() {
	if(!state.repeatAlert) { state.repeatAlert = false }
}


def mainPage() {
    dynamicPage(name: "mainPage") {
    	installCheck()
		if(state.appInstalled == 'COMPLETE'){
			section(getFormat("title", "${getImage("Blank")}" + " ${app.label}")) {
				paragraph "<div style='color:#1A77C9'>This application supplies Severe Weather alert TTS notifications.</div>"
			}
			section(getFormat("header-green", " General")) {
       			label title: "Enter a name for parent app (optional)", required: false
			}
			section(getFormat("header-green", " Configuration")) {
				input name: "whatAlert", type: "enum", title: "Choose Alerts Severity to check for: ", options: ["moderate": "Moderate", "severe,extreme": "Severe & Extreme", "moderate,severe,extreme": "Moderate, Severe & Extreme"], required: true, multiple: false, defaultValue: "Severe & Extreme"
				input "repeatYes", "bool", title: "Repeat alerts after certain amount of minutes?", require: false, defaultValue: false, submitOnChange: true
				if(repeatYes){ input name:"repeatMinutes", type: "text", title: "Number of minutes before repeating the alert?", require: false, defaultValue: "30" }
			    input "pushovertts", "bool", title: "Send a 'Pushover' message for NOAA Weather Alerts", required: true, defaultValue: false, submitOnChange: true 
			    if(pushovertts == true){ input "pushoverdevice", "capability.notification", title: "PushOver Device", required: true, multiple: true}
				paragraph "Configure your TTS devices"
			      input "speechMode", "enum", required: true, title: "Select Speaker Type", submitOnChange: true,  options: ["Music Player", "Speech Synth"] 
				if (speechMode == "Music Player"){
           	   		      input "speaker1", "capability.musicPlayer", title: "Choose speaker(s)", required: true, multiple: true, submitOnChange: true
					input (name: "echoSpeaks", type: "bool", defaultValue: "false", title: "Is this an 'echo speaks' device?", description: "Echo speaks device?")
					input "volume1", "number", title: "Speaker volume", description: "0-100%", required: true, defaultValue: "75"
          	            }   
        	            if (speechMode == "Speech Synth"){
         	            	input "speaker1", "capability.speechSynthesis", title: "Choose speaker(s)", required: true, multiple: true
          	            }

			}
			section(getFormat("header-green", " Customization")) {
				input (name: "introduction", type: "text", title: "Announcement Introduction Phrase:", require: false, defaultValue: "Attention, Attention,") 
			}	
			section(getFormat("header-green", " Restrictions")) {
				input "modesYes", "bool", title: "Enable restriction by current mode(s)", required: true, defaultValue: false, submitOnChange: true	
				if(modesYes){	
				    input(name:"modes", type: "mode", title: "Restrict actions when current mode is:", multiple: true, required: false)
				}
				if(!modesYes){
			          input "restrictbySwitch", "capability.switch", title: "Or use a switch to restrict:", required: false, multiple: false, defaultValue: null
				}
			}
		}
		display()
	}
}

def installCheck(){         
    state.appInstalled = app.getInstallationState() 
    if(state.appInstalled != 'COMPLETE'){
    	section{paragraph "Please hit 'Done' to install '${app.label}' parent app "}
    }
    else{
    if (logEnable) log.debug "NOAA Weather Alerts is Installed Correctly"
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

def display(){
	section() {
		input "runTest", "bool", title: "Run a test Alert?", required: false, defaultValue: false, submitOnChange: true
		if(runTest) {
			testalert = "Testing Severe Weather Alert for the following counties: Springfield County.  The founder, Jebediah Springfield has spotted a cloud above the nuclear power plant towers.  Expect heavy polution, possible fish with three eyes, and a Simpson asleep at the console. . . This is the end of this Severe Weather Announcement."
			talkNow(testalert)
			runTest = false
		}
 		input "logEnable", "bool", title: "Enable Debug Logging?", required: false, defaultValue: true
		paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center'>Developed by: Aaron Ward<br/>${version()}</div>"
	}       
}

def logsOff(){
    log.warn "Debug logging disabled."
    app?.updateSetting("logEnable",[value:"false",type:"bool"])
}


def refresh() {

    def result = (!modesYes && restrictbySwitch !=null && restrictbySwitch.currentState("switch").value == "on") ? true : false
    result2 =    ( modesYes && modes            !=null && modes.contains(location.mode))                         ? true : false
	if (logEnable) log.debug "Restrictions on: $result, $result2"

	if( ! (result || result2) ) {
		def alertseverity, alertsent, alertarea, alertmsg
		def wxURI = "https://api.weather.gov/alerts/active?point=${location.latitude}%2C${location.longitude}&severity=${whatAlert}"
		if (logEnable) log.debug "URI: ${wxURI}"
	def requestParams =
	[
		uri:  wxURI,
		requestContentType: "application/json",
		contentType: "application/json"
	]
	httpGet(requestParams)	{	  response ->
		if (response?.status == 200)
		{
			if(response.data.features){
				alertseverity = response.data.features[0].properties.severity
				alertsent = response.data.features[0].properties.sent
				alertarea = response.data.features[0].properties.areaDesc
				alertarea = alertarea.replaceAll(";",",")
				alertarea = alertarea.replaceAll("\n"," ")
				StringBuffer buffer = new StringBuffer(alertarea)
				alertarea = buffer.reverse().toString().replaceFirst(",","dna ")
				alertarea = new StringBuffer(alertarea).reverse().toString()
				alertmsg = "${alertseverity} Weather Alert for the following counties: ${alertarea}.  ${response.data.features[0].properties.description}. . . This is the end of this Severe Weather Announcement."
				state.alertmsg = alertmsg.replaceAll("\n"," ")
			} 			
			if(alertarea) {
				if (logEnable) log.debug "AlertSent: '${alertsent}  Pastalert: '${state.pastalert}'"
				if(alertsent != state.pastalert){
					talkNow(state.alertmsg)
					pushNow()
					if(repeatYes) {
						runIn((60*repeatMinutes.toInteger()),talkNow(state.alertmsg))
					}
					state.pastalert = alertsent
					log.info "Speaking: ${alertmsg}"
					if (logEnable) log.debug "AlertSent: '${alertsent}  Pastalert: '${state.pastalert}'"
				} else 	log.info "No new alerts."				
			} else log.info "No new alerts."
		}
		else
		{
			log.warn "${response?.status}"
		}
		log.info "Waiting 5 minutes before next poll..."
	}
	} else log.info "Restrictions are enabled!  Waiting 5 minutes before next poll..."
}

def talkNow(alertmsg) {								
	state.fullMsg1 = "${introduction} ${alertmsg}"
		state.volume = volume1
		
  		if (speechMode == "Music Player"){ 
			if(echoSpeaks) {
				speaker1.setVolumeSpeakAndRestore(state.volume, state.fullMsg1)
			}
			if(!echoSpeaks) {
    			speaker1.playTextAndRestore(state.fullMsg1)

			}
  		}   
		if (speechMode == "Speech Synth"){ 
			speaker1.speak(state.fullMsg1)
		}
}

def pushNow() {
	if (pushovertts==true) {
        Pattern p = Pattern.compile("\\b.{1,1023}\\b\\W?")
        Matcher m = p.matcher(alert.fullMsg1)
        while(m.find()) {
                pushoverdevice.deviceNotification(m.group().trim())
        }
	}
}
