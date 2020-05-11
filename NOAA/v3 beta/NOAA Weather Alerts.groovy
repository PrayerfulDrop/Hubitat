//** NOAA Weather Alerts 3.0 **//

def version() {
    version = "3.0.0"
    return version
}

import groovy.transform.Field
import groovy.json.*
import java.util.regex.*
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

definition(
    name:"NOAA Weather Alerts",
    namespace: "aaronward",
    author: "Aaron Ward",
    description: "NOAA Weather Alerts Application ",
    category: "Weather",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	documentationLink: "https://github.com/PrayerfulDrop/Hubitat/blob/master/NOAA/README.md")

preferences { 
    page name: "mainPage", title: "", install: true, uninstall: false
    page name: "NotificationPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
    page name: "ConfigPage", title: "", install: false, uninstall: false, nextPage: "mainPage"
    page name: "AdvConfigPage", title: "", install: false, uninstall: false, nextPage: "mainPage"
    page name: "RestrictionsPage", title: "", install: false, uninstall: false, nextPage: "mainPage"
    page name: "SettingsPage", title: "", install: false, uninstall: true, nextPage: "mainPage"
    
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        installCheck()
        if(state.appInstalled == 'COMPLETE'){
            section(UIsupport("logo","")) {
                href(name: "NotificationPage", title: "Setup Notification Device(s)", required: false, page: "NotificationPage", description: "Select TTS and PushOver Devices")
			    href(name: "ConfigPage", title: "Weather Alert Settings", required: false, page: "ConfigPage", description: "Change default settings for weather alerts to monitor")
			    href(name: "AdvConfigPage", title: "Advanced Alert Settings", required: false, page: "AdvConfigPage", description: "Add additional detailed weather settings to monitor")
			    href(name: "RestrictionsPage", title: "Restrictions", required: false, page: "RestrictionsPage", description: "Setup restriction options")
                href(name: "SettingsPage", title: "Settings", required: false, page: "SettingsPage", description: "Modify NOAA Weather Alerts Application Settings")
                paragraph UIsupport("line","")
                paragraph UIsupport("footer","")
            }                
        }        
    }
}

def NotificationPage() {
    dynamicPage(name: "NotificationPage") {
        section(UIsupport("logo","")) {
            paragraph UIsupport("header", " Setup Notification Device(s)")
				// PushOver Devices
			    input "pushovertts", "bool", title: "Use 'Pushover' device(s)?", required: false, defaultValue: false, submitOnChange: true 
			    if(pushovertts == true){ input "pushoverdevice", "capability.notification", title: "PushOver Device", required: true, multiple: true}
			     
				// Music Speakers (Sonos, etc)
				input(name: "musicmode", type: "bool", defaultValue: "false", title: "Use Music Speaker(s) for TTS?", description: "Music Speaker(s)?", submitOnChange: true)
				if (musicmode) input "musicspeaker", "capability.musicPlayer", title: "Choose speaker(s)", required: false, multiple: true, submitOnChange: true
				
				// Speech Speakers
				input(name: "speechmode", type: "bool", defaultValue: "false", title: "Use Google or Speech Speaker(s) for TTS?", description: "Speech Speaker(s)?", submitOnChange: true)
   	            if (speechmode) input "speechspeaker", "capability.speechSynthesis", title: "Choose speaker(s)", required: false, multiple: true, submitOnChange: true
						
				// Echo Speaks devices
				input (name: "echoSpeaks2", type: "bool", defaultValue: "false", title: "Use Echo Speaks device(s) for TTS?", description: "Echo Speaks device?", submitOnChange: true)
					 if(echoSpeaks2 == true) input "echospeaker", "capability.musicPlayer", title: "Choose Echo Speaks Device(s)", required: false, multiple: true, submitOnChange: true 
				
				// Master Volume settings
				input "speakervolume", "number", title: "Notification Volume Level:", description: "0-100%", required: false, defaultValue: "75", submitOnChange: true
				input "speakervolRestore", "number", title: "Restore Volume Level:", description: "0-100", required: false, defaultValue: "60", submitOnChange: true
				
				// Switch to set when alert active
				input (name: "alertSwitch", type: "capability.switch", title: "Switch to turn ON with Alert? (optional)", required: false, defaultValue: false, submitOnChange: true)

        }
    }
}

