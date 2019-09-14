/**
 *
 * Hubitat Import URL: https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/Roomba/Roomba-app.groovy
 *
 *  ****************  Roomba Scheduler  ****************
 *
 *  Design Usage:
 *  This app is designed to integrate any WiFi enabled Roomba devices to have direct local connectivity and integration into Hubitat.  This applicatin will create a Roomba device based on the 
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
 *   1.0.2 - add ability for advanced scheduling multiple times per day
 *   1.0.1 - added ability for all WiFi enabled Roomba devices to be added and controlled
 *   1.0.0 - Inital concept from Dominick Meglio
**/

def setVersion(){
    // *  V2.0.0 - 08/18/19 - Now App Watchdog compliant
	if(logEnable) log.debug "In setVersion - App Watchdog Parent app code"
    // Must match the exact name used in the json file. ie. YourFileNameParentVersion, YourFileNameChildVersion or YourFileNameDriverVersion
    state.appName = "RoombaSchedulerParentVersion"
	state.version = "1.0.2"
    if(awDevice) {
    try {
        if(sendToAWSwitch && awDevice) {
            awInfo = "${state.appName}:${state.version}"
		    awDevice.sendAWinfoMap(awInfo)
            if(logEnable) log.debug "In setVersion - Info was sent to App Watchdog"
	    }
    } catch (e) { log.error "In setVersion - ${e}" }
    }
}

