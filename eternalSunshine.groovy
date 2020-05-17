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

    if(state.paused)
    {
        log.debug "new app label: ${app.label}"
        while(app.label.contains(" (Paused) "))
        {
            app.updateLabel(app.label.minus("(Paused)" ))
        }
        //app.updateLabel(app.label.minus("<font color = 'red'> (Paused) </font>" ))
        app.updateLabel(app.label + ("<font color = 'red'> (Paused) </font>" ))

    }
    else if(app.label.contains("(Paused)"))
    {
        app.updateLabel(app.label.minus("<font color = 'red'> (Paused) </font>" ))
        //app.updateLabel(app.label.minus(" "))
        while(app.label.contains(" (Paused) "))        {app.updateLabel(app.label.minus("(Paused)" ))}

        log.debug "new app label: ${app.label}"
    }


    def pageProperties = [
        name:       "pageSetup",
        title:      "${app.label}",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]

    return dynamicPage(pageProperties) {

        if(state.paused == true)
        {
            state.button_name = "resume"
            logging("button name is: $state.button_name")
        }
        else 
        {
            state.button_name = "pause"
            logging("button name is: $state.button_name")
        }
        section("")
        {
            input "pause", "button", title: "$state.button_name"
        }

        section("Select the dimmers you wish to control") {
            input "dimmers", "capability.switchLevel", title: "pick a dimmer", required:true, multiple: true
            input "override", "bool", title: "Pause this app when all dimmers are off", defaultValue: false
        }
        section("Select Illuminance Sensor") {
            input "sensor", "capability.illuminanceMeasurement", title: "pick a sensor", required:true, multiple: false, submitOnChange: true

            if(sensor){
                input "idk", "bool", title:"I don't know the maximum illuminance value for this device", submitOnChange: true, defaultValue: false
                if(!idk)
                {
                    input "maxValue", "number", title: "Select max lux value for this sensor", default: false, required:true, submitOnChange: true, defaultValue:defset
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
                input "modes", "mode", title:"select modes", required: true, multiple: true, submitOnChange: true

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
        section("Motion Management")        {
            input "usemotion", "bool", title: "Turn On / Off with Motion", submitOnChange: true
            if(usemotion)
            {
                input "motionSensors", "capability.motionSensor", title: "Turn lights on with motion", despcription: "pick a motion sensor", required:false, multiple:true, submitOnChange:true
                if(motionSensors)
                {
                    input "noMotionTime", "number", title: "turn back off after how long?", required: true, description: "time in minutes"
                    input "modetimeout", "bool", title: "Differentiate Timeouts With Location Mode", submitOnChange: true
                    if(modetimeout)
                    {
                        input "timeoutModes", "mode", title:"select modes", required: true, multiple: true, submitOnChange: true

                        if(timeoutModes){
                            def i = 0
                            state.timeoutValMode = []
                            def timeoutValMode = []
                            for(timeoutModes.size() != 0; i < timeoutModes.size(); i++){
                                input "timeoutValMode${i}", "number", required:true, title: "select a timeout value for ${timeoutModes[i]}"
                            }
                        }
                    }
                }
            }
        }
        section("Other App Conflict Management")
        {
            input "otherApp", "bool", title: "These dimmers are turned off by another app", submitOnChange:true
            paragraph "IMPORTANT: Enable this option if you know that these dimmers might be turned off by another app"
        }
        section("modes")        {
            input "restrictedModes", "mode", title:"Pause this app if location is in one of these modes", required: false, multiple: true, submitOnChange: true 
        }
        section() {
            label title: "Assign a name", required: false
        }
        section("logging")
        {
            input"enablelogging", "bool", title:"Enable logging", value:false, submitOnChange:true
            input"enabledescription", "bool", title:"Enable Description Text", value:false, submitOnChange:true
        }
        section("")
        {
            input "update", "button", title: "UPDATE"
            input "run", "button", title: "RUN"
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

    state.maxValue = 1000
    if(!idk)
    {
        state.maxValue = maxValue
    }

    if(enablelogging == true){
        runIn(1800, disablelogging)
        description("disablelogging scheduled to run in ${1800/60} minutes")
    }
    else 
    {
        log.warn "debug logging disabled!"
    }

    state.motionEvents = 0
    state.override = false
    state.lastDimVal = dimmers[0].currentValue("level")


    int i = 0
    int s = 0

    if(usemotion && motionSensors)
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

    if(location.mode in restrictedModes)
    {
        logging("App paused due to modes restrictions")
        return
    }
    logging("$evt.device is now set to $evt.value - - SOURCE: is $evt.source TYPE is $evt.type isPhysical: ${evt.isPhysical()}")
    state.lastEvent = evt.name
    //mainloop() // infinite feedback loop!
}
def locationModeChangeHandler(evt){
    logging("$evt.name is now in $evt.value mode")   
    mainloop()
}
def dimmersHandler(evt){

    if(location.mode in restrictedModes)
    {
        logging("App paused due to modes restrictions")
        return
    }
    logging("$evt.device set to $evt.value, state.dimVal = $state.dimVal, evt.value == state.dimVal :? ${evt.value == state.dimVal} SOURCE: is $evt.source TYPE is $evt.type")

    //mainloop() // infinite feedback loop!
}
def illuminanceHandler(evt){

    if(location.mode in restrictedModes)
    {
        logging("App paused due to modes restrictions")
        return
    }
    logging("$evt.name is now $evt.value")

    // learn max value if required
    def illum = evt.value
    if(idk && illum.toInteger() > state.maxValue.toInteger())
    {
        state.maxValue = illum
        logging("new maximum lux value registered as: $state.maxValue")
    }
    else 
    {
        logging "max value preset by user: ${maxValue}lux"
        state.maxValue = maxValue
    }

    runIn(1,mainloop)

}
def motionHandler(evt){

    if(location.mode in restrictedModes)
    {
        logging("App paused due to modes restrictions")
        return
    }

    description("$evt.device is $evt.value")

    if(usemotion) mainloop()

}
def appButtonHandler(btn) {

    if(location.mode in restrictedModes)
    {
        logging("App paused due to modes restrictions")
        return
    }
    switch(btn) {
        case "pause":state.paused = !state.paused
        log.debug "state.paused = $state.paused"
        if(state.paused)
        {
            log.debug "unsuscribing from events..."
            unsubscribe()  
            log.debug "unschedule()..."
            unschedule()
            break
        }
        else
        {
            updated()            
            break
        }
        case "update":
        state.paused = false
        updated()
        break
        case "run":
        if(!state.paused) mainloop()
        break

    }
}
def mainloop(){


    if(location.mode in restrictedModes)
    {
        logging("App paused due to modes restrictions")
        return
    }
    boolean Active = stillActive()
    boolean dimOff = dimmers.findAll{it.currentValue("switch") == "off"}.size() == dimmers.size() 
    boolean dimAreOff = false
    poll()
    logging("""
nmber of dimmers that are off = ${dimmers.findAll{it.currentValue("switch") == "off"}.size()}
dimmers.size() = ${dimmers.size()}
""")
    // if we don't use motion, then dimAreOff is based on actual status (defined above), otherwise, it's false by default
    // so to allow the app to continue managing the dimmers based on motion and illuminance
    dimAreOff = usemotion ? false : dimOff 
    logging("1: dimAreOff = $dimAreOff")

    // now, if user has selected the override option, then the app will take the off status as override and not turn them back on
    // whether or not usemotion is true (a bit redundant but necessary to avoid discrepancies). 
    dimAreOff = ((override && dimOff) || (dimOff && !usemotion)) ?  true : dimOff
    logging("2: dimAreOff = $dimAreOff")


    logging("""
usemotion = $usemotion
dimAreOff = $dimAreOff
dimOff = $dimOff
outofmodes = $outofmodes
override = $override ${usemotion ? "" : "(Redundant when not using motion)"}
Active = $Active ${usemotion ? "" : "but doesn't use motion"}
location.mode = ${location.mode}
restrictedModes = $restrictedModes

""")

    if(Active && !pauseApp && !dimAreOff)
    {
        def dimVal = getDimVal()
        setDimmers(dimVal)
    }
    else if(!usemotion && dimAreOff)
    {
        log.info "dimmers are off and managed by another app, $app.label will resume when they're turned back on dimAreOff = $dimAreOff"
    }
    else 
    {
        logging("no motion...")
        dimmers.off() 
    }

}
def getDimVal(){

    def currentSensor = switchSensor2?.currentValue("switch") == "switchState" ? sensor2 : sensor
    def illum = currentSensor.currentValue("illuminance")

    logging """
illuminance sensor is: $currentSensor
illuminance is: $illum lux

"""
    def maxIllum = (state.maxValue < 1000 || state.maxValue == null) ? 1000 : state.maxValue 


    def xa = 0    // min illuminance
    def ya = 100  // corresponding dimmer level
    def xb = state.maxValue.toInteger()   // max illuminance
    def yb = 0    // corresponding dimmer level

    logging("xa = $xa,ya = $ya, xb = $xb, yb = $yb, maxValue = $maxValue, state.maxValue = $state.maxValue")

    def slope = (yb-ya)/(xb-xa)  // slope
    def b = ya - slope * xa // solution to ya = slope*xa + b //

    def dimVal = slope*illum + b
    dimVal = dimVal.toInteger()

    // if otherApp is true, then never completely turn off the dimmers so Eternal Sunshine knows when not to mess around
    dimVal = otherApp ? dimVal < 1 ? dimVal = 1 : dimVal : dimVal < 0 ? dimVal = 0 : dimVal

    logging("ALGEBRA RESULTS:  slope = $slope, b = $b, illuminance : ${illum} ${Unit} --> result = $dimVal ")
    return dimVal
}
def setDimmers(int val){

    if(val < 0)
    {
        val = 0
    }

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

    i = 0
    for(s!=0;i<s;i++)
    {
        def a = dimmers[i]
        def aVal = dimmers[i].currentValue("level")
        def message = "$a is currently at ${aVal}% and needs to be set to ${val}%"
        logging(message)
        if(aVal==val){message = "$a level is ok"}
        logging(message)

        a.on()
        if(aVal != val)
        {
            a.setLevel(val)
            logging("${dimmers[i]} set to $val ---")
        }

    }
}


boolean stillActive()
{
    boolean result = true
    int events = 0
    boolean pauseApp = false

    if(usemotion)
    {
        def timeout = getTimeout()
        long deltaMinutes = timeout * 1000 * 60   
        int s = motionSensors.size() 
        int i = 0
        def DeviceEvents = []


        DeviceEvents = motionSensors.findAll{it.eventsSince(new Date(now() - deltaMinutes)).findAll{it.value == "active"}} // collect motion events for each sensor separately
        //description("$DeviceEvents ")
        events = DeviceEvents.size()

        /*
for(s != 0; i < s; i++) // if any of the sensors returns at least one event within the time threshold, then return true
{ 
thisDeviceEvents = motionSensors[i].eventsSince(new Date(now() - deltaMinutes)).findAll{it.value == "active"} // collect motion events for each sensor separately
events += thisDeviceEvents.size() 
}*/
        result = events > 0
    }
    description("$events active motion events in the last $timeout minutes stillActive() returns ${result}")
    return result
}

def getTimeout()
{
    def result = noMotionTime // default
    valMode = location.mode
    if(timeoutModes && location.mode in timeoutModes)
    {
        int s = timeoutModes.size()
        int i = 0
        logging("timeoutModes: $timeoutModes")
        while(i < s && location.mode != timeoutModes[i]){i++}
        logging("${location.mode} == ${timeoutModes[i]} (timeoutModes${i} : index $i) ?: ${location.mode == timeoutModes[i]}")
        valMode = "timeoutValMode${i}" // we need the key as string to search its corresponding value within settings
        logging("valMode = $valMode")
        result = settings.find{it.key == valMode}?.value
        logging("valMode.value == $result")
    }
    if(result == null)
    {
        result = noMotionTime
    }
    logging("timeout is: $result  ${if(modetimeout){"because home in $location.mode mode"}}")
    return result
}

def resetMotionEvents()
{
    logging("No motion event has occured during the past $noMotionTime minutes")
    state.motionEvents = 0   
}

def logging(msg)
{
    def debug = settings.find{it.key == "enablelogging"}?.value
    //log.warn "debug = $debug"
    if (debug) log.debug msg
}

def description(msg)
{
    def debug = settings.find{it.key == "enablelogging"}?.value
    //log.warn "debug = $debug"
    if (enabledescription) log.info msg
}

def disablelogging()
{
    app.updateSetting("enablelogging",[value:"false",type:"bool"])
    log.warn "logging disabled!"
}

def poll()
{
    boolean haspoll = false
    boolean hasrefresh = false
    dimmers.each{
        if(it.hasCommand("poll")){ it.poll() }else{logging("$it doesn't have poll command")}
        if(it.hasCommand("refresh")){ it.refresh() }else{logging("$it doesn't have refresh command")}
    }
}


