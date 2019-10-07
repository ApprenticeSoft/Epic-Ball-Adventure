package bodies;

import utils.Variables;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.utils.ShortArray;
import com.one.button.jam.Couleurs;

public class Polygone extends Obstacle{

	private Texture textureSolid;
	private PolygonSprite polySprite;
	float coordPoly[];
	
	public Polygone(World world, Camera camera, MapObject Object, Couleurs couleurs) {
		super(world, camera, Object, couleurs);
	}
	
	@Override
	public void create(World world, Camera camera, MapObject Object, Couleurs couleurs){
		coordPoly = new float[((PolygonMapObject) Object).getPolygon().getTransformedVertices().length];
    	for(int i = 0; i < ((PolygonMapObject) Object).getPolygon().getTransformedVertices().length; i++){
    		coordPoly[i] = ((PolygonMapObject) Object).getPolygon().getTransformedVertices()[i]*Variables.WORLD_TO_BOX;
    		//System.out.println("coordPoly[" + i + "] : " + coordPoly[i]);
    	}
    	
    	PolygonShape ps = new PolygonShape();
    	ps.set(coordPoly);
    	
    	bodyDef = new BodyDef();
    	if(Object.getProperties().get("type") != null && Object.getProperties().get("type").toString().equals("Light"))
    		bodyDef.type = BodyType.DynamicBody;
    	else
    		bodyDef.type = getBodyType();
    	
		body = world.createBody(bodyDef);
		
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = ps;
        fixtureDef.density = 0.2f;  
        fixtureDef.friction = 0.9f;  
        fixtureDef.restitution = 0.4f;
   
        body.createFixture(fixtureDef).setUserData("Objet");
        body.setUserData("Objet");
        body.setFixedRotation(false);
        
        if(Object.getProperties().get("type") != null && Object.getProperties().get("type").toString().equals("Light")){
	        body.createFixture(fixtureDef).setUserData("Light");
	        body.setUserData("Light");
        }
        
        this.dispose();
        
        /****************DESSIN*****************/
        // Creating the color filling (but textures would work the same way)
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(getCouleur()); 
        pix.fill();
        textureSolid = new Texture(pix);
        TextureRegion textureRegion = new TextureRegion(textureSolid);

        EarClippingTriangulator triangulator = new EarClippingTriangulator();
        ShortArray triangleIndices = triangulator.computeTriangles(coordPoly);

        PolygonRegion polyReg = new PolygonRegion(textureRegion, coordPoly, triangleIndices.toArray());

        polySprite = new PolygonSprite(polyReg);

        //System.out.println("PolySprite X = "  + polySprite.getX());
        //System.out.println("PolySprite Y = "  + polySprite.getY());
        //System.out.println("PolySprite originX = "  + polySprite.getOriginX());
        //System.out.println("PolySprite originY = "  + polySprite.getOriginY());
        //System.out.println("***********************************************");
        /***************************************/
	}
	
	public void draw(PolygonSpriteBatch polyBatch, Couleurs couleurs){
		setPos(polySprite.getX(), polySprite.getY());
	    polySprite.draw(polyBatch);

        //System.out.println("PolySprite X = "  + polySprite.getX());
        //System.out.println("PolySprite Y = "  + polySprite.getY());
        //System.out.println("PolySprite originX = "  + polySprite.getOriginX());
        //System.out.println("PolySprite originY = "  + polySprite.getOriginY());

        //System.out.println("body.getPosition().x = "  + body.getPosition().x);
        //System.out.println("body.getPosition().y = "  + body.getPosition().y);
        //System.out.println("***********************************************");
	}
	
	public void drawOmbre(PolygonSpriteBatch polyBatch){
	    polySprite.draw(polyBatch);
	}
	
	public void setPos(float X, float Y){
        polySprite.setX(X);
        polySprite.setY(Y);
	}
	
	@Override
	public Color getCouleur(){
		return couleurs.getCouleurSol();
	}

}
