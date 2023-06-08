package game;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.bounding.BoundingSphere;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.CameraInput;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * This is the Main Class of your Game. It should boot up your game and do initial initialisation
 * Move your Logic into AppStates or Controls or other java classes
 */
public class Game extends SimpleApplication implements AnalogListener, ActionListener {

    static Random rand = new Random();
    Geometry mapGeometry;
    Geometry playerGeometry;
    float nextBlockTime = 30;
    ArrayList<Geometry> blocks = new ArrayList<>();
    float time = 0;
    boolean inBlock = false;
    int lostCont = 0;
    Material blockMat;
    float score = 0;
    float colorTimer = 0;
    int colorInt = 0;
    ChaseCamera camera;
    private float gameSpeed = FastMath.ZERO_TOLERANCE;
    private float jump = 0;
    private float canCreateCube = -30; // false
    private BitmapText scoreText;
    private boolean gamePaused = false;

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Ball game");
        settings.setGammaCorrection(true);
        settings.setFullscreen(false);
        settings.setResizable(true);

        BufferedImage image;
        try {
            image = ImageIO.read(new File("src/main/resources/Textures/caustics.jpg"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        settings.setIcons(new BufferedImage[]{image});

        Game app = new Game();
        app.setSettings(settings);
        app.setShowSettings(false); //Settings dialog not supported on mac
        app.start();
    }

    @Override
    public void simpleInitApp() {
        ScreenshotAppState screenshotAppState = new ScreenshotAppState("src/main/resources/Textures/Screenshot");
        stateManager.attach(screenshotAppState);

        /*VideoRecorderAppState videoRecorderAppState = new VideoRecorderAppState(new File("src/main/resources/Textures/ScreenRecording.avi"));
        stateManager.attach(videoRecorderAppState);*/


        //flyCam.setMoveSpeed(20);
        flyCam.setEnabled(false);
        setDisplayStatView(false);
        //setDisplayFps(false);


        BitmapFont defaultFont = assetManager.loadFont("Interface/Fonts/Default.fnt");



        blockMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        blockMat.setColor("Color", ColorRGBA.Red.mult(5));
        //blockMat.setColor("GlowColor", ColorRGBA.Cyan);
        Texture texture = assetManager.loadTexture("Common/MatDefs/Water/Textures/caustics.jpg");
        texture.setWrap(Texture.WrapMode.MirroredRepeat);
        blockMat.setTexture("ColorMap", texture);


        LightsAndSky();
        Keys();

        inputManager.setCursorVisible(false);
        startGame();
        inputManager.setCursorVisible(false);



        pausedText = new BitmapText(defaultFont);
        pausedText.setText("paused");
        pausedText.setSize(defaultFont.getCharSet().getRenderedSize() * 4f);
        pausedText.setLocalTranslation(pausedText.getLineWidth() * 1, pausedText.getLineHeight() * 3, 0);
        guiNode.attachChild(pausedText);
        pausedText.setAlpha(0);


        scoreText = new BitmapText(defaultFont);
        scoreText.setText("");
        scoreText.setSize(defaultFont.getCharSet().getRenderedSize());
        scoreText.setLocalTranslation(scoreText.getLineWidth() * 1, scoreText.getLineHeight() * 3, 0);
        guiNode.attachChild(scoreText);

        retryText = new BitmapText(defaultFont);
        retryText.setText("----Press Enter To Retry----");
        retryText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.5f);
        retryText.setLocalTranslation(retryText.getLineWidth() * 0.7f, retryText.getLineHeight() * 5, 0);
        guiNode.attachChild(retryText);
        retryText.setAlpha(1);


    }

    BitmapText retryText;

    BitmapText pausedText;

    private void startGame() {
        inputManager.setCursorVisible(false);
        nextBlockTime = 30;
        blocks = new ArrayList<>();
        time = 0;
        inBlock = false;
        lostCont = 0;
        score = 0;
        colorTimer = 0;
        colorInt = 0;
        gameSpeed = FastMath.ZERO_TOLERANCE;
        jump = 0;
        canCreateCube = -30; // false


        Box map = new Box(40, 2, 400);
        map.scaleTextureCoordinates(new Vector2f(20, 2));
        mapGeometry = new Geometry("Map", map);

        Texture mapTexture = assetManager.loadTexture("Common/MatDefs/Water/Textures/caustics.jpg");
        mapTexture.setWrap(Texture.WrapMode.Repeat);

        Texture mapGlowTexture = assetManager.loadTexture("Common/MatDefs/Water/Textures/caustics.jpg");
        mapGlowTexture.setWrap(Texture.WrapMode.Repeat);

        Material mapMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mapMaterial.setColor("Color", ColorRGBA.Blue.mult(3.14f));
        mapMaterial.setTexture("ColorMap", mapTexture);
        mapMaterial.setColor("GlowColor", ColorRGBA.Cyan.mult(1.3f));
        mapMaterial.setTexture("GlowMap", mapGlowTexture);
        mapGeometry.setMaterial(mapMaterial);

        rootNode.attachChild(mapGeometry);


        Sphere player = new Sphere(32, 32, 2);
        player.scaleTextureCoordinates(new Vector2f(3f, 1f));
        playerGeometry = new Geometry("Player", player);
        playerGeometry.setMaterial(mapMaterial);
        rootNode.attachChild(playerGeometry);
        playerGeometry.rotate(0, FastMath.HALF_PI, 0);
        playerGeometry.setLocalTranslation(0, 4, 0);

        camera = new ChaseCamera(cam, playerGeometry, inputManager);
        camera.setDragToRotate(false);
        camera.setInvertVerticalAxis(true);
        camera.setMaxDistance(1000);

        camera.setDefaultHorizontalRotation(FastMath.PI/2);

        //blocks.add(new Geometry("block", new Box(1, 1, 1)));
        inputManager.setCursorVisible(false);
    }

    private void Keys() {
        for (String s : new String[]{
                CameraInput.CHASECAM_ZOOMIN,
                CameraInput.CHASECAM_ZOOMOUT,
                INPUT_MAPPING_EXIT,
                INPUT_MAPPING_HIDE_STATS
        }) {
            inputManager.deleteMapping(s);
        }

        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("jump", new KeyTrigger(KeyInput.KEY_SPACE), new KeyTrigger(KeyInput.KEY_UP));

        /*{ Just for debuging
            inputManager.addMapping("speed", new KeyTrigger(KeyInput.KEY_1));
            inputManager.addMapping("speed-down", new KeyTrigger(KeyInput.KEY_2));
        }*/

        inputManager.addMapping("F11", new KeyTrigger(KeyInput.KEY_F11));
        inputManager.addMapping("pause", new KeyTrigger(KeyInput.KEY_P), new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping("retry", new KeyTrigger(KeyInput.KEY_RETURN));

        inputManager.addListener(this, "left", "right", "speed", "speed-down");
        inputManager.addListener(this, "jump", "F11", "pause", "retry");
    }

    private void LightsAndSky() {
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(Vector3f.UNIT_XYZ.negate());
        rootNode.addLight(dl);

        FilterPostProcessor postProcessor = new FilterPostProcessor(assetManager);

        BloomFilter bloomFilter = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloomFilter.setBlurScale(1.75f);
        postProcessor.addFilter(bloomFilter);

        viewPort.addProcessor(postProcessor);

        Texture texture = assetManager.loadTexture("Common/MatDefs/Water/Textures/caustics.jpg");
        texture.setWrap(Texture.WrapMode.MirroredRepeat);
        Spatial sky = SkyFactory.createSky(assetManager,
                texture,
                texture,
                texture,
                texture,
                texture,
                texture,
                Vector3f.UNIT_XYZ,
                2
        );
        rootNode.attachChild(sky);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (gamePaused || !startGame) {
            return;
        }
        scoreText.setText(
                "Score:  " + String.format("00000000 00000000 00000000 %07d0 00000000\n", (int) score) +
                "Speed: " + String.format("00000000 %08d \n", (int) gameSpeed));


        inputManager.setCursorVisible(false);
        score += tpf * (colorInt + 1) * gameSpeed / 3;

        gameSpeed += 2.5f * tpf;

        if (gameSpeed < 50) {
            gameSpeed += gameSpeed * tpf;
        } else if (gameSpeed < 100) {
            gameSpeed += tpf * gameSpeed / 10;
        } else if (gameSpeed < 120) {
            gameSpeed += tpf;
        }

        tpf *= gameSpeed;
        time += tpf;

        mapGeometry.setLocalTranslation(0, 0, time % 40);
        playerGeometry.rotate(0, 0, -tpf / FastMath.TWO_PI);

        //cam.setLocation(playerGeometry.getLocalTranslation().add(0, 8, 16));

        if (playerGeometry.getLocalTranslation().y < 4) {
            float x = playerGeometry.getLocalTranslation().x, z = playerGeometry.getLocalTranslation().z;
            playerGeometry.setLocalTranslation(x, 4, z);
            jump = 0;
        }

        if (jump > 0) {
            jump -= tpf / 16;
            playerGeometry.move(0, (jump - 2) / 2 * tpf, 0);
        } else if (playerGeometry.getLocalTranslation().y > 4) {
            playerGeometry.move(0, -tpf * 4, 0);
        }


        for (Geometry i : blocks) {
            i.move(0, 0, tpf);
        }

        createCube(tpf);

        try {
            if (blocks.get(0).getLocalTranslation().z > 20) {
                Geometry last = blocks.get(0);
                last.removeFromParent();
                blocks.remove(0);
            }
        } catch (Exception e) {
            //System.err.println("error in blocks");
        }

        CollisionResults results = new CollisionResults();
        BoundingSphere boundingSphere = new BoundingSphere(2, playerGeometry.getLocalTranslation());
        rootNode.collideWith(boundingSphere, results);

        boolean findBlock = false;

        for (int i = 0; i < results.size(); i++) {
            CollisionResult result = results.getCollision(i);
            String name = result.getGeometry().getName();

            if (name.equals("block")) {
                findBlock = true;

                if (inBlock) {
                    break;
                }

                inBlock = true;

                System.err.println("Colloid with block" + rand.nextInt());
                lostCont += 1;
                startGame = false;
                retryText.setAlpha(1);
                //restartGame();

                break;
            }

        }

        inBlock = findBlock;




        colorLogic(time);
        inputManager.setCursorVisible(false);
    }

    boolean startGame = true;
    private void restartGame() {
        rootNode.detachAllChildren();
        camera.setSpatial(null);
        camera.setEnabled(false);
        playerGeometry = null;
        mapGeometry = null;

        cam.setFrustumPerspective(45f, (float) cam.getWidth() / cam.getHeight(), 1f, 1000f);
        cam.setLocation(new Vector3f(0f, 0f, 10f));
        cam.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);

        startGame();
    }

    private void colorLogic(float time) {
        if (time >= colorTimer) {
            colorTimer += 700 + colorInt * 150;
            gameSpeed -= 24f / (colorInt + 1f);

            switch (colorInt % 13) {
                case 0 -> {
                    playerGeometry.getMaterial().setColor("Color", ColorRGBA.Blue.mult(3.14f));
                    playerGeometry.getMaterial().setColor("GlowColor", ColorRGBA.Cyan.mult(1.3f));

                    blockMat.setColor("Color", ColorRGBA.Red.mult(5));
                    blockMat.setColor("GlowColor", ColorRGBA.BlackNoAlpha);
                }
                case 1 -> {
                    playerGeometry.getMaterial().setColor("Color", ColorRGBA.White);
                    playerGeometry.getMaterial().setColor("GlowColor", ColorRGBA.Green.mult(10));
                    blockMat.setColor("Color", ColorRGBA.Magenta);
                }
                case 2 -> {
                    playerGeometry.getMaterial().setColor("Color", ColorRGBA.Gray);
                    playerGeometry.getMaterial().setColor("GlowColor", ColorRGBA.Green.mult(2));
                    blockMat.setColor("Color", ColorRGBA.White);
                }
                case 3 -> {
                    playerGeometry.getMaterial().setColor("Color", ColorRGBA.Magenta);
                    playerGeometry.getMaterial().setColor("GlowColor", ColorRGBA.Green);
                    blockMat.setColor("Color", ColorRGBA.White.setAlpha(0.15f));
                }
                case 4 -> {
                    playerGeometry.getMaterial().setColor("Color", ColorRGBA.Green);
                    playerGeometry.getMaterial().setColor("GlowColor", ColorRGBA.Magenta.mult(3));
                    blockMat.setColor("Color", ColorRGBA.White.mult(5.15f));

                    camera.setMaxDistance(0);
                }
                case 5 -> {
                    playerGeometry.getMaterial().setColor("Color", ColorRGBA.Magenta.mult(3));
                    playerGeometry.getMaterial().setColor("GlowColor", ColorRGBA.Green.mult(0.1f));
                    blockMat.setColor("Color", ColorRGBA.Cyan);
                }
                case 6 -> {
                    playerGeometry.getMaterial().setColor("Color", ColorRGBA.Cyan);
                    playerGeometry.getMaterial().setColor("GlowColor", ColorRGBA.Red.mult(10));
                    blockMat.setColor("Color", ColorRGBA.Red);
                    blockMat.setColor("GlowColor", ColorRGBA.Cyan.mult(5));
                }
                case 7 -> {
                    playerGeometry.getMaterial().setColor("Color", ColorRGBA.Green.mult(0.1f));
                    playerGeometry.getMaterial().setColor("GlowColor", ColorRGBA.Green.mult(10));
                    blockMat.setColor("Color", ColorRGBA.Red.mult(0.1f));
                    blockMat.setColor("GlowColor", ColorRGBA.Cyan.mult(5));

                    camera.setMaxDistance(40);
                    camera.setDefaultDistance(20);
                }
                case 8 -> {
                    playerGeometry.getMaterial().setColor("Color", ColorRGBA.Black);
                    playerGeometry.getMaterial().setColor("GlowColor", ColorRGBA.Cyan.mult(10));
                    blockMat.setColor("Color", ColorRGBA.Red.mult(10));
                    blockMat.setColor("GlowColor", ColorRGBA.Magenta);

                    Geometry border = new Geometry("border", new Box(FastMath.ZERO_TOLERANCE, 50, 400));
                    Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                    material.setColor("Color", ColorRGBA.Blue);
                    border.setMaterial(material);
                    rootNode.attachChild(border);
                }
                case 9 -> {
                    playerGeometry.getMaterial().setColor("Color", ColorRGBA.White);
                    playerGeometry.getMaterial().setColor("GlowColor", ColorRGBA.Blue.mult(10));
                    blockMat.setColor("Color", ColorRGBA.Black);
                    blockMat.setColor("GlowColor", ColorRGBA.White.mult(5));
                }
                case 10 -> {
                    playerGeometry.getMaterial().setColor("Color", ColorRGBA.White);
                    playerGeometry.getMaterial().setColor("GlowColor", ColorRGBA.Black.mult(10));
                    blockMat.setColor("Color", ColorRGBA.Black);
                    blockMat.setColor("GlowColor", ColorRGBA.White.mult(0.5f));
                }
                case 11 -> {
                    playerGeometry.getMaterial().setColor("Color", ColorRGBA.Black);
                    playerGeometry.getMaterial().setColor("GlowColor", ColorRGBA.White.mult(0.1f));
                    blockMat.setColor("Color", ColorRGBA.White);
                    blockMat.setColor("GlowColor", ColorRGBA.Black.mult(5));
                }
                case 12 -> {
                    score += 10000 * colorInt;
                    canCreateCube = -30;

                    Geometry border = new Geometry("border", new Box(40, 50, FastMath.ZERO_TOLERANCE));
                    Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                    material.setColor("Color", ColorRGBA.Black);
                    border.setMaterial(material);
                    border.move(0, 0, -100);
                    rootNode.attachChild(border);
                }
            }

            if (gameSpeed < FastMath.ZERO_TOLERANCE) {
                gameSpeed = FastMath.ZERO_TOLERANCE;
            }
            colorInt++;
        }

    }



    private void createCube(float tpf) {
        if (canCreateCube > nextBlockTime) {
            canCreateCube = 0;
        } else {
            canCreateCube += tpf;
            return;
        }


//        playerX+difficulty+30,playerX+difficulty+90


        int pick = rand.nextInt(100);
        BlockType type;

        if (pick < 20) {
            type = BlockType.multySideWay;
        } else if (pick < 30) { // 10%
            type = BlockType.any;
        } else if (pick < 80) { // 50%
            type = BlockType.sideWay;
        } else if (pick < 85) { // 5%
            type = BlockType.down;
        } else if (pick < 95) { // 10%
            type = BlockType.jumpOn;
        } else { // 5%
            type = BlockType.midWay;
        }


        switch (type) {
            case jumpOn -> {
                Box box = new Box(40, rand.nextFloat(2, 4), 1);
                box.scaleTextureCoordinates(new Vector2f(5, 1));
                Geometry block = new Geometry("block", box);
                block.getLocalTranslation().set(0, 6, -150);
                nextBlockTime = 30;

                block.setMaterial(blockMat);
                rootNode.attachChild(block);
                blocks.add(block);
            }
            case down -> {
                Box box = new Box(40, rand.nextFloat(2, 4), 1);
                box.scaleTextureCoordinates(new Vector2f(5, 1));
                Geometry block = new Geometry("block", box);
                block.getLocalTranslation().set(0, 16, -150);
                nextBlockTime = 30;

                block.setMaterial(blockMat);
                rootNode.attachChild(block);
                blocks.add(block);
            }
            case sideWay -> {
                Box box = new Box(1.5f, 10, 1.5f);
                box.scaleTextureCoordinates(new Vector2f(1, 10));
                Geometry block = new Geometry("block", box);
                block.getLocalTranslation().set(rand.nextFloat(-20, 20), 10, -150);
                nextBlockTime = 40;

                block.setMaterial(blockMat);
                rootNode.attachChild(block);
                blocks.add(block);
            }
            case midWay -> {
                float x = rand.nextFloat(-15, 15);

                Box box = new Box(15, 10, 5);
                box.scaleTextureCoordinates(new Vector2f(3, 1));
                Geometry block = new Geometry("block", box);
                block.getLocalTranslation().set(x - 19f, 10, -150);

                Geometry block2 = new Geometry("block", box);
                block2.getLocalTranslation().set(x + 19f, 10, -150);

                nextBlockTime = 200;

                block.setMaterial(blockMat);
                rootNode.attachChild(block);
                blocks.add(block);

                block2.setMaterial(blockMat);
                rootNode.attachChild(block2);
                blocks.add(block2);
            }
            case multySideWay -> {
                for (int i = 0; i < 15; i++) {
                    Box box = new Box(0.7f, 10, 0.7f);
                    box.scaleTextureCoordinates(new Vector2f(1, 10));
                    Geometry block = new Geometry("block", box);
                    block.getLocalTranslation().set(rand.nextFloat(-38, 38), 10, rand.nextFloat(-300, -150));

                    block.setMaterial(blockMat);
                    rootNode.attachChild(block);
                    blocks.add(block);
                }

                nextBlockTime = 200;
            }
            case any -> {
                Box box = new Box(2f, 2f, 2f);
                box.scaleTextureCoordinates(new Vector2f(4, 4));
                Geometry block = new Geometry("block", box);
                block.getLocalTranslation().set(rand.nextFloat(-20, 20), 10, -150);
                nextBlockTime = 10;

                block.setMaterial(blockMat);
                rootNode.attachChild(block);
                blocks.add(block);
            }


            //default ->  block = new Geometry("block", new Box(1, 1, 1));
        }


    }

    @Override
    public void simpleRender(RenderManager rm) {
        //add render code here (if any)
    }

    /**
     * Called to notify the implementation that an analog event has occurred.
     * <p>
     * The results of KeyTrigger and MouseButtonTrigger events will have tpf
     * == value.
     *
     * @param name  The name of the mapping that was invoked
     * @param value Value of the axis, from 0 to 1.
     * @param tpf   The time per frame value.
     */
    @Override
    public void onAnalog(String name, float value, float tpf) {
        tpf *= gameSpeed;
        switch (name) {
            case "left" -> {
                if (gamePaused || !startGame) return;
                if (playerGeometry.getLocalTranslation().x <= -38) {
                    playerGeometry.setLocalTranslation(-38, playerGeometry.getLocalTranslation().y, 0);
                } else {
                    playerGeometry.move(-tpf / 2, 0, 0);
                }
            }
            case "right" -> {
                if (gamePaused || !startGame) return;
                if (playerGeometry.getLocalTranslation().x >= 38) {
                    playerGeometry.setLocalTranslation(38, playerGeometry.getLocalTranslation().y, 0);
                } else {
                    playerGeometry.move(tpf / 2, 0, 0);
                }
            }
            case "speed" -> gameSpeed += tpf;
            case "speed-down" -> gameSpeed -= tpf;
        }
    }

    /**
     * Called when an input to which this listener is registered to is invoked.
     *
     * @param name      The name of the mapping that was invoked
     * @param isPressed True if the action is "pressed", false otherwise
     * @param tpf       The time per frame value.
     */
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch (name) {
            case "jump" -> {
                if (isPressed) {
                    if (jump > 0) {
                        jump = 0;
                    } else if (playerGeometry.getLocalTranslation().y <= 4) {
                        jump = 4;
                    }
                }
            }
            case "F11" -> {
                // TODO: 6/6/2023 Fullscreen
            }
            case "pause" -> {
                if (isPressed) {
                    gamePaused = !gamePaused;
                    camera.setEnabled(!camera.isEnabled());
                    inputManager.setCursorVisible(!inputManager.isCursorVisible());
                    pausedText.setAlpha(gamePaused ? 1:0);
                }
            }
            case "retry" -> {
                startGame = true;
                retryText.setAlpha(0);
                restartGame();
            }
        }
    }


    enum BlockType {
        jumpOn,
        down,
        sideWay,
        midWay,
        multySideWay,
        any,
    }
}
