package omniWheel;

import java.io.*;
import java.net.*;
import lejos.hardware.*;
import lejos.hardware.motor.*;
import lejos.hardware.port.*;
import lejos.hardware.sensor.MindsensorsAbsoluteIMU;
import lejos.robotics.navigation.OmniPilot;
import lejos.robotics.GyroscopeAdapter;
import lejos.robotics.SampleProvider;
import lejos.hardware.ev3.LocalEV3;

@SuppressWarnings("deprecation")
public class OmniWheel extends Thread {
	public static final int port = 7360;
	private Socket client;
	private static boolean looping = true;
	private static ServerSocket server;

	private OmniPilot pilot;
	private MindsensorsAbsoluteIMU myCompass = new MindsensorsAbsoluteIMU(SensorPort.S1);
	private SampleProvider direction = myCompass.getCompassMode();
	float[] ev3Sample = new float[direction.sampleSize()];
	static Power battery = LocalEV3.get().getPower();
	private SampleProvider gyro = myCompass.getAngleMode();
	private GyroscopeAdapter myGyro = new GyroscopeAdapter(gyro,200f);
	ObjectInputStream ois;
	InputStream in;

	private boolean absolute=false;
	public static final int CLOSE = 0;
	public static final int FORWARD = 87, // W = main up
			STRAIGHT = 83, // S = straight
			LEFT = 65, // A = left
			RIGHT = 68, // D = right
			BACKWARD = 88, // X = main down
			TURNRIGHT = 69,
			TURNLEFT = 81;

	@SuppressWarnings("deprecation")
	public OmniWheel(Socket client) {
		this.client = client;
		Button.ESCAPE.addKeyListener(new EscapeListener());
		pilot = new OmniPilot(48f,85.0f,Motor.A, Motor.C,Motor.B,true,true,battery,myGyro);
		pilot.setLinearSpeed(80.0);
		toggle();
	}

	public static void main(String[] args) throws IOException {
		server = new ServerSocket(port);
		while(looping) {
			System.out.println("Awaiting client..");
			new OmniWheel(server.accept()).start();
		}
	}

	/*	
	private void readCompass() {
		while (true) {
			direction.fetchSample(ev3Sample, 0);
			for (int i=0; i<ev3Sample.length;i++) {
				System.out.println("D: "+ev3Sample[i]);
			}
		}
	}
	*/

	// driving car with keyboard
	@SuppressWarnings("deprecation")
	public void carAction(int command) {
		direction.fetchSample(ev3Sample, 0);
		for (int i=0; i<ev3Sample.length;i++) {
			System.out.println("D: "+ev3Sample[i]);
		}
		switch(command) {
		case BACKWARD:
			pilot.travel(-20,((double) ev3Sample[0])+180);
			break;
		case FORWARD: // north
			pilot.travel(20.0, -((double) ev3Sample[0]));
			break;
		case STRAIGHT:
			//A.rotateTo(0);
			break;
		case RIGHT:
			pilot.travel(-20,((double) ev3Sample[0])+270);
			break;
		case LEFT:
			pilot.travel(-20,((double) ev3Sample[0])+90);
			break;
		case TURNLEFT:
			pilot.rotate(-45);
			break;
		case TURNRIGHT:
			pilot.rotate(45);
			break;	
		}
	}

	// driving car with spaceNavigator
	@SuppressWarnings("deprecation")
	public void carAction(int linearSpeed, int angle, int angularSpeed) {
		direction.fetchSample(ev3Sample, 0);
		if (angularSpeed <-10 || angularSpeed>10) {
			pilot.spinningMove(0, angularSpeed, 0);
		}
		// absolute movement
		else if (absolute) {
				pilot.moveStraight(linearSpeed, (int) (-ev3Sample[0]+angle));
		}
		// relative movement
		else if (!absolute) {
			pilot.moveStraight(linearSpeed, (int) angle);
			// The spinning move method is more sophisticated but difficult to control
			// pilot.spinningMove(linearSpeed, angularSpeed, (int) angle);
		}
		//Uncomment for debugging data
		//System.out.println("S: "+linearSpeed+" A: "+angle+" Com: "+ev3Sample[0]+" C: "+(int) (-ev3Sample[0]+angle)+" Sp: "+angularSpeed);
	}
	
	private void toggle() {
		if (absolute) {
			absolute=false;
			Sound.beep();
			Button.LEDPattern(1);
			System.out.println("Relative");
		}
		else {
			absolute=true;
			Sound.beep();
			Button.LEDPattern(2);
			System.out.println("Absolute");
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	public void run() {
		System.out.println("CLIENT CONNECT");
		try {
			in = client.getInputStream();
			DataInputStream dIn = new DataInputStream(in);			
			while(client != null) {
				String receivedNumString = dIn.readUTF();
				String[] numstrings = receivedNumString.split("@@");
				int[] myMessageArray = new int[numstrings.length];
				int indx = 0;
				for(String numStr: numstrings){
					myMessageArray[indx++] = Integer.parseInt(numStr);
				}
				if(myMessageArray[0]  == CLOSE) {
					// close command
					client.close();
					client = null;
				} else if (myMessageArray[0]==2){
					// keyboard command
					carAction(myMessageArray[1]);
					//TODO: create absolute/relative switch 	
				} else if (myMessageArray[0]==1) {
					// spaceNavigator
					carAction(myMessageArray[1], myMessageArray[2], myMessageArray[3]);
				}
				else if (myMessageArray[0]==3) {
					// toggle absolute/relative
					toggle();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class EscapeListener implements KeyListener {

		public void keyPressed(Key k) {
			looping = false;
			System.exit(0);
		}

		public void keyReleased(Key k) {}
	}
}