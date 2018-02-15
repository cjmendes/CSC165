package myGameEngine;

import a1.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rml.Degreef;

public class RotateCameraLeft extends AbstractInputAction {

	private MyGame game;
	
	public RotateCameraLeft(MyGame g) {
		game = g;
	}
	
	public void performAction(float time, Event event) {
		game.getEngine().getSceneManager().getSceneNode(game.getActiveNode().getName()).yaw(Degreef.createFrom(3f));
	}
}
