
NOAA Tile
chat_bubble_outline
more_vert

aaron
arrow_drop_downAccounts
Dashboards
Devices
Apps
Settings
Advanced
codeApps Code
codeDrivers Code
System Events
Logs
NOAA TileImport   HelpDeleteSave
50
    refresh()
51
}
52
​
53
def installed(){
54
    log.info "NOAA Tile has been Installed."
55
    sendEvent(name: "Alerts", value: "No weather alerts to report.", displayed: true)
56
}
57
​
58
def refresh() {
59
    if(logEnable) runIn(900,logsOff)
60
    count = true
61
    getTile()
62
    sendEvent(name: "Alerts", value: "No weather alerts to report.", displayed: true)
63
}
64
​
65
def logsOff(){
66
    log.warn "Debug logging disabled."
67
    device.updateSetting("logEnable",[value:"false",type:"bool"])
68
}
69
​
70
​
71
def getTile() {
72
        if(logEnable) log.info "Requesting current weather alert from NOAA App."
73
        noaaData = []
74
        noaaData = parent.getTile()
75
        log.debug noaaData
76
        if(!noaaData) { 
77
            sendEvent(name: "Alerts", value: "No weather alerts to report.", displayed: true) 
78
            runIn(60, getTile)
79
        }
80
        else {
81
            fullalert = []
82
            for(x=0;x<noaaData.size();x++) {
83
                m = noaaData[x].alertmsg =~ /(.|[\r\n]){1,378}\W/
84
                fullmsg = []
85
                while (m.find()) {
86
                    fullmsg << m.group()
87
                }
88
                for(i=0;i<fullmsg.size();i++) {
89
                    noaaTile = "<table style='position:relative;top:-10px;left:10px;border-collapse:collapse;width:97%'><tr style='border-bottom:medium solid #FFFFFF;'><td valign='bottom' style='text-align:left;width:50%;border-bottom:medium solid #FFFFFF;height:13px'><font style='font-size:12px;'>"
90
                    noaaTile += "Alert: ${x+1}/${noaaData.size()}"
91
                    noaaTile += "</font></td><td valign='bottom' style='text-align:right;border-bottom:medium solid #FFFFFF;width:50%;height:13px'><font style='font-size:12px;'>Page: ${i+1}/${fullmsg.size()}</font></td></tr>"
92
                    noaaTile += "<tr><td colspan=2 valign='top' style='line-height:normal;width:90%;height:100px;text-align:left;border-top-style:none;border-top-width:medium;'><font style='font-size:13px'>${fullmsg[i]}"
93
                    noaaTile += "</font></td></tr></table>"
94
                    sendEvent(name: "Alerts", value: noaaTile, displayed: true)      
95
                    pauseExecution(8000)
96
                }
97
            }
98
        }
99
}
100
​
Location: Hub2
Terms of Service
Documentation
Community
Support
Copyright 2019 Hubitat, Inc.