def ConfigPage() {
    dynamicPage(name: "ConfigPage") {
        section(UIsupport("logo","")) {
            paragraph UIsupport("header", " Alert Settings")
			input name: "whatAlertSeverity", type: "enum", title: "Weather Severity to monitor: ", 
				options: [
						"minor": "Minor",
						"moderate": "Moderate", 
						"severe": "Severe", 
						"extreme": "Extreme"], required: true, multiple: true, defaultValue: "Severe"
			input name: "whatPoll", type: "enum", title: "Poll Frequency: ", options: ["1": "1 Minute", "5": "5 Minutes", "10": "10 Minutes", "15": "15 Minutes"], required: true, multiple: false, defaultValue: "5 Minutes"
			input "repeatYes", "bool", title: "Repeat Alert?", require: false, defaultValue: false, submitOnChange: true
			if(repeatYes) {
                input name:"repeatTimes", type: "text", title: "Number of times to repeat the alert?", require: false, defaultValue: "1", submitOnChange:true
                input name:"repeatMinutes", type: "text", title: "Number of minutes between each repeating alert?", require: false, defaultValue: "15", submitOnChange:true 
            }
			input name: "useCustomCords", type: "bool", title: "Use Custom Coordinates?", require: false, defaultValue: false, submitOnChange: true
			if(useCustomCords) {
			    paragraph "Below coordinates are acquired from your Hubitat Hub.  Enter your custom coordinates:"
				input name:"customlatitude", type:"text", title: "Latitude coordinate:", require: false, defaultValue: "${location.latitude}", submitOnChange: true
				input name:"customlongitude", type:"text", title: "Longitude coordinate:", require: false, defaultValue: "${location.longitude}", submitOnChange: true
			}
			input name:"useAlertIntro", type: "bool", title: "Use a pre-notification message for TTS device(s)?", require: false, defaultValue: false, submitOnChange: true
			if(useAlertIntro) input name:"AlertIntro", type: "text", title: "Alert pre-notification message:", require: false, defaultValue:"Attention, Attention"              
            input name: "alertCustomMsg", type: "text", title: "Custom Alert Message (use customization instructions):", require: false, defaultValue: "{alertseverity} Weather Alert for the following counties: {alertarea} {alertheadline} This is the end of this Weather Announcement.", submitOnChange: true
        }	
        section("Alert Message Customization Instructions:", hideable: true, hidden: true) {
          	paragraph "<b>Alert message variables:</b>"
		   	paragraph "{alertseverity} = alertseverity"
    		paragraph "{alertcertainty} = alert certainty of occuring"
    		paragraph "{alerturgency} = alert urgency"
    		paragraph "{alertevent} = alert event type"
	    	paragraph "{alertheadline} = alert headline"
			paragraph "{alertdescription} = alert description"
		   	paragraph "{alertinstruction} = alert instructions"
		    paragraph "{alertarea} = counties, cities or area"	
		    paragraph " "
		    paragraph "<b>Example:</b>{alertseverity} weather alert. Certainty is {alertcertainty}. Urgency is {alerturgency}. {alertheadline}. {alertinstruction}. This is the end of the weather announcement."
		}            
    }
}

def AdvConfigPage() {
    dynamicPage(name: "AdvConfigPage") {
        section(UIsupport("logo","")) {
            paragraph UIsupport("header", " Advanced Alert Settings")
            paragraph "Use with caution as below settings may cause undesired results.  Reference <a href='https://www.weather.gov/documentation/services-web-api?prevfmt=application%2Fcap%2Bxml/default/get_alerts#/default/get_alerts' target='_blank'>Weather.gov API</a> and use the API response test button below to determine your desired results."
			input "myWeatherAlert", "enum", title: "Watch for a specific Weather event(s)?", required: false, multiple: true, submitOnChange: true,
                  options: [
							"BZW":	"Blizzard Warning",
                            "CFA":	"Coastal Flood Watch",
                            "CFW":	"Coastal Flood Warning",
                            "DSW":	"Dust Storm Warning",
                            "EWW":	"Extreme Wind Warning",
                            "FFA":	"Flash Flood Watch",
                            "FFW":	"Flash Flood Warning",
                            "FLA":	"Flood Watch",
                            "FLW":	"Flood Warning",
                            "HWA":	"High Wind Watch",
                            "HWW":	"High Wind Warning",
                            "HUA":	"Hurricane Watch",
	                        "HUW":	"Hurricane Warning",
                            "SVA":	"Severe Thunderstorm Watch",
                            "SVR":	"Severe Thunderstorm Warning",
                            "SQW":	"Snow Squall Warning",
                            "SMW":	"Special Marine Warning",
                            "SSA":	"Storm Surge Watch",
                            "SSW":	"Storm Surge Warning",
                            "TOA":	"Tornado Watch",
                            "TOR":	"Tornado Warning",
                            "TRA":	"Tropical Storm Watch",
                            "HUA":	"Tropical Storm Warning",   
							"TSA":	"Tsunami Watch",
                            "TSW":	"Tsunami Warning",
                            "WSA":	"Winter Storm Watch",
                            "WSW":	"Winter Storm Warning"
                        ]
			input name: "whatAlertUrgency", type: "enum", title: "Watch for a specific Alert Urgency: ", multiple: true, submitOnChange: true, 
						options: [
								"immediate": "Immediate", 
								"expected": "Expected",
								"future": "Future"
                        ]
				
			input name: "whatAlertCertainty", type: "enum", title: "Watch for a specific Alert Certainty: ", required: false, multiple: true, submitOnChange: true,
						options: [
								"possible": "Possible",
								"likely": "Likely",
								"observed": "Observed"
                        ]
			}
    }
}

def RestrictionsPage() {
    dynamicPage(name: "RestrictionsPage") {
        section(UIsupport("logo","")) {
            paragraph UIsupport("header", " Restrictions")
 			input "modesYes", "bool", title: "Enable restriction of notifications by current mode(s)?", required: true, defaultValue: false, submitOnChange: true	
            if(modesYes) input(name:"modes", type: "mode", title: "Restrict notifications when current mode is:", multiple: true, required: false, submitOnChange: true)
            input "switchYes", "bool", title: "Restrict notifications using a switch?", required: true, defaultValue: false, submitOnChange: true
            if(switchYes) input "restrictbySwitch", "capability.switch", title: "Use a switch to restrict notfications?", required: false, multiple: false, defaultValue: null, submitOnChange: true
            input "modeSeverityYes", "bool", title: "Ignore restrictions for certain severity types?", required: true, defaultValue: false, submitOnChange: true	          
            if(pushovertts) input "pushoverttsalways ", "bool", title: "Enable Pushover notifications even when restricted?", required: false, defaultValue: false, submitOnChange: true
            if (modeSeverityYes) input name: "modeSeverity", type: "enum", title: "Severity option(s) that will ignore restrictions: ", 
		        options: [
						"minor": "Minor",
						"moderate": "Moderate", 
						"severe": "Severe", 
						"extreme": "Extreme"], required: true, multiple: true, defaultValue: "Severe"             
    	}           
    }
}

