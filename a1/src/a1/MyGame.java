package a1;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.stream.IntStream;

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
import myGameEngine.SprintAction;
import ray.input.GenericInputManager;
import ray.input.InputManager;
import ray.input.action.Action;
import ray.rage.*;
import ray.rage.asset.material.Material;
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
	
	private int NUM_OF_COINS = 30;
	private int NUM_OF_EXTRA_OBJECTS = 10;
	private int SIZE_OF_SPACE = 20;
	private float MAX_SPEED = 0.3f;
	
	//Entity dolphinE;
	private SceneNode activeNode;
	
	//***** Input Devices and Actions *****
	private InputManager im;
	private Action quitGameAction, moveForwardAction,
			moveBackwardAction, moveRightAction, 
			moveLeftAction, rotateCameraDown,
			rotateCameraUp, rotateCameraRight,
			rotateCameraLeft, rideDolphinAction,
			sprintAction;
	//***** End Input Devices and Actions *****
	
	private SceneNode onDolphinNode;
	private boolean onDolphin = false;
	private boolean sprint = true;
	private boolean tooFar = false;
	
	private int[] coinList = new int[NUM_OF_COINS];
	private int position = 0;
	
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
   
        //makeSkyBox(eng, sm);
        setupAxis(eng, sm);
        
        activeNode = this.getEngine().getSceneManager().getSceneNode("MainCameraNode");
        
       RotationController rc = new RotationController(Vector3f.createUnitVectorY(), 0.01f);
       
       for( int i = 0; i < NUM_OF_EXTRA_OBJECTS; i++)
    	   rc.addNode(createDiamond(eng, sm, i));
       
       sm.addController(rc);
       
       RotationController rcCoin = new RotationController(Vector3f.createUnitVectorZ(), 0.4f);
              
       for( int i = 0; i < NUM_OF_COINS; i++)
    	   rcCoin.addNode(makeCoin(eng, sm, i));
       
       sm.addController(rcCoin);

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
		dispStr = "Time = " + elapsTimeStr + "   Position = " + getPosition() + "   Coins picked up = " + counterStr;
		rs.setHUD(dispStr, 15, 15);
		
		//System.out.println();
		checkTooFar(engine.getSceneManager());
			
		// Tell the input manager to process the inputs
		im.update(elapsTime);
		
		// Check collision of camera and coins
		for(int i = 0; i < NUM_OF_COINS; i++) {
			if(checkCollision(engine.getSceneManager().getSceneNode("coin" + Integer.toString(i) + "Node"), engine.getSceneManager().getSceneNode("MainCameraNode")) < 0.1) {
				int temp = i;
				if(!IntStream.of(coinList).anyMatch(x -> x == temp)) {
					engine.getSceneManager().getSceneNode("coin" + Integer.toString(i) + "Node").detachAllObjects();
					coinList[position] = i;
					counter++;
					position++;
				}
			}
		}
		
	}

//******************************************************************************************************************
   
    protected void setupInputs() {
    	im = new GenericInputManager();
    	String kbName = im.getKeyboardName();
    	
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
    	sprintAction = new SprintAction(this); 
    	
    	// Attach the action objects to keyboard and gamepad components
    	// Keyboard Action
    	im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.ESCAPE, 
    			quitGameAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    	
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
    	
    	im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.LSHIFT, 
    			sprintAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    	

    	// Gamepad Action 
    	if(im.getFirstGamepadName() != null) {
	    	String gpName = im.getFirstGamepadName();
	    		
		    im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.Y, 
					moveForwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		    	
		    im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._1, 
		    		moveLeftAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		    	
		    im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._1, 
		    		moveBackwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		    	
		    im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._1, 
		    		moveRightAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		    	
		    im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._1, 
		    		rotateCameraUp, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		    	
		    im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._1, 
		    		rotateCameraDown, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		    	
		    im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._1, 
		    		rotateCameraRight, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		    	
		    im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.X, 
		    		rotateCameraLeft, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			
		    im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._0, 
		    		rideDolphinAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    	}

    }
 
