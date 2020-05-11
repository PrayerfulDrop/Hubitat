/**
 *  ****************  NOAA Tile Driver  ****************
 *
 *  importUrl: "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/NOAA/NOAA-Tile-Driver.groovy"
 *
 */

metadata {
	definition (
		name: "NOAA Tile",
		namespace: "aaronward",
		author: "Aaron Ward",
		importUrl: "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/NOAA/NOAA-Tile-Driver.groovy"
		)
		{
		command "sendNoaaTile", ["string"]
		capability "Actuator"
		capability "Refresh"
    	attribute "Alerts", "string"
		}

	preferences() {    	
			//input("timeExpire", "text", title: "Minutes to scroll alert?", required: true, defaultValue: "5")
            input("logEnable", "bool", title: "Enable logging", required: false, defaultValue: true)
    }
}

def initialize() {
	log.info "NOAA Tile Driver Initializing."
	refresh()
}

def updated() {
	refresh()
}

def installed(){
    log.info "NOAA Tile has been Installed."
	sendEvent(name: "Alerts", value: "No weather alerts to report.", displayed: true)
}

def refresh() {
	if (logEnable) runIn(900,logsOff)
    log.info "NOAA Tile has been updated."
	state.clear()
}

def logsOff(){
    log.warn "Debug logging disabled."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}


def sendNoaaTile(noaaData) {
    count = true
    if(noaaData=="" || noaaData==null) alertmsg = "No weather alerts to report."
	else alertmsg = "${noaaData}"
    
	if(logEnable) log.info "Received weather alert from NOAA App."
    
	if (logEnable) log.debug "Length of Alert: ${alertmsg.length()}"
	if(alertmsg.length() > messageSize) {
		//determine how many pages will the alert is
		def m = alertmsg =~ /(.|[\r\n]){1,380}\W/
		state.index = 0
		def x = 0
		state.fullmsg = []
		while (m.find()) {
		   state.fullmsg << m.group()
		   state.index = state.index +1
		}
    }
	if(noaaData.length() < messageSize) { 
        noaaTile = "<center><table width='90%' height='90%'><tr>"
        noaaTile += "<td style='text-align: center;'>"
	    noaaTile += "<div style='font-size: ${fontSize}px'> ${alertmsg}</div>"
		noaaTile += "<div align='right' style='font-size: ${fontSize}px'></div>" 
 		noaaTile += "</td></tr></table>"
		sendEvent(name: "Alerts", value: noaaTile, displayed: true)  
		if(logEnable) log.info "NOAA Weather Alert displayed on dashboard."                
    } else {
        if(logEnable) log.info "NOAA Weather Alert rotating on dashboard."
	    while(count){
	        noaaTile = "<center><table width='90%' height='90%'><tr>"
		    noaaTile += "<td style='text-align: justify;'>"
		    if(x==(state.index-1)) { noaaTile += "<div style='font-size: 15px'> ${state.fullmsg[x]}</div>" }
		    else { noaaTile += "<div style='font-size: 15px'> ${state.fullmsg[x]}...</div>" }
		    noaaTile += "<div align='right' style='font-size: 15px'>${x+1}/${state.index}</div>"
		    noaaTile += "</td></tr></table>"
		    sendEvent(name: "Alerts", value: noaaTile, displayed: true)
		    if(x == (state.index-1)) {x=0} else {x=x+1}
		    pauseExecution(8000)
		}
    }
}