def SettingsPage() {
    dynamicPage(name: "SettingsPage") {
        section(UIsupport("logo","")) {
            paragraph UIsupport("header", " Settings")
       			label title: "Custom Application Name (for multiple instances of NOAA):", required: false            
				input "runTest", "bool", title: "Run a test Alert?", required: false, defaultValue: false, submitOnChange: true
				if(runTest) {
					app?.updateSetting("runTest",[value:"false",type:"bool"])
                    runtestAlert()
				}
 				input "logEnable", "bool", title: "Enable Debug Logging?", required: false, defaultValue: true, submitOnChange: true
                if(logEnable) input "logMinutes", "text", title: "Log for the following number of minutes (0=logs always on):", required: false, defaultValue:15, submitOnChange: true 
            
            input "init", "bool", title: "Initialize?", required: false, defaultValue: false, submitOnChange: true
            if(init) {
                app?.updateSetting("init",[value:"false",type:"bool"])
                initialize()
            }
                
				input "getAPI", "bool", title: "Test above configuration and display current weather.gov API response?", required: false, defaultValue: false, submitOnChange: true
				if(getAPI) {
					app?.updateSetting("getAPI",[value:"false",type:"bool"])
					getAlertMsg()
                    buildAlertMsg()
                    def result = (!modesYes && restrictbySwitch !=null && restrictbySwitch.currentState("switch").value == "on") ? true : false
                    def result2 =    ( modesYes && modes !=null && modes.contains(location.mode)) ? true : false
                    def result3 = (modeSeverityYes && modeSeverity !=null && modeSeverity.contains(state.alertseverity.toLowerCase())) ? true : false
                    def testresult = (!(result || result2) || result3) ? true : false
					def date = new Date()
					sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a")
					if(state.alertJSON) {paragraph "Current poll of weather API at: ${sdf.format(date)}<br/><br/>URI: <a href='${state.wxURI}' target=_blank>${state.wxURI}</a><br><br>AlertMSG Built based on configuration:<br><br>${state.alertmsg}<br><br>Restrictions enabled?  Modes: ${result2}, Switch: ${result}, Severity Override: ${result3}<br><br><table border=1px><tr><th>Field Name</th><th>Value</th></tr><tr><td>Severity</td><td>${state.alertseverity}</td></tr><tr><td>Area</td><td>${state.alertarea}</td></tr><tr><td>Sent</td><td>${state.alertsent}</td></tr><tr><td>Effective</td><td>${state.alerteffective}</td></tr><tr><td>Expires</td><td>${state.alertexpires}</td></tr><tr><td>Status</td><td>${state.alertstatus}</td></tr><tr><td>Message Type</td><td>${state.alertmessagetype}</td></tr><tr><td>Category</td><td>${state.alertcategory}</td></tr><tr><td>Certainty</td><td>${state.alertcertainty}</td></tr><tr><td>Urgency</td><td>${state.alerturgency}</td></tr><tr><td>Sender Name</td><td>${state.alertsendername}</td></tr><tr><td>Event Type</td><td>${state.alertevent}</td></tr><tr><td>Headline</td><td>${state.alertheadline}</td></tr><tr><td>Description</td><td>${state.alertdescription}</td></tr><tr><td>Instruction</td><td>${state.alertinstruction}</td></tr></table>"}
					else { paragraph "No JSON feed currently available for your coordinates.  Either there are no weather alerts in your area or you need to change options above to acquire desired results."}
				}
			}
		}
}            

// Main Application Routines
def main() {
    // Get the alert message
	getAlertMsg()	
    if(state.ListofAlerts) {
	    if(state.ListofAlerts[0].alertsent.compareTo(state.previousTS)<0) { 
		    if (logEnable) log.info "No new alerts.  Waiting ${whatPoll.toInteger()} minutes before next poll..."
        } else {
             alertNow(state.ListofAlerts[0].alertmsg, false)
             if(repeatYes) repeatNow()
	    }
    }
    tileNow()
}

def alertNow(alertmsg, repeatCheck){
	// check restrictions based on Modes and Switches
    def result = (switchYes && restrictbySwitch !=null && restrictbySwitch.currentState("switch").value == "on") ? true : false
    def result2 =    ( modesYes && modes !=null && modes.contains(location.mode)) ? true : false
    def result3 = (modeSeverityYes && modeSeverity !=null && modeSeverity.contains(state.alertseverity.toLowerCase())) ? true : false
    if (logEnable) log.debug "Restrictions on?  Modes: ${result2}, Switch: ${result}, Severity Override: ${result3}"
   
    // no restrictions
    if(!(result || result2) || result3) {  
            log.info "Sending alert: ${state.alertmsg}"
            pushNow(alertmsg, repeatCheck)
		    if(alertSwitch) { alertSwitch.on() }
		    talkNow(alertmsg, repeatCheck)  
     } else {
            if(pushoverttsalways) {	
                log.info "Restrictions are enabled but PushoverTTS enabled.  Waiting ${whatPoll.toInteger()} minutes before next poll..."
                pushNow(alertmsg, repeatCheck) 
            }
            else log.info "Restrictions are enabled!  Waiting ${whatPoll.toInteger()} minutes before next poll..."
    }
}

def repeatNow(){
    if(logEnable) log.debug "In repeatNow subroutine state.repeat: ${state.repeat}, state.count: ${state.count}, state.num: ${state.num}"
   
    if(state.repeat) {
        alertNow(state.alertmsg, true)
        state.count = state.count + 1
        if(logEnable) log.debug "Repeating alert in ${state.frequency} minute(s).  This is ${state.count}/${repeatTimes} repeated alert(s). Repeat State: ${state.repeat}"
     } 
    
	 if(state.num > 0){
	    state.num = state.num - 1
        runIn(state.frequency*60,repeatNow)
     } else { 
        if(logEnable) log.debug "Finished repeating alerts."
        state.count = 0
        state.num = repeatTimes.toInteger()
        state.repeat = false 
     }
}

