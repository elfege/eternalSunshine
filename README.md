# eternalSunshine
App that sets dimmers with luminescence and that allows you to select the following options:

- ## Motion sensors
Set different timeouts dealys with each location modes of your choice
- ## Customizable Max Lux value for your dimmer
- ## Basic Machine Learning: 
The app can learn the max value of your sensor over time and increase its performance and accuracy accordingly
- ## Optional advanced logarithmic settings
Use a graph helper so you can set variables (offset, log base and multiplier) : this will allow you to fine tune your settings at will. 
- ## pause the app using a physical button
- ## Conflict prevention: 
**Although it is HIGHLY recommended to have those dimmers exclusively managed by Eternal Sunshine,** you can make the app aware of the fact that your dimmers are also controlled by a different app.
If you select this option, the app will never completely turn off your dimmers but, instead, bring them down to minimal dimming value (1) so your other app doesn't get triggered by an "off" event and can still turn them off if needed: Eternal Sunshine will "know" that this event wasn't triggered internally and pause its activity until those lights are turned back on. **Again, be aware that for the technical reason mentioned above, ES will NEVER completely turn off your lights if you have selected this option.** 
- ## Use 2 different lux sensors in sync with a switch:
For example, if you close your curtains/blinders, providing your main sensor is outside, the app will still read a high lux value and keep your lights low. BUT, if you use the alternate sensor and select your curtains/blinders as a switch capability, then the app will read the lux value from this alternate sensor that sits inside your room, instead of reading these values from the outdoor sensor, thus keeping your lights up. I have found this feature to be very convenient, especially in the summer when I need to coold down the place and therefore close my blinds and curtains. 
- ## Turn off your curtains with illuminance and temperature 
Using the same switch as the one mentioned above, close/open your curtains/blinds depending on outside illuminance (if you have, for example, a sensor placed in front of a souht facing window) and inside temperature. This allows you to have your curtains close automatically when it gets too hot inside due to reverberation and thus to save some substantial amount of AC power (you might also want to take a look at my Thermostat Manager, which offers some very cool features too!). 

Feel free to send your feedback over Hubitat's community thread: 
https://community.hubitat.com/t/release-eternal-sunshine-luminescence-and-dimmers/34744/94 


