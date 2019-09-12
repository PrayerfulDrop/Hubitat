/**
 *  Roomba Integration
 *
 *  Copyright 2019 Dominick Meglio
 *
 */

definition(
    name: "Roomba Integration",
    namespace: "dcm.roomba",
    author: "Dominick Meglio",
    description: "Connects to Roomba via Dorita",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	page(name: "prefDorita", title: "Dorita Interface")
}

def prefDorita() {
	return dynamicPage(name: "prefDorita", title: "Connect to Dorita/Rest980", uninstall:true, install: true) {
		section("Dorita Information"){
			input("doritaIP", "text", title: "Dorita IP Address", description: "Dorita IP Address", required: true)
			input("doritaPort", "number", title: "Dorita Port", description: "Dorita Port", required: true, defaultValue: 3000, range: "1..65535")
            input("debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false)
		}
	}
}

def installed() {
	logDebug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	logDebug "Updated with settings: ${settings}"
    unschedule()
	initialize()
}

def uninstalled() {
	logDebug "Uninstalled app"

	for (device in getChildDevices())
	{
		deleteChildDevice(device.deviceNetworkId)
	}	
}

def initialize() {
	logDebug "initializing"

	cleanupChildDevices()
	createChildDevices()
    schedule("0/30 * * * * ? *", updateDevices)
}

def createChildDevices() {
    def result = executeAction("/api/local/info/state")

	if (result && result.data)
    {
        if (!getChildDevice("roomba:"+result.data.hwPartsRev.navSerialNo))
            addChildDevice("roomba", "Roomba", "roomba:" + result.data.hwPartsRev.navSerialNo, 1234, ["name": result.data.name, isComponent: false])
    }
}

def cleanupChildDevices()
{
    def result = executeAction("/api/local/info/state")
	for (device in getChildDevices())
	{
		def deviceId = device.deviceNetworkId.replace("roomba:","")
		
        if (result.data.hwPartsRev.navSerialNo != deviceId)
            deleteChildDevice(device.deviceNetworkId)
	}
}

def updateDevices() {
    def result = executeAction("/api/local/info/state")
    
    if (result && result.data)
    {
        def device = getChildDevice("roomba:" + result.data.hwPartsRev.navSerialNo)
        
        device.sendEvent(name: "battery", value: result.data.batPct)
        if (!result.data.bin.present)
            device.sendEvent(name: "consumableStatus", value: "missing")
        else if (result.data.bin.full)
            device.sendEvent(name: "consumableStatus", value: "maintenance_required")
        else
            device.sendEvent(name: "consumableStatus", value: "good")
        
		def status = ""
		switch (result.data.cleanMissionStatus.phase)
		{
			case "hmMidMsn":
			case "hmPostMsn":
			case "hmUsrDock":
				status = "homing"
				break
			case "charge":
				status = "charging"
				break
			case "run":
				status = "cleaning"
				break
			case "stop":
				status = idle
				break		
		}
        device.sendEvent(name: "cleanStatus", value: status)
    }
}

def handleStart(device, id) 
{
    def result = executeAction("/api/local/action/start")
    if (result.data.success == "null")
        device.sendEvent(name: "cleanStatus", value: "cleaning")
}

def handleStop(device, id) 
{
    def result = executeAction("/api/local/action/stop")
    if (result.data.success == "null")
        device.sendEvent(name: "cleanStatus", value: "idle")
}

def handlePause(device, id) 
{
    def result = executeAction("/api/local/action/pause")
    if (result.data.success == "null")
        device.sendEvent(name: "cleanStatus", value: "idle")
}

def handleResume(device, id) 
{
    def result = executeAction("/api/local/action/resume")
    if (result.data.success == "null")
        device.sendEvent(name: "cleanStatus", value: "cleaning")
}

def handleDock(device, id) 
{
    def result = executeAction("/api/local/action/dock")
	if (result.data.success == "null")
        device.sendEvent(name: "cleanStatus", value: "homing")
}

def executeAction(path) {
	def params = [
        uri: "http://${doritaIP}:${doritaPort}",
        path: "${path}",
		contentType: "application/json"
	]
	def result = null
	logDebug "calling action ${path}"
	try
	{
		httpGet(params) { resp ->
			result = resp
		}
	}
	catch (e) 
	{
		log.debug "HTTP Exception Received: $e"
	}
	return result
}

def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}
