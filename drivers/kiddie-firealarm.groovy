/**
 *  GOControl Contact Sensor:Smoke/Carbon Monoxide Detector
 * 
 *  importUrl: "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/drivers/kiddie-firealarm.groovy"
 *
 *  Copyright 2019 Aaron Ward
 *
 *  Original code ported and modified for hubitat from: https://github.com/luder888/SmartThingsPublic/blob/master/devicetypes/luder888/zwave-door-window-sensor-as-smoke-detector.src/zwave-door-window-sensor-as-smoke-detector.groovy
 *
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
 *  Changes:
 *  1.0.2 - added support for Carbon Monoxide capabilities
 *  1.0.1 - added AppWatchDogv2 support
 *  1.0.0 - initial code port and modifications
 */

metadata {
	definition (
            name: "GOControl Contact Sensor:Smoke/Carbon Monoxide Detector", 
            namespace: "aaronward", 
            author: "Aaron Ward",
            importUrl: "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/drivers/kiddie-firealarm.groovy"
     ) {
		
        //capability "Contact Sensor"
            capability "Smoke Detector"
		    capability "Sensor"
		    capability "Battery"
		    capability "Configuration"
            capability "CarbonMonoxideDetector"
		    attribute "dwDriverInfo", "string"
		    command "updateVersion"
	}
	preferences() {    
            input("smokedetection", "bool", title: "Use as Smoke Detector? (False = Carbon Monoxide Settings)", required: true, defaultValue: true)
            input("logEnable", "bool", title: "Enable logging", required: true, defaultValue: false)
    }    

	// simulator metadata
	simulator {
		// status messages
		status "detected":  "command: 2001, payload: FF"
		status "clear": "command: 2001, payload: 00"
		status "wake up": "command: 8407, payload: "
	}
}

def setVersion(){
    appName = "KiddieFireAlarmDriver"
	version = "1.0.2" 
    dwInfo = "${appName}:${version}"
    sendEvent(name: "dwDriverInfo", value: dwInfo, displayed: true)
}

def updateVersion() {
    log.info "In updateVersion"
    setVersion()
}

def parse(String description) {
	def result = null
	if (description.startsWith("Err 106")) {
		if (state.sec) {
			if(logEnable) log.debug description
		} else {
			result = createEvent(
				descriptionText: "This sensor failed to complete the network security key exchange.",
				eventType: "ALERT",
				name: "secureInclusion",
				value: "failed",
				isStateChange: true,
			)
		}
	} else if (description != "updated") {
		def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x30: 1, 0x31: 5, 0x80: 1, 0x84: 1, 0x71: 3, 0x9C: 1])
		if (cmd) {
        	if(logEnable) log.debug "valid command"
			result = zwaveEvent(cmd)
		}
	}
	if(logEnable) log.debug "parsed '$description' to $result"
	return result
}

def updated() {
    if (logEnable) {
        log.debug "Logs enabled. Logs will be disabled in 15 minutes."
        runIn(900,logsOff)
    }
	def cmds = []
	if (!state.MSR) {
		cmds = [
			command(zwave.manufacturerSpecificV2.manufacturerSpecificGet()),
			"delay 1200",
			zwave.wakeUpV1.wakeUpNoMoreInformation().format()
		]
	} else if (!state.lastbat) {
		cmds = []
	} else {
		cmds = [zwave.wakeUpV1.wakeUpNoMoreInformation().format()]
	}
	response(cmds)
    
}

def configure() {
	commands([
		zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
		zwave.batteryV1.batteryGet()
	], 6000)
}

def logsOff(){
    log.warn "Debug logging disabled."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def sensorValueEvent(value) {
    if(smokedetection) {
        if (value) {
    		createEvent(name: "smoke", value: "detected", descriptionText: "$device.displayName detected smoke")
    	} else {
    		createEvent(name: "smoke", value: "clear", descriptionText: "$device.displayName is clear (no smoke)")
    	}
    } else {
        if (value) {
    		createEvent(name: "carbonMonoxide", value: "detected", descriptionText: "$device.displayName detected Carbon Monoxide")
    	} else {
    		createEvent(name: "carbonMonoxide", value: "clear", descriptionText: "$device.displayName is clear (no Carbon Monoxide)")
    	}
    }
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd)
{
	sensorValueEvent(cmd.value)
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd)
{
	sensorValueEvent(cmd.value)
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
	sensorValueEvent(cmd.value)
}

def zwaveEvent(hubitat.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
	sensorValueEvent(cmd.sensorValue)
}

def zwaveEvent(hubitat.zwave.commands.sensoralarmv1.SensorAlarmReport cmd)
{
	sensorValueEvent(cmd.sensorState)
}

def zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd)
{
	def result = []
	if (cmd.notificationType == 0x06 && cmd.event == 0x16) {
		result << sensorValueEvent(1)
	} else if (cmd.notificationType == 0x06 && cmd.event == 0x17) {
		result << sensorValueEvent(0)
	} else if (cmd.notificationType == 0x07) {
		if (cmd.v1AlarmType == 0x07) {  // special case for nonstandard messages from Monoprice door/window sensors
			result << sensorValueEvent(cmd.v1AlarmLevel)
		} else if (cmd.event == 0x01 || cmd.event == 0x02) {
			result << sensorValueEvent(1)
		} else if (cmd.event == 0x03) {
			result << createEvent(descriptionText: "$device.displayName covering was removed", isStateChange: true)
			if(!state.MSR) result << response(command(zwave.manufacturerSpecificV2.manufacturerSpecificGet()))
		} else if (cmd.event == 0x05 || cmd.event == 0x06) {
			result << createEvent(descriptionText: "$device.displayName detected glass breakage", isStateChange: true)
		} else if (cmd.event == 0x07) {
			if(!state.MSR) result << response(command(zwave.manufacturerSpecificV2.manufacturerSpecificGet()))
			result << createEvent(name: "motion", value: "active", descriptionText:"$device.displayName detected motion")
		}
	} else if (cmd.notificationType) {
		def text = "Notification $cmd.notificationType: event ${([cmd.event] + cmd.eventParameter).join(", ")}"
		result << createEvent(name: "notification$cmd.notificationType", value: "$cmd.event", descriptionText: text, displayed: false)
	} else {
		def value = cmd.v1AlarmLevel == 255 ? "active" : cmd.v1AlarmLevel ?: "inactive"
		result << createEvent(name: "alarm $cmd.v1AlarmType", value: value, displayed: false)
	}
	result
}

