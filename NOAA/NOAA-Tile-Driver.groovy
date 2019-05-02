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
 *  1.1.0 - Longer alerts will scroll on dashboard for 5 minutes, fixed justification of text alignment
 *  1.0.0 - Initial release thanks to bptworld!
 */

metadata {
	definition (name: "NOAA Tile", namespace: "Aaron Ward", author: "Aaron Ward") {
   		capability "Actuator"

		command "sendNoaaMap1", ["string"]
		
    	attribute "Alerts", "string"
		attribute "AlertCount", "string"
	}

	preferences() {    	
        section(){
			input("fontSize", "text", title: "Data Font Size", required: true, defaultValue: "15")
			input("timeExpire", "text", title: "Minutes to scroll alert?", required: true, defaultValue: "5")
            input("logEnable", "bool", title: "Enable logging", required: true, defaultValue: true)
        }
    }
}

def sendNoaaMap1(noaaMap1) {
	if (logEnable) log.debug "Received weather alert from NOAA App."
	if(noaaMap1.length() > 380){
		if (logEnable) log.debug "Scrolling alert on dashboard for ${timeExpire} minutes."
  	  	def now = new Date()
		long unxNow = now.getTime()
		long unxEnd = now.getTime() + (timeExpire.toInteger()*60*1000)
		unxNow = unxNow/1000
    	unxEnd = unxEnd/1000
		alertmsg =  "${noaaMap1}"
			def m = alertmsg =~ /(.|[\r\n]){1,380}\W/
		def index = 0
		def x = 0
		def fullmsg = []
			while (m.find()) {
			   fullmsg << m.group()
			   index = index +1
			}
		while(unxNow < unxEnd){
			state.noaaMap1 = "<center><table width='90%' height='90%'><tr>"
			state.noaaMap1 += "<td style='text-align: justify;'>"
			if(x==(index-1)) {state.noaaMap1 += "<div style='font-size: ${fontSize}px'> ${fullmsg[x]}</div>"}
			else {state.noaaMap1 += "<div style='font-size: ${fontSize}px'> ${fullmsg[x]}...</div>"}
			state.noaaMap1 += "<div align='right' style='font-size: ${fontSize}px'>${x+1}/${index}</div>"
			state.noaaMap1 += "</td></tr></table>"
			sendEvent(name: "Alerts", value: state.noaaMap1, displayed: true)
			sendEvent(name: "AlertCount", value: fullmsg[x].length(), displayed: true)
			if(x == (index-1)) {x=0} else {x=x+1}
			pauseExecution(8000)
			now = new Date()
			unxNow = now.getTime()
			unxNow = unxNow/1000
		}
			state.noaaMap1 = "<center><table width='90%' height='90%'><tr>"
			state.noaaMap1 += "<td style='text-align: justify;'>"
			state.noaaMap1 += "<div style='font-size: ${fontSize}px'> ${fullmsg[0]}...</div>"
			state.noaaMap1 += "<div align='right' style='font-size: ${fontSize}px'>1/${index}</div>"
			state.noaaMap1 += "</td></tr></table>"
			sendEvent(name: "Alerts", value: state.noaaMap1, displayed: true)
			sendEvent(name: "AlertCount", value: fullmsg[0].length(), displayed: true)
		if (logEnable) log.debug "End scrolling of alerts."
	} else {
	if (logEnable) log.debug "Alert is under character limit.  No scrolling required."
	state.noaaMap1 = "${noaaMap1}"
	state.noaaMap1 = "<center><table width='90%' height='90%'><tr>"
	state.noaaMap1 += "<td style='text-align: justify;'>"
	state.noaaMap1 += "<div style='font-size: ${fontSize}px'> ${state.noaaMap1}</div>"
	state.noaaMap1 += "</td></tr></table>"
	state.noaaMap1Count = state.noaaMap1.length()
	sendEvent(name: "Alerts", value: state.noaaMap1, displayed: true)
	sendEvent(name: "AlertCount", value: state.noaaMap1Count, displayed: true)
	}
}

def installed(){
    log.info "NOAA Tile has been Installed"
	sendEvent(name: "Alerts", value: "No weather alerts to report.", displayed: true)
	sendEvent(name: "AlertCount", value: "0", displayed: true)
}

def updated() {
	if (logEnable) runIn(900,logsOff)
    log.info "NOAA Tile has been Updated"
}

def logsOff(){
    log.warn "Debug logging disabled."
    app?.updateSetting("logEnable",[value:"false",type:"bool"])
}
