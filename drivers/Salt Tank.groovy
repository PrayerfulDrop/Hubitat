/**
 *  ****************  Salt Tank MQTT Driver  ****************
 *
 *  Design Usage:
 *  This driver is designed to show dynamic icons of salt water tank and will connect to a MQTT broker.
 *
 *  Copyright 2019 Aaron Ward
 *  
 *  This driver is free and you may do as you like with it.  
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
 *  1.0.5 - changes to MQTT reconnection and salt level options
 *  1.0.4 - added additional MQTT client response error checking
 *  1.0.3 - fixed dashboard CSS styling issues
 *  1.0.2 - added switch to remove WATO requirements
 *  1.0.1 - added dynamic dashboard tile
 *  1.0.0 - Modified Generic MQTT Driver
 */

metadata {
    definition (name: "Salt Tank Device", namespace: "aaronward", author: "Aaron Ward", importURL: "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/drivers/Salt%20Tank.groovy") {
        capability "Initialize"
        capability "Switch"
        capability "Actuator"
        command "updateVersion"
	command "publishMsg", ["String"]
	attribute "delay", "number"
	attribute "distance", "number"
    attribute "Salt Level", "number"
	attribute "dwDriverInfo", "string"
    attribute "SaltTile", "string"
        attribute "Notification Perc", "string"
	   }

    preferences {
        input name: "salttank", type: "text", title: "Softner tank Salt Block capacity:", required: true, displayDuringSetup: true, defaultValue: "4"
        input name: "MQTTBroker", type: "text", title: "MQTT Broker Address:", required: true, displayDuringSetup: true
		input name: "username", type: "text", title: "MQTT Username:", description: "(blank if none)", required: false, displayDuringSetup: true
		input name: "password", type: "password", title: "MQTT Password:", description: "(blank if none)", required: false, displayDuringSetup: true
		input name: "topicSub", type: "text", title: "Topic to Subscribe:", description: "Example Topic (topic/device/#)", required: false, displayDuringSetup: true
		input name: "topicPub", type: "text", title: "Topic to Publish:", description: "Topic Value (topic/device/value)", required: false, displayDuringSetup: true
        input name: "QOS", type: "text", title: "QOS Value:", required: false, defaultValue: "1", displayDuringSetup: true
        input name: "retained", type: "bool", title: "Retain message:", required: false, defaultValue: false, displayDuringSetup: true
	    input("logEnable", "bool", title: "Enable logging", required: true, defaultValue: true)
    }

}


def installed() {
    log.info "installed..."
}

def setVersion(){
    appName = "SaltTankDriver"
	version = "1.0.5" 
    dwInfo = "${appName}:${version}"
    sendEvent(name: "dwDriverInfo", value: dwInfo, displayed: true)
}

def updateVersion() {
    log.info "In updateVersion"
    setVersion()
}

// Parse incoming device messages to generate events
def parse(String description) {
	Date date = new Date(); 
	topic = interfaces.mqtt.parseMessage(description).topic
	topic = topic.substring(topic.lastIndexOf("/") + 1)
	payload = interfaces.mqtt.parseMessage(description).payload
	if (logEnable) log.debug topic
	if (logEnable) log.debug payload
	sendEvent(name: "${topic}", value: "${payload}", displayed: true)
	sendEvent(name: "Last Payload Received", value: "Topic: ${topic} - ${date.toString()}", displayed: true)
	state."${topic}" = "${payload}"
	state.lastpayloadreceived = "Topic: ${topic} : ${payload} - ${date.toString()}"
    tileNow()

}

def publishMsg(String s) {
    if (logEnable) log.debug "Sent this: ${s} to ${settings?.topicPub} - QOS Value: ${settings?.QOS.toInteger()} - Retained: ${settings?.retained}"
    interfaces.mqtt.publish(settings?.topicPub, s, settings?.QOS.toInteger(), settings?.retained)
}

def updated() {
    if (logEnable) log.info "Updated..."
    initialize()
}

def uninstalled() {
    if (logEnable) log.info "Disconnecting from mqtt"
    interfaces.mqtt.disconnect()
}


def initialize() {
	if (logEnable) runIn(900,logsOff)
    mqttConnect()

}

def mqttConnect() {
	try {
        if(settings?.retained==null) settings?.retained=false
        if(settings?.QOS==null) setting?.QOS="1"
        //open connection
		mqttbroker = "tcp://" + settings?.MQTTBroker + ":1883"
        interfaces.mqtt.connect(mqttbroker, "hubitat-salttank", settings?.username,settings?.password)
        //give it a chance to start
        pauseExecution(1000)
        //log.info "Connection established"
		if (logEnable) log.debug "Subscribed to: ${settings?.topicSub}"
        interfaces.mqtt.subscribe(settings?.topicSub)
    } catch(e) {
        if (logEnable) log.debug "Initialize error: ${e.message}"
    }
}

def mqttClientStatus(String status) {
    if(!status.contains("succeeded")) {
        try { interfaces.mqtt.disconnect() }
        catch (e) { }
        
        if(logEnable) log.debug "Broker: ${status} Will restart in 5 seconds"
        runIn (5,mqttConnect)  
    }
} 

def logsOff(){
    log.warn "Debug logging disabled."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def on() {sendEvent(name: "switch", value: "on", isStateChange: true)
          runIn(5, off)}
def off() {sendEvent(name: "switch", value: "off", isStateChange: true)}

def tileNow(){ 
    if(salttank==null || salttank=="") salttank="4"
    if(state.distance==null || state.distance=="") state.distance=0
    int saltlevel = ((salttank.toInteger()-1) * 12) - state.distance.toInteger()
    int tank = (salttank.toInteger()-1) * 12
    float perc = 100
    if(saltlevel < tank) {
        perc = (saltlevel/tank*100)
    }
    else perc = perc
    float tanknotify = tank*0.35
    if(logEnable) log.debug "Salt Level: ${saltlevel} Tank: ${tank} Salt Perc: ${perc.round()} - Notify at perc: ${tanknotify.round()}"
    if(saltlevel < (tank * 0.35)) {img = "salt-low.png"
                                   on()}
    if((saltlevel > (tank *0.35)) && (saltlevel < (tank * 0.75))) img = "salt-half.png"
    if(saltlevel > (tank * 0.75)) img = "salt-full.png"                                               
    sendEvent(name: "Salt Level", value: saltlevel, displayed: true)
    state.saltlevel = saltlevel
    img = "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/support/images/${img}"
    html = "<style>img.salttankImage { max-width:80%;height:auto;}div#salttankImgWrapper {width=100%}div#salttankWrapper {font-size:13px;margin: 30px auto; text-align:center;}</style><div id='salttankWrapper'>"
    html += "<div id='salttankImgWrapper'><center><img src='${img}' class='saltankImage'></center></div>"
    html += "Salt Level: ${perc.round()}%</div>"
    sendEvent(name: "Notification Perc", value: tanknotify, displayed: true)
    sendEvent(name: "SaltTile", value: html, displayed: true)
}    