def zwaveEvent(hubitat.zwave.commands.wakeupv1.WakeUpNotification cmd)
{
	def event = createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)
	def cmds = []
	if (!state.MSR) {
		cmds << command(zwave.manufacturerSpecificV2.manufacturerSpecificGet())
		cmds << "delay 1200"
	}
	if (!state.lastbat || now() - state.lastbat > 53*60*60*1000) {
		cmds << command(zwave.batteryV1.batteryGet())
	} else {
		cmds << zwave.wakeUpV1.wakeUpNoMoreInformation().format()
	}
	[event, response(cmds)]
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "${device.displayName} has a low battery"
		map.isStateChange = true
	} else {
		map.value = cmd.batteryLevel
	}
	state.lastbat = now()
	[createEvent(map), response(zwave.wakeUpV1.wakeUpNoMoreInformation())]
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	def result = []

	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	log.debug "msr: $msr"
	updateDataValue("MSR", msr)

	retypeBasedOnMSR()

	result << createEvent(descriptionText: "$device.displayName MSR: $msr", isStateChange: false)

	if (msr == "011A-0601-0901") {  // Enerwave motion doesn't always get the associationSet that the hub sends on join
		result << response(zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId))
	} else if (!device.currentState("battery")) {
		if (msr == "0086-0102-0059") {
			result << response(zwave.securityV1.securityMessageEncapsulation().encapsulate(zwave.batteryV1.batteryGet()).format())
		} else {
			result << response(command(zwave.batteryV1.batteryGet()))
		}
	}

	result
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1, 0x25: 1, 0x30: 1, 0x31: 5, 0x80: 1, 0x84: 1, 0x71: 3, 0x9C: 1])
	// log.debug "encapsulated: $encapsulatedCommand"
	if (encapsulatedCommand) {
		state.sec = 1
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(hubitat.zwave.Command cmd) {
	createEvent(descriptionText: "$device.displayName: $cmd", displayed: false)
}

private command(hubitat.zwave.Command cmd) {
	if (state.sec == 1) {
		zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	} else {
		cmd.format()
	}
}

private commands(commands, delay=200) {
	delayBetween(commands.collect{ command(it) }, delay)
}

def retypeBasedOnMSR() {
	switch (state.MSR) {
		case "0086-0002-002D":
			if(logEnable) log.debug "Changing device type to Z-Wave Water Sensor"
			setDeviceType("Z-Wave Water Sensor")
			break
		case "011F-0001-0001":  // Schlage motion
		case "014A-0001-0001":  // Ecolink motion
		case "014A-0004-0001":  // Ecolink motion +
		case "0060-0001-0002":  // Everspring SP814
		case "0060-0001-0003":  // Everspring HSP02
		case "011A-0601-0901":  // Enerwave ZWN-BPC
			if(logEnable) log.debug "Changing device type to Z-Wave Motion Sensor"
			setDeviceType("Z-Wave Motion Sensor")
			break
		case "013C-0002-000D":  // Philio multi +
			if(logEnable) log.debug "Changing device type to 3-in-1 Multisensor Plus (SG)"
			setDeviceType("3-in-1 Multisensor Plus (SG)")
			break
		case "0109-2001-0106":  // Vision door/window
			if(logEnable) log.debug "Changing device type to Z-Wave Plus Door/Window Sensor"
			setDeviceType("Z-Wave Plus Door/Window Sensor")
			break
		case "0109-2002-0205": // Vision Motion
			if(logEnable) log.debug "Changing device type to Z-Wave Plus Motion/Temp Sensor"
			setDeviceType("Z-Wave Plus Motion/Temp Sensor")
			break
	}
}
