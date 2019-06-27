/**
 *
 * Hubitat Import URL: https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/You%20Got%20Mail/yougotmail.groovy
 *
 *  **************** You Got Mail  ****************
 *
 *  Design Usage:
 *  This app is designed to notify you when a contact sensor on a mailbox is opened.  Custom messages are announced depending on mail delivery times.
 *
 *  Copyright 2018 Aaron Ward
 *
 *  Special thanks to csteele for all his help and education of Groovy.  Another special thanks to contributions from bptworld!
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
 *   1.0.0 - initial code concept
**/

import groovy.json.*
import java.util.regex.*
import java.text.SimpleDateFormat
	
def version(){"v1.0.0"}

definition(
    name:"You Got Mail",
    namespace: "aaronward",
    author: "Aaron Ward",
    description: "Alerts when mailbox is opened",
    category: "Misc",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
    )

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
} 

def mainPage() {
    dynamicPage(name: "mainPage") {
			section(getFormat("title", "${getImage("Blank")}" + " ${app.label}")) {
				paragraph "<div style='color:#1A77C9'>This application provides notifications when a contact sensor on a mailbox is opened.</div>"
			}
			section(getFormat("header-green", " Notification Devices")) {
				// PushOver Devices
			    input name: "pushovertts", type: "bool", title: "Use 'Pushover' device(s)?", required: false, defaultValue: false, submitOnChange: true 
			    if(pushovertts) input name: "pushoverdevice", type: "capability.notification", title: "PushOver Device", required: true, multiple: true
			     
				// Music Speakers (Sonos, etc)
				input name: "musicmode", type: "bool", defaultValue: "false", title: "Use Music Speaker(s) for TTS?", description: "Music Speaker(s)?", submitOnChange: true
				if (musicmode) input name: "musicspeaker", type: "capability.musicPlayer", title: "Choose speaker(s)", required: false, multiple: true, submitOnChange: true
				
				// Speech Speakers
				input name: "speechmode", type: "bool", defaultValue: "false", title: "Use Google or Speech Speaker(s) for TTS?", description: "Speech Speaker(s)?", submitOnChange: true
   	            if (speechmode) input name: "speechspeaker", type: "capability.speechSynthesis", title: "Choose speaker(s)", required: false, multiple: true, submitOnChange: true
						
				// Echo Speaks devices
				input name: "echoSpeaks2", type: "bool", defaultValue: "false", title: "Use Echo Speaks device(s) for TTS?", description: "Echo Speaks device?", submitOnChange: true
			    if(echoSpeaks2) input name: "echospeaker", type: "capability.musicPlayer", title: "Choose Echo Speaks Device(s)", required: false, multiple: true, submitOnChange: true 
				
				// Master Volume settings
				input name :"speakervolume", type: "number", title: "Notification Volume Level:", description: "0-100%", required: false, defaultValue: "75", submitOnChange: true
				input name: "speakervolRestore", type: "number", title: "Restore Volume Level:", description: "0-100", required: false,  defaultValue: "75", submitOnChange: true
				
				// Switch to set when alert active
				input name: "mailboxSwitch", type: "capability.switch", title: "Switch to turn ON when Mailbox Opened?", required: false, defaultValue: false, submitOnChange: true

			}
            section(getFormat("header-green", " Mailbox Configuration")) {
                input name: "maildeliveryStartTime", type: "time", title: "Mail Delivery Start Time:", required: false
        		input name: "maildeliveryStopTime", type: "time", title: "Mail Delivery End Time:", required: false
                //input name: "frequency", type: "decimal", title: "How many minutes to wait between notifications?", required: false, defaultValue: "1", submitOnChange: true
		        input name: "mailboxcontact", type: "capability.contactSensor", title: "Choose contact sensor on mailbox:", required: false, multiple: false, submitOnChange: true
                input name: "mailnotification", type: "text", title: "Message when mail is delivered: (for random messages seperate using semicolon)", required: false, submitOnChange: true, defaultValue: "Mail has arrived. You've. got. mail!;Look what was just delivered.  Your mail!;Tally ho!... Mail has arrived!"
                paragraph "<b>Current message(s) to announce:</b><ul>"
					def messages = "${mailnotification}".split(";")
					messagelist = ""
    				messages.each { item -> messagelist += "<li>${item}</li>"}
					paragraph "${messagelist}"
                paragraph "</ul><hr>"
                input name: "notifyopenmailbox", type: "bool", defaultValue: "false", title: "Notify if mailbox is opened outside of mail delivery times?", required: false, submitOnChange: true
                if(notifyopenmailbox) {
                    input name: "mailboxopen", type: "text", title: "Message when mailbox is opened: ", required: false, submitOnChange: true, defaultValue: "Someone has opened your mailbox."
                    input name: "pushoveronly", type: "bool", title: "Notify via Pushover only?", required: false, submitOnChange: true, defaultValue: true
                }
            }
        	section(getFormat("header-green", " Logging and Testing")) {
 				input name: "logEnable", type: "bool", title: "Enable Debug Logging?", required: false, defaultValue: false, submitOnChange: true
                if(logEnable) input name: "logMinutes", type: "text", title: "Log for the following number of minutes (0=logs always on):", required: false, defaultValue:15, submitOnChange: true                

				paragraph getFormat("line")
				paragraph "<div style='color:#1A77C9;text-align:center'>Developed by: Aaron Ward<br/>${version()}<br><br><a href='https://paypal.me/aaronmward?locale.x=en_US' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br><br>Donations always appreciated!</div>"
			}

    }
}

