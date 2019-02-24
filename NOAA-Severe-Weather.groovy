/**
 *  ****************  NOAA Weather Alerts App  ****************
 *
 *  Design Usage:
 *  This app is designed to notify you of severe and extreme weather alerts to a specified TTS device.  This is only for US based users.  
 *  Willing to collaborate with others to create world-wide solution.
 *
 *  Copyright 2018 Aaron Ward
 *
 *  Special thanks to bptorld and Cobra for use of their code and various other bits and pieces to get this put together.
 *  
 *  This App is free and to be honest designed for my needs.
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
 *     1.0 - Initial poor mans code.  Additional updates coming soon.
**/

import groovy.json.*
	
def version(){"v1.0.0"}

definition(
    name:"NOAA Weather Alerts",
    namespace: "prayerfuldrop",
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
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
	log.info "Checking for Severe Weather"
	runEvery5Minutes(refresh)
	runIn(5, refresh)
}

def initialize() {

}


def mainPage() {
    dynamicPage(name: "mainPage") {
    	installCheck()
		if(state.appInstalled == 'COMPLETE'){
			section(getFormat("title", "${app.label}")) {
				paragraph "<div style='color:#1A77C9'>This application supplies Severe Weather alert TTS notifications.</div>"
			//	paragraph getFormat("line")
			}
			section(getFormat("header-green", "${getImage("Blank")}"+" General")) {
       			label title: "Enter a name for parent app (optional)", required: false
 			}
			section(getFormat("header-green", "${getImage("Blank")}"+" Configuration")) {}
			section("Location:", hideable: false, hidden: false) {
				paragraph "Enter your location information that is provided within Settings->Location and Modes"
				input "longitude", "text", title: "Longitude", required: true, multiple: false, defaultValue: "-/+XX.XXXXXX"
				input "latitude", "text", title: "Latitude", required: true, multiple: false, defaultValue: "XX.XXXXXX"
				
			}
			section("TTS Configuration:") {
				paragraph "Configure your TTS devices"
			    input "speechMode", "enum", required: true, title: "Select Speaker Type", submitOnChange: true,  options: ["Music Player", "Speech Synth"] 
				if (speechMode == "Music Player"){ 
           	   		input "speaker1", "capability.musicPlayer", title: "Choose speaker(s)", required: true, multiple: true, submitOnChange: true
					input(name: "echoSpeaks", type: "bool", defaultValue: "false", title: "Is this an 'echo speaks' device?", description: "Echo speaks device?")
					input "volume1", "number", title: "Speaker volume", description: "0-100%", required: true, defaultValue: "75"
          	}   
        	if (speechMode == "Speech Synth"){ 
         		input "speaker1", "capability.speechSynthesis", title: "Choose speaker(s)", required: true, multiple: true
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
    	log.info "Application Installed Correctly"
  	}
}

def getImage(type) {
    def loc = "<img src=https://raw.githubusercontent.com/bptworld/Hubitat/master/resources/images/"
    if(type == "Blank") return "${loc}blank.png height=40 width=5}>"
}

def getFormat(type, myText=""){
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

def display(){
	section() {
		paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center'>${version()}</div>"
	}       
}


def refresh() {
		def alertseverity, alertsent, alertarea, alertmsg
		def wxURI = "https://api.weather.gov/alerts?point=${longitude}%2C${latitude}"
		log.info "URI: ${wxURI}"
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
				alertmsg = "Attention, Attention, ${alertseverity} Weather Alert for the following counties: ${alertarea}.  ${response.data.features[0].properties.description}. . . This is the end of this Severe Weather Announcement."
				alertmsg = alertmsg.replaceAll("\n"," ")
			} else {
				log.info "No Alerts at this time."
				}
			
				if(alertarea) {
					log.info "AlertSent: '${alertsent}  Pastalert: '${state.pastalert}'"
					if(alertsent != state.pastalert){
						talkNow(alertmsg)
						state.pastalert = alertsent
						log.info "Speaking: ${alertmsg}"
						log.info "AlertSent: '${alertsent}  Pastalert: '${state.pastalert}'"
					} else {
						log.info "No new alerts."
				}
			}
		}
		else
		{
			log.warn "${response?.status}"
		}
	}

}

def talkNow(alertmsg) {								
		state.fullMsg1 = alertmsg
		state.volume = volume1
		if(!echoSpeaks) speaker1.setLevel(state.volume)
		
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

