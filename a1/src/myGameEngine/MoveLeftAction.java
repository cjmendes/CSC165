package myGameEngine;

import a1.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;

public class MoveLeftAction extends AbstractInputAction {

	private MyGame game;
	
	public MoveLeftAction(MyGame g) {
		game = g;
	}
	
	public void performAction(float time, Event event) {
		if(game.getSprint())
			game.getEngine().getSceneManager().getSceneNode(game.getActiveNode().getName()).moveRight(game.getSpeed() * 0.05f);
		else
			game.getEngine().getSceneManager().getSceneNode(game.getActiveNode().getName()).moveRight(game.getSpeed());
	}
}