def getImage(type) {
    def loc = "<img src='https://github.com/PrayerfulDrop/Hubitat/raw/master/You%20Got%20Mail/Support/mailbox.png'>"
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
    log.debug "Installed with settings: ${settings}"
	initialize()
}


def updated () {
    if (logEnable) log.debug "Updated with settings: ${settings}"
    initialize()
}

def initialize() {
   if (logEnable && logMinutes.toInteger() != 0) {
      if(logMinutes.toInteger() !=0) log.warn "Debug messages set to automatically disable in ${logMinutes} minute(s)."
      runIn((logMinutes.toInteger() * 60),logsOff)
    }
    subscribe(mailboxcontact, "contact", contactSensorHandler)   
}

def contactSensorHandler(evt) {
    if(evt.value=="open") {

                if(timeOfDayIsBetween(toDateTime(maildeliveryStartTime), toDateTime(maildeliveryStopTime), new Date(), location.timeZone)) {
                    if (logEnable) log.debug "Current time is within mail delivery timeframe"
                    def values = "${mailnotification}".split(";")
	            	vSize = values.size()
	            	count = vSize.toInteger()
                	def randomKey = new Random().nextInt(count)
                    if (logEnable) log.debug values[randomKey]
		            alertNow(values[randomKey])
                    state.lastTime = now
                } else {
                    if (logEnable) log.debug "Current time is outside of mail delivery timeframe"
                    if(notifyopenmailbox && pushoveronly == false) { alertNow(mailboxopen) }
                    else { pushNow(mailboxopen) }
                }
    }
 }

def talkNow(alertmsg) {								
	if(useAlertIntro) alertmsg = "${AlertIntro}" + alertmsg
		speechDuration = Math.max(Math.round(alertmsg.length()/12),2)+3		
		atomicState.speechDuration2 = speechDuration * 1000
	
  		if (musicmode){ 
				if (logEnable) log.debug "Sending alert to Music Speaker(s)."
				try {
					musicspeaker.playTextAndRestore(alertmsg, speakervolume)
				}
				catch (any) {log.warn "Music Player device(s) has not been selected."}
  		}   
	
		if(echoSpeaks2) {
			if (logEnable) log.debug "Sending alert to Echo Speaks device(s)."
			try {
				echospeaker.setVolumeSpeakAndRestore(speakervolume, alertmsg)
				}
			catch (any) {log.warn "Echo Speaks device(s) has not been selected."}
		}
	
		if (speechmode){ 
			if (logEnable) log.debug "Sending alert to Google and Speech Speaker(s)"
			try {
				if (logEnable) log.debug "Setting Speech Speaker to volume level: ${speakervolume}"
				speechspeaker.setVolume(speakervolume)
				pauseExecution(1000)
				speechspeaker.speak(alertmsg)
				if (speakervolRestore) {
					pauseExecution(atomicState.speechDuration2)
					if (logEnable) log.debug "Restoring Speech Speaker to volume level: ${speakervolRestore}"
					speechspeaker.setVolume(speakervolRestore)	
				}
			}
			catch (any) {log.warn "Speech device(s) has not been selected."}
		}
	
}

def pushNow(alertmsg) {
	if (pushovertts) {
	    if (logEnable) log.debug "Sending Pushover message."
        pushoverdevice.deviceNotification(alertmsg) 
	}
}
def alertNow(alertmsg){
		pushNow(alertmsg)
		if(mailboxSwitch) { mailboxSwitch.on() }
		talkNow(alertmsg)
}