//******************************************************************************************************************
    
    public SceneNode getActiveNode() {
    	return activeNode;
    }
    
    public void setActiveNode(SceneNode sn) {
    	activeNode = sn;
    }
    
    public String getPosition() {
    	
    	if(activeNode.getName().equals("MainCameraNode")) {
    		
    		return "OFF_DOLPHIN";
    	}
    	else
    		return "ON_DOLPHIN";
    }
    
    public static float randInRangeFloat(int min, int max) {
        return min + (float) (Math.random() * ((1 + max) - min));
    }
    
    public void changeSprint() {
    	if(sprint)
    		sprint = false;
    	else
    		sprint = true;
    }
    
    public boolean getSprint() {
    	return sprint;
    }
    
    public float getSpeed() {
    	return MAX_SPEED;
    }
    
    public boolean getTooFar() {
    	return tooFar;
    }
    
    public void checkTooFar(SceneManager sm) {
    	if( sm.getSceneNode("MainCameraNode").getLocalPosition().compareTo(
    			sm.getSceneNode("myDolphinNode").getLocalPosition().add(Vector3f.createFrom(0.0f, 10.0f, 0.0f))) == 1){
    		System.out.println("Too Far");
    	}
    }
    
    public float checkCollision(SceneNode a, SceneNode b) {
    	float ax = a.getLocalPosition().x();
    	float ay = a.getLocalPosition().y();
    	float az = a.getLocalPosition().z();
    	float bx = b.getLocalPosition().x();
    	float by = b.getLocalPosition().y();
    	float bz = b.getLocalPosition().z();
    	
    	return (float)Math.sqrt((double)(ax - bx) * (ax - bx) + (ay-by) * (ay - by) + (az - bz) * (az - bz));
    }
    
