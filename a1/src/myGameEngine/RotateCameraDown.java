package myGameEngine;

import a1.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rml.Degreef;

public class RotateCameraDown extends AbstractInputAction {

	private MyGame game;
	
	public RotateCameraDown(MyGame g) {
		game = g;
	}
	
	public void performAction(float time, Event event) {
		game.getEngine().getSceneManager().getSceneNode(game.getActiveNode().getName()).pitch(Degreef.createFrom(3f));
	}
}