def getAlertMsg() {
    def ListofAlerts = []
    def result = getResponseURL()
    def date = new Date()
    SimpleDateFormat objSDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
    String timestamp = date.format("yyyy-MM-dd'T'HH:mm:ssXXX")
    Date currentTS = objSDF.parse(timestamp)
    state.previousTS = currentTS
   
    for(i=0; i<result.data.features.size();i++) {
        Date alertsent = objSDF.parse(result.data.features[i].properties.sent)
        Date alerteffective = objSDF.parse(result.data.features[i].properties.effective)
        Date alertexpires = objSDF.parse(result.data.features[i].properties.expires)
        if(alertexpires.compareTo(currentTS)>=0) {
            alertarea = alertFormatArea(result.data.features[i].properties.areaDesc)
            alertheadline = alertFormatHeadline(result.data.features[i].properties.headline)
            alertdescription = result.data.features[i].properties.description
            if(result.data.features[i].properties.instruction==null) alertinstruction = result.data.features[i].properties.description 
			else alertinstruction = result.data.features[i].properties.instruction      
            alertmsg = alertCustomMsg
	        try {alertmsg = alertmsg.replace("{alertarea}","${alertarea}") }
	        catch (any) {}
	        try {alertmsg = alertmsg.replace("{alertseverity}","${result.data.features[i].properties.severity}") }
	        catch (any) {}
	        try {alertmsg = alertmsg.replace("{alertcertainty}","${result.data.features[i].properties.certainty}") }
	        catch (any) {}
	        try {alertmsg = alertmsg.replace("{alerturgency}","${result.data.features[i].properties.urgency}") }
	        catch (any) {}
	        try {alertmsg = alertmsg.replace("{alertheadline}","${alertheadline}") }
	        catch (any) {}
	        try {alertmsg = alertmsg.replace("{alertdescription}","${alertdescription}") }
	        catch (any) {}
	        try {alertmsg = alertmsg.replace("{alertinstruction}","${alertinstruction}") }
	        catch (any) {}
	        try {alertmsg = alertmsg.replace("{alertevent}","${result.data.features[i].properties.event}") }
	        catch (any) {}					
	        try {alertmsg = alertmsg.replaceAll("\n"," ") }
	        catch (any) {}
            try {alertmsg = alertmsg.trim().replaceAll("[ ]{2,}", ", ") }
            catch (any) {}
            alertmsg = alertmsg.replaceAll("\\s+", " ")
            ListofAlerts << [alertseverity:result.data.features[i].properties.severity, alertarea:alertarea, alertsent:alertsent, alerteffective:alerteffective, alertexpires:alertexpires, alertstatus:result.data.features[i].properties.status, alertmessagetype:result.data.features[i].properties.messageType, alertcategory:result.data.features[i].properties.category, alertcertainty:result.data.features[i].properties.certainty, alerturgency:result.data.features[i].properties.urgency, alertsendername:result.data.features[i].properties.senderName, alertheadline:alertheadline, alertdescription:result.data.features[i].properties.description, alertinstruction:alertinstruction, alertevent:result.data.features[i].properties.event, alertmsg:alertmsg]
        }
    }
    if(ListofAlerts==null) state.ListofAlerts = null
    else state.ListofAlerts = ListofAlerts
}                    

