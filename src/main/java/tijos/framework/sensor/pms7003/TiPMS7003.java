package tijos.framework.sensor.pms7003;

import java.io.IOException;

import tijos.framework.devicecenter.TiGPIO;
import tijos.framework.devicecenter.TiUART;
import tijos.framework.util.BigBitConverter;
import tijos.framework.util.Delay;

/**
 * Plantower PMS7003 dust sensor driver for TiJOS
 *
 */
public class TiPMS7003 {

	private static final int FRAME_LEN = 32;

	/**
	 * UART
	 */
	private TiUART uartObj = null;

	/**
	 * GPIO
	 */
	private TiGPIO gpioPort = null;

	int gpioSetPin = 0;

	/**
	 * buffer for one package data
	 */
	byte[] dataBuffer = new byte[FRAME_LEN];

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
		this.uartObj = uart;
		this.gpioPort = gpio;
		this.gpioSetPin = setPin;

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
		return DATA[4];
	}

	/**
	 * PM2.5
	 * 
	 * @return
	 */
	public int getPM2_5() {
		return DATA[5];
	}

	/**
	 * PM10
	 * 
	 * @return
	 */
	public int getPM10() {
		return DATA[6];
	}

	/**
	 * Read the data from the device and parse the PM value
	 * 
	 * @throws IOException
	 */
	public void measure() throws IOException {

		byte[] ioBuffer = new byte[FRAME_LEN];
		int total = 0;

		while (true) {
			// find the data head
			int dataLen = this.uartObj.read(ioBuffer, 0, FRAME_LEN);
			// System.out.println("data len " + dataLen);
			if (dataLen > 0) {

				// head
				for (int i = 0; i < dataLen; i++) {
					if (ioBuffer[i] == 0x42 && ioBuffer[i + 1] == 0x4d) {
						int available = dataLen - i;
						System.arraycopy(ioBuffer, i, this.dataBuffer, total, available);
						total = available;
						break;
					}
				}

				// enough data for one package
				if (total >= FRAME_LEN) {
					process();
					return;
				}

				// read left data
				int counter = 100;
				while (total < FRAME_LEN) {
					int left = FRAME_LEN - total;

					dataLen = this.uartObj.read(ioBuffer, 0, left);
					if (dataLen == 0) {
						counter--;
						if (counter <= 0) {
							throw new IOException("No more data arrived.");
						}

						Delay.msDelay(10);
						continue;
					}

					System.arraycopy(ioBuffer, 0, this.dataBuffer, total, dataLen);
					total += dataLen;
				}

				// enough data for one package
				process();
				return;
			}
		}
	}

	/**
	 * Parse the data to get the PM values
	 * 
	 * @throws IOException
	 */
	private void process() throws IOException {

		int length = BigBitConverter.ToUInt16(this.dataBuffer, 2);

		// invalid data, clear
		if (length != FRAME_LEN - 4) {
			throw new IOException("Invalid data length " + length);
		}

		int SUM = BigBitConverter.ToUInt16(this.dataBuffer, FRAME_LEN - 2);
		int sum = sum(this.dataBuffer, 0, FRAME_LEN - 2);
		if (SUM != sum) {
			// invalid data clear
			throw new IOException("Invalid Verification Code " + SUM + " : " + sum);
		}

		// Data Part
		for (int i = 0; i < 13; i++) {
			DATA[i] = BigBitConverter.ToUInt16(this.dataBuffer, i * 2 + 4);
		}

	}

	/**
	 * Calculate check sum
	 * 
	 * @param buffer
	 * @param offset
	 * @param len
	 * @return
	 */
	private int sum(byte[] buffer, int offset, int len) {
		int sum = 0;

		for (int i = 0; i < len; i++) {
			sum += (buffer[offset + i] & 0xFF);
		}

		return sum & 0xFFFF;
	}
}
