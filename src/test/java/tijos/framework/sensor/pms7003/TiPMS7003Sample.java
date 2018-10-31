package tijos.framework.sensor.pms7003;

import java.io.IOException;

import tijos.framework.devicecenter.TiGPIO;
import tijos.framework.devicecenter.TiUART;
import tijos.framework.util.Delay;

/**
 * Plantower PMS7003 Dust Sensor Sample
 *
 */
public class TiPMS7003Sample {
	public static void main(String[] args) {
 
		int gpioPort0 = 0;
		int gpioPin0 = 1;

		int uartPort = 0;

		System.out.println("Hello PMS7003");
		
		try {

			// Open UART and GPIO
			TiGPIO gpio0 = TiGPIO.open(gpioPort0, gpioPin0);
			TiUART uart = TiUART.open(uartPort);

			uart.setWorkParameters(8, 1, TiUART.PARITY_NONE, 9600);

			TiPMS7003 pms = new TiPMS7003(uart, gpio0, gpioPin0);

			pms.initialize();
			pms.active();

			while (true) {
				try {
					pms.measure();
				} catch (IOException ie) {
					ie.printStackTrace();
					continue;
				}

				System.out.println(" PM1 " + pms.getPM1() + " PM2.5 " + pms.getPM2_5() + " PM10 " + pms.getPM10());

				Delay.msDelay(3000);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
