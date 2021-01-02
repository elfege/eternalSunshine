/*
*  Copyright 2016 elfege
*
*    Eternal Sunshine©: Adjust dimmers with illuminance and (optional) motion
*
*    Software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
*    
*    The name Eternal Sunshine as an illuminance management software is protected under copyright
*
*  Author: Elfege
*/

definition(
    name: "Eternal Sunshine©",
    namespace: "elfege",
    author: "elfege",
    description: "Adjust dimmers with illuminance",
    category: "Convenience",
    iconUrl: "http://statric1.squarespace.com/static/5751f711d51cd45f35ec6b77/t/59c561cb268b9638e8ba6c23/1512332763339/?format=1500w",
    iconX2Url: "http://static1.squarespace.com/static/5751f711d51cd45f35ec6b77/t/59c561cb268b9638e8ba6c23/1512332763339/?format=1500w",
    iconX3Url: "http://static1.squarespace.com/static/5751f711d51cd45f35ec6b77/t/59c561cb268b9638e8ba6c23/1512332763339/?format=1500w",
)

preferences {

    page name:"pageSetup"

}
def pageSetup() {

    if(atomicState.paused)
    {
        logging "new app label: ${app.label}"
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

        logging "new app label: ${app.label}"
    }


    def pageProperties = [
        name:       "pageSetup",
        title:      "${app.label}",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]

    return dynamicPage(pageProperties) {

        section("")
        {
            atomicState.button_name = atomicState.paused == true ? "resume" : "pause"
            input "pause", "button", title: "$atomicState.button_name"
            input "buttonPause", "capability.pushableButton", title: "Pause/resume this app when I press a button", multiple: true, required: false, submitOnChange:true
            if(buttonPause)
            {
                boolean doubletapable = buttonPause.every{element -> element.hasCapability("DoubleTapableButton")}
                boolean holdable = buttonPause.every{element -> element.hasCapability("HoldableButton")}
                boolean pushable = buttonPause.every{element -> element.hasCapability("PushableButton")}
                boolean releasable = buttonPause.every{element -> element.hasCapability("ReleasableButton")}
log.debug """
doubletapable ? $doubletapable
holdable ?      $holdable
pushable ?      $pushable
releasable ?    $releasable
"""
                def list = releasable ?  ["pushed", "held", "doubleTapped", "released"] : doubletap ? ["pushed", "held", "doubleTapped"] : holdable ? ["pushed", "held"] : ["push"]

                input "buttonAction", "enum", title: "Select a button action", options:["pushed", "held", "doubleTapped"], required:true, submitOnChange:true
                input "buttonNumber", "number", title: "Select button number", required: true
            }
        }

        section("Select the dimmers you wish to control") {
            input "dimmers", "capability.switchLevel", title: "pick a dimmer", required:true, multiple: true
            input "override", "bool", title: "OVERRIDE ? (prevents conflicts with other motion lighting apps)", defaultValue: false, submitOnChange:true
            if(override)
            {
                def message = "error"
                if(usemotion)
                {
                    app.updateSetting("override",[value:"false",type:"bool"]) // fool proofing...
                    message = "You cannot use the override option and motion sensors at the same time!"
                }
                else {
                    message = "It is strongly advised not to use this app in parallel with any other motion lighting app controling the same dimmers. This option (OVERRIDE) may not work depending on your setup or even depending on the quality of your mesh network. If you see that your lights turn on/off constantly when there is no motion, please uninstall your motion lighting app, disable the override option and use the motion lighting built-in within this app for better compatibility. Thank you!"
                }

                paragraph "<div style=\"width:102%;background-color:#1C2BB7;color:red;padding:4px;font-weight: bold;box-shadow: 1px 2px 2px #bababa;margin-left: -10px\">${message}</div>"
            }
        }
        section("Select Illuminance Sensor") {
            input "sensor", "capability.illuminanceMeasurement", title: "pick a sensor", required:true, multiple: false, submitOnChange: true

            if(sensor){
                input "idk", "bool", title:"I don't know the maximum illuminance value for this device", submitOnChange: true, defaultValue: false

                if(!idk)
                {
                    input "maxValue", "number", title: "Select max lux value for this sensor", default: false, required:true, submitOnChange: true
                }
                else 
                {
                    if(logarithm) { app.updateSetting("logarithm",[value:"false",type:"bool"])

                                   def message = "This option isn't compatible with logarithmic variations - are you sure ? If Yes, enable it again"
                                   paragraph formatText(message, "red", "white")

                                  }
                    else 
                    {
                        paragraph "It will take up to 72 hours for the app to learn the maxium illuminance value this device can return, but it will start working immediately based on a preset value"
                        atomicState.maxValue = atomicState.maxValue == null ? 1000 : atomicState.maxValue // temporary value
                    }
                }
                logging( "maxValue = $atomicState.maxValue")

            }

            input "sensor2", "capability.illuminanceMeasurement", title: "pick a second sensor", required:false, multiple: false, submitOnChange: true
            if(sensor2){
                input "switchSensor2", "capability.switch", title: "when this switch is on/off, use second sensor", required:true, multiple:false, submitOnChange:true
                input "switchState", "enum", title: "when it is on or off?", options: ["on", "off"], required: true, submitOnChange:true
                if(switchSensor2 && switchState)
                {
                    input "highLuxSwitch", "bool", title:"when $sensor returns high lux, turn $switchState $switchSensor2", submitOnChange:true
                    if(highLuxSwitch)
                    {
                        input "onlyIfTempHigh", "bool", title:"Do this only when a sensor returns a temperature that is higher than a certain value", submitOnChange:true
                    }
                    if(onlyIfTempHigh) 
                    {
                        input "highTempSensor", "capability.temperatureMeasurement", title: "Select a temperature sensor", required:true, submitOnChange:true
                        if(highTempSensor)
                        {
                            input "tempThreshold", "number", title: "select a temperature threshold", required: true, submitOnChange:true
                        }
                    }
                    input "toggleBack", "bool", title: "turn $switchSensor2 back ${switchState == "off" ? "on" : "off"} once lux are back to a lower value", submitOnChange:true 
                }
            }
        }
        section("Location Modes Management") {
            input "modemgt", "bool", title: "Differentiate Maximum Dimmers' Values With Location Mode", submitOnChange: true
            if(modemgt)
            {
                input "modes", "mode", title:"select modes", required: true, multiple: true, submitOnChange: true

                if(modes){
                    def i = 0
                    atomicState.dimValMode = []
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
                            atomicState.timeoutValMode = []
                            def timeoutValMode = []
                            for(timeoutModes.size() != 0; i < timeoutModes.size(); i++){
                                input "timeoutValMode${i}", "number", required:true, title: "select a timeout value for ${timeoutModes[i]}"
                            }
                        }
                    }
                }
                input "switches", "capability.switch", title: "also turn on/off some light switches", multiple: true, required:false
            }
        }
        section("Other App Conflict Management"){
            input "otherApp", "bool", title: "These dimmers are turned off by another app", submitOnChange:true
            paragraph "IMPORTANT: Enable this option if you know that these dimmers might be turned off by another app"
        }
        section("modes"){
            input "restrictedModes", "mode", title:"Pause this app if location is in one of these modes", required: false, multiple: true, submitOnChange: true 
        }
        section(){
            label title: "Assign a name", required: false
        }
        section(""){
            input"logarithm", "bool", title:"Use logarithmic variations (beta)", value:false, submitOnChange:true
            if(logarithm)
            {
                if(idk) {app.updateSetting("idk",[value:"false",type:"bool"])
                         message} // fool proof, idk isn't compatible with logarith
                input "advanced", "bool", title: "Advanced logarithm settings (use with caution and with the graph helper)", submitOnChange:true, defaultValue:false
                if(!advanced)
                {
                    logarithmPref()
                }
                else 
                {
                    advancedLogPref()
                }
            }
        }
        section("logging"){
            input"enablelogging", "bool", title:"Enable logging", value:false, submitOnChange:true
            input"enabledescription", "bool", title:"Enable Description Text", value:false, submitOnChange:true
        }
        section(""){
            input "update", "button", title: "UPDATE"
            input "run", "button", title: "RUN"
        }
        section("Support this app's development"){
            // def url = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=6JJV76SQGDVD6&source=url"
            def url = "<a href='https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=6JJV76SQGDVD6&source=url' target='_blank'><div style=\"color:blue;font-weight:bold\"><center>CLICK HERE TO SUPPORT THIS APP!</center></div></a>"
            paragraph """

$url
"""
        }
    }
}

