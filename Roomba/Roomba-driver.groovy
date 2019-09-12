metadata {
    definition (name: "Roomba", namespace: "roomba", author: "dmeglio@gmail.com") {
		capability "Battery"
        capability "Consumable"
		capability "Actuator"
        
        attribute "cleanStatus", "string"
        
        command "start"
        command "stop"
        command "pause"
        command "resume"
        command "dock"
    }
}

def start() 
{
    parent.handleStart(device, device.deviceNetworkId.split(":")[1])
}

def stop() 
{
    parent.handleStop(device, device.deviceNetworkId.split(":")[1])
}

def pause() 
{
    parent.handlePause(device, device.deviceNetworkId.split(":")[1])
}

def resume() 
{
    parent.handleResume(device, device.deviceNetworkId.split(":")[1])
}

def dock() 
{
    parent.handleDock(device, device.deviceNetworkId.split(":")[1])
}
