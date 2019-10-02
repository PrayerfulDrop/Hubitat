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
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *              Donations are always appreciated: https://www.paypal.me/aaronmward
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  Changes:
 *   1.2.1 - fixed current day scheduling bug, minor tweaks and fixes
 *   1.2.0 - fixed scheduling bug
 *   1.1.9 - fixed notifcations for unknown error codes, couple additional bugs discovered in logic 
 *   1.1.8 - added more error traps, error8 - bin issue attempt to restart cleaning, advanced presence options
 *   1.1.7 - fixed bug if unknown error occurs to continue monitoring
 *   1.1.6 - support for dashboard changes in CSS
 *   1.1.5 - full customization of notification messages
 *   1.1.4 - added ability to have multiple Roomba Schedulers 
 *   1.1.3 - reduced device handler complexity, added support for device switch.on/off and options for off
 *   1.1.2 - fixed dead battery logic, added Roomba information page, added specific error codes to notifications, setup and config error checking
 *   1.1.1 - fixed notification options to respect user choice for what is notified
 *   1.1.0 - fixed global variables not being set
 *   1.0.9 - ability to set Roomba 900+ device settings, advanced docking options for non-900+ devices
 *   1.0.8 - determine if Roomba battery has died during docking
 *   1.0.7 - add duration for dashboard tile, minor grammar fixes
 *   1.0.6 - add all messages for dynamic dashboard tile
 *   1.0.5 - added bin full notifications, refined presence handler for additional cleaning scenarios, support for dynamic dashboard tile
 *   1.0.4 - added presence to start/dock roomba
 *   1.0.3 - changed frequency polling based on Roomba event.  Also fixed Pushover notifications to occur no matter how Roomba events are changed
 *   1.0.2 - add ability for advanced scheduling multiple times per day
 *   1.0.1 - added ability for all WiFi enabled Roomba devices to be added and controlled
 *   1.0.0 - Inital concept from Dominick Meglio
**/

def setVersion(){
    // *  V2.0.0 - 08/18/19 - Now App Watchdog compliant
	if(logEnable) log.debug "In setVersion - App Watchdog Parent app code"
    // Must match the exact name used in the json file. ie. YourFileNameParentVersion, YourFileNameChildVersion or YourFileNameDriverVersion
    state.appName = "RoombaSchedulerParentVersion"
	state.version = "1.2.1"
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
    page name: "pageroombaInfo", title: "", install: false, uninstall: false, nextPage: "mainPage"
    page name: "pageroombaNotify", title: "", install: false, uninstall: false, nextPage: "mainPage"
}

