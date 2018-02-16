package myGameEngine;

import a1.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;

public class SprintAction extends AbstractInputAction {

	private MyGame game;
	
	public SprintAction(MyGame g) {
		game = g;
		
	}
	
	public void performAction(float time, Event event) {
		game.changeSprint();
	}
}