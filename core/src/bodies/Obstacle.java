package bodies;

import utils.Variables;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.one.button.jam.Couleurs;

public class Obstacle extends PolygonShape{
	
	public Body body;
	public BodyDef bodyDef;
	public float posX, posY, width, height, angle;
	protected Vector2 initialPosition;
	protected FixtureDef fixtureDef;
	Camera camera;
	protected Couleurs couleurs;
	protected Color couleur;
	
	public Obstacle(World world, Camera camera, float posX, float posY, float width, float height, String angle){
		super();
		this.camera = camera;
		this.posX = posX;
		this.posY = posY;
		this.width = width;
		this.height = height;
		this.angle = -Float.parseFloat(angle)*MathUtils.degreesToRadians;
		
		bodyDef = new BodyDef();
    	
		this.setAsBox(width, height);

		bodyDef.position.set(new Vector2(posX, posY));
    	bodyDef.type = getBodyType();
		body = world.createBody(bodyDef);
		
		fixtureDef = new FixtureDef();
		fixtureDef.shape = this;
        fixtureDef.density = 1.0f;  
        fixtureDef.friction = 0.9f;  
        fixtureDef.restitution = 0.4f;
   
        body.createFixture(fixtureDef).setUserData("Objet");
        body.setUserData("Objet");
        body.setFixedRotation(false);
        
        body.setTransform(body.getPosition().x, body.getPosition().y, this.angle);
        this.dispose();
	}
	
	public Obstacle(World world, Camera camera, MapObject rectangleObject, Couleurs couleurs){	
		super();
		
		this.couleurs = couleurs;
		couleur = couleurs.getCouleurSol();
		create(world, camera, rectangleObject, couleurs);
		
		initialPosition = new Vector2(body.getPosition());
        
	}
	
	public float getWidth(){
		return width;
	}
	
	public float getHeight(){
		return height;
	}
	
	public float getX(){
		return posX;
	}
	
	public float getY(){
		return posY;
	}

	public void setX( float X){
		posX = X;
	}

	public void setY( float Y){
		posY = Y;
	}
	
	public BodyType getBodyType(){
		return BodyType.StaticBody;
	}
	
	public Color getCouleur(){
		return couleurs.getCouleurSol();
	}
	
	public void create(World world, Camera camera, MapObject rectangleObject, Couleurs couleurs){

		Rectangle rectangle = ((RectangleMapObject) rectangleObject).getRectangle();
		
		this.camera = camera;
		this.posX = (rectangle.x + rectangle.width/2) * Variables.WORLD_TO_BOX;
		this.posY = (rectangle.y + rectangle.height/2) * Variables.WORLD_TO_BOX;
		this.width = (rectangle.width/2) * Variables.WORLD_TO_BOX;
		this.height = (rectangle.height/2) * Variables.WORLD_TO_BOX;
		
		if(rectangleObject.getProperties().get("rotation") != null)
			this.angle = -Float.parseFloat(rectangleObject.getProperties().get("rotation").toString())*MathUtils.degreesToRadians;
		
		bodyDef = new BodyDef();
    	
		this.setAsBox(width, height);

		bodyDef.position.set(new Vector2(posX, posY));
    	bodyDef.type = getBodyType();
		body = world.createBody(bodyDef);
		
		fixtureDef = new FixtureDef();
		fixtureDef.shape = this;
        fixtureDef.density = (float)(Variables.DENSITÉ/(5 * width * height));  
        fixtureDef.friction = 0.5f;  
        fixtureDef.restitution = 0.5f;
   
        body.createFixture(fixtureDef).setUserData("Objet");
        body.setUserData("Objet");
        
        if(rectangleObject.getProperties().get("rotation") != null){
            /*
             * Positions x' et y', à partir des positions x et y après une rotation d'un angle A
             * x' = x*cos(A) - y*sin(A)
             * y' = x*sin(A) + y*cos(A)
             * 
             * Ceci est vrai si la rotation est autour de l'origine (0,0)
             */
        	float X = (float)(body.getPosition().x - width + width * Math.cos(angle) + height * Math.sin(angle));
        	float Y = (float)(width * Math.sin(angle) + body.getPosition().y + height - height * Math.cos(angle));
        	body.setTransform(X, Y, this.angle);
        }
        
        this.dispose();  
	}
	
	public void draw(SpriteBatch batch, TextureAtlas textureAtlas){
		batch.setColor(couleur);
		batch.draw(textureAtlas.findRegion("WhiteSquare"), 
				this.body.getPosition().x - width, 
				this.body.getPosition().y - height,
				width,
				height,
				2 * width,
				2 * height,
				1,
				1,
				body.getAngle()*MathUtils.radiansToDegrees);
	}
	
	public void drawOmbre(SpriteBatch batch, TextureAtlas textureAtlas){
		batch.setColor(0,0,0,1);
		batch.draw(textureAtlas.findRegion("WhiteSquare"), 
				this.body.getPosition().x - width + Variables.ombresX, 
				this.body.getPosition().y - height + Variables.ombresY,
				width,
				height,
				2 * width,
				2 * height,
				1,
				1,
				body.getAngle()*MathUtils.radiansToDegrees);
	}
	
	public void actif(){
		
	}
	
	public void activity(){
		
	}
	
	public void initiate(){
		
	}
}
