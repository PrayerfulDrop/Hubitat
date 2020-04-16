/**
 *
 * Hubitat Import URL: https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/Roomba/Roomba-app.groovy
 *
 *  ****************  iRobot Scheduler  ****************
 *
 *  Design Usage:
 *  This app is designed to integrate any WiFi enabled Roomba or Braava devices to have direct local connectivity and integration into Hubitat.  This application will create a Roomba/Braava device based on the 
 *  the name you gave your Roomba/Braava device in the cloud app.  With this app you can schedule multiple cleaning times, automate cleaning when presence is away, receive notifications about status
 *  of the Roomba/Braava (stuck, cleaning, died, etc) and also setup continous cleaning mode for non-900 series WiFi Roomba devices.
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
 *   1.3.5 - fixed spelling of "Braava" not "Brava". Added a setting to use a switch to override presence settings (great for a global pandemic!)
 *   1.3.4 - removed AppWatchDog, added last cleaning visibility, added ability to start a Braava device after successful docking/charging
 *   1.3.3 - changed app name to iRobot Scheduler and added contact sensors for cleaning schedule restrictions
 *   1.3.2 - Added basic support for Braava m6 (supports notifications for tank being empty instead of bin being full)
 *   1.3.1 - finalized logic fixes for unique use case scenarios (thx dman2306 for being a patient tester)
 *   1.3.0 - fixed additional logic for unique options set, optimized presence handler
 *   1.2.9 - fixed bug in notifications and battery % start
 *   1.2.8 - lowest battery option for start cleaning, new delay presence options, new day cleaning enforcement, restricted days for cleaning, more error checking, reset application state option, fixed updateDevices scheduling issue
 *   1.2.7 - optimized scheduling code (thanks StepHack!), fixed additional scheduling bugs (thx dman2306)
 *   1.2.6 - fixed i7series result set for Roomba information
 *   1.2.5 - fixed restriction logic so restrictions work, more notification choices, UI updates
 *   1.2.4 - i7 series modifications to dock roomba correctly
 *   1.2.3 - added ability to restrict cleaning based on switch, turn off restricted switch if presence away options
 *   1.2.2 - added additional notification options for errors, add time-delay for notification of errors
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
def version() {
    version = "1.3.4"
    return version
}

definition(
    name: "iRobot Scheduler",
    namespace: "aaronward",
    author: "Aaron Ward",
    description: "Scheduling and local execution of iRobot services",
    category: "Misc",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: " ")

preferences {
	page name: "mainPage", title: "", install: true, uninstall: true
    page name: "pageroombaInfo", title: "", install: false, uninstall: false, nextPage: "mainPage"
    page name: "pageroombaNotify", title: "", install: false, uninstall: false, nextPage: "mainPage"
}

def mainPage() {
    debug=false
	dynamicPage(name: "mainPage") {
        section(getFormat("title", "${getImage("Blank")}" + " ${app.label}")) {
				paragraph "<div style='color:#1A77C9'>This application provides iRobot Roomba and Braava local integration and advanced scheduling.</div>"
			}

        section(getFormat("header-blue", " Rest980/Dorita980 Connectivity:")){
            if(state.roombaName==null || state.error) paragraph "<b><font color=red>Rest980 Server cannot be reached - check IP Address</b></font>"
			input "doritaIP", "text", title: "Rest980 Server IP Address:", description: "Rest980 Server IP Address:", required: true, submitOnChange: true, width: 6
			input "doritaPort", "number", title: "Rest980 Server Port:", description: "Dorita Port", required: true, defaultValue: 3000, width: 6
            if(state.roombaName!=null && state.roombaName.length() > 0) href "pageroombaInfo", title: "Information about my Roomba: ${state.roombaName}", description:""
		}
        section(getFormat("header-blue", " Notification Device(s):")) {
		    // PushOver Devices
		     input "pushovertts", "bool", title: "Use 'Pushover' device(s)?", required: false, defaultValue: false, submitOnChange: true
             if(pushovertts == true) {
                input "pushoverdevice", "capability.notification", title: "PushOver Device(s):", required: true, multiple: true
                input "pushoverStart", "bool", title: "Notify when Roomba starts cleaning?", required: false, defaultValue:false, submitOnChange: true, width: 6
                input "pushoverBin", "bool", title: "Notify when Roomba's bin is full?", required: false, defaultValue:false, submitOnChange: true, width: 6     
				input "pushoverTank", "bool", title: "Notify when Braava's tank is empty?", required: false, defaultValue:false, submitOnChange: true, width: 6  				
                input "pushoverStop", "bool", title: "Notify when Roomba stops cleaning?", required: false, defaultValue:false, submitOnChange: true, width: 6
                input "pushoverDead", "bool", title: "Notify when Roomba's Battery dies?", required: false, defaultValue:false, submitOnChange: true, width: 6 
                input "pushoverDock", "bool", title: "Notify when Roomba is docked and charging?", required: false, defaultValue:false, submitOnChange: true, width: 6
                input "pushoverError", "bool", title: "Notify when Roomba has an error?", required: false, defaultValue:false, submitOnChange: true, width: 6
                href "pageroombaNotify", title: "Change default notification messages from ${state.roombaName}", description: ""
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
            input "timeperday", "text", title: "Number of times per day to clean:", required: true, defaultValue: "1", submitOnChange:true, width: 6
            paragraph "", width: 6
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
                                                        input "day${i}", "time", title: "${proper} time:",required: true, width: 6
                                                   }    
                                                }           
        }
        section(getFormat("header-blue", " Presence Options:")) {
            input "usePresence", "bool", title: "Use presence?", defaultValue: false, submitOnChange: true
            if(usePresence) {
                input "roombaPresence", "capability.presenceSensor", title: "Choose presence device(s):", multiple: true, required: true, submitOnChange: true
                input "roombaPresenceClean", "bool", title: "Immediately start cleaning if everyone leaves (outside of normal schedule)?", defaultValue: false, submitOnChange: true
                if(roombaPresenceCleandelay==null || roombaPresenceCleandelay=="") roombaPresenceCleandelay="5"
                if(roombaPresenceClean) {
                    input "roombaPresenceCleandelay", "text", title: "Delay this many minutes prior to cleaning? (0-1440)", defaultValue: 5, range:"0-1440", submitOnChange: true
                    if(roombaPresenceCleandelay.toInteger()<0 || roombaPresenceCleandelay.toInteger()>1440) paragraph "<font style='color:red'><b>Error:</b> Please enter number of minutes between 0 and 1440.</font>"   
                }
                input "roombaPresenceDock", "bool", title: "Dock Roomba if someone arrives home?", defaultValue: false, submitonChange: true                
                if(roombaPresenceDelay) paragraph getFormat("wordline", "Delay Cleaning when presence home:") 
                input "roombaPresenceDelay", "bool", title: "If presence is home, delay the cleaning schedule?", defaultValue: false, submitOnChange: true
                if(roombaPresenceDelayTime==null || roombaPresenceDelayTime=="" ) roombaPresenceDelayTime = "90"
                if(roombaPresenceDelay) { 
                    input "roombaPresenceDelayTime", "text", title: "Minutes to delay cleaning schedule if people present? (0-1440)", defaultValue: 90, range:"0-1440", submitOnChange: true
                    if(roombaPresenceDelayTime.toInteger()<0 || roombaPresenceDelayTime.toInteger()>1440) paragraph "<font style='color:red'><b>Error:</b> Please enter number of minutes between 0 and 1440.</font>"                  
                    input "roombaDelayDay", "bool", title: "If presence still present, cancel cleaning but reschedule for next schedule cleaning time/day?", defaultValue: false, submitOnChange: true
                    if(roombaDelayDay) {
                        input "roombaafterday", "bool", title: "Would you like to reschedule a cleaning for any day (even outside of cleaning schedule) if cleaning cancelled for more than a certain number of days?", defaultValue: false, submitOnChange: true
                        if(roombaafterday) {
                            input "roombaaftertimeday", "text", title: "Number of days that can be missed before a cleaning must occur?", defaultValue: 0, submitOnChange: true
                            input "roombarestrictSched", "enum", title: "Day(s) that should be restricted from rescheduling the missed cleaning after ${roombaaftertimeday} days occur?", required: false, multiple: true, submitOnChange: true,
                                options: [
        	        		        "0":	"Sunday",
                                    "1":	"Monday",
                                    "2":	"Tuesday",
                                    "3":	"Wednesday",
                                    "4":	"Thursday",
                                    "5":	"Friday",
                                    "6":	"Saturday"
                                ] 
                        }
                    }
                }
				input "roombaIgnorePresenceSwitch", "capability.switch", title: "Ignore presence settings if the following switch is turned on"
            }
        }
        section(getFormat("header-blue", " Advanced Options:")) {
            paragraph "Roomba models below 900+ series do not have the ability to find a docking station prior to the battery dying.  Options below provide similar functionality or at least a better chance for Roomba to dock before dying."
            input "roombaBatteryLevel", "enum", title: "Lowest battery percentage Roomba is allowed to start cleaning?", defaultValue:"60", required:false, multiple: false, submitOnChange:true,
                options: [
                    "80":"80%",
                    "70":"70%",
                    "60":"60%",
                    "50":"50%",
                    "40":"40%"]                   
            input "useTime", "bool", title: "Limit Roomba's cleaning time?", defaultValue: false, submitOnChange: true, width: 6
            if(useTime) {
                input "roombaLimitTime", "text", title: "How many minutes to run (minimum 20)?", defaultValue: "60", required: true, submitOnChange: true
                if(roombaLimitTime==null) roombaLimitTime="20"
                if(roombaLimitTime.toInteger() < 20) { paragraph "<b><font color=red>Please enter a number greater than 20</b></font><br>"
                            app?.updateSetting("roombaLimitTime",[value:"60",type:"text"])
                }
            }
            
            input "useBattery", "bool", title: "Have Roomba dock based on battery percentage?", defaultValue: false, submitOnChange: true
            if(useBattery) {
            input "roombaBattery", "enum", title: "What percent to have Roomba begin docking?", defaultValue: "30", required: false, multiple: false, submitOnChange: true, 
            options: [
                "40": "40%",
                "30": "30%",
                "20": "20%",
                "10": "10%"]
            }
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
            paragraph "<hr><u>Settings for Braava devices:</u>"
            input "BraavaYes", "bool", title: "Start Braava(s) after iRobot is done cleaning?", required: false, defaultValue: false, submitOnChange: true
            if(BraavaYes) input "BraavaDevice", "capability.switch", title: "Select Braava robot(s) to turn on after iRobot docks:", required: false, multiple: true, defaultValue: null, submitOnChange: true
            
        }
        
        section(getFormat("header-blue", " Logging and Restrictions:")) { }   
            section() {
                input "modesYes", "bool", title: "Enable restrictions?", required: true, defaultValue: false, submitOnChange: true
                if(modesYes) { 
                    input "restrictbySwitch", "capability.switch", title: "Use a switch to restrict cleaning schedule:", required: false, multiple: false, defaultValue: null, submitOnChange: true
                    input "restrictbyContact", "capability.contactSensor", title: "Use a contact sensor(s) to restrict cleaning schedule:", required: false, multiple: true, defaultValue: null, submitOnChange: true
                    input "pushoverRestrictions", "bool", title: "Send Pushover Msg if restrictions are on and Roomba tries to clean?", required: false, defaultValue:false, submitOnChange: true, width: 6
                }
                if(modesYes && usePresence) input "turnoffSwitch", "bool", title: "Turn off switch if presence away?", required: false, multiple: false, defaultValue: false, submitOnChange: true, width: 6
                paragraph "Note: resetting the application state settings should only be used if you are experiencing issues with Roomba starting correctly."
                input "resetApp", "bool", title: "Reset application state settings?", required: false, defaultValue:false, submitOnChange: true
                input "logEnable", "bool", title: "Debug Logging?", required: false, defaultValue: true, submitOnChange: true
                if(logEnable) input "logMinutes", "text", title: "Log for the following number of minutes (0=logs always on):", required: false, defaultValue:15, submitOnChange: true
                if(debug) input "init", "bool", title: "Initialize?", required: false, defaultVale:false, submitOnChange: true // For testing purposes
                if(app.init) {
                    log.debug "Initalizing button clicked."
                    try {
                        initialize()
                    }
                    catch (any) { log.error "${any}" }
                    app?.updateSetting("init",[value:"false",type:"bool"])
                }
                if(app.resetApp) {
                    app?.updateSetting("resetApp",[value:"false",type:"bool"])
                    resetApp()
                    paragraph "<font style='color:green;'><b>Success!</b></font> Roomba Scheduler's application state settings have been reset."
                }
                if(debug) {
                    paragraph "<hr>state.schedDelay: ${state.schedDelay} - state.lastcleaning: ${state.lastcleaning} - state.presence: ${state.presence}<br>state.errors: ${state.errors} - state.prevcleaning: ${state.prevcleaning} - state.DaysSinceLastCleaning: ${state.DaysSinceLastCleaning}"
                }
				paragraph getFormat("line")
				paragraph "<div style='color:#1A77C9;text-align:center'>Developed by: Aaron Ward<br/>v${version()}<br><br><a href='https://paypal.me/aaronmward?locale.x=en_US' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br><br>Donations always appreciated!</div>"
			}
	}
}

def pageroombaInfo() {
    def result = executeAction("/api/local/info/state")
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
    if(result.data?.bin?.full) bin="Full"
    else bin="Empty"
    img = "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/Roomba/support/${img}"
    temp = "<div><h2><img max-width=100% height=auto src=${img} border=0>&nbsp;&nbsp;${state.roombaName}</h2>"
    temp += "<p style=font-size:20px><b>Roomba SKU:</b> ${result.data.sku}</p>"
	if (result.data.mac != null)
		temp += "<p style=font-size:20px><b>Roomba MAC:</b> ${result.data.mac}</p>"
	else if (result.data.hwPartsRev?.wlan0HwAddr != null)
		temp += "<p style=font-size:20px><b>Roomba MAC:</b> ${result.data.hwPartsRev?.wlan0HwAddr}</p>"
    temp += "<p style=font-size:20px><b>Software Version:</b> ${result.data.softwareVer}</p>"
    temp += "<p style=font-size:20px><b>Current State:</b> ${msg}</p>"
    if(cleantime) temp += "<p style=font-size:20px><b>Elapsed Time:</b> ${result.data.cleanMissionStatus.mssnM} minutes</p>"
    temp += "<p style=font-size:20px><b>Battery Status:</b> ${result.data.batPct}%"
	if (result.data.bin != null)
		temp += "<p style=font-size:20px><b>Bin Status:</b> ${bin}"
	else if (result.data.tankLvl != null)
		temp += "<p style=font-size:20px><b>Tank Level:</b> ${result.data.tankLvl}%"
    temp += "<p style=font-size:20px><b># of cleaning jobs:</b> ${String.format("%,d",result.data.cleanMissionStatus.nMssn)}</p>"
    temp += "<p style=font-size:20px><b>Total Time Cleaning:</b> ${String.format("%,d",result.data.bbrun.hr)} hours and ${result.data.bbrun.min} minutes</p>"
    temp += "<p style=font-size:20px><b>Last cleaning occured on:</b> ${state.lastcleaningcycle}</p>"
    temp += "<p style=font-size:20px><b>Days since last cleaning:</b> ${String.format("%,d", state.DaysSinceLastCleaning)}</p></div>"


           
	dynamicPage(name: "pageroombaInfo", title: "", nextPage: "mainPage", install: false, uninstall: false) {
        section(getFormat("title", "${getImage("Blank")}" + " ${app.label}")) {
        paragraph "<div style='color:#1A77C9'>This application provides Roomba local integration and advanced scheduling.</div>"
        }
        section(getFormat("header-blue", " Device Information:")) {
            paragraph temp
            paragraph getFormat("line")
		    paragraph "<div style='color:#1A77C9;text-align:center'>Developed by: Aaron Ward<br/>v${version()}<br><br><a href='https://paypal.me/aaronmward?locale.x=en_US' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br><br>Donations always appreciated!</div>"
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
			input "pushoverTankMsg", "text", title: "Tank is Empty:", required: false, defaultValue:"%device%'s tank is empty", submitOnChange: true 
            input "pushoverDeadMsg", "text", title: "Battery dies:", required: false, defaultValue:"%device% battery has died", submitOnChange: true 
            input "pushoverErrorMsg", "text", title: "Error:", required: false, defaultValue:"%device% has stopped because", submitOnChange: true 
            input "pushoverErrorMsg2", "text", title: "Error2 - Both wheels are stuck:", required: false, defaultValue:"both wheels are stuck", submitOnChange: true 
            input "pushoverErrorMsg3", "text", title: "Error3 - Left wheel is stuck:", required: false, defaultValue:"left wheel is stuck", submitOnChange: true 
            input "pushoverErrorMsg4", "text", title: "Error4 - Right wheel is stuck:", required: false, defaultValue:"right wheel is stuck", submitOnChange: true 
            input "pushoverErrorMsg5", "text", title: "Error5 - Roomba is wedged under something:", required: false, defaultValue: "it is wedged under something", submitOnChange: true
            input "pushoverErrorMsg7", "text", title: "Error7 - Bin is missing:", required: false, defaultValue:"cleaning bin is missing", submitOnChange: true 
            input "pushoverErrorMsg16", "text", title: "Error16 - Stuck on object:", required: false, defaultValue:"stuck on an object", submitOnChange: true 
            
        }
    }
}

def getRoombaSchedule() {
    def roombaSchedule = []
    for(i=0; i <timeperday.toInteger(); i++) {
         roombaSchedule.add(Date.parse("yyyy-MM-dd'T'HH:mm:ss", app."day${i}").format('HH:mm'))                      
    }
    state.roombaSchedule = roombaSchedule.sort()
}

def RoombaScheduler(delayday) {
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
    if(roombarestrictSched!=null) restricteddaysofweek = roombarestrictSched.join(",")
    else restricteddaysofweek = []
    foundschedule=false
    def cleaningday = day
    nextcleaning = state.roombaSchedule[0]
    // Check if we clean today
    if(delayday) {
        tempday = day
         while(!foundschedule) {
             tempday = tempday + 1
             if(tempday>7) { tempday=1 }
             if(!restricteddaysofweek.contains(tempday.toString())) { 
                 foundschedule=true
                 cleaningday = map2[tempday]
                 weekday = map[tempday]
             }
         }        
    }
        
    if(daysofweek.contains(day.toString()) && !foundschedule) { 
        // Check when next scheduled cleaning time will be
            for(it in state.roombaSchedule) {
                if((it > current) && !foundschedule) {
                    nextcleaning = it
                    cleaningday = "*"
                    weekday = "Today"
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
    log.info "Next scheduled cleaning: ${weekday} at ${Date.parse("HH:mm", nextcleaning).format('h:mm a')}" 
    schedule("0 ${Date.parse("HH:mm", nextcleaning).format('mm H')} ? * ${cleaningday} *", RoombaSchedStart) 
}

def RoombaSchedStart() {
    def result = executeAction("/api/local/info/state")
    def device = getChildDevice("roomba:" + result.data.name)
    def presence = getPresence()
    
    
    // If Delay cleaning is selected
    if(debug) log.debug "Current variables:  roombaPresenceDelay: ${roombaPresenceDelay} - presence: ${presence} - state.presence: ${state.presence}"
    if(((roombaPresenceDelay && presence) || state.presence) && (roombaIgnorePresenceSwitch == null || roombaIgnorePresenceSwitch.currentValue("switch") == "off")) {
        if(debug) log.debug "roomba PresenceDelay or Presence leave values equal true"
        
        if(state.presence==true) timer = roombaPresenceCleandelay
        else timer = roombaPresenceDelayTime 
        
        if(!state.schedDelay && state.startDelayTime==null) {
            if(roombaPresenceDelay && state.presence) log.info "Presence has departed with delay start.  Waiting ${timer} minute(s) before starting cleaning"
            else log.info "Roomba Schedule was initiated but presence is detected.  Waiting ${timer} minutes before starting"
            def now = new Date()
            long temp = now.getTime()
            state.startDelayTime = temp
            state.schedDelay = true
            runIn(60,RoombaDelay)
            RoombaScheduler(false)
        } else { 
            if(state.startDelayTime==null || state.startDelayTime == "") { 
                log.warn "Application state is inaccurate.  Initializing state variables."
                initialize() 
            } else {
                long timeDiff
   		        def now = new Date()
    	        long unxNow = now.getTime()
    	        unxPrev = state.startDelayTime
    	        unxNow = unxNow/1000
    	        unxPrev = unxPrev/1000
    	        timeDiff = Math.abs(unxNow-unxPrev)
    	        timeDiff = Math.round(timeDiff/60)
                if(logEnable) log.debug "Time delay difference is currently: ${timeDiff.toString()} of ${timer} minute(s)"
                if(timeDiff <= timer.toInteger()-1) {
                    runIn(60,RoombaDelay)
                } else {
                    if(roombaDelayDay && state.DaysSinceLastCleaning.toInteger()>roombaaftertimeday.toInteger()-1) {
                        RoombaScheduler(true)
                    } else {
                        RoombaScheduler(false)
                        if(roombaDelayDay) log.debug "Delay time has expired, skip cleaning is selected due to presence is home.  Current days since last cleaning: ${state.DaysSinceLastCleaning}"
                        else { 
                                log.info "Delay time has expired.  Starting expired cleaning schedule"
                                device.start()
                             }
                    }
                    updateDevices()
                    state.schedDelay = false
                    state.presence = false
                    state.startDelayTime=null
                }
            }
        }
    } 
    // Delay cleaning is not selected
    else { 
           if(debug) log.debug "RoombaDelay or Immediate Presence values false...starting Roomba normal cleaning schedule"
           if(logEnable) "Starting Roomba normal cleaning schedule" 
           device.start()
           updateDevices() 
           RoombaScheduler(false)
    }
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
		if (result.data.bin != null)
		{
			if (!result.data.bin.present) device.sendEvent(name: "bin", value: "missing")
			if (result.data.bin.full) {
				device.sendEvent(name: "bin", value: "full")
				if(pushoverBin && state.sendBinNotification) {
					state.sendBinNotification = false
					pushNow(state.pushoverBinMsg)
				}
			} else {
				device.sendEvent(name: "bin", value: "good")
				state.sendBinNotification = true
			}
		}
		else if (result.data.tankLvl != null)
		{
			if (result.data.detectedPad.contains("Wet"))
			{			
				if (!result.data.mopReady.tankPresent) 
					device.sendEvent(name: "tank", value: "missing")
				else if (result.data.tankLvl == 0) {
					device.sendEvent(name: "tank", value: "empty")
					if(pushoverTank && state.sendTankNotification) {
						state.sendTankNotification = false
						pushNow(state.pushoverTankMsg)
					}
				}
				else {
					device.sendEvent(name: "tank", value: "good")
					state.sendTankNotification = true
				}
			}
		}
        
		def status = state.prevcleaning
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
                        state.errors = false
                }      
				break
			case "charge":
				status = "charging"
                if(pushoverDock) msg=state.pushoverDockMsg
                state.batterydead = false 
                state.errors = false
                if(!state.docked) {
                    if(BraavaYes) BraavaDevice.on()
                    state.docked = true             
                 }
                // working on
                long daystimeDiff = 0                
   		        def daynow = new Date()
	            long dayunxNow = daynow.getTime()
    	        dayunxPrev = state.lastcleaning
    	        dayunxNow = dayunxNow/1000/60/60/24
    	        dayunxPrev = dayunxPrev/1000/60/60/24
    	        daytimeDiff = Math.abs(dayunxNow-dayunxPrev)
                daytimeDiff = daytimeDiff.trunc()
    	        daytimeDiff = Math.round(daytimeDiff)
                state.DaysSinceLastCleaning = daytimeDiff    
				break
			case "run":
                state.docked=false
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
                state.errors = false
				break
			case "stop":
                status = state.prevcleaning
                if(result.data.cleanMissionStatus.notReady.toInteger() > 0) {
                    if(state.errors==false && state.notified==false) {
   		                        def errornow = new Date()
                                long errortemp = errornow.getTime()
                                state.errorstarttime = errortemp
                                state.errors = true
                        if(logEnable) log.warn "Detected possible cleaning error with ${state.roombaName}"
                        } else {
                            if(!state.cleaning.contains("error") && state.errors) {
                            	long errortimeDiff = 0
   		                        def errornow = new Date()
    	                        long errorunxNow = errornow.getTime()
    	                        errorunxPrev = state.errorstarttime
    	                        errorunxNow = errorunxNow/1000
    	                        errorunxPrev = errorunxPrev/1000
    	                        errortimeDiff = Math.abs(errorunxNow-errorunxPrev)
    	                        errortimeDiff = Math.round(errortimeDiff/60)
                                if(logEnable) log.warn "Checking how long since error detected.  Time difference is currently: ${errortimeDiff.toString()} minute(s)"
                                if(errortimeDiff > 5) status = "error"
                            }
                   }
                   if(status.contains("error")) {
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
                           case "5":
                               temp += "${stat.pushoverErrorMsg5}"
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
                                break
                       }
                       if(pushoverError) msg = temp
                       status = "error"
                    }
                } else {         
                    status = "idle"
                    state.errors = false
                    if(pushoverStop) msg=state.pushoverStopMsg
                }
				break		
		}
        if(debug) log.trace "Before: state.cleaning: '${state.cleaning}'  state.prevcleaning: '${state.prevcleaning}'  state.notified: '${state.notified}'"
        state.cleaning = status

        device.sendEvent(name: "cleanStatus", value: status)
        if(debug) log.trace "Sending '${status}' to ${device} dashboard tile"
        device.roombaTile(state.cleaning, result.data.batPct, result.data.cleanMissionStatus.mssnM)

        if(!state.notified && !state.cleaning.contains(state.prevcleaning)) {
            if(msg!=null) {
                state.notified = true
                pushNow(msg)
            }
            state.prevcleaning = state.cleaning
        } else state.notified = false
    }
    }
    catch (e) { if(logEnable) log.error "iRobot cloud error.  ${e} "
                if(logEnable) log.warn "Retrying updating devices in 30 seconds." }                   
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

def switchHandler(evt) {
    if(evt.value == "on") {
        def result = executeAction("/api/local/info/state")
        def device = getChildDevice("roomba:" + result.data.name)
        if(result.data.cleanMissionStatus.phase.contains("run")) {
            pushNow("Restriction switch turned on while ${state.roombaName} was cleaning. Sending ${state.roombaName} home to dock.")   
            device.dock()
        }
    }
}

def presenceHandler(evt) {   
    try {
		if (roombaIgnorePresenceSwitch?.currentValue("switch") == "on") {
			if(logEnable) log.info "Presence arrived but override switch is on"
			return
		}
			
        def result = executeAction("/api/local/info/state")

        if (result && result.data) {
            def device = getChildDevice("roomba:" + result.data.name)
            def presence = getPresence()          
            state.presence = false
        
            // Dock Roomba if presence is true and roombaPresenceDock is true
            if(presence && result.data.cleanMissionStatus.phase.contains("run") && roombaPresenceDock) {
                if(logEnable) log.info "Docking ${state.roombaName} based on presence options"
                state.startDelayTime = null
                state.schedDelay = false   
                device.dock()
                RoombaScheduler(false) 
            } 
        
            // Reset restriction switch based on presence away and turnoffSwitch is true
            if(!presence && turnoffSwitch && restrictbySwitch.currentState("switch").value == "on") {
                log.info "Restriction switch '${restrictbySwitch.displayName} is ${restrictbySwitch.currentState("switch").value}.  Presence away, turning off ${restrictbySwitch.displayName}"
                restrictbySwitch.off
            }

            // If roombaPresenceClean is true start cleaning based on a delay
            if(!presence && roombaPresenceClean && result.data.cleanMissionStatus.phase.contains("charge") || result.data.cleanMissionStatus.phase.contains("stop")) {
                if(logEnable) log.info "Presence cleaning option is selected AND presence has departed."
                state.presence = true
                state.startDelayTime = null
                state.schedDelay = false   
                RoombaDelay()
            }
            
            // if RoombaPresenceDelay is true start cleaning based if presence departs and cleaning schedule still valid
            if(!presence && roombaPresenceDelay && state.schedDelay) {
                if(logEnable) log.info "RoombaPresenceDelay is true AND presence has departed."
                    state.schedDelay = false  
                    state.startDelayTime = null
                    device.start() 
                    RoombaScheduler(false) 
            }    

            //update status of Roomba
            updateDevices()
        }
    } 
catch (e) { log.error "iRobot communication error. ${e}" }
}

def getContacts() {
    def contacts = false
    if(restrictbyContact.findAll { it?.currentContact == "open"}) { contacts = true }
    return contacts
}

def handleDevice(device, id, evt) {
    try {
    def restrict = (modesYes && ((restrictbySwitch !=null && restrictbySwitch.currentState("switch").value == "on") || (restrictbyContact !=null && getContacts())) ) ? true : false
    def device_result = executeAction("/api/local/info/state")
    def result = ""
    switch(evt) {
        case "stop":
            result = executeAction("/api/local/action/stop")
            break
        case "start":
            if(!restrict) {
                if(device_result.data.cleanMissionStatus.phase.contains("run") || device_result.data.cleanMissionStatus.phase.contains("hmUsrDock") || device_result.data.batPct.toInteger()<roombaBatteryLevel.toInteger()) 
                    { log.warn "${device} was currently cleaning.  Scheduled times may be too close together." }
                else {
                    if(device_result.data.cleanMissionStatus.phase.contains("charge") && device_result.data.batPct.toInteger()>roombaBatteryLevel.toInteger()) {
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
                            setLastCycle()
                        } else {
                            result = executeAction("/api/local/action/pause")
                            pauseExecution(1500)
                            result = executeAction("/api/local/action/start")
                            setLastCycle()
                        }
                    } else log.warn "${device} is currently not on the charging station.  Cannot start cleaning."
                }
            } else { if(device_result.data.cleanMissionStatus.phase.contains("run")) {
                         log.warn "Cleaning schedule for ${state.roombaName} is currently restricted.  Turn off '${restrictbySwitch.displayName}'"
                         if(pushoverRestrictions) pushNow("Current cleaning for ${state.roombaName} has been restricted.  Sending ${state.roombaName} to dock.")
                         result = executeAction("/api/local/action/pause")
                         pauseExecution(1500)
                         result = executeAction("/api/local/action/dock")
                     } else if(pushoverRestrictions) pushNow("Cleaning schedule for ${state.roombaName} is currently restricted.  Turn off '${restrictbySwitch.displayName}' to resume cleaning schedule.")
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
                    result = executeAction("/api/local/action/pause")
                    pauseExecution(1500)
                    result = executeAction("/api/local/action/dock")
            } else {
                    result = executeAction("/api/local/action/dock")
            }               
            break
        case "off":
            if(roombaOff=="dock") {
                    if(device_result.data.cleanMissionStatus.phase.contains("run") || device_result.data.cleanMissionStatus.phase.contains("hmUsrDock")) {
                        result = executeAction("/api/local/action/pause")
                        pauseExecution(1500)
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

def setLastCycle() {
    def now = new Date()
    state.lastcleaningcycle=now.format("MM/dd/YYYY h:mm a")
    app.updateLabel("iRobot Scheduler - ${state.roombaName} - <span style='color:green'>Last Cleaning: ${state.lastcleaningcycle}</span>")
    state.lastcleaning=now.getTime()
    
}

def setStateVariables() {
    // Ensure variables are set
    def result = executeAction("/api/local/info/state")
   pushoverStartMsg="%device% has started cleaning"
   pushoverStopMsg ="%device% has stopped cleaning"
   pushoverDockMsg="%device% is charging"
   pushoverBinMsg="%device%'s bin is full"
   pushoverTankMsg="%device%'s tank is empty"
   pushoverDeadMsg="%device% battery has died"
   pushoverErrorMsg="%device% has stopped because"
   pushoverErrorMsg2="both wheels are stuck"
   pushoverErrorMsg3="left wheel is stuck"
   pushoverErrorMsg4="right wheel is stuck"
   pushoverErrorMsg5="it is wedged under something"
   pushoverErrorMsg7="cleaning bin is missing"
   pushoverErrorMsg16="stuck on an object"
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
    try {state.pushoverTankMsg = pushoverTankMsg.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverTankMsg = pushoverTankMsg}
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
    try {state.pushoverErrorMsg4 = pushoverErrorMsg5.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverErrorMsg4 = pushoverErrorMsg5}    
    try {state.pushoverErrorMsg7 = pushoverErrorMsg7.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverErrorMsg7 = pushoverErrorMsg7}
    try {state.pushoverErrorMsg16 = pushoverErrorMsg16.replace("%device%",state.roombaName)}
        catch (e) {state.pushoverErrorMsg16 = pushoverErrorMsg16}
    if(state.prevcleaning==null || state.prevcleaning=="") state.prevcleaning = "idle"
    state.notified = false
    if(state.batterydead==null) state.batterydead = false
    if(state.sendBinNotification==null) state.sendBinNotification = true
	if(state.sendTankNotification==null) state.sendTankNotification = true
    if(state.schedDelay==null) state.schedDelay = false
    state.errors = false
    if(state.lastcleaning==null) {
        def now = new Date()
        long nowtemp = now.getTime()
        state.lastcleaning=nowtemp
    }
    state.presence = false
    state.startDelayTime=null
}

def resetApp(){
    if(logEnable) log.warn "Application state variables have been reset."
    state.schedDelay = false
    state.batterydead = false
    state.notified = false
    state.presence = false
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
    def loc = "<img src='https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/Roomba/support/roomba-clean.png'>"
}

def getFormat(type, myText=""){
    if(type == "header-blue") return "<div style='color:#ffffff;font-weight: bold;background-color:#1A7BC7;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
    if(type == "wordline") return "<font style='font-weight:bold;font-size:14px;color:#1A77C9;border-bottom:1px solid #1A77C9; padding-bottom:3px;width:100%;float:left;'>${myText}</font>"
    
}


def logsOff(){
    log.warn "Debug logging disabled."
    app?.updateSetting("logEnable",[value:"false",type:"bool"])
}

// Start-up stuff

def initialize() {
    if(usePresence) subscribe(roombaPresence, "presence", presenceHandler)
    if(modesYes) subscribe(restrictbySwitch, "switch", switchHandler)
	log.info "Initializing $app.label...scheduling jobs."
    setStateVariables()
    cleanupChildDevices()
	createChildDevices()
    getRoombaSchedule()
    RoombaScheduler(false)
    updateDevices()
    schedule("0/30 * * * * ?", updateDevices)
    app.updateLabel("iRobot Scheduler - ${state.roombaName}")
    if (logEnable && logMinutes.toInteger() != 0) {
    if(logMinutes.toInteger() !=0) log.warn "Debug messages set to automatically disable in ${logMinutes} minute(s)."
        runIn((logMinutes.toInteger() * 60),logsOff)
        } else { if(logEnable && logMinutes.toInteger() == 0) {log.warn "Debug logs set to not automatically disable." } }

}

def installed() {
	log.info "Installed with settings: ${settings}"
    state.error=false
	initialize()
}

def updated() {
	log.info "Updated with settings: ${settings}"
	initialize()
}

def uninstalled() {
	log.warn "Uninstalled app"

	for (device in getChildDevices())
	{
		deleteChildDevice(device.deviceNetworkId)
	}	
}

//imports
import groovy.time.TimeCategory
import hubitat.helper.RMUtils
