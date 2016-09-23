package kura.gpio.raspberrypi;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class PirSensorService{
	static final Logger s_logger = LoggerFactory.getLogger(PirSensorService.class);

    static final String APP_ID = "PirSensorService";
    
    static final GpioController gpioSensor = GpioFactory.getInstance();
    static final GpioPinDigitalInput motionSensor = gpioSensor.provisionDigitalInputPin(RaspiPin.GPIO_04);
    static final GpioController gpioLED = GpioFactory.getInstance(); 
    static final GpioPinDigitalOutput led = gpioLED.provisionDigitalOutputPin(RaspiPin.GPIO_05,PinState.LOW);
    private static CoapServer server = new CoapServer();
    private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
    private static String sensorState;
	public void activate(ComponentContext componentContext) {

		s_logger.info("Activating {} ... ", APP_ID);
		
		server.add(new HelloResource());
		 
		for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
			if (addr instanceof Inet4Address || addr.isLoopbackAddress()) {
				InetSocketAddress bindToAddress = new InetSocketAddress(addr, COAP_PORT);
				server.addEndpoint(new CoapEndpoint(bindToAddress));
			}
				
		}

		server.start();
		
		motionSensor.addListener(new GpioPinListenerDigital() {  
			
		    @Override       
		    public void handleGpioPinDigitalStateChangeEvent(
		    		GpioPinDigitalStateChangeEvent event) { 
		    	
		        if (event.getState().isHigh()) {  
		        	led.high();
		        	sensorState = "Detected!";
		        	s_logger.info("[{}] : Sensor detected a movement!",APP_ID);
		        }   
		        
		        if (event.getState().isLow()) {   
		        	led.low();
		        	sensorState = "Waiting...";
		        	s_logger.info("[{}] : Sensor is waiting...",APP_ID);
		        }   
		        
		    }   
		    
		});
		
		System.out.println("succesfull");
		
		while (true) {
            try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		
	}

	public void deactivate(ComponentContext componentContext) {

		s_logger.debug("Deactivating {} ... ", APP_ID);
		gpioSensor.shutdown();
		gpioLED.shutdown();
		server.stop();
		s_logger.debug("Deactivating {} ... Done!", APP_ID);
		System.out.println("succesfull");
		
	}
	public static class HelloResource extends CoapResource {
        public HelloResource() {

            // resource identifier
            super("PirSensor");

            // set display name
            getAttributes().setTitle("PirSensor Resource");
        }
        
        @Override
        public void handleGET(CoapExchange exchange) {
            exchange.respond(sensorState);
        }
    }
	
}