//******************************************************************************************************************
//********************** Create Objects in Game Section ************************************************************
//******************************************************************************************************************
    
    //***** Make Dolphins *****
    private void makeDolphin(Engine eng, SceneManager sm) throws IOException {
    	Entity dolphinE = sm.createEntity("myDolphin", "dolphinHighPoly.obj");
    	dolphinE.setPrimitive(Primitive.TRIANGLES);

    	SceneNode dolphinN = sm.getRootSceneNode().createChildSceneNode(dolphinE.getName() + "Node");
    	dolphinN.moveDown(0.4f);
    	dolphinN.moveLeft(0.4f);
    	dolphinN.attachObject(dolphinE);
    	
    	onDolphinNode = sm.getSceneNode("myDolphinNode").createChildSceneNode("OnDolphinNode");
    	onDolphinNode.moveUp(0.3f);
    }
    
    //***** Make Coins *****
    private SceneNode makeCoin(Engine eng, SceneManager sm, int num) throws IOException {
    	Entity coinE = sm.createEntity("coin" + Integer.toString(num),	"coin.obj");
    	coinE.setPrimitive(Primitive.TRIANGLES);
    	
    	SceneNode coinN = sm.getRootSceneNode().createChildSceneNode(coinE.getName() + "Node");
    	coinN.moveForward(randInRangeFloat(-SIZE_OF_SPACE, SIZE_OF_SPACE));
    	coinN.moveUp(randInRangeFloat(-SIZE_OF_SPACE, SIZE_OF_SPACE));
    	coinN.moveRight(randInRangeFloat(-SIZE_OF_SPACE, SIZE_OF_SPACE));
    	coinN.rotate(Degreef.createFrom(90f), Vector3f.createUnitVectorX());
    	coinN.rotate(Degreef.createFrom(180f), Vector3f.createUnitVectorZ());
    	coinN.attachObject(coinE);
    	coinN.scale(0.25f, 0.25f, 0.25f);

    	return coinN;
    }
   
    
    //***** Make Pyramids *****
    protected ManualObject makeDiamond(Engine eng, SceneManager sm, int num)	throws IOException { 
		ManualObject pyr = sm.createManualObject("Diamond" + Integer.toString(num));
		ManualObjectSection pyrSec = pyr.createManualSection("DiamondSection");
		pyr.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));
		float[] vertices = new float[] {
				-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.0f, 0.5f, 0.0f, //front
				0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.0f, 0.5f, 0.0f, //right
				0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.0f, 0.5f, 0.0f, //back
				-0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, 0.0f, 0.5f, 0.0f, //left
				0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f,0.0f, -1.5f, 0.0f, //front t2
				0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.0f, -1.5f, 0.0f, //right t2
				-0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.0f, -1.5f, 0.0f, //back t2
				-0.5f, -0.5f, 0.5f, -0.5f, -0.5f, -0.5f, 0.0f, -1.5f, 0.0f //left t2
		};
		
		float[] texcoords = new float[] { 
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f
		};
		
		float[] normals = new float[] { 
			0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
			0.0f, 1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, -1.0f,
			-1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f,
			0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
			0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f
		};
		
		int[] indices = new int[] { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24 };
		
		FloatBuffer vertBuf = BufferUtil.directFloatBuffer(vertices);
		FloatBuffer texBuf = BufferUtil.directFloatBuffer(texcoords);
		FloatBuffer normBuf = BufferUtil.directFloatBuffer(normals);
		IntBuffer indexBuf = BufferUtil.directIntBuffer(indices);
		pyrSec.setVertexBuffer(vertBuf);
		pyrSec.setTextureCoordsBuffer(texBuf);
		pyrSec.setNormalsBuffer(normBuf);
		pyrSec.setIndexBuffer(indexBuf);
		Texture tex = eng.getTextureManager().getAssetByPath("silver.jpg");
		TextureState texState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		texState.setTexture(tex);
		FrontFaceState faceState = (FrontFaceState) sm.getRenderSystem().createRenderState(RenderState.Type.FRONT_FACE);
		pyr.setDataSource(DataSource.INDEX_BUFFER);
		pyr.setRenderState(texState);
		pyr.setRenderState(faceState);
		
		return pyr;
    }
    
    private SceneNode createDiamond(Engine eng, SceneManager sm, int num) throws IOException {
    	ManualObject dia = makeDiamond(eng, sm, num);
        SceneNode diaN = sm.getRootSceneNode().createChildSceneNode("Diamond" + Integer.toString(num) + "Node");
        diaN.scale(0.75f, 0.75f, 0.75f);
        diaN.moveForward(randInRangeFloat(-SIZE_OF_SPACE, SIZE_OF_SPACE));
        diaN.moveUp(randInRangeFloat(-SIZE_OF_SPACE, SIZE_OF_SPACE));
        diaN.moveRight(randInRangeFloat(-SIZE_OF_SPACE, SIZE_OF_SPACE));
        diaN.attachObject(dia);
        
        return diaN;
    }
    
    private void setupAxis(Engine eng, SceneManager sm) throws IOException{
    	ManualObject vertX = sm.createManualObject("VertX");
    	ManualObjectSection vertXSec = vertX.createManualSection("VertXSection");
    	vertX.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));
    	
    	float[] verticesX = new float[] {
    			-1000.0f, 0.0f, 0.0f,
    			1000.0f, 0.0f, 0.0f
    	};
    	
    	int[] indicesX = new int[] {0, 1};
    	
    	vertXSec.setPrimitive(Primitive.LINES);
    	
    	FloatBuffer vertXBuf = BufferUtil.directFloatBuffer(verticesX);
    	IntBuffer indexXBuf = BufferUtil.directIntBuffer(indicesX);
    	
    	vertXSec.setVertexBuffer(vertXBuf);
    	vertXSec.setIndexBuffer(indexXBuf);
    	
    	Material matX = sm.getMaterialManager().getAssetByPath("default.mtl");
    	matX.setEmissive(Color.WHITE);
    	Texture texX = sm.getTextureManager().getAssetByPath(matX.getTextureFilename());
    	TextureState tstateX = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
    	tstateX.setTexture(texX);
    	vertXSec.setRenderState(tstateX);
    	vertXSec.setMaterial(matX);
    	
    	SceneNode vertXN = sm.getRootSceneNode().createChildSceneNode("VertXNode");
    	vertXN.attachObject(vertX);
    }
    
    /*private void makeSkyBox(Engine engine, SceneManager sm) throws IOException {
        Configuration conf = engine.getConfiguration();
        TextureManager textureMgr = engine.getTextureManager();

        textureMgr.setBaseDirectoryPath(conf.valueOf("assets.skyboxes.path")); 
        Texture front = textureMgr.getAssetByPath("cyclone-island_ft.png");
        Texture back = textureMgr.getAssetByPath("cyclone-island_bk.png");
        Texture left = textureMgr.getAssetByPath("cyclone-island_rt.png");
        Texture right = textureMgr.getAssetByPath("cyclone-island_lf.png");
        Texture top = textureMgr.getAssetByPath("cyclone-island_up.png");
        Texture bottom = textureMgr.getAssetByPath("cyclone-island_dn.png");
        textureMgr.setBaseDirectoryPath(conf.valueOf("assets.textures.path"));

        // cubemap textures must be flipped up-side-down to face inward; all textures must have the same dimensions, so any image height will do
        AffineTransform xform = new AffineTransform();
        xform.translate(0, front.getImage().getHeight());
        xform.scale(1d, -1d);

        front.transform(xform);
        back.transform(xform);
        left.transform(xform);
        right.transform(xform);
        top.transform(xform);
        bottom.transform(xform);

        SkyBox sb = sm.createSkyBox("SkyBox");
        sb.setTexture(front, SkyBox.Face.FRONT);
        sb.setTexture(back, SkyBox.Face.BACK);
        sb.setTexture(left, SkyBox.Face.LEFT);
        sb.setTexture(right, SkyBox.Face.RIGHT);
        sb.setTexture(top, SkyBox.Face.TOP);
        sb.setTexture(bottom, SkyBox.Face.BOTTOM);
        sm.setActiveSkyBox(sb);
    }*/
}
