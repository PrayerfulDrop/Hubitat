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
 *  Special thanks to csteele for all his help and education of Groovy to me.
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

	
def version(){"v2.0.0"}

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
				input (name: "alertCustomMsg", type: "text", title: "Alert Message:", require: false, defaultValue: "Attention, Attention. {alertseverity} weather alert. Certainty is {alertcertainty}. Urgency is {alerturgency}. {alertheadline}. {alertinstruction}. This is the end of this weather announcement.")
			}	
			section("Alert Message Customization Instructions:", hideable: true, hidden: true) {
        		paragraph "<b>Alert message variables:</b>"
				paragraph "{alertsent} = timestamp when alert sent."
				paragraph "{alerteffective} = timestamp when alert is effective for"
				paragraph "{alertexpires} = timestamp when alert expires"
				paragraph "{alertstatus} = alert current status"
				paragraph "{alertmessagetype} = alert message type"
				paragraph "{alertcategory} = alert category"
				paragraph "{alertseverity} = alertseverity"
				paragraph "{alertcertainty} = alert certainty of occuring"
				paragraph "{alerturgency} = alert urgency"
				paragraph "{alertevent} = alert event type"
				paragraph "{alertsendername} = alert sender"
				paragraph "{alertheadline} = alert headline"
				paragraph "{alertdescription} = alert description"
				paragraph "{alertinstruction} = alert instructions"
				paragraph "{alertarea} = counties or area being affected"				
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
			section(getFormat("header-green", " Advanced Configuration")) {
				input name: "whatAlert", type: "enum", title: "Choose Alerts Severity to check for: ", options: ["moderate": "Moderate", "severe,extreme": "Severe & Extreme", "moderate,severe,extreme": "Moderate, Severe & Extreme"], required: true, multiple: false, defaultValue: "Severe & Extreme"
				input name: "whatPoll", type: "enum", title: "Choose poll frequency: ", options: ["1": "1 Minute", "5": "5 Minutes", "10": "10 Minutes", "15": "15 Minutes", "30": "30 Minutes"], required: true, multiple: false, defaultValue: "5 Minutes"
				input "repeatYes", "bool", title: "Repeat alerts after certain amount of minutes?", require: false, defaultValue: false, submitOnChange: true
				if(repeatYes){ input name:"repeatMinutes", type: "text", title: "Number of minutes before repeating the alert?", require: false, defaultValue: "30" }

			}
		}
			section(getFormat("header-green", " Logging and Testing")) {
				input "runTest", "bool", title: "Run a test Alert?", required: false, defaultValue: false, submitOnChange: true
				if(runTest) {
					app?.updateSetting("runTest",[value:"false",type:"bool"])
					testalert = "Testing Severe Weather Alert for the following counties: Springfield County.  The founder, Jebediah Springfield has spotted a cloud above the nuclear power plant towers.  Expect heavy polution, possible fish with three eyes, and a Simpson asleep at the console. . . This is the end of this Severe Weather Announcement."
					talkNow(testalert)
					pushNow(testalert)
				}
 				input "logEnable", "bool", title: "Enable Debug Logging?", required: false, defaultValue: true
				paragraph getFormat("line")
				paragraph "<div style='color:#1A77C9;text-align:center'>Developed by: Aaron Ward<br/>${version()}</div>"
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
	log.info "Checking for Severe Weather"
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
		case 30: 
			runEvery30Minutes(refresh)
			break
		default: 
			runEvery5Minutes(refresh)
			break
	}
}

