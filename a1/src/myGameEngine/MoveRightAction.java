package myGameEngine;

import a1.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;

public class MoveRightAction extends AbstractInputAction {

	private MyGame game;
	
	public MoveRightAction(MyGame g) {
		game = g;
	}
	
	public void performAction(float time, Event event) {
		if(game.getSprint())
			game.getEngine().getSceneManager().getSceneNode(game.getActiveNode().getName()).moveLeft(game.getSpeed() * 0.03f);
		else
			game.getEngine().getSceneManager().getSceneNode(game.getActiveNode().getName()).moveLeft(game.getSpeed());
	}
}
