/*
*  Copyright 2016 elfege
*
*    Software distributed under the License is distributed on an "AS IS" BASIS, 
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*    for the specific language governing permissions and limitations under the License.
*
*    Eternal Sunshine: Adjust dimmers with illuminance and (optional) motion
*
*  Author: Elfege
*/

definition(
    name: "Eternal Sunshine",
    namespace: "elfege",
    author: "elfege",
    description: "Adjust dimmers with illuminance",
    category: "Convenience",
    iconUrl: "http://static1.squarespace.com/static/5751f711d51cd45f35ec6b77/t/59c561cb268b9638e8ba6c23/1512332763339/?format=1500w",
    iconX2Url: "http://static1.squarespace.com/static/5751f711d51cd45f35ec6b77/t/59c561cb268b9638e8ba6c23/1512332763339/?format=1500w",
    iconX3Url: "http://static1.squarespace.com/static/5751f711d51cd45f35ec6b77/t/59c561cb268b9638e8ba6c23/1512332763339/?format=1500w",
)

preferences {

    page name:"pageSetup"
    page name:"settings"
    page name:"Options"

}

def pageSetup() {
    def pageProperties = [
        name:       "pageSetup",
        title:      "Status",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]

    return dynamicPage(pageProperties) {

        section("Select the dimmers you wish to control") {
            input "dimmers", "capability.switchLevel", title: "pick a dimmer", required:true, multiple: true      
        }

        section("Select Illuminance Sensor") {
            input "sensor", "capability.illuminanceMeasurement", title: "pick a sensor", required:true, multiple: false, submitOnChange: true

            if(sensor){
                input "idk", "bool", title:"I don't know the maximum illuminance value for this device", submitOnChange: true, defaultValue: false
                if(!idk)
                {
                    input "maxValue", "decimal", title: "Select max lux value for this sensor", default: false, required:true, submitOnChange: true, defaultValue:defset
                    state.maxValue = maxValue
                }
                else 
                {
                    paragraph "It will take up to 72 hours for the app to learn the maxium illuminance value this device can return, but it will start working immediately based on a preset value"
                    state.maxValue = 1000
                }
                logging( "maxValue = $state.maxValue")

            }

            input "sensor2", "capability.illuminanceMeasurement", title: "pick a second sensor", required:false, multiple: false, submitOnChange: true
            if(sensor2){
                input "switchSensor2", "capability.switch", title: "when this switch is on/off, use second sensor", required:true, multiple:false, submitOnChange:true
                input "switchState", "enum", title: "when it is on or off?", options: ["when on", "when off"]
            }
        }


        section("Location Modes Management") {
            input "modemgt", "bool", title: "Differentiate Maximum Dimmers' Values With Location Mode", submitOnChange: true
            if(modemgt)
            {
                input "modes", "mode", title:"select modes", required: false, multiple: true, submitOnChange: true

                if(modes){
                    def i = 0
                    state.dimValMode = []
                    def dimValMode = []
                    for(modes.size() != 0; i < modes.size(); i++){
                        input "dimValMode${i}", "number", required:true, title: "select a maximum value for ${modes[i]}"

                    }
                }
            }
        }
        section("Motion Management")
        {
            input "usemotion", "bool", title: "Turn On / Off with Motion", submitOnChange: true
            if(usemotion)
            {
                input "motionSensors", "capability.motionSensor", title: "Turn lights on with motion", despcription: "pick a motion sensor", required:false, multiple:true, submitOnChange:true
                if(motionSensors)
                {
                    input "noMotionTime", "number", title: "turn back off after how long?", required: true, description: "time in minutes"
                }
            }
        }
        section("modes")
        {
            input "restrictedModes", "mode", title:"Pause this app if location is in one of these modes", required: false, multiple: true, submitOnChange: true 
        }
        section() {
            label title: "Assign a name", required: false
        }
        section("logging")
        {
            input("enablelogging", "bool", title:"Enable logging", value:false)   
        }

    }
}

def installed() {
    logging("Installed with settings: ${settings}")
    initialize()
}

