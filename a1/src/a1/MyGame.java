package a1;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import myGameEngine.MoveBackwardAction;
import myGameEngine.MoveForwardAction;
import myGameEngine.MoveLeftAction;
import myGameEngine.MoveRightAction;
import myGameEngine.QuitGameAction;
import myGameEngine.RideDolphinAction;
import myGameEngine.RotateCameraDown;
import myGameEngine.RotateCameraLeft;
import myGameEngine.RotateCameraRight;
import myGameEngine.RotateCameraUp;
import ray.input.GenericInputManager;
import ray.input.InputManager;
import ray.input.action.Action;
import ray.rage.*;
import ray.rage.asset.texture.*;
import ray.rage.game.*;
import ray.rage.rendersystem.*;
import ray.rage.rendersystem.Renderable.*;
import ray.rage.scene.*;
import ray.rage.scene.Camera.Frustum.Projection;
import ray.rage.scene.controllers.*;
import ray.rage.util.BufferUtil;
import ray.rage.util.Configuration;
import ray.rml.*;
import ray.rage.rendersystem.gl4.GL4RenderSystem;
import ray.rage.rendersystem.states.TextureState;
import ray.rage.rendersystem.states.*;
import ray.rage.rendersystem.shader.*;

public class MyGame extends VariableFrameRateGame {

	// to minimize variable allocation in update()
	GL4RenderSystem rs;
	float elapsTime = 0.0f;
	String elapsTimeStr, counterStr, dispStr;
	int elapsTimeSec, counter = 0;
	
	//Entity dolphinE;
	private SceneNode activeNode;
	
	//***** Input Devices and Actions *****
	private InputManager im;
	private Action quitGameAction, moveForwardAction,
			moveBackwardAction, moveRightAction, 
			moveLeftAction, rotateCameraDown,
			rotateCameraUp, rotateCameraRight,
			rotateCameraLeft, rideDolphinAction;
	//***** End Input Devices and Actions *****
	
	private SceneNode onDolphinNode;
	private boolean onDolphin = false;

    public MyGame() {
        super();
		System.out.println("press T to render triangles");
		System.out.println("press L to render lines");
		System.out.println("press P to render points");
		System.out.println("press C to increment counter");
    }

    public static void main(String[] args) {
        Game game = new MyGame();
        try {
            game.startup();
            game.run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            game.shutdown();
            game.exit();
        }
    }
	
//******************************************************************************************************************
    
	@Override
	protected void setupWindow(RenderSystem rs, GraphicsEnvironment ge) {
		rs.createRenderWindow(new DisplayMode(1000, 700, 24, 60), false);
	}

//******************************************************************************************************************

    @Override
    protected void setupCameras(SceneManager sm, RenderWindow rw) {
        SceneNode rootNode = sm.getRootSceneNode();
        Camera camera = sm.createCamera("MainCamera", Projection.PERSPECTIVE);
        rw.getViewport(0).setCamera(camera);
		
		camera.setRt((Vector3f)Vector3f.createFrom(1.0f, 0.0f, 0.0f));
		camera.setUp((Vector3f)Vector3f.createFrom(0.0f, 1.0f, 0.0f));
		camera.setFd((Vector3f)Vector3f.createFrom(0.0f, 0.0f, -1.0f));
		
		camera.setPo((Vector3f)Vector3f.createFrom(0.0f, 0.0f, 0.0f));

		camera.setMode('r');
        SceneNode cameraNode = rootNode.createChildSceneNode(camera.getName() + "Node");
        cameraNode.attachObject(camera);
    }

//******************************************************************************************************************
   
    @Override
    protected void setupScene(Engine eng, SceneManager sm) throws IOException {
    	// Setup the input actions
    	setupInputs();
    	
        // Create Dolphin
        makeDolphin(eng, sm);
   
        makeCoin(eng, sm);
        
        activeNode = this.getEngine().getSceneManager().getSceneNode("MainCameraNode");
        
        // Create Pyramid
        ManualObject pyr = makePyramid(eng, sm);
        SceneNode pyrN = sm.getRootSceneNode().createChildSceneNode("PyrNode");
        pyrN.scale(0.75f, 0.75f, 0.75f);
        pyrN.moveForward(2.0f);
        pyrN.attachObject(pyr);
        
       RotationController rc = new RotationController(Vector3f.createUnitVectorY(), 0.02f);
       rc.addNode(pyrN);
       sm.addController(rc);

       sm.getAmbientLight().setIntensity(new Color(.1f, .1f, .1f));
		
       //***** Light Node *****
       Light plight = sm.createLight("testLamp1", Light.Type.POINT);
       plight.setAmbient(new Color(.3f, .3f, .3f));
       plight.setDiffuse(new Color(.7f, .7f, .7f));
       plight.setSpecular(new Color(1.0f, 1.0f, 1.0f));
       plight.setRange(5f);
		
       SceneNode plightNode = sm.getRootSceneNode().createChildSceneNode("plightNode");
       plightNode.attachObject(plight);

    }

//******************************************************************************************************************
    
