package myGameEngine;

import ray.input.action.AbstractInputAction;
import ray.rml.Vector3f;
import a1.MyGame;
import net.java.games.input.Event;

public class RideDolphinAction extends AbstractInputAction {
	private MyGame game;
	private boolean onDolphin;
	
	public RideDolphinAction(MyGame g, boolean r) { 
		game = g;
		onDolphin = r;
	}
	
	public void performAction(float time, Event event) { 
		if(onDolphin) {
			game.getEngine().getSceneManager().getSceneNode("OnDolphinNode").detachAllChildren();
			game.getEngine().getSceneManager().getSceneNode("MainCameraNode").setLocalPosition(game.getEngine().getSceneManager().getSceneNode("myDolphinNode").getLocalPosition().add(Vector3f.createFrom(-0.3f, 0.2f, 0.0f)));
			game.setActiveNode(game.getEngine().getSceneManager().getSceneNode("MainCameraNode"));
			onDolphin = false;
		}
		else {
			game.getEngine().getSceneManager().getSceneNode("OnDolphinNode").attachChild(game.getEngine().getSceneManager().getSceneNode("MainCameraNode"));
			game.getEngine().getSceneManager().getSceneNode("MainCameraNode").setLocalPosition(0.0f, 0.0f, 0.0f);
			//game.getEngine().getSceneManager().getCamera("MainCamera").setMode('r');
			//System.out.println("Riding");
			game.setActiveNode(game.getEngine().getSceneManager().getSceneNode("myDolphinNode"));
			onDolphin = true;
		}
	}
}