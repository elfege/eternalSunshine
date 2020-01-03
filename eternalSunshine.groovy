definition(
    name: "A.I. Thermostat Manager",
    namespace: "elfege",
    author: "ELFEGE",

    description: """This A.I. will manage a thermostat using inputs from different sources and help you save energy:
- contact sensors
- switches (for fans or windows mostly)
- alternate thermostat 
- bed sensor
- motion sensors
- humidity (computes a humidity threshold and then adjust your setpoint accordingly)
- temperature AImap (all temps are pre-set, app will learn with user's inputs)

- Deep Learning: It learns from your habits!

""",

    category: "Green Living",
    iconUrl: "https://www.philonyc.com/assets/penrose.jpg",
    iconX2Url: "https://www.philonyc.com/assets/penrose.jpg",
    iconX3Url: "https://www.philonyc.com/assets/penrose.jpg", 
    image: "https://www.philonyc.com/assets/penrose.jpg"
)

preferences {

    page name: "MainPage"
    page name: "settings"
    page name: "contactSensors"
    page name: "powerSaving"

}

def MainPage(){
    def pageProperties = [
        name:       "MainPage",
        title:      "MainPage",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]

    return dynamicPage(pageProperties) {



        section("modes")
        {
            input(name: "modes", type: "mode", title:"set for specific modes", required:true, multiple:true, submitOnChange: true)            
        }

        section("") {
            href "settings", title: "Thermostat", description: ""
            href "contactSensors", title: "Contacts sensors", description: ""
            href "powerSaving", title: "Save power", description: ""
        }

        section("logging")
        {
            input("enablelogging", "bool", title:"Enable logging", value:false)   
        }
        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}

def settings() {

    def pageProperties = [
        name:       "settings",
        title:      "Thermostats and other devices",
        nextPage:   null,
        install: false,
        uninstall: true
    ]

    dynamicPage(pageProperties) {

        section("Select the thermostat you want to control") { 

            input(name: "Thermostat", type: "capability.thermostat", title: "select a thermostat", required: true, multiple: false, description: null, submitOnChange:true)
            if(modes && Thermostat)
            {
                input(name: "setThermostatOutOfMode", type: "enum", title: "When out of modes, set $Thermostat to this mode:", required: false, options: ["heat", "cool", "auto", "off"])
                if(setThermostatOutOfMode == "auto")
                {
                    paragraph "BEWARE THAT AUTO MODE IS THE OVERRIDE MODE. OPERATIONS WILL NOT RESUME UNTIL YOU SET $Thermostat back to 'heat', 'cool' or 'off'!"
                }
            }

            input(name: "altSensor", type:"capability.temperatureMeasurement", title: "Optional: Poll Temperature From A Differente Sensor", multiple: true, required: false, submitOnChange:true)
            input(name: "outdoor", type:"capability.temperatureMeasurement",
                  title: "Adjust temperature with outside's weather",
                  description: "Select a weather station", 
                  required: true,
                  submitOnChange:true)
            if(altSensor)
            {
                input(name: "offset", type: "number", range: "-20..20", title: "Optional: an offset for $altSensor temperature reports", required: false, defaultValue: 0)
            }
        }
    }
}

def contactSensors() {

    def pageProperties = [
        name:       "contactSensors",
        title:      "Contact sensors",
        nextPage:   null,
        install: false,
        uninstall: true
    ]

    dynamicPage(pageProperties) {

        logging("Thermostat = $Thermostat")

        section("Select at least one contact sensor per thermostat, if required") { 

            if(!Thermostat){       
                def message = "You haven't selected any thermostat yet. Return to main page"
                paragraph "$message"
                logging("$message")
            }
            else 
            {
                input(name: "contact", type:"capability.contactSensor", title: "Turn off ${Thermostat} when these contacts are open", multiple: true, required: false)
            }
        }
    }
}

def powerSaving() {

    def pageProperties = [
        name:       "powerSaving",
        title:      "Save power",
        nextPage:   null,
        install: false,
        uninstall: true
    ]

    dynamicPage(pageProperties) {

        section("Select motion parameters") { 
            input(name: "savingModes", type: "mode", title: "select modes under wich you wish to save power", required:false, multiple: true, submitOnChange: true)
            if(savingModes)
            {
                paragraph "Note that saving modes will increase cooling and lower heating set points"
            }
            if(Thermostat){             
                input(name: "motionSensors", type:"capability.motionSensor", 
                      title: "Turn off ${Thermostat} when these sensors reported no motion for a certain period of time", 
                      multiple: true,
                      submitOnChange:true,
                      required: false)
                input(name: "motionModes", type:"mode", 
                      title: "Use motion only in these modes", 
                      multiple: true,
                      submitOnChange:true,
                      required: false)
                input(name: "windows", type:"capability.switch", 
                      title: "Turn on a switch when it's too hot inside and temperature is nice outside", 
                      description: "select a switch",
                      multiple:true,
                      required: false, 
                      submitOnChange: true) 
                input(name: "door", type: "capability.contactSensor",
                      title: "when this contact is closed, ignore motion conditions", 
                      required:false, 
                      multiple:false,
                      submitOnChange:true)
                if(windows)
                {
                    input(name: "windowDelay", type:"number", 
                          title: "Turn off $windows after a certain time", 
                          description: "time in seconds",
                          required: false)
                    input(name: "windowsModes", type:"mode", 
                          title: "Use $windows only in these modes", 
                          multiple: true,
                          submitOnChange:true,
                          required: false)
                    input(name: "activeFans", type: "bool",
                          title: "When $windows are on, activate fan circulation",
                          defaultValue: true)
                    input(name: "okWindSavingMode", type: "bool",
                          title: "When Home is in saving mode, open $windows whenever suitable",
                          defaultValue: true)
                }
            }
            else 
            {
                def message = "You haven't selected any thermostat yet. Return to main page"
                paragraph "$message"
                logging("$message")
            }
        }
        section("Inactive Motion Time Limit")
        {
            if(motionSensors)
            {
                input(name: "noMotionTime", type: "number", required: true, title: "Lenght of time without motion", description: "Time in minutes")
            }
        }

    }
}



def installed() {
    logging("Installed with settings: ${settings}")


    runIn(5, createDb) // must be run only from this scope or user would lose all the acruded machine learning 


    initialize()
}

def updated() {

    logging("""updated with settings: ${settings}-""")
    unsubscribe()
    unschedule()
    initialize()
}

def initialize()
{
    state.setpointHandlerRunning = false
    state.motionEvents = 0 
    state.lastMotionEvent = now() as long
        state.lastOnResend = now() as long
        state.lastOffResend = now() as long
        state.lastBoost = now() as long

        state.lastMessage = now() as long
        state.heatMode = 0
    state.coolMode = 0
    state.lastCoolMode = now() as long
        state.lastHeatMode = now() as long
        state.learnBase = [:]
    state.megaBase = []
    state.learnedDesiredTemp = 72.0
    state.learnedOutsideTemp = 50.0
    state.lastNeed = "cool"
    state.desired = 72
    state.closeTime = now() as long
        state.openTime = now() as long

        state.alreadyClosed = false
    state.alreadyOpen = false
    state.okToOpen = false




    logging("subscribing to events...")

    subscribe(location, "mode", ChangedModeHandler) 

    subscribe(Thermostat, "temperature", temperatureHandler)

    subscribe(Thermostat, "heatingSetpoint", setPointHandler)
    subscribe(Thermostat, "coolingSetpoint", setPointHandler)


    if(pw)
    {
        subscribe(pw, "power", powerMeterHandler)  
    }
    subscribe(Thermostat, "thermostatMode", temperatureHandler)

    subscribe(outdoor, "temperature", temperatureHandler)
    boolean hasfeelsLike = outdoor?.hasAttribute("feelsLike")
    boolean hasFeelsLike = outdoor?.hasAttribute("FeelsLike") // uppercase F
    boolean hasRealFeel = outdoor?.hasAttribute("RealFeel")
    boolean hasrealFeel = outdoor?.hasAttribute("realFeel")


    if(hasfeelsLike)
    {
        subscribe(outdoor, "feelsLike", temperatureHandler)
        logging("$outdoor has feelsLike attribute")
        state.FeelString = "feelsLike"
    }
    else if(hasFeelsLike)
    {
        subscribe(outdoor, "FeelsLike", temperatureHandler)
        logging("$outdoor has hasFeelsLike attribute (uppercase F)")
        state.FeelString = "FeelsLike"
    }
    else if(hasRealFeel)
    {
        subscribe(outdoor, "RealFeel", temperatureHandler)
        logging("$outdoor has RealFeel attribute")
        state.FeelString = "RealFeel"
    }
    else  if(hasrealFeel)
    {    
        subscribe(outdoor, "realFeel", temperatureHandler)
        logging("$outdoor has realFeel attribute")
        state.FeelString = "realFeel"
    }

    else 
    {
        state.FeelString = null
        logging("$outdoor has NO REAL FEEL attribute")
    }


    if(altSensor)
    {
        subscribe(altSensor, "temperature", temperatureHandler)
    }


    if(contact){
        logging("subscribing $contact")
        subscribe(contact, "contact.open", contactHandler)
        subscribe(contact, "contact.closed", contactHandler)
        logging("$contact subscribed to events")
    }
    else 
    {
        logging("NO CONTACTS SELECTED! ----------------------------------")
    }

    if(door)
    {
        subscribe(door, "contact.open", doorContactHandler)
        subscribe(door, "contact.closed", doorContactHandler)
        logging("$door subscribed to events")
    }

    if(bedSensor)
    {
        logging("subscribing $bedSensor")
        subscribe(bedSensor, "contact.open", bedSensorHandler)
        subscribe(bedSensor, "contact.closed", bedSensorHandler)
        logging("$bedSensor subscribed to events")
    }

    /// MOTION SUBSCRIPTION
    if(motionSensors)
    {
        subscribe(motionSensors, "motion", motionSensorHandler)
        logging("$motionSensors subscribed to events")
    }
    else 
    {
        logging("$Thermostat has no motion sensor attached")
    }

    if(criticalSensors)
    {
        subscribe(criticalSensors, "temperature", temperatureHandler)

        logging("$criticalSensors subscribed to events")
    }

    schedule("0 0/1 * * * ?", evaluation)
    schedule("0 0/5 * * * ?", Poll)
    Poll()
    //runIn(5, createDb) // don't run this at every update or you'll lose all what the app learned from your habits. This is only for debug purpose. 
    //runIn(10, evaluation)

    /*if(motionSensors)
{
schedule("0 0/$noMotionTime * * * ?", resetMotionEvents) // eventsSince collection is too time consuming!
}*/

    boolean WAreOpen = windowsAreOpen() || contactsAreOpen()
    if(WAreOpen)
    {
        state.openByApp = true // allow closing at startup to initialize these booleans properly
    }
    else 
    {
        state.closedByApp = true // closedByApp is a reciprocal boolean to openByApp
    }

    logging( "---------------------------------------END OF INITIALIZATION --------------------------------------------")

}

def resetMotionEvents()
{
    state.motionEvents = 0   
}

def createDb()
{

    state.tempAImap = [
        0:"75",1:"75",2:"75",3:"75",4:"75",5:"75",6:"75",7:"75",8:"75",9:"75",10:"75",
        11:"75",12:"75",13:"75",14:"75",15:"75",16:"75",17:"75",18:"75",19:"75",20:"75",
        21:"75",22:"75",23:"75",24:"75",25:"75",26:"75",27:"75",28:"75",29:"75",30:"75",
        31:"74",32:"74",33:"74",34:"74",35:"74",36:"73",37:"73",38:"73",39:"73",40:"73",
        41:"73",42:"73",43:"73",44:"73",45:"73",46:"73",47:"73",48:"73",49:"73",50:"73",
        51:"73",52:"73",53:"73",54:"73",55:"73",56:"73",57:"73",58:"73",59:"73",60:"73",
        61:"73",62:"73",63:"72",64:"72",65:"72",66:"72",67:"72",68:"73",69:"73",70:"73",
        71:"73",72:"73",73:"73",74:"73",75:"73",76:"73",77:"73",78:"73",79:"73",80:"73",
        81:"73",82:"73",83:"73",84:"73",85:"74",86:"74",87:"74",88:"74",89:"74",90:"74",
        91:"74",92:"75",93:"75",94:"75",95:"75",96:"75",97:"75",98:"75",99:"75",100:"76",
        101:"76",102:"76",103:"76",104:"76",105:"77",106:"77",107:"77",108:"77"]

    logging("""
-----------------------
A.I. map installed
-----------------------
-""")

}

def powerMeterHandler(evt)
{
    logging("$evt.device returns ${evt.value}Watts")
}

def ChangedModeHandler(evt)
{
    logging("Home is now in ${evt.value} mode")
    boolean inWindowsMode = evt.value in windowsModes
    boolean WAreOpen = windowsAreOpen() 

    if(WAreOpen)
    {
        state.closedByApp = false // allow new attempts after location is back to a window mode
        state.openByApp = true // allow new attempt
    }
    else
    {
        state.closedByApp = true // allow new attempts after location is back to a window mode
        state.openByApp = false // allow new attempt
    }

    if(evt.value in modes || evt.value in savingModes)
    {

        logging( """new mode is within parameters...-""")

    }
    else 
    {
        logging("Mode is outside operations")
        if(setThermostatOutOfMode && Thermostat.currentValue("thermostatMode") != "auto")
        {
            logging("Thermostat is now set to $setThermostatOutOfMode")
            setThermostatMode(Thermostat, setThermostatOutOfMode, "54gh")
        }
    }

    evaluation()
}

def setPointHandler(evt){

    state.setpointHandlerRunning = true

    log.warn "setPointHandler $evt.device $evt.name $evt.value "

    float thisTemp = evt.value.toFloat()
    def eventName = evt.name

    def currMode = location.mode
    def outsideTemp = outdoor.currentValue("temperature")
    def thermMode = Thermostat.currentValue("thermostatMode")
    boolean bedSensorClosed = !bedSensorIsOpen()
    boolean override = false //thermMode == "auto" //// OVERRIDE BOOLEAN // no longer in use here, allow A.I. to learn from input when user overrides
    boolean consistent = (state.lastNeed == "heat" && evt.name == "heatingSetpoint") || (state.lastNeed == "cool" && evt.name == "coolingSetpoint") 

    boolean Active = motionTest() 
    boolean inRegularModes = currMode in modes 
    boolean inSavingModes = !bedSensorClosed && currMode in savingModes || !Active
    boolean noLearn = state.NoLearnMode
    boolean NoLearnMode = noLearn || inSavingModes || Thermostat.currentValue("coolingSetpoint") > 75

    def GlobalHumidity = getHumidity()
    def outsideHumidity = GlobalHumidity[0]
    def insideHumidity = GlobalHumidity[1]

    /*logging*/logging(""" --LEARNING METHOD PARAMETERS-- 
state.NoLearnMode = $state.NoLearnMode
NoLearnMode = $NoLearnMode
evt.source = $evt.source 
evt.name = $evt.name 
evt.value = $evt.value
inSavingModes = $inSavingModes
inRegularModes = $inRegularModes (currMode = $currMode && regular modes are: $modes)
Active $Active
override = $override
consistent = $consistent
"""
                       )

    def comfort = null

    if(!NoLearnMode && !override && consistent)
    {
        logging( "************learning new desired temperature: $evt.value for outside temp: ${outsideTemp}F******")


        def i = 0
        def s = 3
        def val = outsideTemp.toInteger()
        def valplus = outsideTemp.toInteger()
        def valminus = outsideTemp.toInteger()



        for(s!=0;i<s;i++) // apply new value to further outsideTemp values in the AImap
        {
            valplus += 1
            valminus -= 1
            state.tempAImap."${val.toString()}" = evt.value
            state.tempAImap."${valplus.toString()}" = evt.value
            state.tempAImap."${valminus.toString()}" = evt.value
            logging( """
valplus = $valplus
valminus = $valminus
-""")
        }


     logging( """
       NEW AImap with outsideTemp = $outsideTemp:
$state.tempAImap
-""")

    }
    else {

        if(override)
        {
            logging( "NOT LEARNING DUE TO OVERRIDE MODE")
        }
        if(noLearn)
        {
            logging( "NOT LEARNING from this input ($evt.name: $evt.value) because NoLearnMode has been called")
        }
        if(!consistent)
        {
            logging( "NOT LEARNING from this input ($evt.name: $evt.value) because it isn't consistent with current thermostat's mode")
        }
        if(inSavingMode)
        {
            logging(  "NOT LEARNING from this input ($evt.name: $evt.value) because inSavingMode returned true")
        }

    }

    //resetNoLearnMode()

    state.setpointHandlerRunning = false
    //evaluation()
}

def motionSensorHandler(evt)
{
    logging("$evt.device returns $evt.value")

    if(evt.value == "active")
    {
        //logging( "collecting $evt.value event for $evt.device")
        state.motionEvents += 1

        long lastMotionEvent = now() as long
            logging( "lastMotionEvent: ${lastMotionEvent}")
        state.lastMotionEvent = lastMotionEvent as long


            }

    // evaluation()
}
def temperatureHandler(evt)
{
    logging("$evt.device returns ${evt.name}:${evt.value}")

    // measure consistency
    // count how many changes of operating state have occured in the last 10 minutes
    // if more than x, then we're having erratic ocscillations due to mid-season 
    if(evt.value == "cool")
    {
        state.coolMode = state.coolMode + 1
        state.lastCoolMode = now()  as long
            }
    else if(evt.value == "heat")
    {
        state.heatMode = state.heatMode + 1
        state.lastHeatMode = now()  as long
            }
    long lastCoolMode = state.lastCoolMode as long
        long lastHeatMode = state.lastHeatMode as long
        long timeLimit = 1000 * 60 * 10 as long
        if(now() - lastCoolMode > timeLimit)
    {
        logging("reseting lastCoolMode")
        state.lastCoolMode = now() as long
            state.coolMode = 0
            }
    if(now() - lastHeatMode > timeLimit)
    {
        logging("reseting lastHeatMode")
        state.lastHeatMode = now() as long
            state.heatMode = 0
            }

    evaluation()
}
def contactHandler(evt)
{
    logging( "$evt.device returns $evt.value")

    if(evt.value == "open" && Thermostat.currentValue("thermostatMode") != "auto")
    {
        setThermostatMode(Thermostat, "off", "54tyh9")
    }
    evaluation()
}
def doorContactHandler(evt)
{
    logging("$evt.device returns $evt.value")
    evaluation()

}

def bedSensorHandler(evt){

    logging( """$evt.device is $evt.value -""")

}


def evaluation()
{

    logging("START")

    if(!enablelogging)
    {
        state.enablelogging = false
    }
    //state.enablelogging = true // for debug purpose, must remain otherwise comented out 
    //runIn(10, evaluation) // FOR DEBUG ONLY


    //state.setpointHandlerRunning = false

    if(state.tempAImap != null && state.setpointHandlerRunning == false)
    {
        Master()
    }
    else if(state.setpointHandlerRunning == true)
    {
        log.warn "NOT EVALUATING BECAUSE A.I. is in the works of learning new input"
    }
    else
    {
        log.warn "NOT EVALUATING BECAUSE AI Database HASN'T BEEN INSTALLED YET"
        // createDb()
    }



}

def Master()
{

    boolean bedSensorClosed = !bedSensorIsOpen()
    //logging( "bedSensorClosed = $bedSensorClosed"

    def need = "OVERRIDE MODE"
    def ThisThermostat = Thermostat
    def thermMode = ThisThermostat.currentValue("thermostatMode")
    def swthmmode // used below when swt has therm capability only

    float outsideTemp = outdoor.currentValue("temperature")
    float feel = outsideTemp
    if(state.FeelString != null)
    {
        def feelString = state.FeelString
        logging("gettting value for $state.FeelString attribute: feelString = $feelString")
        feel = outdoor.currentValue(feelString).toFloat()
    }

    boolean override = thermMode == "auto" 
    state.override = override

    def currMode = location.mode
    boolean inRegularModes = currMode in modes //modes.find{it == "$currMode"}
    boolean Active = motionTest() 
    boolean inSavingModes = !bedSensorClosed && currMode in savingModes || (inRegularModes && !Active)
    //boolean inSavingModes = currMode in savingModes 
    // if there's been no activity around motion sensors (and no bedsensor pressed), then run in power saving mode
    boolean inMotionMode = currMode in motionModes 

    /*   def dumbtestList = ["a","b","c"]
def currDumbMode = "a"
boolean inCurrDumbMode = currDumbMode in dumbtestList
logging( """
location.mode = $location.mode
location.modes = $location.modes
location.mode = $location.mode
currMode = $currMode
savingModes :: $savingModes
motionModes :: $motionModes 
location.mode in savingModes = ${location.mode in savingModes}
inSavingModes = $inSavingModes
inMotionMode = $inMotionMode
--------------------------------inRegularModes = $inRegularModes

Dumb test: 
dumbtestList = $dumbtestList
currDumbMode = $currDumbMode
inCurrDumbMode = $inCurrDumbMode


-""")
*/

    def savingmodeMSG = "-------"
    state.criticalTempH = 80
    state.criticalTempC = 69
    def desired = getComfort() // state.learnedDesiredTemp 
    state.desired = desired

    def currHSP = ThisThermostat.currentValue("heatingSetpoint")
    def currCSP = ThisThermostat.currentValue("coolingSetpoint")

    float currTemp = getCurrTemp() //ThisThermostat.currentValue("temperature")



    def GlobalHumidity = getHumidity()
    def outsideHumidity = GlobalHumidity[0]
    def insideHumidity = GlobalHumidity[1]
    def humthreshold = getHumidityThresHold()  // this will get a higher humidity threshold as it's warmer outside 
    boolean humidityOk = insideHumidity < humthreshold && outsideHumidity < humthreshold
    logging("humidityOk = $humidityOk (outsideHumidity < humthreshold: $outsideHumidity < $humthreshold)// higher hum threshold when it's warmer outside")
    float currTempAtThermostat = ThisThermostat.currentValue("temperature")


    boolean Open = contactsAreOpen()
    boolean WAreOpen = windowsAreOpen() 
    boolean okToOpen = false
    long openTime = 1000 * 60 * 10 // 10 minutes open time (if outside is cold, or if inside remains hot)
    def contactEvents = contact.collect{ it.eventsSince(new Date(now() - openTime)) }.flatten()
    def openEvents = contactEvents?.findAll{it.name == "open"}
    boolean contactTimerOk = !contactsOpen || openEvents.size() < 1 || outsideTemp >= 70
    boolean contactsOpen = Open && contactTimerOk 

    def fanMode = Thermostat.currentValue("thermostatFanMode")
    if(contactsOpen && (currTemp <= desired || feel <= 66))
    {
        setFans("auto")
    }
    else 
    {
        if(!contactsOpen && Thermostat.currentValue("thermostatFanMode") == "on")
        {
            setFans("auto")
        }
        else if(activeFans && contactsOpen)
        {
            setFans("on") 
            logging( "4548")
        }
    }
    logging("Found ${contactEvents.size()} contact events in the last ${openTime/1000/60} minutes")
    logging("Found ${openEvents.size()} OPEN contact events in the last ${openTime/1000/60} minutes")

    boolean criticalCold = currTemp <= state.criticalTempC 
    boolean criticalHot =  currTemp >= state.criticalTempH
    boolean thermodeIsOk

    boolean okToChangeToCool = state.coolMode <= 4 || thermMode == "off"
    boolean okToChangeToHeat = state.heatMode <= 4 || thermMode == "off"

    
    boolean inWindowsMode = true // always true by default
    if(windowsModes){inWindowsMode = location.mode in windowsModes}

    /*
int Amplitude = currTemp - outsideTemp 
    boolean AmplitudeHeatOk = Math.abs(Amplitude) >= 2 
    boolean AmplitudeCoolOk = Math.abs(Amplitude) >= 2
    logging( "AmplitudeHeatOk = $AmplitudeHeatOk AmplitudeCoolOk = $AmplitudeCoolOk Amplitude = $Amplitude Math.abs($Amplitude) = ${Math.abs(Amplitude)}"
*/

    def timeSinceLastHeatModeChange = (now() - state.lastHeatMode)/1000/60 
    def timeSinceLastCoolModeChange = (now() - state.lastCoolMode)/1000/60 


    /////////////////////////////// END OF DEFINITIONS ///////////////////////////////////////////////

    if(inRegularModes || inSavingModes)
    {   
        if(!override)
        {
            if(inSavingModes) // can be triggered by !Active during regular modes...
            {
                savingmodeMSG = "*****************POWER SAVING MODE*****************"
                log.warn(savingmodeMSG)

                if(outsideTemp > 68)
                {
                    // cool needed, define cooling setpoints in power saving mode
                    need = "cool"
                    state.lastNeed = need
                    if(!Active || !motionSensors)
                    {
                        desired = 80
                    }
                    else 
                    {
                        desired = 76 // remember that a saving mode can be either "away" or "no motion", so keep some cooling if there's motion
                    }
                    if(insideHumidity >= 65)
                    {
                        desired = desired - 2.0 // protect electronic equipment from excessive humidity
                    }
                    state.desired = desired
                    logging("setting power saving mode cooling set points")
                    setCoolingSP(ThisThermostat, desired, currTempAtThermostat, altSensor, currTemp)

                    // WINDOWS PW SAVING
                    // ||||||||||||||||||||||||
                    // VVVVVVVVVVVVVVVVVVVVVVVV
                    okToOpen = windows && currTemp >= 68 && outsideTemp < desired && outsideHumidity < 70 

                    if(okToOpen)
                    // windows modes not accounted for here and no ALGEBRA humidity threshold
                    { 
                        if(okWindSavingMode || location.mode in savingModes) // user may have chosen windows not to open when !Active triggered saving mode
                        {
                            state.needWindowsOpen = true
                            need = "off"
                            logging("NOW in Power saving mode with windows open. 'need' set to $need goz5a6regui")
                            openWindows(0, "jhdfye")
                        }


                        if(contactsAreOpen()) // prevent repeated requests
                        {
                            logging("$ThisThermostat kept to $need because windows are now open 5547fde")

                        }
                        else // windows still not open? Try again (function will apply a timer between attempts)
                        {
                            state.closedByApp = true // allow new attempt
                            state.openByApp = false // allow new attempt
                            openWindows(0, "5467f") // windows are off while shoudl be on, turn them on again (it may take 5 minutes) 
                            need = "cool" // inconsistency, so set to cool
                            state.lastNeed = need
                            logging("need set to $need INCONSISTENCY")
                        }

                    }
                    else // if, later on, cool is still needed but it's not ok to open windows
                    {
                        state.needWindowsOpen = false
                        closeWindows("54daf")

                        if(currTemp > desired + 1)
                        {
                            need = "cool"
                            state.lastNeed = need
                            logging("need set to $need 1z4er")
                        }
                        else 
                        {
                            need = "off"
                            logging("need set to $need 544r")
                        }
                    }
                } 
                //  heat is required (and windows are of no use to save power in this case
                else if(outsideTemp < 60)
                {
                    need = "heat"
                    logging( "need set to $need 5677er")
                    if(!Active || !motionSensors)
                    {
                        state.desired = 66
                    }
                    else 
                    {
                        state.desired = 69 // remember that a saving mode can be either "away" or "no motion", so keep some heat if there's motion
                    }
                    setHeatingSP(ThisThermostat, desired)
                    closeWindows("57tdf")
                }
                if(contactsOpen && !criticalCold){
                    need = "off"
                    logging("need set to $need 7f4rz")
                }
            }
            else //if(inRegularModes)
            {  
                logging( "standard modes evaluation")
                okToChangeToCool = state.coolMode <= 4 || thermMode == "off"
                okToChangeToHeat = state.heatMode <= 4 || thermMode == "off"
                

                if(outsideTemp <= 68 && currTemp < desired && !contactsOpen && outsideTemp < desired) 
                // if the amplitude of the difference between desired temp and outside temperature is superior or equal to 10... 
                // then heat is needed
                {
                    if(okToChangeToHeat)
                    {
                        need = "heat"
                        state.lastNeed = need
                        logging( "$ThisThermostat will be set to $need 4578fsd")
                    }
                    else 
                    {
                        logging("too many operating mode changes occured recently, not changing 521dzgh7")
                        need = ThisThermostat.currentValue("thermostatMode") 
                    }
                }
                else if(currTemp > desired + 1 /*&& AmplitudeCoolOk*/ && !contactsOpen)
                    // no need to compare to outside here, if cold outside (except through amplitude), heat is triggered b4 this scope 
                // if the amplitude of the difference between desired temperature and outside temperature is inferior or equal to 10, 
                // AND if outside temperature is superior to desired temperature
                // then this is when cooling starts to be needed over heating
                {
                    if(okToChangeToCool)
                    {
                        need = "cool"
                        state.lastNeed = need
                        logging("need set to $need 7frez5")
                    }
                    else 
                    {
                        logging("too many operating mode changes occured recently, not changing 54ef98")
                        need = ThisThermostat.currentValue("thermostatMode") 
                    }
                }
                else 
                { 
                    need = "off" // inside temperature is already ok or contacts open, turn off thermostat   
                    logging("need set to $need 45azefd")
                }
                //boolean humidity4windows = outsideHumidity < humthreshold
                okToOpen = okToOpenWindows(feel, desired, outsideTemp, currTemp, humidityOk)

                logging( "okToOpen = $okToOpen")
                if(windows && inWindowsMode)
                {
                    logging("Evaluating the windows situation")

                    if(okToOpen) // windows conditions
                    {
                        logging("state.windowsStatus returns: '${state.windowsStatus}' && WAreOpen returns $WAreOpen 455712fhgjnz")
                        if(!WAreOpen)
                        {
                            openWindows(duration, "78er")
                            state.needWindowsOpen = true // this is just for debug... 
                        }
                    }
                    else 
                    {
                        logging("windows are to remain closed 5578rff")
                        state.needWindowsOpen = false // this is just for debug... 
                        closeWindows("748rtze")
                    }
                }
                else 
                {
                    logging("No windows management")
                    state.needWindowsOpen = false
                }

                if(contactsOpen && !criticalCold && !override)
                {
                    need = "off"
                    logging( """need set to $need 5d4fog
$ThisThermostat is off because some windows are open-""")
                }
                else if(contactsOpen && criticalCold)
                {
                    if(currTemp <= desired - 4 && !override)
                    {
                        need = "heat"
                        logging( "need set to $need frghl45")
                    }
                    logging("CRITICAL COLD!!, closing windows! (if any)")
                    state.closedByApp = true
                    windows?.off()
                }

                else if(contactsOpen && criticalHot)
                {
                    if(currTemp >= 80 && !override)
                    {
                        need = "cool"
                        state.lastNeed = need
                        logging("need set to $need 87fg4zj")
                    }
                    logging("CRITICAL HOT!, closing windows! (if any)")
                    state.closedByApp = true
                    windows?.off()
                }


                // end of regular modes eval
            }



            /*********************************************************************************************/
            /* END OF EVALS... NOW SET THERRMOSTAT TO SUBSEQUENT MODES AND SP's */
            /*********************************************************************************************/

            if(need == "heat")
            {
                setHeatingSP(ThisThermostat, desired)
                closeWindows("57tdf")
            }
            else if(need == "cool")
            {
                setCoolingSP(ThisThermostat, desired, currTempAtThermostat, altSensor, currTemp)
            }
            logging( "sendind thermostat mode cmd: $need  654ffg")
            setThermostatMode(Thermostat, need, "548d5g")

        }
        else 
        {
            logging( """


GLOBAL OVERRIDE MODE...doing nothing


-""")
        }

    }
    else 
    {
        logging( """
LOCATION IS NOT IN ANY OF THE DESIGNATED OPERATING MODES... doing nothing
current mode: ${location.mode} / active modes: $modes  &  $savingModes
-""")
    }

    logging( """

${savingmodeMSG}

Currently used Thermostat: ThisThermostat = $ThisThermostat (main is: $Thermostat)
bedSensorClosed = $bedSensorClosed
abidebysamerules = $abidebysamerules


location.mode = $location.mode
location.modes = $location.modes
location.mode = $location.mode
currMode = $currMode
modes are: $modes
inRegularModes = $inRegularModes
savingModes are: $savingModes
inSavingModes = ${inSavingModes}
motionModes = $motionModes
inMotionMode = ${inMotionMode}
inWindowsMode = $inWindowsMode
noMotionTime = $noMotionTime
motionSensors = $motionSensors
Active = $Active
windowsModes = $windowsModes


need = $need ('off' cmd is ignored when ThisThermostat != Thermostat --> ? ${ThisThermostat != Thermostat})
state.lastNeed = $state.lastNeed
thermMode = $thermMode
current operation = ${ThisThermostat.currentValue("thermostatOperatingState")}
thermodeIsOk = $thermodeIsOk
state.heatMode = $state.heatMode
state.coolMode = $state.coolMode
state.lastCoolMode = $state.lastCoolMode
state.lastHeatMode = $state.lastHeatMode 
elapsed timeSinceLastHeatModeChange = ${timeSinceLastHeatModeChange} minutes
elapsed timeSinceLastCoolModeChange = ${timeSinceLastCoolModeChange} minutes

okToChangeToCool = $okToChangeToCool
okToChangeToHeat = $okToChangeToHeat

state.desired = $state.desired
state.learnedDesiredTemp = $state.learnedDesiredTemp 
desired = $desired    
currTemp = $currTemp 
currTempAtThermostat = $currTempAtThermostat
outsideTemp = $outsideTemp
outsideHumidity = $outsideHumidity
insideHumidity = $insideHumidity
humthreshold = $humthreshold
humidityOk = $humidityOk
feel = $feel
okToOpen = $okToOpen
override = $override
criticalCold = $criticalCold ($currTemp <= $state.criticalTempC)
criticalHot = $criticalHot ($currTemp >= $state.criticalTempH)
state.criticalTempH = $state.criticalTempH
state.criticalTempC = $state.criticalTempC    

currHSP = $currHSP
currCSP = $currCSP

altSensor = ${altSensor}

contacts = $contact
contactsOpen = $contactsOpen
WAreOpen = $WAreOpen
contactTimerOk = $contactTimerOk
state.needWindowsOpen = $state.needWindowsOpen



state.openByApp = $state.openByApp
state.closedByApp = $state.closedByApp
state.alreadyClosed = $state.alreadyClosed 
state.alreadyOpen = $state.alreadyOpen

state.NoLearnMode = $state.NoLearnMode
state.learnBase = $state.learnBase
state.megaBase = $state.megaBase
megaBase = $megaBase


--------------- END ---------------
-""")

    if(!state.enablelogging)
    {
        logging( """all functions ok.
inSavingModes = $inSavingModes (currMode = $currMode && regular modes are: $modes && saving modes are: $savingModes)
need   ${need}
CSP:   ${Thermostat.currentValue("coolingSetpoint")}
HSP:   ${Thermostat.currentValue("heatingSetpoint")} 
State: ${Thermostat.currentValue("thermostatOperatingState")}
contacts open: ${contactsOpen}

""")
    }

}


boolean contactsAreOpen() 
{
    def s = contact.size()
    def i = 0
    boolean Open = false
    for(s!=0;i<s;i++)
    {
        if(contact[i]?.currentValue("contact") == "open")
        {
            Open = true
            break
        }
    }
    return Open
}
boolean windowsAreOpen()
{
    boolean WAreOpen = false
    s = windows?.size()
    i = 0
    for(s!=0;i<s;i++)
    {
        if(windows[i]?.hasCapability("Contact Sensor"))
        {       
            if(contact[i]?.currentValue("contact") == "open")
            {
                logging( "contact capability for ${windows[i]}")
                WAreOpen = true
                break // one open suffices
            }
        }
        else 
        {
            logging( "no contact capability for ${windows[i]}")
        }
    }
    logging( "windowsAreOpen returns $WAreOpen")
    return WAreOpen
}

def getCurrTemp()
{
    float result = Thermostat.currentValue("temperature")
    if(altSensor)
    {
        // if user selected multiple sensors, then get the average temperature so to get a field's value... sort of... 
        def s = altSensor.size()
        def i = 0
        float sum = 0
        for(s != 0;i < s; i++)
        {
            def device = altSensor[i]
            float deviceTemp = device.currentValue("temperature")
            sum = sum + deviceTemp
            logging( "$device returns $deviceTemp")
        }
        result = sum / s
        logging("Alternative Sensor ARE: $altSensor and they return an average temperature of ${result}F")
        // previous attempts to adjust Set points with amplitude between altsensor and thermostat's returned temp value
        // failed because they override user's settings and A.I. learning process... making the whole thing veeery annoying
        // despite being efficient
    }   
    logging( "Current Temperature is: $result")
    return result
}

boolean okToOpenWindows(feel, desired, outsideTemp, currTemp, humidityOk)
{
    int a = 1
    int b = 3
    int c = 6
    int d = 10
    //boolean Open = contactsAreOpen()
    boolean WAreOpen = windowsAreOpen() 
    boolean conditions = outsideTemp < desired - b && outsideTemp >= desired - d && state.lastNeed == "cool" && (humidityOk || desired >= outsideTemp + c)  

    boolean result = false
    if(feel > 55 && feel <= 74)
    {
        result = conditions
    }

    if(Thermostat.currentValue("thermostatMode") == "auto")
    {
        result = false
    }

    def debugMessage = """

feel > 55 && feel <= 74 ?: ${feel > 55} && ${feel <= 74} --> ${feel > 55 && feel <= 74}
state.lastNeed == "cool" ?: ${state.lastNeed == "cool"}
humidityOk || desired >= outsideTemp + $c ?: ${humidityOk} || ${desired >= outsideTemp + c} 
currTemp <= desired + c --- $currTemp < ${desired + c} -->  ${currTemp <= desired + c}
outsideTemp >= desired - d --- $outsideTemp >= ${desired - d} -->  ${outsideTemp >= desired - d}
outsideTemp < desired - b --- $outsideTemp < ${desired - b} -->  ${outsideTemp < desired - b}

"""

    logging( """
okToOpenWindows(
WAreOpen = $WAreOpen
state.closedByApp = $state.closedByApp 
state.openByApp = $state.openByApp
state.lastNeed = $state.lastNeed (MUST NEVER appear as 'off' )
feel:$feel, 
desired:$desired, 
outsideTemp:$outsideTemp, 
currTemp:$currTemp, 
inWindowsMode = ${location.mode in windowsModes}

$debugMessage


state.okToOpen = $state.okToOpen
result = $result
-""")

    state.okToOpen = result
    return result
}


def setThermostatMode(device, mode, cmdorigin)
{
    if(contactsAreOpen())
    {
        device.setThermostatMode("off")
    }
    else if(device.currentValue("thermostatMode") != mode)
    {
        device.setThermostatMode(mode)
        logging( "$device set to $mode $cmdorigin")
    }
    else 
    {
        logging("$device already set to $mode $cmdorigin")
    }
}
def setHeatingSP(ThisThermostat, desired)
{
    def adjustHum = adjustWithHumidity("heat")
    desired = desired + adjustHum

    state.NoLearnMode = true
    if(Thermostat.currentValue("heatingSetPoint") != desired)
    {
        state.NoLearnMode = true
        while(!state.NoLearnMode)  // if and only if nolearn is true, to prevent false learning input
        {
            logging( "Waiting for database to update properly 756324dfg")
        }

        Thermostat.setHeatingSetpoint(desired)
        logging( """
$Thermostat HeatingSetpoint set to $desired
-""")


    }
    else 
    {
        logging("$Thermostat already set to $desired 548fgr")
    }
}

def setCoolingSP(Thermostat, desired, currTempAtThermostat, altSensor, currTemp)
{
    logging("setting CSP")
    state.NoLearnMode = true
    while(!state.NoLearnMode)  // if and only if nolearn is true, to prevent false learning input
    {
        logging( "Waiting for database to update properly 8874ffghy")
    }

    def B4adjustWithHumidity = desired
    def adjustHum = adjustWithHumidity("cool")

    if(Thermostat.currentValue("coolingSetpoint") != desired)
    {
        Thermostat.setCoolingSetpoint(desired)
        logging( "${Thermostat}'s CoolingSetpoint set to $desired (= ${desired - adjustHum} + ${adjustHum})")
    }
    else 
    {
        logging( "$Thermostat already set to $desired 4478rrtt")
    }
    resetNoLearnMode()
}


def AImap(int outsideTemp)
{
    //state.TempAImap = [["outside":"desired"]]
    //state.humidAImap = ["desired":"humidity"]

    logging("reading from AImap: $state.tempAImap")

    /*  */

    //def map = 
    logging("state.tempAImap (AImap()) = $state.tempAImap")

    def result = state.tempAImap["${outsideTemp}".toString()] 

    logging("AImap() map returns: $result ( resB4 = $resB4 ) from outsideTemp = $outsideTemp")

    return result
}

def getComfort()
{
    float outsideTemp = outdoor.currentValue("temperature")
    // def GlobalHumidity = getHumidity()
    //  def outsideHumidity = GlobalHumidity[0]
    //  def insideHumidity = GlobalHumidity[1]


    def newComfort = AImap(outsideTemp.toInteger()).toInteger()

    logging( """
******************************************************************************************
outsideHumidity = $outsideHumidity
newComfort = $newComfort
******************************************************************************************
-""")



    return newComfort
    //return 71
}

def linearCoolHumidity(outsideHumidity)
{
    def xa = 98 // outsideHumidity 
    def ya = 70 // desired temp for outsideHumidity a 
    def xb = 40  // outsideHumidity 
    def yb = 75 // desired temp for outsideHumidity b  
    def coef = (yb-ya)/(xb-xa)
    // solve intercept, b
    def b = ya - coef * xa // solution to ya = coef*xa + b // HSPSet = coef*outsideTemp + b //
    //b = ya - coef * outsideTemp  //
    def y = coef * outsideHumidity + b 
    logging("linearCoolHumidity returns $y")
    return y
}
def linearCoolOutsideTemp(outsideTemp)
{
    def xa = 110 // outsideTemp 
    def ya = 78 // desired temp for outsideHumidity a 
    def xb = 70  // outsideTemp 
    def yb = 70 // desired temp for outsideHumidity b  
    def coef = (yb-ya)/(xb-xa)
    // solve intercept, b
    def b = ya - coef * xa // solution to ya = coef*xa + b // HSPSet = coef*outsideTemp + b //
    //b = ya - coef * outsideTemp  //
    def y = coef * outsideTemp + b 
    logging("linearCoolOutsideTemp returns $y")
    return y
}
def LogCoolAll(comfortHumidity, comfortOutside)
{
    logging( """
comfortHumidity = $comfortHumidity
comfortOutside = $comfortOutside
-""")
    def y = Math.log(comfortHumidity) / Math.log(comfortOutside) 
    logging("LogCoolAll returns $y")
    return y
}
def adjustWithHumidity(String need)
{

    def newComfortHumid = 0
    def GlobalHumidity = getHumidity()
    def outsideHumidity = GlobalHumidity[0]
    def insideHumidity = GlobalHumidity[1]
    float averageHumidity = (insideHumidity + outsideHumidity)/2
    def outsideTemp = outdoor.currentValue("temperature")
    def threshold = getHumidityThresHold()
    logging( """

outsideHumidity adjustments...
outsideHumidity = $outsideHumidity
insideHumidity = $insideHumidity
averageHumidity = $averageHumidity
humidity threshold = $threshold
need = $need
-""")

    if(need == "cool") // cooling conditions
    {
        if(averageHumidity > threshold)
        {
            newComfortHumid = -1
        }
    }
    else // heating
    {
        if(averageHumidity > threshold)
        {
            newComfortHumid = 1
        }
    }

    logging("Humidity adjustment returns $newComfortHumid as value to be added")
    //return newComfortHumid

    return newComfortHumid 

}

def getHumidity()
{
    boolean outDoorHasHumidity = outdoor.hasAttribute("humidity")
    boolean insideHasHumidity  = Thermostat.hasAttribute("humidity")

    def Outside = 50
    def Inside = 50
    if(insideHasHumidity)
    {
        Inside = Thermostat.currentValue("humidity")  
        logging("$Thermostat has humidity measurement capability and it returns: $Inside")
    }
    else 
    {
        logging("$Thermostat doesn't have humidity measurement capability")
    }
    if(outDoorHasHumidity)
    {
        Outside = outdoor.currentValue("humidity")
    }
    logging("getHumidity() returns: [Outside:$Outside, Inside:$Inside]")
    return [Outside, Inside]
}

def getHumidityThresHold()
{
    // the lower the outside temp, the higher the humidity tolerance 

    //float currTemp = Thermostat.currentValue("temperature")
    float outsideTemp = outdoor.currentValue("temperature")
    def xa = 72 // temp a
    def ya = 75 //  Humidity tolerance for temp a 
    def xb = 65  // temp b 
    def yb = 90 //  Humidity tolerance for temp b
    def coef = (yb-ya)/(xb-xa)
    //solve intercept, b
    def b = ya - coef * xa // solution to ya = coef*x + b // HSPSet = coef*outsideTemp + b //
    def y = coef * outsideTemp + b 
    logging("new outsideHumidity threshold is ${y.round().toInteger()}")
    return y.round().toInteger()
}

boolean motionTest() 
{
    int events = state.motionEvents
    boolean inMotionMode = location.mode in motionModes
    boolean result = true // default is true  always return Active = true when no sensor is selected by the user

    boolean doorOpen = true // true by default so when this option is not selected motion test will still run
    if(door)
    {
        doorOpen = door?.currentValue("contact") == "open" // if open, manage motion, if not, don't count motion events 
    }

    if(motionSensors && inMotionMode && doorOpen)
    {

        long lastMotionEvent = now() - state.lastMotionEvent as long
            long timeOutMillis = noMotionTime * 1000 * 60 as long
            boolean MotiontimeOut = lastMotionEvent > timeOutMillis
        int minutes = lastMotionEvent/1000/60
        /*******************  *******************************
thisDeviceEvents = motionSensors[i].eventsSince(new Date(now() - deltaMinutes)).findAll{it.value == "active"}
THIS WORKS BUT TAKES FOREVER FOR SOME REASON (up to 10 seconds measured!)
********************  *******************************/

        logging( "MotiontimeOut = $MotiontimeOut | lastMotionEvent = ${if((lastMotionEvent/1000)>=60){"${Math.round(minutes)} minutes"}else{"${(lastMotionEvent/1000)} seconds"}} ago")

        if(MotiontimeOut)
        {
            state.motionEvents = 0
        }

        result = !MotiontimeOut 
    } 
    else 
    {
        logging("""motion test IS NOT running for any of the following reasons :
- because location is not in motion modes (${!(location.mode in motionModes)}
- or because user hasn't selected any motion sensor (${!motionSensors})
- or because a door contact option was selected and it is closed (${door?.currentValue("contact") == "open"}
so returning default result (true)""")
    }


    logging( "motion returns ${result} with ${state.motionEvents} events and doorOpen = $doorOpen and inMotionMode = $inMotionMode (motion modes are: $motionModes")
    return result
}

def minutes(long s)
{
    if(s >= 60){ return s/60}else{return 0}   
}

def hours(long m)
{
    if(m >= 60){ return m/60}else{return 0}   
}

def days(long h)
{
    if(h >= 24){ return h/24}else{return 0}   
}

def stopWindows()
{
    logging("stopping $windows")
    windows.stop()

}


def Poll()
{
    boolean thermPoll = Thermostat.hasCommand("poll")
    boolean thermRefresh = Thermostat.hasCommand("refresh") 
    boolean outsidePoll = outdoor.hasCommand("poll")
    boolean outsideRefresh = outdoor.hasCommand("refresh")
    boolean override = state.override
    if(location.mode in modes && !override)
    {
        if(thermRefresh){
            Thermostat.refresh()
            logging("refreshing $Thermostat")
        }
        else if(thermPoll){
            Thermostat.poll()
            logging("polling $Thermostat")
        }
        if(outsideRefresh){
            outdoor.refresh()
            logging("refreshing $outdoor")
        }
        else if(outsidePoll){
            outdoor.poll()
            logging("polling $outdoor")
        }

    }
    else if(override)
    {
        logging("not polling devices due to override mode")
    }

}

def openWindows(int duration, String cmdOrigin)
{
    def windowsStatus = "NA"
    def currMode = location.mode
    boolean inRegularModes = currMode in modes
    boolean Active = motionTest()
    boolean inSavingModes = (currMode in savingModes || (inRegularModes && !Active)) && bedSensorIsOpen()
    boolean inMotionMode = currMode in motionModes 
    boolean WAreOpen = windowsAreOpen() || contactsAreOpen()
    def thermMode = Thermostat.currentValue("thermostatMode")

    long lastOpen = state.openTime as long
        long lastClosed = state.closeTime as long
        long timeLimit = 1000 * 60 * 15 as long
        boolean timesinceLastCloseOk = now() - lastClosed >  timeLimit //&& !WAreOpen // can reopen after a minumum of 5 minutes
    boolean timesinceLastOpenOk = now() - lastOpen > timeLimit //&& !WAreOpen


    def cur = new Date(now()-lastClosed).format('mm:ss',location.timeZone)
    def cur2 = new Date(now()-lastOpen).format('mm:ss',location.timeZone)
    def msg="Time Elapsed since last closing Event: $cur"
    def msg2="Time Elapsed since last opening Event: $cur2"

    logging( """ARE WINDOWS OPEN NOW ? ${if(WAreOpen){"YES"}else{"NO"}} (openWindows())
WAreOpen = $WAreOpen
timesinceLastCloseOk = $timesinceLastCloseOk
timesinceLastOpenOk = $timesinceLastOpenOk
$msg
$msg2
cmd Origin is: $cmdOrigin
WAreOpen = $WAreOpen
-""")


    if(timesinceLastCloseOk && timesinceLastOpenOk || inSavingModes) 
    // if it has been closed for long enough and last open happened some time ago 
    // OR IF IN SAVING MODES
    {
        if((!state.openByApp && !WAreOpen) || inSavingModes && thermMode != "auto" )
        {
            windows.on()
            state.windowsStatus = "alreadyopen"
            state.openTime = now() as long
                state.alreadyClosed = false
            state.alreadyOpen = true
            state.closedByApp = false
            state.openByApp = true
            logging( "opening $windows for a duration of $duration ")
            windowsStatus = "open"

            if(WAreOpen) // make sure it's open (in case it didn't work, we want ac to continue running)
            {
                setThermostatMode(Thermostat, "off", "54df5")

            }
            if(!windowDelay || windowDelay == 0)
            {
                logging( "no stop delay, $windows stay on")
            }
            else
            {
                def dura = getDuration()
                runIn(dura, stopWindows)
            }
        }
        else {
            if(WAreOpen)
            {
                logging( "Windows are already open")
            }
            if(!inSavingModes)
            {
                logging( "Windows not in saving mode")
            }
        }

    }
    else 
    {
        logging("Windows have been closed or opened too recently, not opening 8574ddft")
    }
}

def getDuration()
{
    def outsideTemp = outdoor.currentValue("temperature")
    def dura = 0
    if(windowDelay && windowDelay != 0)
    {
        dura = windowDelay //defined by user   
        logging( "User has defined an operating time")
    }
    logging("windowDelay base = $windowDelay")

    def xa = dura // max duration defined by user
    def ya = 85 // outside temperature  
    def xb = 5  // duration b
    def yb = 65 // outside temperature 
    def coef = (yb-ya)/(xb-xa)
    // solve intercept, b
    // solution to ya = coef*xa + b
    def b = ya - coef * xa // HSPSet = coef*outsideTemp + b /
    def y = coef * outsideTemp + b 
    y = Math.round(y).toInteger()
    logging("getDuration returns $y")
    return y
}

def closeWindows(String cmdOrigin)
{

    logging( """cmd Origin is: $cmdOrigin (closeWindows() 
state.closedByApp = $state.closedByApp (must be false)
state.openByApp = $state.openByApp (must be true)
state.okToOpen + $state.okToOpen (must be false)""")

    if(!state.okToOpen)
    {
        boolean WAreOpen = windowsAreOpen() || contactsAreOpen()

        // MAJOR SAFETY: we don't want windows to close on a, for example, fire emergency situation... 
        // so we give a 30 min delay before trying to close them again if they're still open after a closing request
        long lastOpen = state.openTime as long
            long lastClosed = state.closeTime as long
            long timeLimit = 1000 * 60 * 30 as long
            boolean timesinceLastCloseOk = now() - lastClosed > timeLimit // * 2 && !WAreOpen // one can vent their place for 5 minutes
        boolean timesinceLastOpenOk = now() - lastOpen > timeLimit //&& WAreOpen

        def cur = new Date(now()-lastClosed).format('mm:ss',location.timeZone)
        def cur2 = new Date(now()-lastOpen).format('mm:ss',location.timeZone)
        def msg="Time Elapsed since last closing Event: $cur"
        def msg2="Time Elapsed since last opening Event: $cur2"

        logging( """ARE WINDOWS OPEN NOW ? ${if(WAreOpen){"YES"}else{"NO"}} (closeWindows())
WAreOpen = $WAreOpen
timesinceLastCloseOk = $timesinceLastCloseOk
timesinceLastOpenOk = $timesinceLastOpenOk
$msg
$msg2
cmd Origin is: $cmdOrigin
WAreOpen = $WAreOpen
---------------
-""")

        if(timesinceLastCloseOk && timesinceLastOpenOk)
        { // if it's been opened for long enough AND last close has happened long enough ago
            if(!state.closedByApp && WAreOpen) // temporary avoid discrepancy when window failed to close, resend the command
            {
                windows.off()
                setFans("auto")
                state.windowsStatus = "closed"
                state.closeTime = now() as long
                    state.alreadyClosed = true
                state.alreadyOpen = false
                state.closedByApp = true
                state.openByApp = false
                logging( "closing $windows")
            }
            else 
            {
                logging("$windows already closed")
            }

        }
        else 
        {
            logging("Windows have been opened or closed too recently, not closing")
        }
    }
    else 
    {
        logging( "ignoring closing windows cmd because state.okToOpen = $state.okToOpen")
    }
}

def setFans(String val)
{
    def fanMode = Thermostat.currentValue("thermostatFanMode")
    boolean fanModeOk = thermMode in [val]
    if(!fanModeOk && activeFans) 
    {
        Thermostat.setThermostatFanMode(val)
    }
}
//**************

def send(msg){
    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage) {
            logging("sending push message")
            sendPushMessage(msg)
        }
        if (phone) {
            logging("sending text message")
            sendSms(phone, msg)
        }
    }

    logging(msg)
}
boolean bedSensorIsOpen(){

    def minutes = 2
    def delay = minutes * 60 // this one must be in seconds
    def ContactsEventsSize = null
    boolean ConsideredOpen = true // IF NO BED SENSOR ALWAYS CONSIDERED OPEN

    // such sensors can return unstable values (open/close/open/close)
    // when it is under a certain weight, values will tend be consistently closed
    // so when unstable it mostly means that there's no more substantial weight on it
    // so what we want to do here is consider it as open every time it's unstable
    // and closed when stable (only 1 event within the false alarm time frame)

    if(bedSensor){
        ConsideredOpen = false // false by default to avoid false open positives

        def CurrentContacts = bedSensor?.currentValue("contact")
        boolean isOpen = CurrentContacts == "open"
        logging("$bedSensor isOpen = $isOpen")

        // get the size of events within false alarm threshold
        def evts = bedSensorEvtSize()
        logging("bedSensor events within false alarm threshold = ${evts}")  

        if(evts >= 1) {
            logging("several events (evts = $evts), so $bedSensor is considered as pressed/closed")
            ConsideredOpen = false 
            // many events, therefore it will be considered as closed since that means that there's still someone there
        }
        else {
            // if 0 events then..  
            if(!isOpen){
                // if last status is closed while there has been no event during false alarm threshold,
                // then it's a prolonged closed status (someone is sitting or lying down here)

                ConsideredOpen = false
                state.attempt = 0
                logging("no new event within the last $minutes minutes && $bedSensor is still closed, so it is now considered as closed")

            }
            else if(isOpen) { 
                // if it is declared open with 0 events within time threshold, then it is considered as open
                ConsideredOpen = true // 
                logging("no new event within the last $minutes minutes && $bedSensor is still open, so it is now considered as open")
            }
        }
    }
    logging("ConsideredOpen = $ConsideredOpen")
    return ConsideredOpen
}
def bedSensorEvtSize() {

    // here we check how many events occured during a time windows of 2 minutes to work around false positives
    def minutes = 2 
    def deltaMinutes = minutes * 60000 as Long

    //def ContactsEvents = bedSensor?.collect{ it.eventsSince(new Date(now() - deltaMinutes)) }.flatten()
    // def ContactsEvents = bedSensor?.collect{ it.eventsSince(new Date(now() - (60000 * deltaMinutes))) }.flatten() // needs for loop to distinguish events per device
    //bedSensor[0].statesBetween("contact", start, end, [max: 200]))

    def events = bedSensor.eventsSince(new Date(now() - deltaMinutes)) 
    logging("$bedSensor events = ${events?.size()}")      

    def size = events?.size()
    logging("Timer Found ${size} events in the last $minutes minutes at ${bedSensor}")

    return size
}


def resetNoLearnMode()
{

    while(state.NoLearnMode)  
    {
        state.NoLearnMode = false
        logging( "Waiting for database to update properly 54dfgzer8")

    }
}

def logging(message)
{
    state.enablelogging = true
    if(state.enablelogging) log.debug message

}

