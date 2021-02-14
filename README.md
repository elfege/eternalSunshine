# eternalSunshine
App that sets dimmers with luminescence and:

- Motion timers that can be set with different values with location modes
- Customizable Max Lux value for your dimmer
- Learning process: the app can also learn the max value of your sensor over time and increase its performance and accuracy
- Optional advanced logarithmic settings with a graph helper so you can set variables (offset, log base and multiplier) : this will allow you to fine tune your settings at will. 
- Possibility to pause the app using a physical button
- Possibility to make the app aware of the fact that your dimmers are also controlled by another app (in which case it will never really turn them off but bring them down to minimal dimming value (1) so your other app doesn't get triggered by an "off" event and can still turn them off if needed: Eternal Sunshine will "know" that this event wasn't triggered internally and pause its activity until those lights are turned back on. Again, be aware that for the technical reason mentioned above, ES will NEVER completely turn off your lights if you have selected this option. 
- Possibility to use 2 different lux sensors in sync with a switch: For example, if you close your curtains/blinders, providing your main sensor is outside, the app will still read a high lux value and keep your lights low. BUT, if you use the alternate sensor and select your curtains/blinders as a switch capability, then the app will read the lux value from this alternate sensor that sits inside your room, instead of reading these values from the outdoor sensor, thus keeping your lights up. I have found this feature to be very convenient, especially in the summer when I need to coold down the place and therefore close my blinds and curtains. 

Feel free to send your feedback over Hubitat's community thread: 
https://community.hubitat.com/t/release-eternal-sunshine-luminescence-and-dimmers/34744/94 


