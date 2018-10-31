package tijos.framework.sensor.pms7003;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import tijos.framework.devicecenter.TiGPIO;
import tijos.framework.devicecenter.TiUART;
import tijos.framework.util.BigBitConverter;
import tijos.framework.util.Delay;
import tijos.framework.util.Formatter;

/**
 * Plantower PMS7003 dust sensor driver for TiJOS
 *
 */
public class TiPMS7003 {

	/**
	 * UART
	 */
	InputStream input = null;

	/**
	 * GPIO
	 */
	private TiGPIO gpioPort = null;

	int gpioSetPin = 0;

	/**
	 * parsed PM data
	 */
	int[] DATA = new int[13];

	/**
	 * Initialize with Uart and GPIO
	 * 
	 * @param uart
	 * @param gpio
	 * @param setPin
	 */
	public TiPMS7003(TiUART uart, TiGPIO gpio, int setPin) {
		this.gpioPort = gpio;
		this.gpioSetPin = setPin;

		this.input = new BufferedInputStream(new TiUartInputStream(uart), 256);
	}

	/**
	 * Initialize the GPIO pin mode
	 * 
	 * @throws IOException
	 */
	public void initialize() throws IOException {
		this.gpioPort.setWorkMode(gpioSetPin, TiGPIO.OUTPUT_PP);
	}

	/**
	 * set the device to working mode
	 * 
	 * @throws IOException
	 */
	public void active() throws IOException {
		this.gpioPort.writePin(this.gpioSetPin, 1);
	}

	/**
	 * set the device to sleep mode
	 * 
	 * @throws IOException
	 */
	public void sleep() throws IOException {
		this.gpioPort.writePin(this.gpioSetPin, 0);
	}

	/**
	 * PM1
	 * 
	 * @return
	 */
	public int getPM1() {
		return DATA[3];
	}

	/**
	 * PM2.5
	 * 
	 * @return
	 */
	public int getPM2_5() {
		return DATA[4];
	}

	/**
	 * PM10
	 * 
	 * @return
	 */
	public int getPM10() {
		return DATA[5];
	}

	/**
	 * Read the data from the device and parse the PM value
	 * 
	 * @throws IOException
	 */
	public void measure() throws IOException {

		int lastData = 0;
		int timeOut = 5000; //5 seconds timeout 
		while (timeOut > 0) {
			try {
				Delay.msDelay(10);
				timeOut -= 10;
				int val = input.read();
				if (val <= 0) {
					continue;
				}
				
				if(lastData == 0x42 && val == 0x4d) {
					
					while (input.available() < 30 && timeOut > 0) {
						Delay.msDelay(10);
						timeOut -= 10;
						continue;
					}
					
					int lenH = input.read();
					int lenL = input.read();
					int len = lenH * 256 + lenL;

					System.out.println("len " + len);
					if (len != 2 * 13 + 2) {
						lastData = lenL;
						continue;
					}

					byte[] data = new byte[len];
					input.read(data);

					int initValue = lastData + val + lenH + lenL;
					int sum1 = calCheckSum(initValue, data, 0, len - 2);
					int sum2 = BigBitConverter.ToUInt16(data, len - 2) & 0x00FF;

					if (sum1 != sum2) {
						// invalid data clear

						System.out.println("Invalid Verification Code " + sum1 + " : " + sum2);						
						lastData = 0;
						continue;
					}

					System.out.println("Time " + timeOut + " PM Data " + Formatter.toHexString(data));
					// Data Part
					for (int i = 0; i < 13; i++) {
						DATA[i] = BigBitConverter.ToUInt16(data, i * 2);
					}
				
					return;
				}
				else {
					lastData = val;
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		throw new IOException("UART timeout.");

	}

	/**
	 * Checksum calculation
	 * 
	 * @param initalValue
	 *            initialize value
	 * @param buf
	 * @param start
	 * @param len
	 * @return Checksum
	 */
	public static int calCheckSum(int initalValue, byte[] buf, int start, int len) {
		int sum = initalValue;
		int i = 0;

		for (i = start; i < start + len; i++) {
			sum += buf[i];
		}

		return (sum & 0x00FF);
	}

}