def alertFormatHeadline(alertheadline) {
    if(alertheadline.contains("NWS")) alertheadline=alertheadline.replaceAll("NWS","the National Weather Service of")
    if(alertheadline.contains("AL")) alertheadline = alertheadline.replaceAll("AL","Alabama")
    if(alertheadline.contains("AK")) alertheadline = alertheadline.replaceAll("AK","Alaska")
    if(alertheadline.contains("AZ")) alertheadline = alertheadline.replaceAll("AZ","Arizona")
    if(alertheadline.contains("AR")) alertheadline = alertheadline.replaceAll("AR","Arkansas")
    if(alertheadline.contains("CA")) alertheadline = alertheadline.replaceAll("CA","California") 
    if(alertheadline.contains("CO")) alertheadline = alertheadline.replaceAll("CO","Colorado")
    if(alertheadline.contains("CT")) alertheadline = alertheadline.replaceAll("CT","Connecticut")
    if(alertheadline.contains("DE")) alertheadline = alertheadline.replaceAll("DE","Deleware")
    if(alertheadline.contains("FL")) alertheadline = alertheadline.replaceAll("FL","Florida")
    if(alertheadline.contains("GA")) alertheadline = alertheadline.replaceAll("GA","Georgia")
    if(alertheadline.contains("HI")) alertheadline = alertheadline.replaceAll("HI","Hawaii")
    if(alertheadline.contains("ID")) alertheadline = alertheadline.replaceAll("ID","Idaho")
    if(alertheadline.contains("IL")) alertheadline = alertheadline.replaceAll("IL","Illinois")
    if(alertheadline.contains("IN")) alertheadline = alertheadline.replaceAll("IN","Indiana")
    if(alertheadline.contains("IA")) alertheadline = alertheadline.replaceAll("IA","Iowa")
    if(alertheadline.contains("KS")) alertheadline = alertheadline.replaceAll("KS","Kansas")
    if(alertheadline.contains("KY")) alertheadline = alertheadline.replaceAll("KY","Kentucky")
    if(alertheadline.contains("LA")) alertheadline = alertheadline.replaceAll("LA","Louisiana")
    if(alertheadline.contains("ME")) alertheadline = alertheadline.replaceAll("ME","Maine")
    if(alertheadline.contains("MA")) alertheadline = alertheadline.replaceAll("MA","Massachusetts")
    if(alertheadline.contains("MD")) alertheadline = alertheadline.replaceAll("MD","Maryland")
    if(alertheadline.contains("MI")) alertheadline = alertheadline.replaceAll("MI","Michigan")
	if(alertheadline.contains("MN")) alertheadline = alertheadline.replaceAll("MN","Minnesota")
    if(alertheadline.contains("MS")) alertheadline = alertheadline.replaceAll("MS","Mississippi")
    if(alertheadline.contains("MO")) alertheadline = alertheadline.replaceAll("MO","Missouri")
    if(alertheadline.contains("MT")) alertheadline = alertheadline.replaceAll("MT","Montana")
    if(alertheadline.contains("NE")) alertheadline = alertheadline.replaceAll("NE","Nebraska")
    if(alertheadline.contains("NV")) alertheadline = alertheadline.replaceAll("NV","Nevada")
    if(alertheadline.contains("NH")) alertheadline = alertheadline.replaceAll("NH","New Hampshire")
    if(alertheadline.contains("NJ")) alertheadline = alertheadline.replaceAll("NJ","New Jersey")
    if(alertheadline.contains("NM")) alertheadline = alertheadline.replaceAll("NM","New Mexico")
    if(alertheadline.contains("NY")) alertheadline = alertheadline.replaceAll("NY","New York")
    if(alertheadline.contains("NC")) alertheadline = alertheadline.replaceAll("NC","North Carolina")
    if(alertheadline.contains("ND")) alertheadline = alertheadline.replaceAll("ND","North Dakota")
    if(alertheadline.contains("OH")) alertheadline = alertheadline.replaceAll("OH","Ohio")
    if(alertheadline.contains("OK")) alertheadline = alertheadline.replaceAll("OK","Oklahoma")
    if(alertheadline.contains("OR")) alertheadline = alertheadline.replaceAll("OR","Oregon")
    if(alertheadline.contains("PA")) alertheadline = alertheadline.replaceAll("PA","Pennsylvania")
    if(alertheadline.contains("RI")) alertheadline = alertheadline.replaceAll("RI","Rhode Island")
    if(alertheadline.contains("SC")) alertheadline = alertheadline.replaceAll("SC","South Carolina")
    if(alertheadline.contains("SD")) alertheadline = alertheadline.replaceAll("SD","South Dakota")
    if(alertheadline.contains("TN")) alertheadline = alertheadline.replaceAll("TN","Tennessee")
    if(alertheadline.contains("TX")) alertheadline = alertheadline.replaceAll("TX","Texas")
    if(alertheadline.contains("UT")) alertheadline = alertheadline.replaceAll("UT","Utah")
    if(alertheadline.contains("VT")) alertheadline = alertheadline.replaceAll("VT","Vermont")
    if(alertheadline.contains("VA")) alertheadline = alertheadline.replaceAll("VA","Virginia")
    if(alertheadline.contains("WA")) alertheadline = alertheadline.replaceAll("WA","Washington")
    if(alertheadline.contains("WV")) alertheadline = alertheadline.replaceAll("WV","West Virginia")
    if(alertheadline.contains("WI")) alertheadline = alertheadline.replaceAll("WI","Wisconsin")
    if(alertheadline.contains("WY")) alertheadline = alertheadline.replaceAll("WY","Wyoming")
    // Remove Timezones
    if(alertheadline.contains("AST")) alertheadline = alertheadline.replaceAll("AST","")
    if(alertheadline.contains("EST")) alertheadline = alertheadline.replaceAll("EST","")
    if(alertheadline.contains("EDT")) alertheadline = alertheadline.replaceAll("EDT","")
    if(alertheadline.contains("CST")) alertheadline = alertheadline.replaceAll("CST","")
    if(alertheadline.contains("CDT")) alertheadline = alertheadline.replaceAll("CDT","")
    if(alertheadline.contains("MST")) alertheadline = alertheadline.replaceAll("MST","")
    if(alertheadline.contains("MDT")) alertheadline = alertheadline.replaceAll("MDT","")
    if(alertheadline.contains("PST")) alertheadline = alertheadline.replaceAll("PST","")
    if(alertheadline.contains("PDT")) alertheadline = alertheadline.replaceAll("PDT","")
    if(alertheadline.contains("AKST")) alertheadline = alertheadline.replaceAll("AKST","")
    if(alertheadline.contains("AKDT")) alertheadline = alertheadline.replaceAll("AKDT","")
    if(alertheadline.contains("HST")) alertheadline = alertheadline.replaceAll("HST","")
    if(alertheadline.contains("HAST")) alertheadline = alertheadline.replaceAll("HAST","")
    if(alertheadline.contains("HADT")) alertheadline = alertheadline.replaceAll("HADT","")
    alertheadline = alertheadline.replaceAll("\\s+", " ")
    alertheadline = alertheadline + "."
    return alertheadline
}

