package myGameEngine;

import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Camera;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class MoveRightAction extends AbstractInputAction {
	private Camera camera;
	
	public MoveRightAction(Camera c) {  
		camera = c;
	}
	
	public void performAction(float time, Event e) { 
		Vector3f v = camera.getRt();
		Vector3f p = camera.getPo();
		Vector3f p1 = (Vector3f) Vector3f.createFrom(0.01f*v.x(), 0.01f*v.y(), 0.01f*v.z());
		Vector3f p2 = (Vector3f) p.add((Vector3)p1);
		camera.setPo((Vector3f)Vector3f.createFrom(p2.x(),p2.y(),p2.z()));
	}
}