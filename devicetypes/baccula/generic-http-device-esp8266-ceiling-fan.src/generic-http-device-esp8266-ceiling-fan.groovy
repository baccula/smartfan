/**
 *  Modification of:
 *
 *
 *  Generic HTTP Device v1.0.20160402
 *
 *  Source code can be found here: https://github.com/JZ-SmartThings/SmartThings/blob/master/Devices/Generic%20HTTP%20Device/GenericHTTPDevice.groovy
 *
 *  Copyright 2016 JZ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
	definition (name: "Generic HTTP Device - ESP8266 - Ceiling Fan", author: "baccula", namespace:"baccula") {
		capability "Switch"
		capability "Switch Level"
        capability "Refresh"

		command "fanOff"
        command "lowSpeed"
        command "medSpeed"
        command "highSpeed"
        command "push"
        
        attribute "currentSpeed", "string"
	}


	preferences {
		input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
		input("DevicePort", "string", title:"Device Port", description: "Please enter port 80 or your device's Port", required: true, displayDuringSetup: true)
		input("DevicePath", "string", title:"URL Path", description: "Rest of the URL, include forward slash.", displayDuringSetup: true)
		input(name: "DevicePostGet", type: "enum", title: "POST or GET", options: ["POST","GET"], required: true, displayDuringSetup: true)
		//input("DeviceBodyText", "string", title:'Body Content', description: 'Type in "GateTrigger=" or "CustomTrigger="', required: true, displayDuringSetup: true)
	}

	simulator {
	}

	tiles {
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: '${name}', action: "switch.on", icon:"st.Lighting.light24", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: '${name}', action: "switch.off", icon:"st.Lighting.light24", backgroundColor: "#79b821"    

		}
        //Slider not show in display but kept in for trouble shooting / testing, if needed. 
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
			state "level", action:"switch level.setLevel"
		}
        
        //displays current speed as off, low, med, high
        valueTile("currentSpeed", "device.currentSpeed", canChangeIcon: false, inactiveLabel: false, decoration: "flat") {
            state ("default", label:'${currentValue}')
        }

		//Speed control row
        standardTile("fanOff", "device.level", inactiveLabel: false, decoration: "flat") {
            state "fanOff", label:'OFF', action:"fanOff", icon:"st.Home.home30"
        }
        standardTile("lowSpeed", "device.level", inactiveLabel: false, decoration: "flat") {
            state "lowSpeed", label:'LOW', action:"lowSpeed", icon:"st.Home.home30"
        }
        standardTile("medSpeed", "device.level", inactiveLabel: false, decoration: "flat") {
            state "medSpeed", label:'MED', action:"medSpeed", icon:"st.Home.home30"
        }
        standardTile("highSpeed", "device.level", inactiveLabel: false, decoration: "flat") {
            state "highSpeed", label:'HIGH', action:"highSpeed", icon:"st.Home.home30"
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		main(["switch"])
		details(["switch", "refresh", "currentSpeed", "fanOff", "lowSpeed", "medSpeed", "highSpeed"])
	}
}

def updated(){
		// Device-Watch simply pings if no device events received for 32min(checkInterval)
		sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	def value = "when off"
	if (cmd.configurationValue[0] == 1) {value = "when on"}
	if (cmd.configurationValue[0] == 2) {value = "never"}
	[name: "indicatorStatus", value: value, displayed: false]
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd) {
	[name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	log.debug "manufacturerId:   ${cmd.manufacturerId}"
	log.debug "manufacturerName: ${cmd.manufacturerName}"
	log.debug "productId:        ${cmd.productId}"
	log.debug "productTypeId:    ${cmd.productTypeId}"
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)
	updateDataValue("manufacturer", cmd.manufacturerName)
	createEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}


def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def fanOff() {
	log.debug "Fan Off!!!"
    sendEvent(name: "switch", value: "on", isStateChange: false, display: false)
    sendEvent(name: "currentSpeed", value: "OFF" as String)
	runCmd("fan=fanoff")
}

def on() {
	log.debug "Switch On!!!"
	sendEvent(name: "switch", value: "on", isStateChange: true, display: false)
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	])
}

def off() {
	log.debug "Switch Off!!!"
	sendEvent(name: "switch", value: "off", isStateChange: true, display: false)
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	])
}
def highSpeed() {
	log.debug "High Speed!!!"
    sendEvent(name: "switch", value: "on", isStateChange: false, display: false)
    sendEvent(name: "currentSpeed", value: "HIGH" as String)
	runCmd("fan=speedhi")
}
def medSpeed() {
	log.debug "Medium Speed!!!"
    sendEvent(name: "switch", value: "on", isStateChange: false, display: false)
    sendEvent(name: "currentSpeed", value: "MED" as String)
	runCmd("fan=speedmed")
}
def lowSpeed() {
	log.debug "Low Speed!!!"
    sendEvent(name: "switch", value: "on", isStateChange: false, display: false)
    sendEvent(name: "currentSpeed", value: "LOW" as String)
	runCmd("fan=speedlo")
}

def setLevel(val){
    log.info "setLevel $val"
    sendEvent(name:"level",value:val)        											
        if (val == 0){ 
        	runCmd("fan=fanoff")
        	sendEvent(name: "switch", value: "on", isStateChange: false, display: false)
       		log.info "Fan off"
  		  	sendEvent(name: "currentSpeed", value: "OFF" as String)
   			}
        if ((val >= 1) & (val <=30)) {
        	runCmd("fan=speedlo")
        	sendEvent(name: "switch", value: "on", isStateChange: true, display: false)
			log.info "Fan low"
    		sendEvent(name: "currentSpeed", value: "LOW" as String)
       		}
        if ((val >= 31) & (val <=60)) {
        	runCmd("fan=speedmed")
        	sendEvent(name: "switch", value: "on", isStateChange: true, display: false)
        	log.info "Fan Med"
    		sendEvent(name: "currentSpeed", value: "MED" as String)
			}
        if ((val >= 61) & (val <=100)) {
        	runCmd("fan=speedhi")
        	sendEvent(name: "switch", value: "on", isStateChange: true, display: false)
        	log.info "Fan High"
    		sendEvent(name: "currentSpeed", value: "HIGH" as String)
        	}                                                  
	}


def runCmd(String varCommand) {
	def host = DeviceIP
	def hosthex = convertIPtoHex(host).toUpperCase()
	def porthex = convertPortToHex(DevicePort).toUpperCase()
	device.deviceNetworkId = "$hosthex:$porthex"
	def userpassascii = "${HTTPUser}:${HTTPPassword}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()

	log.debug "The device id configured is: $device.deviceNetworkId"

	//def path = DevicePath
	def path = DevicePath + varCommand
	log.debug "path is: $path"
	log.debug "Uses which method: $DevicePostGet"
	def body = ""//varCommand
	log.debug "body is: $body"

	def headers = [:]
	headers.put("HOST", "$host:$DevicePort")
	headers.put("Content-Type", "application/x-www-form-urlencoded")
	if (HTTPAuth) {
		headers.put("Authorization", userpass)
	}
	log.debug "The Header is $headers"
	def method = "GET"
	try {
		if (DevicePostGet.toUpperCase() == "GET") {
			method = "GET"
			}
		}
	catch (Exception e) {
		settings.DevicePostGet = "POST"
		log.debug e
		log.debug "You must not have set the preference for the DevicePOSTGET option"
	}
	log.debug "The method is $method"
	try {
		def hubAction = new physicalgraph.device.HubAction(
			method: method,
			path: path,
			body: body,
			headers: headers
			)
		hubAction.options = [outputMsgToS3:false]
		//log.debug hubAction
		hubAction
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x20: 1, 0x70: 1])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	if (result?.name == 'hail' && hubFirmwareLessThan("000.011.00602")) {
		result = [result, response(zwave.basicV1.basicGet())]
		log.debug "Was hailed: requesting state update"
	} else {
		log.debug "Parse returned ${result?.descriptionText}"
	}
	return result
}

def poll() {
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	])
}

/**
  * PING is used by Device-Watch in attempt to reach the Device
**/
def ping() {
		refresh()
}

def refresh() {
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	])
}

private String convertIPtoHex(ipAddress) {
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	//log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
	return hex
}
private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
	//log.debug hexport
	return hexport
}
private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}
private String convertHexToIP(hex) {
	//log.debug("Convert hex to ip: $hex")
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
private getHostAddress() {
	def parts = device.deviceNetworkId.split(":")
	//log.debug device.deviceNetworkId
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
}