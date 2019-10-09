definition(
name:"App Watchdog 3",
namespace: "BPTWorld",
author: "Bryan Turcotte & Aaron Ward",
description: "Central app/driver update and notification applications",
category: "Convenience",
iconUrl: "",
iconX2Url: "",
iconX3Url: "",
importUrl: "https://raw.githubusercontent.com/bptworld/Hubitat/master/Apps/Apps%20Watchdog%202/AW2-parent.groovy"
)

preferences {
 page name: "mainPage", title: "", install: true, uninstall: true
 page name: "pageTest", title: "", install: false, uninstall: false, nextPage: "mainPage"
 page name: "pageTest2", title: "", install: false, uninstall: false, nextPage: "mainPage"
} 


def mainPage() {	
dynamicPage(name: "mainPage") {
    section ("Developers:"){
        input "devname", "enum", title: "Select Developers you are currently using in Hubitat:", required: false, multiple: true, submitOnChange: true, options:getDevelopers("","devname")
        
        if(devname!=null) {
            for(i=0;i<devname.size();i++){
                name=devname[i]
                paragraph "<b>${name}</b>"
                
                //acquire all the apps of a certain developer
                templist = getDeveloperApps(getDevelopers(name,""), "apps")
                //create dynamic input name to store user selected apps
                input "${name.replaceAll("\\s","")}-app", "enum", title: "Application list:", required: false, multiple: true, options: templist, width:6
                
                //acquire all the drivers of a certain developer
                templist = getDeveloperApps(getDevelopers(name,""), "drivers")
                //create dynamic input name to store user selected drivers
                input "${name.replaceAll("\\s","")}-driver", "enum", title: "Drivers list:", required: false, multiple: true, options: templist, width:6
            
                hrefparams = [
                              devname : name,
                              jsonURL : getDevelopers(name,""),
                              passed: true
                    ]
                href "pageTest", title: "Developer: ${name}", params: hrefparams, description: "Click here to view app list."
                paragraph "<hr>"
            }
        }
        
         // call another page to test if we can bring back the user selected apps/drivers dynamically based off devname
         hrefparams = [
             devname : devname,
             passed: true
                    ]
         href "pageTest2", title: "Testing", params: hrefparams, description: "Testing of selected developers"
    }
}
}


// Testing to see if we can pull the user selected apps back dynamically
def pageTest2(params) {
    if (params.passed) {
    atomicState.params = params // We got something, so save it otherwise it's a page refresh for submitOnChange
}
    def devname = atomicState.params?.devname ?: ""
    dynamicPage(name: "devPage2") {
        section("Testing") {
            def templist = []
            for(x=0;x<devname.size();x++){
                paragraph devname[x]            
            }
        }
    }
}


