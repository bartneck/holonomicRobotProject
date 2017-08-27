import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
//import net.java.games.input.Version;


public class SpaceNavigator {
	private ControllerEnvironment controllerEnvironment;
	private Controller spaceNavigator;
	private Component[] components;
	private double[] sn={0,0,0,0,0,0,0};

	public SpaceNavigator() {
		controllerEnvironment = ControllerEnvironment.getDefaultEnvironment();
		Controller[] controllers = controllerEnvironment.getControllers();
		for(Controller controller: controllers){
			if ("SpaceNavigator".equalsIgnoreCase(controller.getName())){
				spaceNavigator = controller;
				System.out.println("USING Device ["+controller.getName()+"] of type ["+controller.getType().toString()+"]");
				components = spaceNavigator.getComponents();
			}
		}
	}

	public double[] getData() {
		// CHECK FOR INPUT
		if (spaceNavigator != null) {
			if (spaceNavigator.poll()) {
				for(Component component: components) {
					switch(component.getName()) {
					case "x":
						sn[0]=component.getPollData();
						break;
					case "y":
						sn[1]=component.getPollData();
						break;
					case "z":
						sn[2]=component.getPollData();
						break;
					case "rx":
						sn[3]=component.getPollData();
						break;
					case "ry":
						sn[4]=component.getPollData();
						break;
					case "rz":
						sn[5]=component.getPollData();
						break;
					case "0":
						sn[6]=component.getPollData();
						break;	
					}
				}
			}
		}
		return sn;
	}
	
}
