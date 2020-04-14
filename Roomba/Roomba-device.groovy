/**
 *
 * Hubitat Import URL: https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/Roomba/Roomba-device.groovy
 *
 *  ****************  Roomba Device  ****************
 *
 *  Design Usage:
 *  This driver supports the Roomba Scheduler App which is designed to integrate any WiFi enabled Roomba devices to have direct local connectivity and integration into Hubitat.  This applicatin will create a Roomba device based on the 
 *  the name you gave your Roomba device in the cloud app.  With this app you can schedule multiple cleaning times, automate cleaning when presence is away, receive notifications about status
 *  of the Roomba (stuck, cleaning, died, etc) and also setup continous cleaning mode for non-900 series WiFi Roomba devices.
 *
 *  Copyright 2019 Aaron Ward
 *
 *  Special thanks to Dominick Meglio for creating the initial integration and giving me permission to use his code to create this application.
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
 * 
 *   1.1.6 - fixed CSS layout for smartly dashboards
 *   1.1.5 - removed AppWatchDog
 *   1.1.4 - Roomba driver backup of failed application scheduled events
 *   1.1.3 - fixed on/off states
 *   1.1.2 - additional CSS fixes to ensure of future dashboard changes won't affect tile
 *   1.1.1 - fixed CSS for dashboard update
 *   1.1.0 - support for switch control on/off, handler changes
 *   1.0.9 - error in namespace preventing creation of adding device
 *   1.0.8 - added logging option
 *   1.0.7 - added battery died notifications
 *   1.0.6 - cleaning duration
 *   1.0.5 - added all possible tile outcomes (cleaning, docking, stopped, error, dead battery)
 *   1.0.4 - added dashboard tile updates
 *   1.0.3 - moved all notifications back to app
 *   1.0.2 - modified how pushover works
 *   1.0.1 - added pushover notification capabilities
 *   1.0.0 - Inital concept from Dominick Meglio
**/
metadata {
    definition (name: "Roomba", namespace: "roomba", author: "Aaron Ward", importUrl: "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/Roomba/Roomba-device.groovy") {
		capability "Battery"
		capability "Actuator"
        capability "Switch"
        
        attribute "cleanStatus", "string"
        attribute "RoombaTile", "string"
        attribute "Last Activity", "string"

        command "start"
        command "stop"
        command "pause"
        command "resume"
        command "dock"
    }
    preferences() {
        input("logEnable", "bool", title: "Enable logging", required: true, defaultValue: true)
    }
}

def start() {
    parent.handleDevice(device, device.deviceNetworkId.split(":")[1], "start")
    if(logEnable) log.debug "Roomba is being started through driver"
    def date = new Date()
    def sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa")
    sendEvent(name: "switch", value: "on", isStateChange: true)
    sendEvent(name: "Last Activity", value: sdf.format(date), isStateChange: true) 
}

def stop() {
    parent.handleDevice(device, device.deviceNetworkId.split(":")[1], "stop")
    if(logEnable) log.debug "Roomba is being stopped through driver"
    sendEvent(name: "switch", value: "off", isStateChange: true)
}

def pause() {
    parent.handleDevice(device, device.deviceNetworkId.split(":")[1], "pause")
    if(logEnable) log.debug "Roomba is being paused through driver"
    sendEvent(name: "switch", value: "off", isStateChange: true)
}

def resume() {
    parent.handleDevice(device, device.deviceNetworkId.split(":")[1], "resume")
    if(logEnable) log.debug "Roomba is resuming through driver"
    sendEvent(name: "switch", value: "off", isStateChange: true)
}

def dock() {
    parent.handleDevice(device, device.deviceNetworkId.split(":")[1], "dock")
    if(logEnable) log.debug "Roomba is being docked through driver"
    sendEvent(name: "switch", value: "off", isStateChange: true)
}

def on() {
    parent.handleDevice(device, device.deviceNetworkId.split(":")[1], "start")
    if(logEnable) log.debug "Roomba is being started through driver"
    sendEvent(name: "switch", value: "on", isStateChange: true)
}

def off() {
    parent.handleDevice(device, device.deviceNetworkId.split(":")[1], "off")
    if(logEnable) log.debug "Roomba off initiated through driver"
    sendEvent(name: "switch", value: "off", isStateChange: true)
}

def timecheck() {
    if(!state.timecheck){
        def now = new Date()
        long temp = now.getTime()
        state.starttime = temp
        state.timecheck = true
        runIn(60, timecheck)
    } else {
        long timeDiff
   		def now = new Date()
    	long unxNow = now.getTime()
    	unxPrev = state.starttime
    	unxNow = unxNow/1000
    	unxPrev = unxPrev/1000
    	timeDiff = Math.abs(unxNow-unxPrev)
    	timeDiff = Math.round(timeDiff/60)  
        if(logEnable) "Driver has not had any communication from parent in ${timeDiff} minute(s)"
        if(timeDiff > 5) { 
            parent.initialize()
            log.warn "Parent Roomba Scheduler stopped working.  Initializing the application"
        }
        else runIn(60, timecheck)
    }
    
}    

def roombaTile(cleaning, batterylevel, cleaningTime) {
    state.timecheck=false  
    timecheck()
    def img = ""
    switch(cleaning) {
        case "cleaning":
            img = "roomba-clean.png"
            msg=cleaning.capitalize()
            sendEvent(name: "switch", value: "on", isStateChange: true)
            break
        case "stopped":
            img = "roomba-stop.png"
            msg=cleaning.capitalize()
            sendEvent(name: "switch", value: "off", isStateChange: true)
            break        
        case "charging":
            img = "roomba-charge.png"
            msg=cleaning.capitalize()
            sendEvent(name: "switch", value: "off", isStateChange: true)
            break        
        case "docking":
            img = "roombadock.png"
            msg=cleaning.capitalize()
            sendEvent(name: "switch", value: "off", isStateChange: true)
            break
        case "dead":
            img = "roomba-dead.png"
            msg="Battery Died"
            sendEvent(name: "switch", value: "off", isStateChange: true)
            break
        case "error":
            img = "roomba-error.png"
            msg = cleaning.capitalize()
            sendEvent(name: "switch", value: "off", isStateChange: true)
            break
        default:
            img = "roomba-stop.png"
            msg=cleaning.capitalize()
            sendEvent(name: "switch", value: "off", isStateChange: true)
            break
    }
    img = "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/Roomba/support/${img}"
    html = "<center><img width=70px height=70px vspace=5px src=${img}><br><font style='font-size:13px'>"
        if(cleaning.contains("docking") || cleaning.contains("cleaning")) html +="${msg} - ${cleaningTime}min<br>Battery: ${batterylevel}%"
        else html+="${msg}<br>Battery: ${batterylevel}%"
    html += "</font></center>"
    sendEvent(name: "RoombaTile", value: html, displayed: true)
    if(logEnable) log.debug "Roomba Status of '${msg}' sent to dashboard"
}

import java.text.SimpleDateFormat
