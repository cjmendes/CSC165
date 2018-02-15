package myGameEngine;

import ray.input.action.AbstractInputAction;
import a1.MyGame;
import net.java.games.input.Event;

public class RideDolphinAction extends AbstractInputAction {
	private MyGame game;
	private boolean isRiding;
	
	public RideDolphinAction(MyGame g, boolean r) { 
		game = g;
		isRiding = r;
	}
	
	public void performAction(float time, Event event) { 
		if(isRiding) {
			game.getEngine().getSceneManager().getSceneNode("myDolphinNode").detachAllChildren();
			game.getEngine().getSceneManager().getCamera("MainCamera").setMode('r');
			isRiding = false;
		}
		else {
			game.getEngine().getSceneManager().getSceneNode("myDolphinNode").attachChild(game.getEngine().getSceneManager().getSceneNode("MainCameraNode"));
			game.getEngine().getSceneManager().getSceneNode("MainCameraNode").setLocalPosition(0.0f, 0.4f, -0.2f);
			game.getEngine().getSceneManager().getCamera("MainCamera").setMode('r');
			System.out.println("Riding");
			isRiding = true;
		}
	}
}