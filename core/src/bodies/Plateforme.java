package bodies;

import utils.Variables;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.World;
import com.one.button.jam.Couleurs;
import com.one.button.jam.MyGdxGame;

public class Plateforme {

	final MyGdxGame game;
	private PolylineMapObject polylineObject;
	private Vector2[] ligne;
	private Vector2 direction;
	private BodyDef bodyDef;
	private PolygonShape ps;
	public Body body;
	private FixtureDef fixtureDef;
	private int �tape;
	public float width, height, largeur, vitesse;
	private boolean retour, boucle;
	
	public Plateforme(final MyGdxGame game, World world, PolylineMapObject polylineObject){
		this.game = game;
		this.polylineObject = polylineObject;
		retour = false;
		�tape = 1;
		
		if(polylineObject.getProperties().get("Speed") != null)
			vitesse = Float.parseFloat((String) polylineObject.getProperties().get("Speed"));
		else vitesse = 5;
		
		if(polylineObject.getProperties().get("Loop") != null)
			boucle = true;
		else boucle = false;
		
		if(polylineObject.getProperties().get("Width") != null)
			largeur = Integer.parseInt((String) polylineObject.getProperties().get("Width"));
		else
			largeur = 2;
		
		width = largeur*(32 * Variables.WORLD_TO_BOX/2);
		height = 16*Variables.WORLD_TO_BOX;
		direction = new Vector2();
		
		ligne = new Vector2[polylineObject.getPolyline().getTransformedVertices().length/2];
    	for(int i = 0; i < ligne.length; i++){
    		ligne[i] = new Vector2(polylineObject.getPolyline().getTransformedVertices()[i*2]*Variables.WORLD_TO_BOX, polylineObject.getPolyline().getTransformedVertices()[i*2 + 1]*Variables.WORLD_TO_BOX);
    	}   
    	
    	ps = new PolygonShape();
    	ps.setAsBox(width, height);

    	bodyDef = new BodyDef();
    	bodyDef.type = BodyType.KinematicBody;
    	bodyDef.position.set(ligne[0]);
    	
    	fixtureDef = new FixtureDef();
		fixtureDef.shape = ps;
        fixtureDef.density = 0.0f;  
        fixtureDef.friction = 1.0f;  
        fixtureDef.restitution = 0f;

    	body = world.createBody(bodyDef);
        body.createFixture(fixtureDef).setUserData("Objet");
        body.setUserData("Objet");
        
        ps.dispose();

        direction = new Vector2(ligne[�tape].x - body.getPosition().x, ligne[�tape].y - body.getPosition().y);
        body.setLinearVelocity(direction.clamp(vitesse, vitesse));
	}
	
	public void d�placement(){
		if(!boucle){
			if(!retour){
				if(!new Vector2(ligne[�tape].x - body.getPosition().x, ligne[�tape].y - body.getPosition().y).hasSameDirection(direction)){
					�tape++;
					
			        if(�tape == ligne.length){
			        	retour = true;
			        	�tape = ligne.length - 2;
			        }
			        
					direction.set(ligne[�tape].x - body.getPosition().x, ligne[�tape].y - body.getPosition().y);
				}
			}
			else{
				if(!new Vector2(ligne[�tape].x - body.getPosition().x, ligne[�tape].y - body.getPosition().y).hasSameDirection(direction)){
					�tape--;
					
			        if(�tape < 0){
			        	retour = false;
			        	�tape = 1;
			        }
			        
					direction.set(ligne[�tape].x - body.getPosition().x, ligne[�tape].y - body.getPosition().y);
				}
			}	
		}
		else{
			if(!new Vector2(ligne[�tape].x - body.getPosition().x, ligne[�tape].y - body.getPosition().y).hasSameDirection(direction)){
				�tape++;
				
		        if(�tape == ligne.length){
		        	�tape = 0;
		        }
		        
				direction.set(ligne[�tape].x - body.getPosition().x, ligne[�tape].y - body.getPosition().y);
			}
		}
        body.setLinearVelocity(direction.clamp(vitesse, vitesse));  
	}
	
	public float getWidth(){
		return width;
	}
	
	public float getHeight(){
		return height;
	}
	
	public float getX(){
		return body.getPosition().x;
	}
	
	public float getY(){
		return body.getPosition().y;
	}
	
	public void draw(SpriteBatch batch, TextureAtlas textureAtlas, Couleurs couleurs){
		batch.setColor(couleurs.getCouleurL�ger());
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
}