    @Override
    protected void update(Engine engine) {
		// build and set HUD
		rs = (GL4RenderSystem) engine.getRenderSystem();
		elapsTime += engine.getElapsedTimeMillis();
		elapsTimeSec = Math.round(elapsTime/1000.0f);
		elapsTimeStr = Integer.toString(elapsTimeSec);
		counterStr = Integer.toString(counter);
		dispStr = "Time = " + elapsTimeStr + "   Keyboard hits = " + counterStr;
		rs.setHUD(dispStr, 15, 15);
		
		// Tell the input manager to process the inputs
		im.update(elapsTime);
	}

//******************************************************************************************************************
   
    protected void setupInputs() {
    	im = new GenericInputManager();
    	String kbName = im.getKeyboardName();
    	//String gpName = im.getFirstGamepadName();
    	
    	// Build some action objects for doing things in response to user input
    	quitGameAction = new QuitGameAction(this);
    	rideDolphinAction = new RideDolphinAction(this, onDolphin);
    	moveForwardAction = new MoveForwardAction(this);
    	moveBackwardAction = new MoveBackwardAction(this);
    	moveLeftAction = new MoveLeftAction(this);
    	moveRightAction = new MoveRightAction(this);
    	rotateCameraUp = new RotateCameraUp(this);
    	rotateCameraDown = new RotateCameraDown(this);
    	rotateCameraRight = new RotateCameraRight(this);
    	rotateCameraLeft = new RotateCameraLeft(this);
    	
    	// Attach the action objects to keyboard and gamepad components
    	// Keyboard Action
    	im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.ESCAPE, 
    			quitGameAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    	// Gamepad Action
    	//im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._9, 
    	//		quitGameAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    	
    	im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.W, 
			    moveForwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    	
    	im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.A, 
    			moveLeftAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    	
    	im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.S, 
    			moveBackwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    	
    	im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.D, 
    			moveRightAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    	
    	im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.UP, 
    			rotateCameraUp, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    	
    	im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.DOWN, 
    			rotateCameraDown, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    	
    	im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.RIGHT, 
    			rotateCameraRight, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    	
    	im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.LEFT, 
    			rotateCameraLeft, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
	
    	im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.SPACE, 
    			rideDolphinAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    	
    }
    
   /* @Override
    public void keyPressed(KeyEvent e) {
    	//Entity dolphin = getEngine().getSceneManager().getEntity("myDolphin");
		SceneNode dolphinN = getEngine().getSceneManager().getSceneNode("myDolphinNode");
		SceneNode playerN = getEngine().getSceneManager().getSceneNode("myPlayerNode");
		Camera c = getEngine().getSceneManager().getCamera("MainCamera");
		
		switch (e.getKeyCode()) { 
		
			case KeyEvent.VK_W:
				if(onDolphin) {
					dolphinN.moveForward(0.1f);
				}
				else {
					c.setFd((Vector3f)Vector3f.createFrom(0.0f, 0.0f, 1.0f));
				}
				break;
				
			case KeyEvent.VK_S:
				if(onDolphin) {
					dolphinN.moveBackward(0.1f);
				}
				else {
					c.setFd((Vector3f)Vector3f.createFrom(0.0f, 0.0f, -1.0f));
				}
				break;
				
			case KeyEvent.VK_A:
				if(onDolphin) {
					dolphinN.moveLeft(0.1f);
				}
				else {
					c.setRt((Vector3f)Vector3f.createFrom(-1.0f, 0.0f, 0.0f));
				}
				break;
				
			case KeyEvent.VK_D:
				if(onDolphin) {
					dolphinN.moveRight(0.1f);
				}
				else {
					c.setRt((Vector3f)Vector3f.createFrom(1.0f, 0.0f, 0.0f));
				}
				break;
				
			case KeyEvent.VK_SPACE:	//on press space-bar camera will move onto/off dolphin
				if(onDolphin) {	//player is currently viewing camera from on top of dolphin
					playerN.attachObject(c);
					playerN.moveUp(0.5f);
					playerN.moveBackward(5.0f);
					c.setMode('r');
				}
				else {			//player is currently viewing from off of the dolphin	
					dolphinN.attachObject(c);
					dolphinN.moveBackward(3.0f);
					c.setMode('c');
				}
				onDolphin = !onDolphin;
				break;
        }
        super.keyPressed(e);
    }*/
    
