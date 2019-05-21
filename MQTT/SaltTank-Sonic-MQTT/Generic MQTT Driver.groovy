metadata {
    definition (name: "Generic MQTT Driver", namespace: "aaronward", author: "Aaron Ward") {
        capability "Initialize"
		
        command "publishMsg", ["String"]


    }

    preferences {
        input name: "MQTTBroker", type: "text", title: "MQTT Broker Address:", required: true, displayDuringSetup: true
		input name: "username", type: "text", title: "MQTT Username:", description: "(blank if none)", required: false, displayDuringSetup: true
		input name: "password", type: "password", title: "MQTT Password:", description: "(blank if none)", required: false, displayDuringSetup: true
		input name: "topicSub", type: "text", title: "Topic to Subscribe:", description: "Example Topic (topic/device/#)", required: false, displayDuringSetup: true
		input name: "topicPub", type: "text", title: "Topic to Publish:", description: "Topic Value (topic/device/value)", required: false, displayDuringSetup: true
    }

}

import static hubitat.helper.InterfaceUtils.alphaV1mqttConnect
import static hubitat.helper.InterfaceUtils.alphaV1mqttDisconnect
import static hubitat.helper.InterfaceUtils.alphaV1mqttSubscribe
import static hubitat.helper.InterfaceUtils.alphaV1mqttUnsubscribe
import static hubitat.helper.InterfaceUtils.alphaV1parseMqttMessage
import static hubitat.helper.InterfaceUtils.alphaV1mqttPublish

def installed() {
    log.warn "installed..."
}

// Parse incoming device messages to generate events
def parse(String description) {
    //log.debug description
   // log.debug alphaV1parseMqttMessage(description)
	topic = alphaV1parseMqttMessage(description).topic
	topic = topic.substring(topic.lastIndexOf("/") + 1)
	payload = alphaV1parseMqttMessage(description).payload
	log.debug topic
	log.debug payload
	sendEvent(name: "${topic}", value: "${payload}", displayed: true)

}

def publishMsg(String s) {
	log.debug "Sent this: ${s} to ${settings?.topicPub}"
    alphaV1mqttPublish(device, settings?.topicPub, s)
}

def updated() {
    log.info "updated..."
    initialize()
}

def uninstalled() {
    log.info "disconnecting from mqtt"
    alphaV1mqttDisconnect(device)
}

def initialize() {
    try {
        //open connection
		mqttbroker = "tcp://" + settings?.MQTTBroker + ":1883"
        alphaV1mqttConnect(device, mqttbroker, "hubitat", settings?.username,settings?.password)
        //give it a chance to start
        pauseExecution(1000)
        log.info "connection established"
		log.debug "Subscribed to: ${settings?.topicSub}"
        alphaV1mqttSubscribe(device, settings?.topicSub)
    } catch(e) {
        log.debug "initialize error: ${e.message}"
    }
}

def mqttClientStatus(String status){
    log.debug "mqttStatus- error: ${status}"
}