def initialize() {
	runIn(5, refresh)
	state.repeatalert = true
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


def logsOff(){
    log.warn "Debug logging disabled."
    app?.updateSetting("logEnable",[value:"false",type:"bool"])
}


def refresh() {
    def result = (!modesYes && restrictbySwitch !=null && restrictbySwitch.currentState("switch").value == "on") ? true : false
    result2 =    ( modesYes && modes            !=null && modes.contains(location.mode))                         ? true : false
	if (logEnable) log.debug "Restrictions on: $result, $result2"

	if( ! (result || result2) ) {
			buildAlertMsg()
			if(state.alertarea && state.alerturgency == "Immediate") {
				if (logEnable) log.debug "AlertSent: '${state.alertsent}  Pastalert: '${state.pastalert}'"
				if(state.alertsent != state.pastalert){
					// play TTS and send PushOver
					talkNow(state.alertmsg)
					pushNow(state.alertmsg)
					// determine if alert needs to be repeated after # of minutes
					if(repeatYes && state.alertrepeat) {
						runIn((60*repeatMinutes.toInteger()),repeatAlert())
						state.alertrepeat = false
					}
					// set the pastalert to the current alertsent timestamp
					state.pastalert = state.alertsent
					log.info "Speaking: ${state.alertmsg}"
					if (logEnable) log.debug "AlertSent: '${state.alertsent}  Pastalert: '${state.pastalert}'"
				} 
				else log.info "No new alerts."			
		log.info "Waiting ${whatPoll.toInteger()} minutes before next poll..."
	}
    else log.info "Restrictions are enabled!  Waiting ${whatPoll.toInteger()} minutes before next poll..."
}
}

def buildAlertMsg() {
		def alertseverity, alertsent, alertarea, alertmsg, 
		def wxURI = "https://api.weather.gov/alerts?point=${location.latitude}%2C${location.longitude}&severity=${whatAlert}"
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
					log.info "${alertseverity}"
					alertarea = response.data.features[0].properties.areaDesc
						alertarea = alertarea.replaceAll(";",",")
						alertarea = alertarea.replaceAll("\n"," ")
						StringBuffer buffer = new StringBuffer(alertarea)
						alertarea = buffer.reverse().toString().replaceFirst(",","dna ")
					state.alertarea = new StringBuffer(alertarea).reverse().toString()
					log.info "${state.alertarea}"
					state.alertsent = response.data.features[0].properties.sent
					log.info "${state.alertsent}"
					alerteffective = response.data.features[0].properties.effective
					log.info "${alerteffective}"
					alertexpires = response.data.features[0].properties.expires
					log.info "${alertexpires}"
					alertstatus = response.data.features[0].properties.status
					log.info "${alertstatus}"
					alertmessagetype = response.data.features[0].properties.messagetype
					log.info "${alertmessagetype}"
					alertcategory = response.data.features[0].properties.category
					log.info "${alertcategory}"
					alertseverity = response.data.features[0].properties.severity
					log.info "${alertseverity}"
					alertcertainty = response.data.features[0].properties.certainty
					log.info "${alertcertainty}"
					state.alerturgency = response.data.features[0].properties.urgency
					log.info "${alerturgency}"
					alertsendername = response.data.features[0].properties.sendername
					log.info "${alertsendername}"
					alertheadline = response.data.features[0].properties.headline
					log.info "${alertheadline}"
					alertdescription = response.data.features[0].properties.description
					log.info "${alertdescription}"
					alertinstruction = response.data.features[0].properties.instruction
					log.info "${alertinstruction}"
					alertresponse = response.data.features[0].properties.response
					log.info "${alertresponse}"
				
				// build the alertmsg
					alertmsg = customAlertMsg
					alertmsg = alertmsg.replace("{alertarea}","${state.alertarea}")
					alertmsg = alertmsg.replace("{alertsent}","${alertsent}")
					alertmsg = alertmsg.replace("{alerteffective}","${alerteffective}")
					alertmsg = alertmsg.replace("{alertexpires}","${alertexpires}")
					alertmsg = alertmsg.replace("{alertstatus}","${alertstatus}")
					alertmsg = alertmsg.replace("{alertmessagetype}","${alertmessagetype}")
					alertmsg = alertmsg.replace("{alertcategory}","${alertcategory}")
					alertmsg = alertmsg.replace("{alertseverity}","${alertseverity}")
					alertmsg = alertmsg.replace("{alertcertainty}","${alertcertainty}")
					alertmsg = alertmsg.replace("{alerturgency}","${state.alerturgency}")
					alertmsg = alertmsg.replace("{alertevent}","${alertevent}")
					alertmsg = alertmsg.replace("{alertsendername}","${alertsendername}")
					alertmsg = alertmsg.replace("{alertheadline}","${alertheadline}")
					alertmsg = alertmsg.replace("{alertdescription}","${alertdescription}")
					alertmsg = alertmsg.replace("{alertinstruction}","${alertinstruction}")
					alertmsg = alertmsg.replace("{alertresponse}","${alertresponse}")
					alertmsg = alertmsg.replace(" CST","")
					alertmsg = alertmsg.replace(" CDT","")
					alertmsg = alertmsg.replace(" MDT","")
					alertmsg = alertmsg.replace(" MST","")
					alertmsg = alertmsg.replace(" PST","")
					alertmsg = alertmsg.replace(" PDT","")
					alertmsg = alertmsg.replace(" EST","")
					alertmsg = alertmsg.replace(" EDT","")
					alertmsg = alertmsg.replace(" NWS "," the national weather service ")
					alertmsg = alertmsg.replaceAll("\n"," ")
					state.alertmsg = alertmsg
					log.info "${state.alertmsg}"
				} 	
			}
			else log.warn "${response?.status}"
		}
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

def pushNow(alertmsg) {
	if (pushovertts) {
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

def repeatAlert() {
	talkNow(state.alertmsg)
	pushNow(state.alertmsg)
	state.alertrepeat = true
}