def updated() {
    logging("updated with settings: ${settings}")
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {

    state.motionEvents = 0
    state.lastMotionEvent = now() as long
        state.override = false
    state.lastDimVal = dimmers[0].currentValue("level")
    state.enablelogging = enablelogging as boolean
        int i = 0
    int s = 0

    if(motionSensors)
    {
        i = 0
        s = motionSensors.size()
        for(s!=0;i<s;i++)
        {
            subscribe(motionSensors[i], "motion", motionHandler)
            log.trace "${motionSensors[i]} subscribed to motion events"
        }
    }
    i = 0
    s = dimmers.size()
    for(s!=0;i<s;i++)
    {
        subscribe(dimmers[i], "level", dimmersHandler)
        subscribe(dimmers[i], "switch", switchHandler)
    }

    subscribe(modes, "mode", locationModeChangeHandler)


    subscribe(sensor, "illuminance", illuminanceHandler)


    schedule("0 0/1 * * * ?", mainloop) 

    logging("initialization ok")
    mainloop()
}

def switchHandler(evt){
    logging("$evt.device is now set to $evt.value - - SOURCE: is $evt.source TYPE is $evt.type")
    state.lastEvent = evt.name
    //mainloop() // infinite feedback loop, idiot!
}

def locationModeChangeHandler(evt)
{
    logging("$evt.name is now in $evt.value mode")   
    mainloop()
}

def dimmersHandler(evt){
    logging("$evt.device set to $evt.value, state.dimVal = $state.dimVal, evt.value == state.dimVal :? ${evt.value == state.dimVal} SOURCE: is $evt.source TYPE is $evt.type")

    state.lastEvent = evt.name 
    //mainloop() // infinite feedback loop, idiot!
}

def illuminanceHandler(evt){
    logging("$evt.device returns $evt.value")

    state.lastEvent = evt.name
    mainloop()

}

def motionHandler(evt){


    log.info "motion is now $evt.value at $evt.device dimmers are: $dimmers"
    if(evt.value == "active")
    {
        dimmers.setLevel(state.dimVal)
        state.motionEvents += 1

        //long lastMotionEvent = now() as long
        log.info """
collecting $evt.value event for $evt.device
state.lastMotionEvent: ${state.lastMotionEvent}"""
        state.lastMotionEvent = now() as long

            }
    mainloop()

}   



def mainloop(){

    /**********************************************************************/
    runIn(10, mainloop) // DEBUG ONLY DON'T FORGET TO COMMENT OUT AFTER
    /**********************************************************************/
    boolean outofmodes = location.mode in restrictedModes
    log.info "outofmodes = $outofmodes || location.mode = ${location.mode} || restrictedModes = $restrictedModes "


    if(outofmodes)
    {
        log.info "App paused due to modes restrictions"
    }
    else
    {

        state.enablelogging = enablelogging as boolean
            // state.enablelogging = true // FOR DEV DEBUG ONLY

            if(stillActive())
        {

            def illum = sensor.currentValue("illuminance")

            def i = 0
            def s = dimmers.size()
            def dimmersLevelState = []
            def dimmersSwitchState = []

            for(s!=0;i<s;i++)
            {
                def dev = dimmers[i]
                def lvState = dev.currentValue("level") 
                def swState = dev.currentValue("switch") 
                dimmersLevelState << lvState
                dimmersSwitchState << swState
            }

            log.info"dimmersLevelState : ${dimmersLevelState} dimmers states are ${dimmersSwitchState}, illuminance is: $illum"
            def dimVal = getDimVal()
            setDimmers(dimVal)
        }
        else 
        {
            logging("no motion...")
            dimmers.off() 
        }
    }

}

def getDimVal()
{
    def illum = sensor.currentValue("illuminance")
    if(sensor2){
        if(switchSensor2.currentValue("switch") == "switchState")
        {
            illum2 = sensor2.currentValue("illuminance")
        }
    }

    //def latest = sensor.currentState("illuminance")
    def Unit = "lux"//latest.getUnit()


    if(!state.maxValue)
    {
        state.maxValue = 1000
        log.info "state.maxValue reset to 1000 because it was null"
    }
    // learn max value
    if(illum.toInteger() > state.maxValue.toInteger())
    {
        state.maxValue = illum
        log.info "new maximum lux value registered as: $state.maxValue"
    }

    def xa = 0    // min illuminance
    def ya = 100  // corresponding dimmer level
    def xb = state.maxValue.toInteger()   // max illuminance
    def yb = 0    // corresponding dimmer level

    logging("xa = $xa,ya = $ya, xb = $xb, yb = $yb, maxValue = $maxValue, state.maxValue = $state.maxValue")

    def coef = (yb-ya)/(xb-xa)  // slope
    def b = ya - coef * xa // solution to ya = coef*xa + b //

    def dimVal = coef*illum + b

    dimVal = dimVal.toInteger()

    logging("ALGEBRA RESULTS:  slope = $coef, b = $b, result = $dimVal | illuminance : ${illum} ${Unit}")
    return dimVal
}

def setDimmers(int val){

    if(val < 0)
    {
        val = 0
    }
    def isNotOff = true

    def i = 0
    def s = dimmers.size()

    if(modes)
    {
        if(location.mode in modes){

            while(location.mode != modes[i]){i++}
            def valMode = "dimValMode${i}" // set as max
            def maxvalinthismode = settings.find{it.key == valMode}.value

            if(val > maxvalinthismode)
            {
                logging("ADJUSTED WITH CURRENT MODE == > valMode = $valMode && maxvalinthismode = $maxvalinthismode")
                val = maxvalinthismode
            }
        }
    }

    state.dimVal = val// for override purposes 
    int c = 0
    while(state.dimVal != val && c < 1000)
    {
        state.dimVal = val
        logging("updating state.dimVal ?: $state.dimVal == $val ${state.dimValdimVal == val}")
        c++
            }

    if(state.dimVal == val)
    {
        
        logging(
            """
-------------------
isNotOff = $isNotOff
motionSensors = $motionSensors
-------------------
"""
               )
        i = 0
        for(s != 0; i < s; i++){
            isNotOff = dimmers[i].currentValue("switch") == "on" 
            if(isNotOff || motionSensors){ // with motion off status doesn't stop the app
                dimmers[i].setLevel(val)

                logging("${dimmers[i]} set to $val")
            }
            else {
                logging("${dimmers[i]} is off")
            }
        }

    }
    else 
    {
        log.info "FAILED TO UPDATE DATABASE"
    }

    logging("state.dimVal = $state.dimVal val = $val")
}


boolean stillActive()
{

    log.info "state.motionEvents = $state.motionEvents"
    int events = state.motionEvents
    boolean result = true // default is true  always return Active = true when no sensor is selected by the user

    if(motionSensors)
    {
        logging("motion test is running and collecting events at $motionSensors")

        long lastMotionEvent = now() - state.lastMotionEvent
        long timeOutMillis = noMotionTime * 1000 * 60 as long
            boolean MotiontimeOut = lastMotionEvent > timeOutMillis
        int minutes = lastMotionEvent/1000/60
        //log.debug "minutes = $minutes"
        log.info "MotiontimeOut = $MotiontimeOut | lastMotionEvent = $lastMotionEvent" // ${if((lastMotionEvent/1000)>=60){"${Math.round(minutes)} minutes"}else{"${(lastMotionEvent/1000)} seconds"}} ago"

        if(MotiontimeOut)
        {
            state.motionEvents = 0
            log.info "motion time out!"
        }
        //result = state.motionEvents >= 1 // loop breaks when 1 sensor returns more than 0 event, so see if that's the case here

        result = !MotiontimeOut
    } 

    log.info "motion returns ${result} with $events events in the last $noMotionTime minutes"
    return result
}

def resetMotionEvents()
{
    state.motionEvents = 0   
}

def logging(message)
{
    state.enablelogging = true
    if(state.enablelogging) log.debug message
}

/* 
 // event collections, once within iteration, can take several seconds on HE platform, 
bugging everything else in the process, for some reason, even after using code recommended by HE's staff... 

//noMotionTime = 1000  /// TEST ONLY
long deltaMinutes = noMotionTime * 1000 * 60   
int s = motionSensors.size() 
int i = 0
def thisDeviceEvents = []
int events = 0

for(s != 0; (i < s && events == 0); i++) // if any of the sensors returns at least one event within the time threshold, then break this loop and return true
{ 
def thisDeviceEvents = motionSensors[i].eventsSince(new Date(now() - deltaMinutes)).findAll{it.value == "active"} // collect motion events for each sensor separately
events += thisDeviceEvents.size() 
}
*/
