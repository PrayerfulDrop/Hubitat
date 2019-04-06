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
 *  V1.0.0 - 04/01/19 - Initial release thanks to bptworld!
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
			input("fontSizeS", "text", title: "Date Font Size", required: true, defaultValue: "12")
            input("logEnable", "bool", title: "Enable logging", required: true, defaultValue: true)
        }
    }
}

def sendNoaaMap1(noaaMap1) {
	state.noaaMap1a = "${noaaMap1}"
	state.noaaMap1b = state.noaaMap1a.take(850)
	state.noaaMap1 = "<table width='100%'><tr>"
	state.noaaMap1 += "<td style='text-align: left;'>"
	state.noaaMap1 += "<div style='font-size: ${fontSize}px'> ${state.noaaMap1b}</div>"
	state.noaaMap1 += "</td></tr></table>"
	state.noaaMap1Count = state.noaaMap1.length()
	if(state.noaaMap1Count <= 1000) {
		if(logEnable) log.debug "noaaMap1 - has ${state.noaaMap1Count} Characters<br>${state.noaaMap1}"
	} else {
		state.noaaMap1 = "Too many characters to display on Dashboard (${state.noaaMap1Count})"
	}
	sendEvent(name: "Alerts", value: state.noaaMap1, displayed: true)
	sendEvent(name: "AlertCount", value: state.noaaMap1Count, displayed: true)
}

def installed(){
    log.info "NOAA Tile has been Installed"
	sendEvent(name: "Alerts", value: "No weather alerts to report.", displayed: true)
	sendEvent(name: "AlertCount", value: "0", displayed: true)
}

def updated() {
    log.info "NOAA Tile has been Updated"
}
