package myGameEngine;

import a1.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;

public class MoveForwardAction extends AbstractInputAction {

	private MyGame game;
	
	public MoveForwardAction(MyGame g) {
		game = g;
	}
	
	public void performAction(float time, Event event) {
		if(game.getSprint())
			game.getEngine().getSceneManager().getSceneNode(game.getActiveNode().getName()).moveForward(0.25f);
		else
			game.getEngine().getSceneManager().getSceneNode(game.getActiveNode().getName()).moveForward(0.05f);
	}
}
