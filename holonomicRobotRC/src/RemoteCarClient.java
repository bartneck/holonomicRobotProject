import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class RemoteCarClient extends Frame implements KeyListener{
	public static final int PORT = 7360;
	public static final int CLOSE = 0;
	public static final int FORWARD = 87, // W = main up
			STRAIGHT = 83, // S = straight
			LEFT = 65, // A = left
			RIGHT = 68, // D = right
			BACKWARD = 88, // X = main down
			TURNRIGHT = 69,
			TURNLEFT = 81;
	private static final int spaceNavigatorButton=3;
	private static final int spaceNavigatorSource=1;
	private static final int keyboard=2;
	private static final int maxSpeed=255;
	private boolean looping=false;

	private SpaceNavigator sn = new SpaceNavigator();
	private double maxValue=1.66;
	private int sampleRate=500;

	Button btnConnect;
	TextField txtIPAddress;
	TextArea messages;
	private Socket socket;
	private DataOutputStream outStream;


	public RemoteCarClient(String title, String ip) {
		super(title);
		this.setSize(400, 300);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("Ending Robot Client");
				disconnect();
				System.exit(0);
			}
		});
		buildGUI(ip);
		this.setVisible(true);
		btnConnect.addKeyListener(this);
		readSpaceNavigator();
	}

	private void testRun(int angle, int spinning) {
		for (int i=0;i<20;i++) {
			sendCommand(spaceNavigatorSource, 100, angle, spinning);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void readSpaceNavigator() {
		while (true) {
			System.out.println(looping);
			while (looping) {
				//testRun(0,90); // move north
				//testRun(180,-90); // move south
				double[] coordinates= sn.getData();
				int leftButton=(int) coordinates[6];
				double x = coordinates[0]*maxValue;
				double y = coordinates[1]*maxValue;
				double spinning=coordinates[5]*maxValue*maxSpeed;
				double speed = Math.sqrt(x*x + y*y)*maxSpeed;
				double theta = Math.atan2(y, x); // direction
				double angle = Math.toDegrees(theta)+90;
				
				if (angle < 0.0d) {angle += 360.0;}
				if (speed>maxSpeed) {speed=maxSpeed;};
				if (speed==0 && spinning==0) {angle=0;}
				if (spinning>maxSpeed) {spinning=maxSpeed;};		  
				if (spinning<-maxSpeed) {spinning=-maxSpeed;};

				// toggle absolute/relative
				if (leftButton==1) {
					sendCommand(spaceNavigatorButton,0,0,0);
				}
				else {
					sendCommand(spaceNavigatorSource, (int) speed, (int) angle, (int) spinning);
				}
				//System.out.println("S: "+(int) speed+"  A: "+ (int) angle +"  Sp: "+ (int) spinning);
				
				try {
					Thread.sleep(sampleRate);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}


	private void sendCommand(int source, int angle, int r, int rotate){
		// Send coordinates to Server:
		messages.setText("status: SENDING command.");
		String message = source+"@@"+angle+"@@"+r+"@@"+rotate;
		try {
			outStream.writeUTF(message);
		} catch (IOException e) {
			messages.setText("status: ERROR Problems occurred sending data.");
		}
		messages.setText("status: Command SENT.");
	} 


	public static void main(String args[]) {
		String ip = "192.168.1.242";
		if(args.length > 0) ip = args[0]; 
		System.out.println("Starting R/C Client...");
		new RemoteCarClient("R/C Client", ip);
	}

	public void buildGUI(String ip) {
		Panel mainPanel = new Panel (new BorderLayout());
		ControlListener cl = new ControlListener();

		btnConnect = new Button("Connect");
		btnConnect.addActionListener(cl);

		txtIPAddress = new TextField(ip,16);

		messages = new TextArea("status: DISCONNECTED");
		messages.setEditable(false);

		Panel north = new Panel(new FlowLayout(FlowLayout.LEFT));
		north.add(btnConnect);
		north.add(txtIPAddress);

		Panel center = new Panel(new GridLayout(5,1));
		center.add(new Label("A-S-D to steer, W-X to move")); 

		Panel center4 = new Panel(new FlowLayout(FlowLayout.LEFT));
		center4.add(messages);

		center.add(center4);

		mainPanel.add(north, "North");
		mainPanel.add(center, "Center");
		this.add(mainPanel);
	}

	private void sendCommand(int command){
		// Send coordinates to Server:
		messages.setText("status: SENDING command.");
		try {
			outStream.writeInt(command);
		} catch(IOException io) {
			messages.setText("status: ERROR Problems occurred sending data.");
		}
		messages.setText("status: Command SENT.");
	} 

	/** A listener class for all the buttons of the GUI. */
	private class ControlListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			String command = e.getActionCommand();
			if (command.equals("Connect")) {
				try {
					socket = new Socket(txtIPAddress.getText(), PORT);
					outStream = new DataOutputStream(socket.getOutputStream());
					messages.setText("status: CONNECTED");
					btnConnect.setLabel("Disconnect");
					looping=true;
				} catch (Exception exc) {
					messages.setText("status: FAILURE Error establishing connection with server.");
					System.out.println("Error: " + exc);
				}
			}
			else if (command.equals("Disconnect")) {
				looping=false;
				disconnect();
			} 
		}
	}

	public void disconnect() {
		try {
			sendCommand(CLOSE,0,0,0);
			socket.close();
			btnConnect.setLabel("Connect");
			messages.setText("status: DISCONNECTED");
		} catch (Exception exc) {
			messages.setText("status: FAILURE Error closing connection with server.");
			System.out.println("Error: " + exc);
		}
	}

	public void keyPressed(KeyEvent e) {
		sendCommand(keyboard, e.getKeyCode(),0,0);
		System.out.println("Pressed " + e.getKeyCode());
	}

	public void keyReleased(KeyEvent e) {}

	public void keyTyped(KeyEvent arg0) {}
}