definition(
    name: "Roomba Scheduler",
    namespace: "aaronward",
    author: "Aaron Ward",
    description: "Scheduling and local execution of Roomba services",
    category: "Misc",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {
	dynamicPage(name: "mainPage") {
        section(getFormat("title", "${getImage("Blank")}" + " ${app.label}")) {
				paragraph "<div style='color:#1A77C9'>This application provides Roomba local integration and advanced scheduling.</div>"
			}
		section(getFormat("header-green", " General")) {
       			label title: "Custom Application Name (for multiple instances of Roomba Scheduler):", required: false
			}
        
		section(getFormat("header-green", " Rest980/Dorita980 Integration")){
			input("doritaIP", "text", title: "Rest980 Server IP Address:", description: "Rest980 Server IP Address:", required: true)
			//input("doritaPort", "number", title: "Dorita Port", description: "Dorita Port", required: true, defaultValue: 3000, range: "1..65535")
		}
        section(getFormat("header-green", " Notification Device(s)")) {
		    // PushOver Devices
		     input "pushovertts", "bool", title: "Use 'Pushover' device(s)?", required: false, defaultValue: false, submitOnChange: true 
             if(pushovertts == true) {
                input "pushoverdevice", "capability.notification", title: "PushOver Device", required: true, multiple: true
                input "notifyStart", "bool", title: "Notifications when Roomba starts cleaning?", required: false, multiple:false, defaultValue:false
                input "notifyDone", "bool", title: "Notifications when Roomba finishes cleaning?", required: false, multiple:false, defaultValue:false
               // input "notifyStuck", "bool", title: "Notifications if stuck?", required: false, multiple:false, defaultValue:false
               // input "notifyBin", "bool", title: "Notifications when Roomba needs maintenance?", required: false, multiple:false, defaultValue:false
            }
        }
        section(getFormat("header-green", " Cleaning Schedule")) { 
            paragraph "<b>Note:</b> Roomba devices require at least 3 hours to have a full battery.  Consider this when scheduling multiple cleaning times in a day."
            input "schedDay", "enum", title: "Select which days to schedule cleaning:", required: true, multiple: true, submitOnChange: true,
                options: [
			        "SUN":	"Sunday",
                    "MON":	"Monday",
                    "TUE":	"Tuesday",
                    "WED":	"Wednesday",
                    "THU":	"Thursday",
                    "FRI":	"Friday",
                    "SAT":	"Saturday"
                ] 
            input "timeperday", "text", title: "Number of times per day to clean:", required: true, defaultValue: "1", submitOnChange:true
            if(timeperday==null) timeperday="1"
            if(timeperday.toInteger()<1 || timeperday.toInteger()>10) { paragraph "<b>Please enter a value between 1 and 10</b><br>"
                                          } else {    for(i=0; i < timeperday.toInteger(); i++) {
                                                        switch (i) {
                                                            case 0:
                                                                proper="First"
                                                                break
                                                            case 1:
                                                                proper="Second"
                                                                break
                                                            case 2:
                                                                proper="Third"
                                                                break
                                                            case 3:
                                                                proper="Fourth"
                                                                break
                                                            case 4:
                                                                proper="Fifth"
                                                                break
                                                            case 5:
                                                                proper="Sixth"
                                                                break
                                                            case 6:
                                                                proper="Seventh"
                                                                break
                                                            case 7:
                                                                proper="Eighth"
                                                                break
                                                            case 8:
                                                                proper="Nineth"
                                                                break
                                                            case 9:
                                                                proper="Tenth"
                                                                break
                                                        }
                                                        input "day${i}", "time", title: "${proper} time:",required: true
                                                   }    
                                                }
        
        }
        section(getFormat("header-green", " Logging and Testing")) { }
            // ** App Watchdog Code **
        section("This app supports App Watchdog 2! Click here for more Information", hideable: true, hidden: true) {
				paragraph "<b>Information</b><br>See if any compatible app needs an update, all in one place!"
                paragraph "<b>Requirements</b><br> - Must install the app 'App Watchdog'. Please visit <a href='https://community.hubitat.com/t/release-app-watchdog/9952' target='_blank'>this page</a> for more information.<br> - When you are ready to go, turn on the switch below<br> - Then select 'App Watchdog Data' from the dropdown.<br> - That's it, you will now be notified automaticaly of updates."
                input(name: "sendToAWSwitch", type: "bool", defaultValue: "false", title: "Use App Watchdog to track this apps version info?", description: "Update App Watchdog", submitOnChange: "true")
			}
            if(sendToAWSwitch) {
                section(" App Watchdog 2") {    
                    if(sendToAWSwitch) input(name: "awDevice", type: "capability.actuator", title: "Please select 'App Watchdog Data' from the dropdown", submitOnChange: true, required: true, multiple: false)
			        if(sendToAWSwitch && awDevice) setVersion()
                }
            }
            // ** End App Watchdog Code **
    
        section() {
             	input "logEnable", "bool", title: "Enable Debug Logging?", required: false, defaultValue: true, submitOnChange: true
                if(logEnable) input "logMinutes", "text", title: "Log for the following number of minutes (0=logs always on):", required: false, defaultValue:15, submitOnChange: true
                input "init", "bool", title: "Initialize?", required: false, defaultVale:false, submitOnChange: true
            if(init) {
                try {
                    initialize()
                }
                catch (any) { log.error "${any}" }
                app?.updateSetting("init",[value:"false",type:"bool"])
            }
				paragraph getFormat("line")
				paragraph "<div style='color:#1A77C9;text-align:center'>Developed by: Aaron Ward<br/>v${state.version}<br><br><a href='https://paypal.me/aaronmward?locale.x=en_US' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br><br>Donations always appreciated!</div>"
			}
	}
}

def getImage(type) {
    def loc = "<img src='https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/Roomba/support/roomba.png'>"
}

def getFormat(type, myText=""){
    if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#1A7BC7;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}


def logsOff(){
    log.warn "Debug logging disabled."
    app?.updateSetting("logEnable",[value:"false",type:"bool"])
}


def installed() {
	if(logEnable) log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	if(logEnable) log.debug "Updated with settings: ${settings}"
    unschedule()
    setVersion()
	initialize()
}

def uninstalled() {
	if(logEnable) log.debug "Uninstalled app"

	for (device in getChildDevices())
	{
		deleteChildDevice(device.deviceNetworkId)
	}	
}

def RoombaSchedule() {
    daysofweek = schedDay.join(",")
    for(i=0; i < timeperday.toInteger(); i++) {
        hours = Date.parse("yyyy-MM-dd'T'HH:mm:ss", day0).format('HH mm')
        if(logEnable) log.debug "Scheduling job at ${Date.parse("yyyy-MM-dd'T'HH:mm:ss", day0).format('hh:mma')} for the following days of the week: ${daysofweek}"
        log.debug "schedule(0 ${hours} * * ${daysofweek}, device.start())"
    }
}

def initialize() {
	if(logEnable) log.debug "Initializing $app.label...unscheduling current jobs."
    unschedule()
	cleanupChildDevices()
	createChildDevices()
    RoombaSchedule()
    schedule("0/30 * * * * ? *", updateDevices)
    schedule("0 0 3 ? * * *", setVersion) 
    if (logEnable && logMinutes.toInteger() != 0) {
    if(logMinutes.toInteger() !=0) log.warn "Debug messages set to automatically disable in ${logMinutes} minute(s)."
        runIn((logMinutes.toInteger() * 60),logsOff)
        } else { if(logEnable && logMinutes.toInteger() == 0) {log.warn "Debug logs set to not automatically disable." } }

}

def createChildDevices() {
    def result = executeAction("/api/local/info/state")

	if (result && result.data)
    {
        if (!getChildDevice("roomba:"+result.data.name))
            addChildDevice("roomba", "Roomba", "roomba:" + result.data.name, 1234, ["name": result.data.name, isComponent: false])
    }
}

def cleanupChildDevices()
{
    def result = executeAction("/api/local/info/state")
	for (device in getChildDevices())
	{
		def deviceId = device.deviceNetworkId.replace("roomba:","")
		
        if (result.data.name != deviceId)
            deleteChildDevice(device.deviceNetworkId)
	}
}

def updateDevices() {
    def result = executeAction("/api/local/info/state")
    
    if (result && result.data)
    {
        def device = getChildDevice("roomba:" + result.data.name)
        
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
    if(doritaPort==null) def doritaPort="3000"
	def params = [
        uri: "http://${doritaIP}:${doritaPort}",
        path: "${path}",
		contentType: "application/json"
	]
	def result = null
	if(logEnable) log.debug "Querying current state: ${path}"
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

