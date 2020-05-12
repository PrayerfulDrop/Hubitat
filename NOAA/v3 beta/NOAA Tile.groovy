/**
 *  ****************  NOAA Tile Driver  ****************
 *
 *  importUrl: "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/NOAA/NOAA-Tile-Driver.groovy"
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
			//input("timeExpire", "text", title: "Minutes to scroll alert?", required: true, defaultValue: "5")
            input("logEnable", "bool", title: "Enable logging", required: false, defaultValue: true)
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
    if(logEnable) log.info "Received weather alert from NOAA App."
    
    if(noaaData==null || noaaData=="") {
        noaaTile = "<center><table width='90%' height='90%'><tr>"
		noaaTile += "<td style='text-align:center;'>"
        noaaTile += "<div style='font-size: 15px'>No weather alerts to report.</div>" 
	    noaaTile += "</td></tr></table>"
		sendEvent(name: "Alerts", value: noaaTile, displayed: true)         
    } else {
        count = true
        messageSize=300
        fullalert = []
        log.error noaaData
        while(count) {
            for(x=0;x<noaaData.size();x++) {
                log.debug noaaData[x].alertmsg
	    		def m = noaaData[x].alertmsg =~ /(.|[\r\n]){1,380}\W/
		    	fullmsg = []
			    while (m.find()) {
    			   fullmsg << m.group()
                }
                for(i=0;i<fullmsg.size();i++) {
                    noaaTile = "<center><table border=0 width='90%' height='90%'><thead><tr><th style='vertical-align:bottot;text-align: left;'><font style='font-size:15px;text-decoration:underline;'>"
                    if(i==0) noaaTile += "Alert ${x}/${noaaData.size()}"
                    else "Alert ${x}/${noaaData.size()} (continued)"
				    noaaTile += "</font></th></tr></thead><tbody><tr><td style='text-align: justify;'><font style='font-size: 15px'> ${fullmsg[i]}</font>"
	    			noaaTile += "</td></tr></tbody></table>"
		    		sendEvent(name: "Alerts", value: noaaTile, displayed: true)                
                    pauseExecution(8000)
                }
            }   
            count = false
        }
    }
}