def alertFormatArea(alertarea) {
    // Remove States
    if(alertarea.contains("AL")) alertarea = alertarea.replaceAll("AL","")
    if(alertarea.contains("AK")) alertarea = alertarea.replaceAll("AK","")
    if(alertarea.contains("AZ")) alertarea = alertarea.replaceAll("AZ","")
    if(alertarea.contains("AR")) alertarea = alertarea.replaceAll("AR","")
    if(alertarea.contains("CA")) alertarea = alertarea.replaceAll("CA","") 
    if(alertarea.contains("CO")) alertarea = alertarea.replaceAll("CO","")
    if(alertarea.contains("CT")) alertarea = alertarea.replaceAll("CT","")
    if(alertarea.contains("DE")) alertarea = alertarea.replaceAll("DE","")
    if(alertarea.contains("FL")) alertarea = alertarea.replaceAll("FL","")
    if(alertarea.contains("GA")) alertarea = alertarea.replaceAll("GA","")
    if(alertarea.contains("HI")) alertarea = alertarea.replaceAll("HI","")
    if(alertarea.contains("ID")) alertarea = alertarea.replaceAll("ID","")
    if(alertarea.contains("IL")) alertarea = alertarea.replaceAll("IL","")
    if(alertarea.contains("IN")) alertarea = alertarea.replaceAll("IN","")
    if(alertarea.contains("IA")) alertarea = alertarea.replaceAll("IA","")
    if(alertarea.contains("KS")) alertarea = alertarea.replaceAll("KS","")
    if(alertarea.contains("KY")) alertarea = alertarea.replaceAll("KY","")
    if(alertarea.contains("LA")) alertarea = alertarea.replaceAll("LA","")
    if(alertarea.contains("ME")) alertarea = alertarea.replaceAll("ME","")
    if(alertarea.contains("MA")) alertarea = alertarea.replaceAll("MA","")
    if(alertarea.contains("MD")) alertarea = alertarea.replaceAll("MD","")
    if(alertarea.contains("MI")) alertarea = alertarea.replaceAll("MI","")
    if(alertarea.contains("MN")) alertarea = alertarea.replaceAll("MN","")
    if(alertarea.contains("MS")) alertarea = alertarea.replaceAll("MS","")
    if(alertarea.contains("MO")) alertarea = alertarea.replaceAll("MO","")
    if(alertarea.contains("MT")) alertarea = alertarea.replaceAll("MT","")
    if(alertarea.contains("NE")) alertarea = alertarea.replaceAll("NE","")
    if(alertarea.contains("NV")) alertarea = alertarea.replaceAll("NV","")
    if(alertarea.contains("NH")) alertarea = alertarea.replaceAll("NH","")
    if(alertarea.contains("NJ")) alertarea = alertarea.replaceAll("NJ","")
    if(alertarea.contains("NM")) alertarea = alertarea.replaceAll("NM","")
    if(alertarea.contains("NY")) alertarea = alertarea.replaceAll("NY","")
    if(alertarea.contains("NC")) alertarea = alertarea.replaceAll("NC","")
    if(alertarea.contains("ND")) alertarea = alertarea.replaceAll("ND","")
    if(alertarea.contains("OH")) alertarea = alertarea.replaceAll("OH","")
    if(alertarea.contains("OK")) alertarea = alertarea.replaceAll("OK","")
    if(alertarea.contains("OR")) alertarea = alertarea.replaceAll("OR","")
    if(alertarea.contains("PA")) alertarea = alertarea.replaceAll("PA","")
    if(alertarea.contains("RI")) alertarea = alertarea.replaceAll("RI","")
    if(alertarea.contains("SC")) alertarea = alertarea.replaceAll("SC","")
    if(alertarea.contains("SD")) alertarea = alertarea.replaceAll("SD","")
    if(alertarea.contains("TN")) alertarea = alertarea.replaceAll("TN","")
    if(alertarea.contains("TX")) alertarea = alertarea.replaceAll("TX","")
    if(alertarea.contains("UT")) alertarea = alertarea.replaceAll("UT","")
    if(alertarea.contains("VT")) alertarea = alertarea.replaceAll("VT","")
    if(alertarea.contains("VA")) alertarea = alertarea.replaceAll("VA","")
    if(alertarea.contains("WA")) alertarea = alertarea.replaceAll("WA","")
    if(alertarea.contains("WV")) alertarea = alertarea.replaceAll("WV","")
    if(alertarea.contains("WI")) alertarea = alertarea.replaceAll("WI","")
    if(alertarea.contains("WY")) alertarea = alertarea.replaceAll("WY","")
    alertarea = alertarea.replaceAll(", ","")
    alertarea = alertarea.replaceAll(",","")
    alertarea = alertarea.replaceAll(";",",")                    
	alertarea = alertarea.replaceAll("\n"," ")
    alertarea = alertarea.replaceAll("\\s+", " ")                        
	StringBuffer buffer = new StringBuffer(alertarea)
	alertarea = buffer.reverse().toString().replaceFirst(",","dna ")
	alertarea = new StringBuffer(alertarea).reverse().toString()
    alertarea = alertarea + "."
    return alertarea
    
}   

//Test Alert Section
def runtestAlert() {
    if (logEnable) log.debug "Initiating a test alert."
    state.alertmsg=buildTestAlert()
    alertNow(state.alertmsg, false)
    if(repeatYes==true) repeatNow()
     else {
       	if (logEnable) log.debug "Resetting NOAA Tile in ${noaaTileReset} minutes."
	    runIn((60*noaaTileReset.toInteger()),tileReset)
     }
}

def buildTestAlert() {
					alertmsg = alertCustomMsg
					try {alertmsg = alertmsg.replace("{alertarea}","Springfield County.") }
						 catch (any) {}
					try {alertmsg = alertmsg.replace("{alertseverity}","Severe") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertcertainty}","Likely") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alerturgency}","Immediate") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertheadline}","The founder, Jebediah Springfield has spotted a cloud above the nuclear power plant towers.") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertdescription}","The founder, Jebediah Springfield has spotted a cloud above the nuclear power plant towers.  Expect heavy polution, possible fish with three eyes, and a Simpson asleep at the console.  Also a notorius yellow haired boy is terrorizing animals with spit wads.  Be on the look out for suspicious activity.") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertinstruction}","Expect heavy polution, possible fish with three eyes, and a Simpson asleep at the console.") }
						catch (any) {}
					try {alertmsg = alertmsg.replace("{alertevent}","Nuclear Power Plant Warning") }
						catch (any) {}
					return alertmsg
}

