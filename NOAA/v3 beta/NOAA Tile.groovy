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
    if(logEnable) log.info "Received weather alert from NOAA App."
    if(noaaData==null || noaaData=="") {
        noaaTile = "<center><table width='90%' height='90%'><tr>"
		noaaTile += "<td style='text-align:center;'>"
        noaaTile += "<div style='font-size: 15px'>No weather alerts to report.</div>" 
	    noaaTile += "</td></tr></table>"
		sendEvent(name: "Alerts", value: noaaTile, displayed: true)         
    } else {
        count = true
        messageSize=300
        fullalert = []
        log.error noaaData
        while(count) {
            for(x=0;x<noaaData.alertmsg.size();x++) {
                log.debug noaaData[x].alertmsg
	    		def m = noaaData[x].alertmsg =~ /(.|[\r\n]){1,380}\W/
		    	fullmsg = []
			    while (m.find()) {
    			   fullmsg << m.group()
                }
                for(i=0;i<fullmsg.size();i++) {
                    noaaTile = "<center><table width='90%' height='90%'><tr>"
				    noaaTile += "<td style='text-align: justify;'>"
                    noaaTile += "<div style='font-size: 15px'> ${fullmsg[i]}</div>" 
	    			noaaTile += "</td></tr></table>"
		    		sendEvent(name: "Alerts", value: noaaTile, displayed: true)                
                    pauseExecution(8000)
                }
            }   
            count = false
        }
    }
}

