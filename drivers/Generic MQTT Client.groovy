/**
 *  ****************  Generic MQTT Driver  ****************
 *
 *  importUrl: https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/drivers/Generic%20MQTT%20Client.groovy
 *
 *  Design Usage:
 *  This driver is a generic MQTT driver to pull and post to a MQTT broker.
 *
 *  Copyright 2019 Aaron Ward
 *  
 *  This driver is free and you may do as you likr with it.  
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
 *  1.0.4 - added generic notification and hub event support
 *  1.0.3 - added retained and QOS support
 *  1.0.2 - added support for AppWatchDogv2
 *  1.0.1 - added importURL and updated to new MQTT client methods
 *  1.0.0 - Initial release
 */
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Generic MQTT Driver", namespace: "aaronward", author: "Aaron Ward", importURL: "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/drivers/Generic%20MQTT%20Client.groovy") {
        capability "Initialize"
        capability "Notification"
        command "updateVersion"
	command "publishMsg", ["String"]
	attribute "delay", "number"
	attribute "distance", "number"
	attribute "dwDriverInfo", "string"
	   }

    preferences {
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
    appName = "MQTTGenericDriver"
	version = "1.0.3" 
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

}

def publishMsg(String s) {
    if (logEnable) log.debug "Sent this: ${s} to ${settings?.topicPub} - QOS Value: ${settings?.QOS.toInteger()} - Retained: ${settings?.retained}"
    interfaces.mqtt.publish(settings?.topicPub, s, settings?.QOS.toInteger(), settings?.retained)
}

def deviceNotification(String s) {
    if (logEnable) log.debug "Sent this: ${s} to ${settings?.topicPub} - QOS Value: ${settings?.QOS.toInteger()} - Retained: ${settings?.retained}"
    
    // Attempt to parse message as JSON
    def slurper = new JsonSlurper()
	def parsed = slurper.parseText(s)

    // If this is a hub event message, do something special
	if (parsed.keySet().contains('path') && 
        parsed.keySet().contains('body') &&
        parsed.body.keySet().contains('name') &&
        parsed.body.keySet().contains('type') &&
        parsed.body.keySet().contains('value') &&
        (parsed.path == '/push')) {
          def body = new JsonOutput().toJson(parsed.body)
          interfaces.mqtt.publish("${settings?.topicPub}/push/${parsed.body.name}/${parsed.body.type}/value", parsed.body.value, settings?.QOS.toInteger(), settings?.retained)
	} else {
    // If its not, or json parse fails dump the input string to the topic
        interfaces.mqtt.publish(settings?.topicPub, s, settings?.QOS.toInteger(), settings?.retained)
    }
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
	try {
        if(settings?.retained==null) settings?.retained=false
        if(settings?.QOS==null) setting?.QOS="1"
        //open connection
		mqttbroker = "tcp://" + settings?.MQTTBroker + ":1883"
        interfaces.mqtt.connect(mqttbroker, "hubitat", settings?.username,settings?.password)
        //give it a chance to start
        pauseExecution(1000)
        log.info "Connection established"
		if (logEnable) log.debug "Subscribed to: ${settings?.topicSub}"
        interfaces.mqtt.subscribe(settings?.topicSub)
    } catch(e) {
        if (logEnable) log.debug "Initialize error: ${e.message}"
    }
}

def mqttClientStatus(String status){
    if (logEnable) log.debug "MQTTStatus- error: ${status}"
}

def logsOff(){
    log.warn "Debug logging disabled."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}