    public SceneNode getActiveNode() {
    	return activeNode;
    }
    
    public void setActiveNode(SceneNode sn) {
    	activeNode = sn;
    }
    
//******************************************************************************************************************
//********************** Create Objects in Game Section ************************************************************
//******************************************************************************************************************
    
    //***** Make Dolphins *****
    private void makeDolphin(Engine eng, SceneManager sm) throws IOException {
    	Entity dolphinE = sm.createEntity("myDolphin", "dolphinHighPoly.obj");
    	dolphinE.setPrimitive(Primitive.TRIANGLES);

    	SceneNode dolphinN = sm.getRootSceneNode().createChildSceneNode(dolphinE.getName() + "Node");
    	//dolphinN.moveBackward(2.0f);
    	dolphinN.attachObject(dolphinE);
    	
    	onDolphinNode = sm.getSceneNode("myDolphinNode").createChildSceneNode("OnDolphinNode");
    	onDolphinNode.moveUp(0.3f);
    }
    
    private void makeCoin(Engine eng, SceneManager sm) throws IOException {
    	Entity coinE = sm.createEntity("coin",	"coin.obj");
    	coinE.setPrimitive(Primitive.TRIANGLES);
    	
    	
    	SceneNode coinN = sm.getRootSceneNode().createChildSceneNode(coinE.getName() + "Node");
    	coinN.moveForward(5.0f);
    	coinN.rotate(Degreef.createFrom(90f), Vector3f.createUnitVectorX());
    	coinN.rotate(Degreef.createFrom(180f), Vector3f.createUnitVectorZ());
    	coinN.attachObject(coinE);
    	coinN.scale(0.25f, 0.25f, 0.25f);
    	
    	Texture tex = eng.getTextureManager().getAssetByPath("coin-texture.jpg");
		TextureState texState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		texState.setTexture(tex);
		coinE.setRenderState(texState);
    }
    
    //***** Make Pyramids *****
    protected ManualObject makePyramid(Engine eng, SceneManager sm)	throws IOException { 
		ManualObject pyr = sm.createManualObject("Pyramid");
		ManualObjectSection pyrSec = pyr.createManualSection("PyramidSection");
		pyr.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));
		float[] vertices = new float[] { 
			-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f, //front
			1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f, //right
			1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f, //back
			-1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f, //left
			-1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, //LF
			1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f //RR
		};
		
		float[] texcoords = new float[] { 
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
		};
		
		float[] normals = new float[] { 
			0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
			0.0f, 1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, -1.0f,
			-1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f,
			0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
			0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f
		};
		
		int[] indices = new int[] { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17 };
		
		FloatBuffer vertBuf = BufferUtil.directFloatBuffer(vertices);
		FloatBuffer texBuf = BufferUtil.directFloatBuffer(texcoords);
		FloatBuffer normBuf = BufferUtil.directFloatBuffer(normals);
		IntBuffer indexBuf = BufferUtil.directIntBuffer(indices);
		pyrSec.setVertexBuffer(vertBuf);
		pyrSec.setTextureCoordsBuffer(texBuf);
		pyrSec.setNormalsBuffer(normBuf);
		pyrSec.setIndexBuffer(indexBuf);
		Texture tex = eng.getTextureManager().getAssetByPath("chain-fence.jpeg");
		TextureState texState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		texState.setTexture(tex);
		FrontFaceState faceState = (FrontFaceState) sm.getRenderSystem().createRenderState(RenderState.Type.FRONT_FACE);
		pyr.setDataSource(DataSource.INDEX_BUFFER);
		pyr.setRenderState(texState);
		pyr.setRenderState(faceState);
		return pyr;
}
}
