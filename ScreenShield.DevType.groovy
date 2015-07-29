metadata {
	// Automatically generated. Make future change here.
	definition (name: "Screen Shield on/off", namespace: "CosmicPuppy", author: "Terry Gauchat") {
		capability "Actuator"
		capability "Switch"
		capability "Sensor"

        attribute "position", "enum", ["raising", "lowering", "up", "down", "stopping", "stopped", "enabled", "disabled"]

        command "enable"
        command "disable"
        attribute "override", "enum", ["stopping", "stopped", "enabled", "disabled"]
	}

	// Simulator metadata
	simulator {
		status "on":  "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6E"
		status "off": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6666"
        status "stop": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A73746F70"

		reply "raw 0x0 { 00 00 0a 0a 6f 6e }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6E"
		reply "raw 0x0 { 00 00 0a 0a 6f 66 66 }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6666"
        reply "raw 0x0 { 00 00 0a 0a 73 74 6f 70 }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A73746F70"
	}

	// UI tile definitions
	tiles {
		standardTile("switchTile", "device.switch", width: 1, height: 1, canChangeIcon: true, canChangeBackground: false) {
			state("on",  label:'${name}', action:"switch.off", icon:"st.doors.garage.garage-closed", backgroundColor: "#79b821", nextState:"turningOff")
			state("off", label:'${name}', action:"switch.on",  icon:"st.doors.garage.garage-open",   backgroundColor: "#0000ff", nextState:"turningOn")
			state("turningOn",  label:'${name}', icon:"st.doors.garage.garage-closing",   backgroundColor: "#0000ff")
			state("turningOff", label:'${name}', icon:"st.doors.garage.garage-opening",   backgroundColor: "#79b821")
        }
   		standardTile("positionTile", "device.position", width: 2, height: 2) {
			state("down", label:'${name} v06', action:"switch.off",   icon:"st.doors.garage.garage-closed", backgroundColor:"#79b821", nextState:"raising")
			state("up",   label:'${name} v06', action:"switch.on",    icon:"st.doors.garage.garage-open",   backgroundColor:"#0000ff", nextState:"lowering")
			state("lowering", label:'${name}', icon:"st.doors.garage.garage-closing", backgroundColor:"#7fff00", nextState: "down")
			state("raising",  label:'${name}', icon:"st.doors.garage.garage-opening", backgroundColor:"#37fdfc", nextState: "up")
            state("stopped",  label:'${name}', icon:"st.doors.garage.garage-opening", backgroundColor:"#888888")
            state("enabled",  label:'${name}', icon:"st.doors.garage.garage-closed",  backgroundColor:"#555555")
		}
        /* TODO: Perhaps make this tile stop AND disable the on/off/up/down buttons? */
		standardTile("stopTile", "device.override", width: 1, height: 1, canChangeIcon: false, canChangeBackground: true, decoration: flat) {
            state("enabled",  label:'STOP',   action:"disable", icon:"st.sonos.stop-btn", backgroundColor: "#cccccc", nextState:"stopping", defaultState:true)
			state("disabled", label:'ENABLE', action:"enable",  icon:"st.sonos.play-btn", backgroundColor: "#bbbbbb")
            state("stopping",  label:'stopping', icon:"st.sonos.pause-btn", backgroundColor: "#cccccc")
        }

        main (["positionTile","switchTile"])
		details (["positionTile","switchTile","stopTile"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
    def evt_onoff = []
    def evt_position = []
	def value = zigbee.parse(description)?.text
	def name = value in ["on","off"] ? "switch" : null

//    log.trace "Parse returned ${evt_onoff?.descriptionText}, name: ${name} value: ${value}."
//	  log.debug "Parse argument description: ${description}"

    if ( name != null ) {
        evt_onoff = createEvent(name: name, value: value)
//        log.trace "Parse returned ${evt_onoff?.descriptionText}, name: ${name} value: ${value}."

        switch (value) {
        	case "on":
            	evt_position = createEvent(name: "position", value: "down", displayed: true, isStateChange: true)
                break;

        	case "off":
            	evt_position = createEvent(name: "position", value: "up", displayed: true, isStateChange: true)
                break;

			case "disable":
            	evt_position = createEvent(name: "position", value: "stopped", displayed: true, isStateChange: true)
                break;
		}
    }

	return [ evt_onoff, evt_position ]
}

// Commands sent to the device
/**
 * TODO: The sendEvent maybe should happen after the shield responds or something.
 *       Currently it looks like the system is allowing a new command to be send before the first is processed.
 *       That situation may be desirable, but then interrupts are needed in the Arduino sketch. For "stop" this would be good.
 */
def on() {
   	sendEvent(name: "position", value: "lowering", displayed: true, isStateChange: true)
	zigbee.smartShield(text: "on").format()
}

def off() {
   	sendEvent(name: "position", value: "raising", displayed: true, isStateChange: true)
    zigbee.smartShield(text: "off").format()
}

def disable() {
 	sendEvent(name: "override", value: "disabled", displayed: true, isStateChange: true)
    sendEvent(name: "position", value: "stopped", displayed: true, isStateChange: true)
    zigbee.smartShield(text: "disable").format()
}

def enable() {
 	sendEvent(name: "override", value: "enabled", displayed: true, isStateChange: true)
    /* TODO: We should save the last state (down or up) so we can enable the correct direction. But for now, make it always assume screen was up. */
    sendEvent(name: "position", value: "up", displayed: true, isStateChange: true)
    sendEvent(name: "switch", value: "off", displayed: true, isStateChange: true)
    zigbee.smartShield(text: "enable").format()
}


/* =========== */
/* End of File */
/* =========== */