def advancedLogPref(){


    if(advanced)
    {
        def url = "<a href='https://www.desmos.com/calculator/vi0qou21ol' target='_blank'><div style=\"color:blue;font-weight:bold\"><center>GRAPH HELPER</center></div></a>"
        //paragraph url
        input "offset", "number", range: "3..10000", required:true, title: "Offset: value named 'a' in graph tool", description:"Integer between 3 and 10000", submitOnChange:true
        logarithmPref()
        input "multiplier", "number", range: "3..3000", required:true, title: "Multipler: value named 'c' in graph tool", description:"Integer between 3 and 3000", submitOnChange:true

        def message =""" $url 
<div style=\"color:black;font-weight:bold\"><center>In the graph helper, move the cursors to create the ideal curve for your specific environment.</center></div>

a. Cursor named "a" is an offset. it moves the curve up and down, without changing the curve's shape. 
b. Cursor named "b" is the logarithm's base. It changes the shape of the curve by making it slightly stipper or flatter
c. Cursor named "c" is the multiplier. It changes the curve's shape more drastically. 
2. Make sure the curve meets the abscisse (the horizontal line) at the level of your sensor's max lux value (unless you want your lights to never turn off). 
3. If your curve ends up crossing the abscisse and go into negative values, those values will be ignored: 
lights will be set to 0 as soon as your sensor returns a value corresponding at the point where the curve crosses the abcisse.
5. Once you've found your ideal curve in the graph helper, simply report the values of a, b and c here.

Suggested values for an environment of 1000 max lux (most indoor sensors): 
a = 300 (offset)
b = 7   (sensitivity as log base)
c = 70  (multiplier; sets the gradient of the curve)

"""
        paragraph """<div style=\"width:102%;background-color:grey;color:white;padding:4px;font-weight: bold;box-shadow: 10px 10px 10px #141414;margin-left: -10px\">${message}</div>"""
    }
}
def logarithmPref(){

    def title = advanced ? "Base: value named 'b' in the graph tool (decimal value such as '5.0' or '4.9')" : "set a sensitivity value"
    input "sensitivity", "decimal", range: "3.0..10.0", required:true, title: "$title", description:"DECIMAL between 3.0 and 10.0", submitOnChange:true // serves as xa basis in linear determination of log() function's multiplier
    if(!advanced)
    {
        paragraph "The higher the value, the more luminance will be needed for $app.name to turn off your lights. For a maximum illuminance of 1000 (max value for most indoor sensors), a value between 5.0 and 6.0 is recommended"
    }

    if(sensitivity)
    {
        boolean wrong = sensitivity > 10.0 || sensitivity < 3.0
        def message = "${wrong ? "WRONG VALUE PLEASE SET A VALUE BETWEEN 3 and 10!" : "sensitivity set to $sensitivity"}"

        if(wrong) {
            paragraph "<div style=\"width:102%;background-color:red;color:black;padding:4px;font-weight: bold;box-shadow: 10px 10px 10px #141414;margin-left: -10px\">${message}</div>"
            log.warn message
        }
        else 
        {
            logging message
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

    atomicState.maxValue = 1000
    if(!idk && !logarithm)
    {
        atomicState.maxValue = maxValue
    }

    if(enablelogging == true){
        runIn(1800, disablelogging)
        description("disablelogging scheduled to run in ${1800/60} minutes")
    }
    else 
    {
        log.warn "debug logging disabled!"
    }

    atomicState.motionEvents = 0
    atomicState.override = false
    atomicState.lastDimVal = dimmers[0].currentValue("level")


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
    if(buttonPause)
    {
        if(buttonAction == "pushed") 
        {
            //subscribe(buttonPause, "pushed", doubleTapableButtonHandler)  
            subscribe(buttonPause, "pushed.$buttonNumber", doubleTapableButtonHandler) 
        }
        else if(buttonAction == "doubleTapped") 
        {
            //subscribe(buttonPause, "doubleTapped", doubleTapableButtonHandler)
            subscribe(buttonPause, "doubleTapped.$buttonNumber", doubleTapableButtonHandler)
        }
        else if(buttonAction == "held")    
        {
            //subscribe(buttonPause, "held", doubleTapableButtonHandler)
            subscribe(buttonPause, "held.$buttonNumber", doubleTapableButtonHandler)
        }
        else if(buttonAction == "released")    
        {
            //subscribe(buttonPause, "released", doubleTapableButtonHandler)
            subscribe(buttonPause, "released.$buttonNumber", doubleTapableButtonHandler)
        }



    }

    subscribe(modes, "mode", locationModeChangeHandler)
    subscribe(sensor, "illuminance", illuminanceHandler)

    schedule("0 0/1 * * * ?", mainloop) 
    schedule("0 0/10 * * * ?", poll) 

    logging("initialization ok")
    mainloop()
}
def switchHandler(evt){
    if(atomicState.paused)
    {
        return
    }
    if(location.mode in restrictedModes)
    {
        logging("App paused due to modes restrictions")
        return
    }

    logging("$evt.device is now set to $evt.value - - SOURCE: is $evt.source TYPE is $evt.type isPhysical: ${evt.isPhysical()}")
    atomicState.lastEvent = evt.name
    //mainloop() // infinite feedback loop!
}
def doubleTapableButtonHandler(evt){

    atomicState.paused = !atomicState.paused
    if(atomicState.paused)
    {
        log.info formatText("${app.label} is now paused", "white", "red")
    }
    else
    {
        log.info formatText("Resuming ${app.label}", "white", "red")
        updated()         
    }
}
def locationModeChangeHandler(evt){
    if(atomicState.paused)
    {
        return
    }
    logging("$evt.name is now in $evt.value mode")   
    atomicState.Tname = "location mode change handler"
    atomicState.T = now() 
    mainloop()
}
def dimmersHandler(evt){

    if(atomicState.paused)
    {
        return
    }
    if(location.mode in restrictedModes)
    {
        logging("App paused due to modes restrictions")
        return
    }
    logging("** $evt.device set to $evt.value **")

    //mainloop() // infinite feedback loop if called from here...
}
def illuminanceHandler(evt){
    if(atomicState.paused)
    {
        return
    }
    if(location.mode in restrictedModes)
    {
        logging("App paused due to modes restrictions")
        return
    }
    logging("$evt.name is now $evt.value")

    // learn max value if required
    def currentSensor = switchSensor2 == null || sensor2 == null ? sensor : switchSensor2?.currentValue("switch") == "switchState" ? sensor2 : sensor 
    def illum = currentSensor.currentValue("illuminance")
    def maxVal = atomicState.maxValue.toInteger()
    if(idk && illum?.toInteger() > maxVal && !logarithm)
    {
        atomicState.maxValue = illum
        logging("new maximum lux value registered as: $atomicState.maxValue")
    }
    else 
    {
        logging "max value preset by user: ${maxValue}lux"
        atomicState.maxValue = maxValue
    }

    atomicState.Tname = "illuminanceHandler"
    atomicState.T = now() 
    mainloop()

}
def motionHandler(evt){
    if(atomicState.paused)
    {
        return
    }
    if(location.mode in restrictedModes)
    {
        logging("App paused due to modes restrictions")
        return
    }

    logging("MOTION ----- $evt.device is $evt.value")

    if(usemotion) 
    {
        atomicState.Tname = "motionHandler"
        atomicState.T = now() 
        mainloop()
    }

}
def appButtonHandler(btn) {

    if(location.mode in restrictedModes)
    {
        logging("App paused due to modes restrictions")
        return
    }
    switch(btn) {
        case "pause":atomicState.paused = !atomicState.paused
        log.debug "atomicState.paused = $atomicState.paused"
        if(atomicState.paused)
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
        atomicState.paused = false
        updated()
        break
        case "run":
        if(!atomicState.paused) 
        {
            atomicState.Tname = "button handler"
            atomicState.T = now() 
            mainloop()
        }
        else
        {
            log.info formatText("APP IS PAUSED!", "white", "red")   
        }
        break

    }
}

def mainloop(){
    if(atomicState.paused)
    {
        return
    }
    atomicState.T = atomicState.T != null ? atomicState.T : now()
    atomicState.T = atomicState.Tname == "end of main loop" ? atomicState.T = now() : atomicState.T // when called by schedule()
    atomicState.Tname = atomicState.Tname == "end of main loop" ? atomicState.Tname = "schedule call" :  atomicState.Tname

    if(location.mode in restrictedModes)
    {
        logging("App paused due to modes restrictions")
        return
    }
    boolean Active = stillActive()
    boolean dimOff = dimmers.findAll{it.currentValue("switch") == "off"}.size() == dimmers.size() 
    boolean keepDimmersOff = false

    logging("""
number of dimmers that are off = ${dimmers.findAll{it.currentValue("switch") == "off"}.size()}
number of dimmers that are set to 0 = ${dimmers.findAll{it.currentValue("level") == 0}.size()}
dimmers.size() = ${dimmers.size()}
""")
    // if we don't use motion, then keepDimmersOff is based on actual status (defined above), otherwise, it's false by default
    // so as to allow the app to continue managing the dimmers based on motion and illuminance
    keepDimmersOff = usemotion ? false : dimOff  //always false with motion management
    logging("1: keepDimmersOff = $keepDimmersOff")

    // now, if user has selected the override option, then the app will take the off status as override and not turn them back on
    // whether or not usemotion is true (a bit redundant but necessary to avoid discrepancies). 
    keepDimmersOff = (override && dimOff && !usemotion) ? true : keepDimmersOff // if override, off and no motion mngt, keep offf
    keepDimmersOff = otherApp ? keepDimmersOff : false // last but not least: always false if !otherApp

    if(override && usemotion)
    {
        app.updateSetting("override",[value:"false",type:"bool"]) // fool proofing... override can't work with usemotion
    }

    logging("""2: keepDimmersOff = $keepDimmersOff
usemotion = $usemotion
""")


    logging("""
usemotion = $usemotion
keepDimmersOff = $keepDimmersOff
dimOff = $dimOff
outofmodes = $outofmodes
override = $override ${usemotion ? "" : "(Redundant when not using motion)"}
Active = $Active ${usemotion ? "" : "but doesn't use motion"}
location.mode = ${location.mode}
restrictedModes = $restrictedModes

""")

    if(Active && !keepDimmersOff)
    {
        def dimVal = logarithm ? getDimValLog() : getDimVal()
        setDimmers(dimVal)
        switches?.on()
        if(switches) logging "${switches} turned off"
    }
    else if(Active && keepDimmersOff)
    {
        description "dimmers are off and managed by a different app, $app.label will resume when they're turned back on keepDimmersOff = $keepDimmersOff"
    }
    else 
    {
        description "no motion..."
        dimmers.off() 
        switches?.off()
        if(switches) logging "${switches} turned off"
    }

    if(highLuxSwitch)
    {
        def outsideIllum = sensor.currentValue("illuminance").toInteger()
        def maxVal = atomicState.maxValue != null ? atomicState.maxValue.toInteger() : maxValue.toInteger()
        def NeedCurtainOff = onlyIfTempHigh ? highTempSensor.currentValue("temperature") >= tempThreshold && outsideIllum >= maxVal : outsideIllum >= maxVal


        description """*********
outsideIllum = $outsideIllum
maxVal (for curtains) = $maxVal

"""
        atomicState.curtainsWereTurnedOff = atomicState.curtainsWereTurnedOff != null ? atomicState.curtainsWereTurnedOff : false

        if(NeedCurtainOff && !atomicState.curtainsWereTurnedOff)
        {
            switchSensor2."${switchState}"()
            atomicState.curtainsWereTurnedOff = true
            logging "turning $switchSensor2 $switchState due to excess of illuminance"
        }
        else if(!NeedCurtainOff && toggleBack && atomicState.curtainsWereTurnedOff)
        {
            switchSensor2."${switchState == "off" ? "on" : "off"}"()
            atomicState.curtainsWereTurnedOff = false
            logging "turning $switchSensor2 ${switchState == "off" ? "on" : "off"} because illumiance is low again"
        }

    }
    float performance = (now() - atomicState.T)/1000
    if(performance > 1)
    {
        logwarn "TOTAL EXECUTION TIME between $atomicState.Tname and 'end of main loop' = ${performance} seconds - HUB IS SLOW"
    }
    else
    {
        description "${performance} seconds between $atomicState.Tname to 'end of main loop'"
    }
    atomicState.Tname = "end of main loop"
    atomicState.T = now()
}

def getDimVal(){
    if(atomicState.paused)
    {
        return
    }
    boolean switchStateTrue = switchSensor2 ? switchSensor2?.currentValue("switch") == switchState : false
    def currentSensor =  switchStateTrue ? sensor2 : sensor
    def illum = currentSensor.currentValue("illuminance")

    logging """
LINEAR
${switchSensor2 ? "switchSensor2 = $switchSensor2" : ""}
${switchSensor2 ? "${switchSensor2?.currentValue("switch")}" : ""}
${switchSensor2 ? "switchStateTrue = $switchStateTrue" : ""}
${switchSensor2 ? "switchState boolean reference is: $switchState" : ""}


illuminance sensor is: $currentSensor
illuminance is: $illum lux
maxValue = ${maxValue ? "$maxValue (user defined value, no learning)" : "atomicState.maxValue = $atomicState.maxValue (learned value)"}
"""
    def maxIllum = idk && !logarithm ? atomicState.maxValue : maxValue  // if idk selected, then use last learned max value (atomicState.maxValue)


    def y = null // value to find
    def x = illum // current illuminance
    def xa = maxIllum // maximum dimming value
    def ya = 0      //coressponding dimming value for when illuminance = xa

    def m = -0.1 // multiplier/slope 

    y = m*(x-xa)+ya // solving y-ya = m*(x-xa)
    //logging "algebra found y = $y"
    dimVal = y.toInteger()
    dimVal = otherApp ? (dimVal < 1 ? dimVal = 1 : dimVal) : (dimVal < 0 ? dimVal = 0 : dimVal)
    dimVal = dimVal > 100 ? 100 : dimVal // useless due to slope being -0.1 but just in case I forget about the slope's value function, I leave this line and its comment here

    logging """illuminance: $illum, maximum illuminance: $maxIllum -|- ${maxValue ? "(user defined maxValue = $maxValue)" : ""}

linear dimming value result = ${dimVal} 
"""
    return dimVal.toInteger()
}
def getDimValLog(){ // exponential calculation
    if(atomicState.paused)
    {
        return
    }
    boolean switchStateTrue = switchSensor2 ? switchSensor2.currentValue("switch") == switchState : false
    def currentSensor =  switchStateTrue ? sensor2 : sensor
    def illum = currentSensor.currentValue("illuminance")


    logging """
LOGARITHMIC
${switchSensor2 ? "switchSensor2 = $switchSensor2" : ""}
${switchSensor2 ? "${switchSensor2?.currentValue("switch")}" : ""}
${switchSensor2 ? "switchStateTrue = $switchStateTrue" : ""}
${switchSensor2 ? "switchState boolean reference is: $switchState" : ""}


illuminance sensor is: $currentSensor
illuminance is: $illum lux 
No max value in logarithmic mode..
"""
    def y = null // value to find
    def x = illum != 0 ? illum : 1 // current illuminance // prevent "ava.lang.ArithmeticException: Division by zero "


    def a = offset ? offset : 300
    def b = sensitivity // this value is the overall sensitivity set by the user
    def c = multiplier ? multiplier : 70 

    y = (Math.log10(1/x)/Math.log10(b))*c+a
    logging "log${b}(1/${x})*${c}+${a} -> $y"
    dimVal = y.toInteger()
    dimVal = otherApp ? (dimVal < 1 ? dimVal = 1 : dimVal) : (dimVal < 0 ? dimVal = 0 : dimVal)
    dimVal = dimVal > 100 ? 100 : dimVal 

    logging "LOGARITHMIC dimming value = ${dimVal} (illuminance: $illum)"
    return dimVal
}
def setDimmers(int val){
    if(atomicState.paused)
    {
        return
    }
    def i = 0
    def s = dimmers.size()

    if(modemgt)
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

    val = val < 0 ? 0 : (val > 100 ? 100 : val) // just a precaution
    if(val == 0) 
    { 
        dimmers.off() // it seems some hue devices don't fully turn off when simply dimmed to 0, so turn them off
    }
    else
    {
        dimmers.on() // make sure it's on, in case some other dumb device drivers don't get that 0+1 != 0... 
    }

    dimmers.setLevel(val)
    logging("${dimmers} set to $val ---")
}
boolean stillActive(){

    boolean result = true
    boolean inTimeOutModes = modetimeout ? location.mode in timeoutModes : true

    if(modetimeout && !inTimeOutModes) // if use timeout modes and not in this mode, then ignore motion (keep lights on)
    {
        logging "Location is outside of time out modes, ignoring motion events"
        return result
    }

    if(usemotion)
    {
        int events = 0
        def timeout = getTimeout()
        long deltaMinutes = timeout * 1000 * 60   
        int s = motionSensors.size() 
        int i = 0

        boolean AnyCurrentlyActive = motionSensors.findAll{it.currentValue("motion") == "active"}?.size() != 0
        if(AnyCurrentlyActive) 
        {
            //description("return true")
            return true  // for faster execution when true
        }


        // if not triggered by motion event, then look for past events of each sensor
        for(s != 0; i < s; i++) // collect active events
        { 
            events += motionSensors[i].eventsSince(new Date(now() - deltaMinutes)).findAll{it.value == "active"}?.size() // collect motion events for each sensor separately
        }

        result = events > 0 
    }
    //logwarn("********* $events active motion events in the last $timeout minutes stillActive() returns ${result} ************")
    description "motion $result"
    return result
}
def getTimeout(){
    def result = noMotionTime // default
    def valMode = location.mode

    if(modetimeout && location.mode in timeoutModes)
    {
        int s = timeoutModes.size()
        int i = 0
        logging("timeoutModes: $timeoutModes")
        while(i < s && location.mode != timeoutModes[i]){i++}
        logging("${location.mode} == ${timeoutModes[i]} (timeoutModes${i} : index $i) ?: ${location.mode == timeoutModes[i]}")
        valMode = "timeoutValMode${i}" // get the key as string to search its corresponding value within settings
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
def resetMotionEvents(){
    logging("No motion event has occured during the past $noMotionTime minutes")
    atomicState.motionEvents = 0   
}
def logging(msg){
    //def debug = settings.find{it.key == "enablelogging"}?.value
    //log.warn "debug = $debug"
    if (enablelogging) log.debug msg
}
def description(msg){
    //def debug = settings.find{it.key == "enablelogging"}?.value
    //log.warn "debug = $debug"
    if (enabledescription) log.info msg
}
def logwarn(msg){
    log.warn msg
}
def disablelogging(){
    app.updateSetting("enablelogging",[value:"false",type:"bool"])
    log.warn "logging disabled!"
}
def poll(){
    logging "polling devices"
    boolean haspoll = false
    boolean hasrefresh = false
    dimmers.each{
        if(it.hasCommand("poll")){ it.poll() }else{logging("$it doesn't have poll command")}
        if(it.hasCommand("refresh")){ it.refresh() }else{logging("$it doesn't have refresh command")}
    }
}
def formatText(title, textColor, bckgColor){
    return  "<div style=\"width:102%;background-color:${bckgColor};color:${textColor};padding:4px;font-weight: bold;box-shadow: 1px 2px 2px #bababa;margin-left: -10px\">${title}</div>"
}

def donate(){
    def a = """
<form action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
<input type="hidden" name="cmd" value="_s-xclick" />
<input type="hidden" name="hosted_button_id" value="6JJV76SQGDVD6" />
<input type="image" src="https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif" border="0" name="submit" title="PayPal - The safer, easier way to pay online!" alt="Donate with PayPal button" />
<img alt="" border="0" src="https://www.paypal.com/en_US/i/scr/pixel.gif" width="1" height="1" />
</form>

"""
    return a
}