//display page of a given developer and their apps/drivers - future code to derive summary page from
def pageTest(params) {
 
if (params.passed) {
    atomicState.params = params // We got something, so save it otherwise it's a page refresh for submitOnChange
}
def developer = atomicState.params?.devname ?: ""
def jsonURL = atomicState.params?.jsonURL ?: ""
    
dynamicPage(name: "devPage") {
    section("${developer}") {
        def results = getDeveloperApps(jsonURL, "")
        paragraph "Developer: ${results.data.Developer}"
        paragraph "GitHub Repository: <a href=${results.data.GitHubURL}>${results.data.GitHubURL}</a>"
        paragraph "PayPal Contributions: <a href=${results.data.PayPalURL}>${results.data.PayPalURL}</a>"
        paragraph "<br><br><u><b>Applications:</b></u>"
        for(i=0; i<results.data.application.size();i++){
            paragraph "Application: ${results.data.application[i].name}"
            paragraph "Parent Version: v${results.data.application[i].version}"
            paragraph "Parent RAW Code: ${results.data.application[i].RAW}"
            paragraph "Number of Child Apps: ${results.data.application[i].child.size()}"
            paragraph "Number of Drivers: ${results.data.application[i].driver.size()}"
            for(x=0;x<results.data.application[i].child.size();x++) {
                paragraph "Child ${x}: ${results.data.application[i].child[x].name}"
                paragraph "Version: v${results.data.application[i].child[x].version}"
                paragraph "RAW Code: ${results.data.application[i].child[x].RAW}"
                paragraph "<br><br>"
            }
            for(x=0;x<results.data.application[i].driver.size();x++) {
                paragraph "Driver ${x}: ${results.data.application[i].driver[x].name}"
                paragraph "Version: v${results.data.application[i].driver[x].version}"
                paragraph "RAW Code: ${results.data.application[i].driver[x].RAW}"
                paragraph "<br><br>"
            }
            paragraph "Hubitat Discussions: ${results.data.application[i].hubitatDiscussion}"
            paragraph "Update Notes: ${results.data.application[i].updateNote}"
            paragraph "<hr>"
         }
         paragraph "<br><br><u><b>Drivers:</b></u>"
         for(i=0; i<results.data.drivers.size();i++){
            paragraph "Driver: ${results.data.drivers[i].name}"
            paragraph "Parent Version: v${results.data.drivers[i].version}"
            paragraph "Parent RAW Code: ${results.data.drivers[i].RAW}"
            for(x=0;x<results.data.drivers[i].child.size();x++) {
                paragraph "Child ${x}: ${results.data.drivers[i].child[x].name}"
                paragraph "Version: v${results.data.drivers[i].child[x].version}"
                paragraph "RAW Code: ${results.data.drivers[i].child[x].RAW}"
                paragraph "<br><br>"
            }
         }
    }
}
}


// Get a developers apps/drivers from JSON URI
def getDeveloperApps(URI, type) {
    log.debug "URI: ${URI} - type: ${type}"
    def requestParams =
	    [	uri:  URI,
			requestContentType: "application/json",
			contentType: "application/json"
		]
    try {
	    httpGet(requestParams)	{	  response -> 
            if (response?.status == 200){
                if(type=="" || type==null) return response
                else {
                    def templist = []
                    switch(type) {
                        case "apps":
                            log.debug "Getting Dev Apps"
                            for(i=0; i<response.data.application.size();i++){
                                templist += response.data.application[i].name
                            }
                            break
                        case "drivers":
                            log.debug "Getting Dev Drivers"
                            for(i=0; i<response.data.drivers.size();i++){
                                templist += response.data.drivers[i].name
                            }
                            break
                    }
                    return templist
                }
            } 
        } 
    } catch (e) { log.error e }
}


// Get a list of pariticpating developers using AppWatchDog for users to select
def getDevelopers(dev, type) {
def wxURI = "https://raw.githubusercontent.com/bptworld/Hubitat/master/MasterAppWatchDog.json"
def devlist = []
def requestParams =
	    [	uri:  wxURI,
			requestContentType: "application/json",
			contentType: "application/json"
		]
try {
	    httpGet(requestParams)	{	  response ->  
       if (response?.status == 200){
           if(dev==null || dev=="") {
           for(i=0;i<response.data.AppWatchDog.developer.size();i++){
               switch(type) {
                   case "devname":
                       devlist+= response.data.AppWatchDog[i].developer
                       break
                   case "devurl":
                       devlist+= response.data.AppWatchDog[i].jsonURL
                       break
               }
           }
           } else {
               if(response.data.AppWatchDog.developer.indexOf(dev)!=-1) return response.data.AppWatchDog[response.data.AppWatchDog.developer.indexOf(dev)].jsonURL
           }
            return devlist
       } else log.warn "Error: ${response?.status}"
    }
 }
 catch (e) { log.error e }
}


//boring stuff
def installed() {
log.debug "Installed with settings: ${settings}"
initialize()
}

def updated() {
log.debug "Updated with settings: ${settings}"
unsubscribe()
initialize()
}

def initialize() {
}