def mainPage() {
	dynamicPage(name: "mainPage") {
        section(getFormat("title", "${getImage("Blank")}" + " ${app.label}")) {
				paragraph "<div style='color:#1A77C9'>This application provides Roomba local integration and advanced scheduling.</div>"
			}

        section(getFormat("header-blue", " Rest980/Dorita980 Integration:")){
            if(state.roombaName==null || state.error) paragraph "<b><font color=red>Rest980 Server cannot be reached - check IP Address</b></font>"
			input "doritaIP", "text", title: "Rest980 Server IP Address:", description: "Rest980 Server IP Address:", required: true, submitOnChange: true
			input "doritaPort", "number", title: "Rest980 Server Port:", description: "Dorita Port", required: true, defaultValue: 3000, range: "1..65535"
            if(state.roombaName!=null && state.roombaName.length() > 0) { href "pageroombaInfo", title: "Information about Roomba: ${state.roombaName}", description:"" }
		}
        section(getFormat("header-blue", " Notification Device(s):")) {
		    // PushOver Devices
		     input "pushovertts", "bool", title: "Use 'Pushover' device(s)?", required: false, defaultValue: false, submitOnChange: true 
             if(pushovertts == true) {
                input "pushoverdevice", "capability.notification", title: "PushOver Device", required: true, multiple: true
                input "pushoverStart", "bool", title: "Notifications when Roomba starts cleaning?", required: false, defaultValue:false, submitOnChange: true 
                input "pushoverStop", "bool", title: "Notifications when Roomba stops cleaning?", required: false, defaultValue:false, submitOnChange: true 
                input "pushoverDock", "bool", title: "Notifications when Roomba docks and is charging?", required: false, defaultValue:false, submitOnChange: true 
                paragraph "<hr>"
                input "pushoverBin", "bool", title: "Notifications when Roomba's bin is full?", required: false, defaultValue:false, submitOnChange: true 
                input "pushoverError", "bool", title: "Notifications when Roomba has an error?", required: false, defaultValue:false, submitOnChange: true 
                input "pushoverDead", "bool", title: "Notifications when Roomba's Battery dies?", required: false, defaultValue:false, submitOnChange: true 
                href "pageroombaNotify", title: "Click to change default notification messages from ${state.roombaName}", description: ""
            }
        }
        section(getFormat("header-blue", " Cleaning Schedule:")) { 
            paragraph "Cleaning schedule must be set for at least <u>one day and one time</u>.<br><b>Note:</b> Roomba devices require at least 2 hours to have a full battery.  Consider this when scheduling multiple cleaning times in a day."
            input "schedDay", "enum", title: "Select which days to schedule cleaning:", required: true, multiple: true, submitOnChange: true,
                options: [
			        "0":	"Sunday",
                    "1":	"Monday",
                    "2":	"Tuesday",
                    "3":	"Wednesday",
                    "4":	"Thursday",
                    "5":	"Friday",
                    "6":	"Saturday"
                ] 
            input "timeperday", "text", title: "Number of times per day to clean:", required: true, defaultValue: "1", submitOnChange:true
            if(timeperday==null) timeperday="1"
            if(timeperday.toInteger()<1 || timeperday.toInteger()>10) { paragraph "<b><font color=red>Please enter a value between 1 and 10</b></font><br>"
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
        section(getFormat("header-blue", " Presence Options:")) {
            input "usePresence", "bool", title: "Use presence?", defaultValue: false, submitOnChange: true
            if(usePresence) {
                input "roombaPresence", "capability.presenceSensor", title: "Choose presence device(s):", multiple: true, required: true, submitOnChange: true
                input "roombaPresenceClean", "bool", title: "Immediately start cleaning if everyone leaves (outside of normal schedule)?", defaultValue: false, submitOnChange: true
                input "roombaPresenceDelay", "bool", title: "Delay cleaning schedule if someone is present in home?", defaultValue: false, submitOnChange: true
                if(roombaPresenceDelay) input "roombaPresenceDelayTime", "text", title: "Minutes to enforce cleaning schedule even if people present?", defaultValue: "90", submitOnChange: true
                input "roombaPresenceDock", "bool", title: "Dock Roomba if someone arrives home?", defaultValue: false, submitonChange: true
            }
        }
        section(getFormat("header-blue", " Advanced Options:")) {
            paragraph "Roomba models below 900+ series do not have the ability to find a docking station prior to the battery dying.  Options below provide similar functionality or at least a better chance for Roomba to dock before dying."
            input "useTime", "bool", title: "Limit Roomba's cleaning time?", defaultValue: false, submitOnChange: true
            if(useTime) {
                input "roombaLimitTime", "text", title: "How many minutes to run (minimum 20)?", defaultValue: "60", required: true, submitOnChange: true
                if(roombaLimitTime==null) roombaLimitTime="20"
                if(roombaLimitTime.toInteger() < 20) { paragraph "<b><font color=red>Please enter a greater than 20</b></font><br>"
                            app?.updateSetting("roombaLimitTime",[value:"60",type:"text"])                                     
                }
            }
            input "useBattery", "bool", title: "Have Roomba dock based on battery percentage?", defaultValue: false, submitOnChange: true
            if(useBattery) input "roombaBattery", "enum", title: "What percent to have Roomba begin docking?", defaultValue: "30", required: false, multiple: false, submitOnChange: true,
            options: [
                "40": "40%",
                "30": "30%",
                "20": "20%",
                "10": "10%"]
            input "roombaOff", "enum", title: "Do the following when Roomba's switch is turned 'Off'?", defaultValue: "dock", required: false, multiple: false, submitOnChange: true,
            options: [
                "dock": "Dock",
                "stop": "Stop"
                ]
            
            paragraph "<hr><u>Settings for Roomba 900+ series devices:</u>"
            input "roomba900", "bool", title: "Configure 900+ options?", defaultValue: false, submitOnChange: true
            if(roomba900){
                paragraph "See ${state.roombaName}'s <a href=http://${doritaIP}:3000/map target=_blank>Cleaning Map</a>"
                input "roombacarpetBoost", "enum", title: "Select Carpet Boost option:", required: false, multiple: false, defaultValue: "auto", submitOnChange: true,
                options: [
			        "auto":	"Auto",
                    "performance":	"Performance",
                    "eco":	"Eco"
                ] 
                input "roombaedgeClean", "bool", title: "Set Edge Cleaning (On/Off):", defaultValue: false, submitOnChange: true
                input "roombacleaningPasses", "enum", title: "Select Cleaning Passes option:", required: false, multiple: false, defaultValue: "auto", submitOnChange: true,
                options: [
			        "auto":	"Auto",
                    "one":	"Pass Once",
                    "two":	"Pass Twice"
                ]                 
                input "roombaalwaysFinish", "bool", title: "Set Always Finish Option (On/Off):", defaultValue: false, submitOnChange: true                
            }
        }

        section(getFormat("header-blue", " Logging and Testing:")) { }
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
             //   input "init", "bool", title: "Initialize?", required: false, defaultVale:false, submitOnChange: true // For testing purposes
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

def pageroombaInfo() {
    def result = executeAction("/api/local/info/state")
    def result2 = executeAction("/api/local/config/preferences")
    def cleantime=false
    switch(state.cleaning) {
        case "cleaning":
            img = "roomba-clean.png"
            cleantime = true
            msg = state.cleaning.capitalize()
            break
        case "stopped":
            img = "roomba-stop.png"
            msg = state.cleaning.capitalize()
            break        
        case "charging":
            img = "roomba-charge.png"
            msg = state.cleaning.capitalize()
            break        
        case "docking":
            img = "roombadock.png"
            cleantime = true
            msg = state.cleaning.capitalize()
            break
        case "dead":
            img = "roomba-dead.png"
            msg = "Battery Died"
            break
        case "error":
            img = "roomba-error.png"
            msg = state.cleaning.capitalize()
            break
        case "idle":
            img = "roomba-stop.png"
            msg = state.cleaning.capitalize()
            break
		}
    if(result.data.bin.full) bin="Full"
    else bin="Empty"
    img = "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/Roomba/support/${img}"
    temp = "<div><h2><img max-width=100% height=auto src=${img} border=0>&nbsp;&nbsp;${state.roombaName}</h2>"
    temp += "<p style=font-size:20px><b>Roomba SKU:</b> ${result2.data.sku}</p>"
    temp += "<p style=font-size:20px><b>Roomba MAC:</b> ${result.data.mac}</p>"
    temp += "<p style=font-size:20px><b>Software Version:</b> ${result2.data.softwareVer}</p>"
    temp += "<p style=font-size:20px><b>Current State:</b> ${msg}</p>"
    if(cleantime) temp += "<p style=font-size:20px><b>Elapsed Time:</b> ${result.data.cleanMissionStatus.mssnM} minutes</p>"
    temp += "<p style=font-size:20px><b>Battery Status:</b> ${result.data.batPct}%"
    temp += "<p style=font-size:20px><b>Bin Status:</b> ${bin}"
    temp += "<p style=font-size:20px><b># of cleaning jobs:</b> ${String.format("%,d",result.data.cleanMissionStatus.nMssn)}</p>"
    temp += "<p style=font-size:20px><b>Total Time Cleaning:</b> ${String.format("%,d",result.data.bbrun.hr)} hours and ${result.data.bbrun.min} minutes</p></div>"

	dynamicPage(name: "pageroombaInfo", title: "", nextPage: "mainPage", install: false, uninstall: false) {
        section(getFormat("title", "${getImage("Blank")}" + " ${app.label}")) {}
        section(getFormat("header-green", " Device Information:")) {
            paragraph temp
            paragraph getFormat("line")
		    paragraph "<div style='color:#1A77C9;text-align:center'>Developed by: Aaron Ward<br/>v${state.version}<br><br><a href='https://paypal.me/aaronmward?locale.x=en_US' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br><br>Donations always appreciated!</div>"
        }
    }
}

def pageroombaNotify() {
    dynamicPage(name: "pageroombaNotify", title: "", nextPage: "mainPage", install: false, uninstall: false) {
        section(getFormat("title", "${getImage("Blank")}" + " ${app.label}")) {}
        section(getFormat("header-green", " Notification Messages:")) {
            paragraph "<b><u>Instructions to use variables:</u></b>"
            paragraph "%device% = Roomba device's name<br><hr>"
            //paragraph "%cleaningstatus% = Roomba device's current cleaning status<br><hr>"
            input "pushoverStartMsg", "text", title: "Start Cleaning:", required: false, defaultValue:"%device% has started cleaning", submitOnChange: true 
            input "pushoverStopMsg", "text", title: "Stop Cleaning:", required: false, defaultValue:"%device% has stopped cleaning", submitOnChange: true 
            input "pushoverDockMsg", "text", title: "Docked and Charging::", required: false, defaultValue:"%device% is charging", submitOnChange: true 
            input "pushoverBinMsg", "text", title: "Bin is Full:", required: false, defaultValue:"%device%'s bin is full", submitOnChange: true 
            input "pushoverDeadMsg", "text", title: "Battery dies:", required: false, defaultValue:"%device% battery has died", submitOnChange: true 
            input "pushoverErrorMsg", "text", title: "Error:", required: false, defaultValue:"%device% has stopped because", submitOnChange: true 
            input "pushoverErrorMsg2", "text", title: "Error2 - Both wheels are stuck:", required: false, defaultValue:"both wheels are stuck", submitOnChange: true 
            input "pushoverErrorMsg3", "text", title: "Error3 - Left wheel is stuck:", required: false, defaultValue:"left wheel is stuck", submitOnChange: true 
            input "pushoverErrorMsg4", "text", title: "Error4 - Right wheel is stuck:", required: false, defaultValue:"right wheel is stuck", submitOnChange: true 
            input "pushoverErrorMsg7", "text", title: "Error7 - Bin is missing:", required: false, defaultValue:"cleaning bin is missing", submitOnChange: true 
            input "pushoverErrorMsg16", "text", title: "Error16 - Stuck on object:", required: false, defaultValue:"stuck on an object", submitOnChange: true 
            
        }
    }
}

def initialize() {
    if(usePresence) subscribe(roombaPresence, "presence", presenceHandler)
	if(logEnable) log.debug "Initializing $app.label...unscheduling current jobs."
    setStateVariables()
    unschedule()
    cleanupChildDevices()
	createChildDevices()
    getRoombaSchedule()
    RoombaScheduler()
    updateDevices()
    app.updateLabel("Roomba Scheduler - ${state.roombaName}")
    schedule("0 0 3 ? * * *", setVersion) 
    if (logEnable && logMinutes.toInteger() != 0) {
    if(logMinutes.toInteger() !=0) log.warn "Debug messages set to automatically disable in ${logMinutes} minute(s)."
        runIn((logMinutes.toInteger() * 60),logsOff)
        } else { if(logEnable && logMinutes.toInteger() == 0) {log.warn "Debug logs set to not automatically disable." } }

}

def installed() {
	if(logEnable) log.debug "Installed with settings: ${settings}"
    state.error=false
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

def getRoombaSchedule() {
    def roombaSchedule = []
    for(i=0; i <timeperday.toInteger(); i++) {
                switch (i) {
            case 0:
                temp = day0
                break
            case 1:
                temp = day1
                break   
            case 2:
                temp = day2
                break
            case 3:
                temp = day3
                break
            case 4:
                temp = day4
                break
            case 5:
                temp = day5
                break
            case 6:
                temp = day6
                break
            case 7:
                temp = day7
                break
            case 8:
                temp = day8
                break
            case 9:
                temp = day9
                break          
        }
    roombaSchedule.add(temp)    
    }
    state.roombaSchedule = roombaSchedule.sort()
}

def RoombaScheduler() {
    def map=[
           0:"Sunday",
           1:"Monday",
           2:"Tuesday",
           3:"Wednesday",
           4:"Thursday",
           5:"Friday",
           6:"Saturday"]
        def map2=[
           0:"SUN",
           1:"MON",
           2:"TUE",
           3:"WED",
           4:"THU",
           5:"FRI",
           6:"SAT"]
    def date = new Date()
    current = date.format("HH:mm")
    def day = date.getDay()
    daysofweek = schedDay.join(",")
    foundschedule=false
    def cleaningday = day
    nextcleaning = state.roombaSchedule[0]
    // Check if we clean today
    if(daysofweek.contains(day.toString())) { 
        // Check when next scheduled cleaning time will be
            for(it in state.roombaSchedule) {
                temp = Date.parse("yyyy-MM-dd'T'HH:mm:ss", it).format('HH:mm')
                if((temp > current) && !foundschedule) {
                    nextcleaning = it
                    cleaningday = "*"
                    weekday = "Today: ${map[day]}"
                    foundschedule=true
                }
            }
        if(!foundschedule) {
             tempday = day
             while(!foundschedule) {
                 tempday = tempday + 1
                 if(tempday>7) { tempday=1 }
                 if(daysofweek.contains(tempday.toString())) { 
                     foundschedule=true
                     cleaningday = map2[tempday]
                     weekday = map[tempday]
                 }
             }
        }
      } else { 
        // Check when the next day we are cleaning
        tempday = day
         while(!foundschedule) {
             tempday = tempday + 1
             if(tempday>7) { tempday=1 }
             if(daysofweek.contains(tempday.toString())) { 
                 foundschedule=true
                 cleaningday = map2[tempday]
                 weekday = map[tempday]
             }
         }
        }
    log.info "Next scheduled cleaning: ${weekday} ${Date.parse("yyyy-MM-dd'T'HH:mm:ss", nextcleaning).format('h:mm a')}" 
    schedule("0 ${Date.parse("yyyy-MM-dd'T'HH:mm:ss", nextcleaning).format('mm H')} ? * ${cleaningday} *", RoombaSchedStart) 
}

def RoombaSchedStart() {
    def result = executeAction("/api/local/info/state")
    def device = getChildDevice("roomba:" + result.data.name)
    def presence = getPresence()
    
    // If Delay cleaning is selected
    if(roombaPresenceDelay) {
        if(!state.schedDelay && presence) {
            log.info "Roomba Schedule was initiated but presence is detected.  Waiting ${roombaPresenceDelayTime.toInteger()} minutes before starting."
            def now = new Date()
            long temp = now.getTime()
            state.startDelayTime = temp
            state.schedDelay = true
            runIn(60,RoombaDelay)
            RoombaScheduler()
        } else {
            long timeDiff
   		    def now = new Date()
    	    long unxNow = now.getTime()
    	    unxPrev = state.startDelayTime
    	    unxNow = unxNow/1000
    	    unxPrev = unxPrev/1000
    	    timeDiff = Math.abs(unxNow-unxPrev)
    	    timeDiff = Math.round(timeDiff/60)
            if(logEnable) log.debug "Time delay difference is currently: ${timeDiff.toString()} minute(s)"
            if(timeDiff < roombaPresenceDelayTime.toInteger()) {
                runIn(60,RoombaDelay)
            } else {
                log.info "Delay time has expired.  Starting expired cleaning schedule."
                device.start()
                updateDevices()
                RoombaScheduler()
                state.schedDelay = false
            }
        }
    } 
    // Delay cleaning is not selected
    else { device.start()
           updateDevices() 
           RoombaScheduler()}
}

def RoombaDelay() {
    RoombaSchedStart()
}
                   
                   
// Device creation and status updhandlers
def createChildDevices() {
    try {
    def result = executeAction("/api/local/info/state")

	if (result && result.data)
    {
        if (!getChildDevice("roomba:"+result.data.name))
            addChildDevice("roomba", "Roomba", "roomba:" + result.data.name, 1234, ["name": result.data.name, isComponent: false])
    }
    }
    catch (e) {log.error "Couldn't create child device due to connection issue." }
}

def cleanupChildDevices()
{
    try {
    def result = executeAction("/api/local/info/state")
	for (device in getChildDevices())
	{
		def deviceId = device.deviceNetworkId.replace("roomba:","")
		
        if (result.data.name != deviceId)
            deleteChildDevice(device.deviceNetworkId)
	}
    }
    catch (e) { log.error "Couldn't clean up child devices due to connection issue."}
}

def updateDevices() {
    try {
    def result = executeAction("/api/local/info/state")
    
    if (result && result.data)
    {
        def device = getChildDevice("roomba:" + result.data.name)
        
        device.sendEvent(name: "battery", value: result.data.batPct)
        if (!result.data.bin.present)
            device.sendEvent(name: "bin", value: "missing")
        else if (result.data.bin.full) {
            device.sendEvent(name: "bin", value: "full")
            if(pushoverBin && state.bin) {
                pushNow(state.pushoverBinMsg)
                state.bin = false
            }
        } else{
            device.sendEvent(name: "bin", value: "good")
            state.bin = true
        }
        
		def status = ""
        def msg = null
		switch (result.data.cleanMissionStatus.phase){
			case "hmMidMsn":
			case "hmPostMsn":
			case "hmUsrDock":  
                    if(result.data.batPct == 0) {
                        if(state.batterydead==false) {
   		                        def now = new Date()
                                long temp = now.getTime()
                                state.starttime = temp
                                state.batterydead = true
                                status = "docking"
                        } else {
                            	long timeDiff
   		                        def now = new Date()
    	                        long unxNow = now.getTime()
    	                        unxPrev = state.starttime
    	                        unxNow = unxNow/1000
    	                        unxPrev = unxPrev/1000
    	                        timeDiff = Math.abs(unxNow-unxPrev)
    	                        timeDiff = Math.round(timeDiff/60)
                                if(logEnable) log.debug "Checking how long since battery was at 0%.  Time difference is currently: ${timeDiff.toString()} minute(s)"
                                if(timeDiff > 10) {
                                    status = "dead"
                                    if(pushoverDead) msg=state.pushoverDeadMsg
                                 } else {
                                    status = "docking"                        
                                }
                        }
                    } else {
    	                status = "docking"                        
                        state.batterydead = false 
                }      
				break
			case "charge":
				status = "charging"
                if(pushoverDock) msg=state.pushoverDockMsg
                state.batterydead = false 
				break
			case "run":
				status = "cleaning"
                if(pushoverStart) msg=state.pushoverStartMsg
                state.batterydead = false
                //control Roomba docking based on Time or Battery %
                if(useTime && roombaTime.toInteger() >= result.data.cleanMissionStatus.mssnM.toInteger()) {
                        device.dock()
                }
                if(useBattery && roombaBattery.toInteger() >= result.data.batPct.toInteger()) {
                        device.dock()
                }     
				break
			case "stop":
                if(result.data.cleanMissionStatus.notReady.toInteger() > 0) {
                    status = "error"
                    //log.warn "Roomba notReady code: ${result.data.cleanMissionStatus.notReady}"
                    temp = state.pushoverErrorMsg
                    switch(result.data.cleanMissionStatus.notReady) {
                        case "2":
                            temp += " ${state.pushoverErrorMsg2}"
                            break
                        case "3":
                            temp += " ${state.pushoverErrorMsg3}"
                            break
                        case "4":
                            temp += " ${state.pushoverErrorMsg4}"
                            break
                        case "7":
                            temp += " ${state.pushoverErrorMsg7}"
                            break
                        case "8":
                            temp += " has a bin error.  Attempting to restart cleaning."
                            device.resume
                            pauseExecution(5000)
                            device.resume
                            break
                        case "16":
                            temp += " ${state.pushoverErrorMsg16}"
                            break
                        default:
                            temp = "${state.roombaName} has an unknown error notReady:${result.data.cleanMissionStatus.notReady}"
                    }
                    if(pushoverError) msg=temp
                } else {         
                    status = "idle"
                    if(pushoverStop) msg=state.pushoverStopMsg
                }
				break		
		}
        
        state.cleaning = status
        
        device.sendEvent(name: "cleanStatus", value: status)
        if(logEnable) log.debug "Sending ${status} to ${device} dashboard tile"
        
        device.roombaTile(state.cleaning, result.data.batPct, result.data.cleanMissionStatus.mssnM)
        
        if(logEnable) log.trace "state.cleaning: ${state.cleaning} - state.prevcleaning: ${state.prevcleaning} - state.notified: ${state.notified}"
        
        if(!state.cleaning.contains(state.prevcleaning) && !state.notifed) {
            state.prevcleaning = state.cleaning
            if(!state.notified && msg!=null) {
                state.notified = true
                pushNow(msg)
            }
        } else {
            if(state.cleaning.contains(state.prevcleaning)) {
            state.notified = false
            }
        } 
    }
    }
    catch (e) { if(logEnable) log.error "iRobot cloud error.  Retrying updating devices in 30 seconds." }                   
    runIn(30, updateDevices)
}

def pushNow(msg) {
    // If user selects Pushover notifications then send message
	if (pushovertts) {
            if (logEnable) log.debug "Sending Pushover message: ${msg}"
        try {pushoverdevice.deviceNotification("${msg}")}
        catch (e) {log.error "Pushover device is not selected."}
	}
}

// Handlers

def getPresence() {
    def presence = false
    if(roombaPresence.findAll { it?.currentPresence == "present"}) { presence = true }
    return presence
}


def presenceHandler(evt) {
    try {
    def result = executeAction("/api/local/info/state")

    if (result && result.data)
    {
        def device = getChildDevice("roomba:" + result.data.name)
        def presence = getPresence()
    
        if(presence) {
            // Presence is detected, Roomba is cleaning AND user chooses to have Roomba dock when someone is home
            if(result.data.cleanMissionStatus.phase.contains("run") && roombaPresenceDock) {
                   device.dock()
            }
        } else {
            // Presence is away start cleaning schedule and variations
            // Check if Roomba is docking, if so stop then start cleaning
            if(result.data.cleanMissionStatus.phase.contains("hmUsrDock") && roombaPresenceClean) {
                device.start()
            }
            // Check if Roomba is charging OR is stopped then start cleaning
            if((result.data.cleanMissionStatus.phase.contains("charge") || result.data.cleanMissionStatus.phase.contains("stop")) && roombaPresenceClean) device.start()
            if(roombaPresenceDelay && state.schedDelay) {
                state.schedDelay = false
                device.start
            }
        }
        //update status of Roomba
        updateDevices()
    }
    } 
    catch (e) { log.error "iRobot Cloud communication error." }
}

def handleDevice(device, id, evt) {
    try {
    def device_result = executeAction("/api/local/info/state")
    def result = ""
    switch(evt) {
        case "stop":
            result = executeAction("/api/local/action/stop")
            break
        case "start":
            if(device_result.data.cleanMissionStatus.phase.contains("run") || device_result.data.cleanMissionStatus.phase.contains("hmUsrDock") || device_result.data.batPct.toInteger()<75) 
                { log.warn "${device} is currently cleaning.  Scheduled times may be too close together." }
            else {
                if(device_result.data.cleanMissionStatus.phase.contains("charge") && device_result.data.batPct.toInteger()>40) {
                    if(roomba900) {
                        result = executeAction("/api/local/config/carpetBoost/${roombacarpetBoost}")
                        if(roombaedgeClean) result = executeAction("/api/local/config/edgeClean/on")
                            else result = executeAction("/api/local/config/edgeClean/off")
                        result = executeAction("/api/local/config/cleaningPasses/${roombacleaningPasses}")
                        if(roombaalwaysFinish) result = executeAction("/api/local/config/alwaysFinish/on")
                            else result = executeAction("/api/local/config/alwaysFinish/off")
                    }
                    if(!device_result.data.cleanMissionStatus.phase.contains("run") || !device_result.data.cleanMissionStatus.phase.contains("hmUsrDock")) {
                        result = executeAction("/api/local/action/start")
                    } else {
                        result = executeAction("/api/local/action/stop")
                        result = executeAction("/api/local/action/start")
                    }
                } else log.warn "${device} is currently not on the charging station.  Cannot start cleaning."
            }
            break
        case "resume":
            result = executeAction("/api/local/action/resume")
            break
        case "pause":
            result = executeAction("/api/local/action/pause")
            break
        case "dock":
            if(device_result.data.cleanMissionStatus.phase.contains("run") || device_result.data.cleanMissionStatus.phase.contains("hmUsrDock")) {
                    result = executeAction("/api/local/action/stop")
                    result = executeAction("/api/local/action/dock")
            } else {
                    result = executeAction("/api/local/action/dock")
            }               
            break
        case "off":
            if(roombaOff=="dock") {
                    if(device_result.data.cleanMissionStatus.phase.contains("run") || device_result.data.cleanMissionStatus.phase.contains("hmUsrDock")) {
                        result = executeAction("/api/local/action/stop")
                        result = executeAction("/api/local/action/dock")
                    } else {
                        result = executeAction("/api/local/action/dock")
                    }
            } else result = executeAction("/api/local/action/stop")
          break
    }
    } 
    catch (e) { log.error "iRobot error.  Cannot start action. ${e}" }
}

def setStateVariables() {
    // Ensure variables are set
    def result = executeAction("/api/local/info/state")
    try { state.roombaName = result.data.name}
        catch (e) {state.roombaName = "RoombaUnknown"}
    try {state.pushoverStartMsg = pushoverStartMsg.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverStartMsg = pushoverStartMsg}
    try {state.pushoverStopMsg = pushoverStopMsg.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverStopMsg = pushoverStopMsg}
    try {state.pushoverDockMsg = pushoverDockMsg.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverDockMsg = pushoverDockMsg}
    try {state.pushoverBinMsg = pushoverBinMsg.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverBinMsg = pushoverBinMsg}
    try {state.pushoverDeadMsg = pushoverDeadMsg.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverDeadMsg = pushoverDeadMsg}
    try {state.pushoverErrorMsg = pushoverErrorMsg.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverErrorMsg = pushoverErrorMsg}
    try {state.pushoverErrorMsg2 = pushoverErrorMsg2.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverErrorMsg2 = pushoverErrorMsg2}
    try {state.pushoverErrorMsg3 = pushoverErrorMsg3.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverErrorMsg3 = pushoverErrorMsg3}
    try {state.pushoverErrorMsg4 = pushoverErrorMsg4.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverErrorMsg4 = pushoverErrorMsg4}
    try {state.pushoverErrorMsg7 = pushoverErrorMsg7.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverErrorMsg7 = pushoverErrorMsg7}
    try {state.pushoverErrorMsg16 = pushoverErrorMsg16.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverErrorMsg16 = pushoverErrorMsg16}
    if(state.prevcleaning==null || state.prevcleaning=="") state.prevcleaning = "settings"
    if(state.notified==null) state.notified = false
    if(state.batterydead==null) state.batterydead = false
    if(state.bin==null) state.bin = true
    if(state.schedDelay==null) state.schedDelay = false
    state.schedDelay = false
}

def executeAction(path) {
	def params = [
        uri: "http://${doritaIP}:${doritaPort}",
        path: "${path}",
		contentType: "application/json"
	]
	def result = null
	try
	{
		httpGet(params) { resp ->
			result = resp
		}
        state.error = false
	}
	catch (e) 
	{
        if(path.contains("carpetBoost")) log.warn "Roomba device does not support Carpet Boost options" 
        else if(path.contains("edgeClean")) log.warn "Roomba device does not support Edge Clean options" 
        else if(path.contains("cleaningPasses")) log.warn "Roomba device does not support Cleaning Passes options" 
        else if(path.contains("alwaysFinish")) log.warn "Roomba device does not support Always Finish options" 
            else if(path.contains("state")) { 
                log.error "Rest980 Server not available: $e"
                state.error = true 
            }
	}
	return result
}

//Application Handlers
def getImage(type) {
    def loc = "<img src='https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/Roomba/support/roomba.png'>"
}

def getFormat(type, myText=""){
    if(type == "header-blue") return "<div style='color:#ffffff;font-weight: bold;background-color:#1A7BC7;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}


def logsOff(){
    log.warn "Debug logging disabled."
    app?.updateSetting("logEnable",[value:"false",type:"bool"])
}

//imports
import groovy.time.TimeCategory
import hubitat.helper.RMUtils