def buildAlertMsg() {
		if (logEnable) log.debug "Building alert message."
		// build the alertmsg
		alertmsg = alertCustomMsg
		try {alertmsg = alertmsg.replace("{alertarea}","${state.alertarea}") }
			 catch (any) {}
		try {alertmsg = alertmsg.replace("{alertseverity}","${state.alertseverity}") }
			catch (any) {}
		try {alertmsg = alertmsg.replace("{alertcertainty}","${state.alertcertainty}") }
			catch (any) {}
		try {alertmsg = alertmsg.replace("{alerturgency}","${state.alerturgency}") }
			catch (any) {}
		try {alertmsg = alertmsg.replace("{alertheadline}","${state.alertheadline}") }
			catch (any) {}
		try {alertmsg = alertmsg.replace("{alertdescription}","${state.alertdescription}") }
			catch (any) {}
		try {alertmsg = alertmsg.replace("{alertinstruction}","${state.alertinstruction}") }
			catch (any) {}
		try {alertmsg = alertmsg.replace("{alertevent}","${state.alertevent}") }
			catch (any) {}					
		try {alertmsg = alertmsg.replaceAll("\n"," ") }
			catch (any) {}
        try {alertmsg = alertmsg.trim().replaceAll("[ ]{2,}", ", ") }
            catch (any) {}
		state.alertmsg = alertmsg
		if (logEnable) log.debug "alertMsg built: ${state.alertmsg}"
} 


// Common Notifcation Routines
def talkNow(alertmsg, repeatCheck) {			
        if(repeatCheck) {
            if(useAlertIntro) { alertmsg = "Repeating previous alert,, ${AlertIntro}" + alertmsg
                              } else { alertmsg = "Repeating previous alert,," + alertmsg }
        } else { if(useAlertIntro) alertmsg = "${AlertIntro}, " + alertmsg }
        
		speechDuration = Math.max(Math.round(alertmsg.length()/12),2)+3		
		atomicState.speechDuration2 = speechDuration * 1000
	
  		if (musicmode){ 
				try {
					musicspeaker.playTextAndRestore(alertmsg.toLowerCase(), speakervolume)
                    if (logEnable) log.debug "Sending alert to Music Speaker(s)."
				}
				catch (any) {log.warn "Music Player device(s) has not been selected or not supported."}
  		}   
	
		if(echoSpeaks2) {
			try {
				echospeaker.setVolumeSpeakAndRestore(speakervolume, alertmsg.toLowerCase())
                if (logEnable) log.debug "Sending alert to Echo Speaks device(s)."
				}
			catch (any) {log.warn "Echo Speaks device(s) has not been selected or are not supported."}
		}
	
		if (speechmode){ 
			try {
                speechspeaker.initialize() 
                if (logEnable) log.debug "Initializing Speech Speaker"
                pauseExecution(2500)
            }
            catch (any) { if(logEnable) log.debug "Speech device doesn't support initialize command" }
            try { 
                speechspeaker.setVolume(speakervolume)
                if (logEnable) log.debug "Setting Speech Speaker to volume level: ${speakervolume}"
				pauseExecution(2000)
            }
            catch (any) { if (logEnable) log.debug "Speech speaker doesn't support volume level command" }
                
			if (logEnable) log.debug "Sending alert to Google and Speech Speaker(s)"
            alertmsg = alertmsg.toLowerCase()
            speechspeaker.speak(alertmsg)
            
            try {
				if (speakervolRestore) {
					pauseExecution(atomicState.speechDuration2)
					speechspeaker.setVolume(speakervolRestore)	
                    if (logEnable) log.debug "Restoring Speech Speaker to volume level: ${speakervolRestore}"
                }
            }
                catch (any) { if (logEnable) log.debug "Speech speaker doesn't support restore volume command" }
		}
	
}

def pushNow(alertmsg, repeatCheck) {
	if (pushovertts) {
	    if (logEnable) log.debug "Sending Pushover message."
        if(repeatCheck) {
            if(repeatTimes.toInteger()>1){ 
                alertmsg = "[Alert Repeat ${state.count}/${repeatTimes}] " + alertmsg
            } else {
                alertmsg = "[Alert Repeat] " + alertmsg
            }
        }
	    def m = alertmsg =~ /(.|[\r\n]){1,1023}\W/
	    def n = alertmsg =~ /(.|[\r\n]){1,1023}\W/
	    def index = 0
	    def index2 = 1
	   	while (m.find()) {
	   	   index = index + 1
	    }

		while(n.find()) {
			fullMsg1 = n.group()
            if(index>1) {
			    pushoverdevice.deviceNotification("(${index2}/${index}) ${fullMsg1}")
            } else {
                pushoverdevice.deviceNotification("${fullMsg1}")
            }
			index2 = index2 + 1
			pauseExecution(1000)
        } 
	}
}

def tileNow() {
    noaaTileDevice = getChildDevice("NOAA")
	if(noaaTileDevice) {
        if(!state.ListofAlerts==null) {
            log.debug "creating tile msg"
            int count = 1
            int size = state.ListofAlerts.size()
            size = size
            def msg = ""
            for(x=0;x<state.ListofAlerts.size();x++) {
                //msg += msg + "${state.ListofAlerts[x].alertmsg}\nAlert ${count}/${size}"
                msg += "Alert ${count}/${size} - ${state.ListofAlerts[x].alertmsg}\n"
                count = count + 1
            }
        } 
        if (logEnable) log.info "Message sent to NOAA Tile device."
		noaaTileDevice.sendNoaaTile(msg)
	}
}

