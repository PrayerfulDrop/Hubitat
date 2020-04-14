/**
 *  ****************  NOAA Tile Driver  ****************
 *
 *  importUrl: "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/NOAA/NOAA-Tile-Driver.groovy"
 *
 *  Design Usage:
 *  This driver formats the NOAA data to be used with Hubitat's Dashboards.
 *
 *  Copyright 2019 Aaron Ward
 *  
 *  This driver is free and you may do as you likr with it.  I give huge KUDDOS to bptworld for supplying v1.0.0 of this driver
 *
 *  Remember...I am not a programmer, everything I do takes a lot of time and research (then MORE research)!
 *  Donations are never necessary but always appreciated.  Donations to support development efforts are accepted via: 
 *
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *
 *  Changes:
 *
 *  1.2.2 - removed AppWatchDog driver code
 *  1.2.1 - added AppWatchDogDriver2 support
 *  1.2.0 - rewrite of the driver app (again) for better optimization, automate disabling of logging
 *  1.1.0 - Longer alerts will scroll on dashboard for 5 minutes, fixed justification of text alignment
 *  1.0.0 - Initial release thanks to bptworld!
 */

metadata {
	definition (
		name: "NOAA Tile",
		namespace: "aaronward",
		author: "Aaron Ward",
		importUrl: "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/NOAA/NOAA-Tile-Driver.groovy"
		)
		{
		command "sendNoaaTile", ["string"]
		capability "Actuator"
		capability "Refresh"
    	attribute "Alerts", "string"
		}

	preferences() {    	
			input("timeExpire", "text", title: "Minutes to scroll alert?", required: true, defaultValue: "5")
            input("logEnable", "bool", title: "Enable logging", required: true, defaultValue: true)
    }
}

def initialize() {
	log.info "NOAA Tile Driver Initializing."
	refresh()
}

def updated() {
	refresh()
}

def installed(){
    log.info "NOAA Tile has been Installed."
	sendEvent(name: "Alerts", value: "No weather alerts to report.", displayed: true)
}

def refresh() {
	if (logEnable) runIn(900,logsOff)
    log.info "NOAA Tile has been updated."
	state.clear()
}

def logsOff(){
    log.warn "Debug logging disabled."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}


def sendNoaaTile(noaaData) {
	def messageSize = 380
	alertmsg =  "${noaaData}"
	log.info "Received weather alert from NOAA App."
	if (logEnable) log.debug "Length of Alert: ${alertmsg.length()}"
		if(alertmsg.length() > messageSize) {
			if (logEnable) log.debug "Scrolling alert on dashboard for ${timeExpire} minutes."
  	  		def now = new Date()
			long unxNow = now.getTime()
			//long unxEnd = now.getTime() + (timeExpire.toInteger()*60*1000)
			long unxEnd = now.getTime() + (timeExpire.toInteger()*15*1000)
			unxNow = unxNow/1000
    		unxEnd = unxEnd/1000
			//determine how many pages will the alert is
			def m = alertmsg =~ /(.|[\r\n]){1,380}\W/
			state.index = 0
			def x = 0
			state.fullmsg = []
			while (m.find()) {
			   state.fullmsg << m.group()
			   state.index = state.index +1
			}
			
			while(unxNow < unxEnd){
				noaaTile = "<center><table width='90%' height='90%'><tr>"
				noaaTile += "<td style='text-align: justify;'>"
				if(x==(state.index-1)) { noaaTile += "<div style='font-size: 15px'> ${state.fullmsg[x]}</div>" }
				else { noaaTile += "<div style='font-size: 15px'> ${state.fullmsg[x]}...</div>" }
				noaaTile += "<div align='right' style='font-size: 15px'>${x+1}/${state.index}</div>"
				noaaTile += "</td></tr></table>"
				sendEvent(name: "Alerts", value: noaaTile, displayed: true)
				if(x == (state.index-1)) {x=0} else {x=x+1}
				pauseExecution(8000)
				now = new Date()
				unxNow = now.getTime()
				unxNow = unxNow/1000
			}
			if (logEnable) log.debug "End scrolling of alerts."
		}
		
		//build final alert to display
		noaaTile = "<center><table width='90%' height='90%'><tr>"
		if(noaaData.length() < messageSize) { 
				noaaTile += "<td style='text-align: center;'>"
				noaaTile += "<div style='font-size: ${fontSize}px'> ${alertmsg}</div>"
				noaaTile += "<div align='right' style='font-size: ${fontSize}px'></div>" 
		}
			else { 
				noaaTile += "<td style='text-align: justify;'>"
				noaaTile += "<div style='font-size: 15px'> ${state.fullmsg[0]}...</div>" 
				noaaTile += "<div align='right' style='font-size: 15px'>1/${state.index}</div>"
			}
		noaaTile += "</td></tr></table>"
		sendEvent(name: "Alerts", value: noaaTile, displayed: true)
		log.info "NOAA Weather Alert displayed on dashboard."
}