// Device creation and status updhandlers
def createChildDevices() {
    try {
        if (!getChildDevice("NOAA")) {
                if (logEnable) log.info "Creating device: NOAA Tile"
                addChildDevice("aaronward", "NOAA Tile", "NOAA", 1234, ["name": "NOAA Tile", isComponent: false])
        }
    }
    catch (e) { log.error "Couldn't create child device. ${e}" }
}

def cleanupChildDevices()
{
    try {
	    for (device in getChildDevices()) {
            deleteChildDevice(device.deviceNetworkId)
	    }
    }
    catch (e) { log.error "Couldn't clean up child devices."}
}

// Application Support Routines
def getResponseURL() {
    def result = null
	def wxURI = "https://api.weather.gov/alerts?point=${state.latitude}%2C${state.longitude}&status=actual&message_type=alert"
    
	// Determine if custom coordinates have been selected
	if(useCustomCords) {
		state.latitude = "${customlatitude}"
		state.longitude = "${customlongitude}"
	} else {
		state.latitude = "${location.latitude}"
		state.longitude = "${location.longitude}"
	}
    
	// Build out the API options
	if(whatAlertUrgency != null) wxURI = wxURI + "&urgency=${whatAlertUrgency.join(",")}"
    if(whatAlertSeverity != null) wxURI = wxURI + "&severity=${whatAlertSeverity.join(",")}"
	 else wxURI = wxURI + "&severity=severe"
	if(whatAlertCertainty !=null) wxURI = wxURI + "&certainty=${whatAlertCertainty.join(",")}"
	if(myWeatherAlert != null) wxURI = wxURI + "&code=${myWeatherAlert.join(",")}"
	state.wxURI = wxURI
	if (logEnable) log.debug "URI: ${wxURI}"
    wxURI = "https://api.weather.gov/alerts?point=44.302018%2C-92.674947&status=actual&message_type=alert&severity=minor,moderate,severe,extreme"

    if (logEnable) log.debug "Connecting to weather.gov service."
    def requestParams =	[ 
        uri: wxURI,
        requestContentType: "application/json",
        contentType: "application/json",
        timeout: 300
    ]
	
	try {
        httpGet(requestParams)	{ response -> result = response }
    }
    catch (e) { log.error "${e}" }
    return result
}

def checkState() {
    if(whatPoll==null) whatPoll=5
    if(logEnable==null) logEnable = true
    if(logMinutes==null) logMinutes=15
    if(repeatTimes==null || repeatMinutes==null) {
        if(repeatTimes==null) { state.num = 1 
                             } else { state.num = repeatTimes.toInteger()
                                      state.repeatTime = repeatTimes.toInteger()}
        if(repeatMinutes==null) { state.frequency = 15
                                } else { state.frequency = repeatMinutes.toInteger() }
    } else {
        state.num = repeatTimes.toInteger()
        state.frequency = repeatMinutes.toInteger()
    }
    state.count = 0
    state.repeat = false
    state.resettileAlert = false
}

def installCheck(){     
	state.appInstalled = app.getInstallationState() 
	if(state.appInstalled != 'COMPLETE'){
		section{paragraph "Please hit 'Done' to install ${app.label} "}
  	}
}

def initialize() {
    checkState()    
    unschedule()
    createChildDevices()
    state.repeat = false
    if (logEnable && logMinutes.toInteger() != 0) {
        if(logMinutes.toInteger() !=0) log.warn "Debug messages set to automatically disable in ${logMinutes} minute(s)."
        runIn((logMinutes.toInteger() * 60),logsOff)
    }
    else { if(logEnable && logMinutes.toInteger() == 0) {log.warn "Debug logs set to not automatically disable." } 
          else {log.info "Debug logs disabled."}
         }
     switch (whatPoll.toInteger()) {
		case 1: 
			runEvery1Minute(main)
			break
		case 5: 
			runEvery5Minutes(main)
			break
		case 10: 
			runEvery10Minutes(main)
			break
		case 15: 
			runEvery15Minutes(main)
			break
		default: 
			runEvery5Minutes(main)
			break
	}
    runIn(5, main)
}

def installed() {
    if (logEnable) log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    if (logEnable) log.debug "Updated with settings: ${settings}"
    initialize()
}

def uninstalled() {
    cleanupChildDevices()
}


def UIsupport(type, txt) {
    switch(type) {
        case "logo": 
            return "<table border=0><thead><tr><th><img border=0 style='max-width:100px' src='https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/NOAA/Support/NOAA.png'></th><th style='padding:10px' align=left><font style='font-size:34px;color:#1A77C9;font-weight: bold'>NOAA Weather Alerts</font><br><font style='font-size:14px;font-weight: none'>This application provides customized Weather Alert announcements.</font></tr></thead></table><br><hr style='margin-top:-15px;background-color:#1A77C9; height: 1px; border: 0;'></hr>"
            break
        case "line": 
            return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
            break
        case "header": 
            return "<div style='color:#ffffff;font-weight: bold;background-color:#1A7BC7;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${txt}</div>"
            break
        case "footer":
            return "<div style='color:#1A77C9;text-align:center'>Developed by: Aaron Ward<br/>v${version()}<br><br><a href='https://paypal.me/aaronmward?locale.x=en_US' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br><br>Donations always appreciated!</div>"
            break
    }
}

def logsOff(){
    log.warn "Debug logging disabled."
    app?.updateSetting("logEnable",[value:"false",type:"bool"])
}
