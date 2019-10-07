package bodies;

import utils.Variables;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.one.button.jam.Couleurs;

public class Balle {

	public Body body;
	private BodyDef bodyDef;
	private FixtureDef fixtureDef;
	private CircleShape balleShape;
	private PolygonShape detecteurShape;
	private float rayon, posXInit, posYInit;
	public boolean droite = false, restart = false;
	private boolean screenTouched = false;
	public float restartDelay = 3f, soundTrigger = 1;
	public Sound soundCountdown;
	
	public Balle(World world, Camera camera, TiledMap tiledMap){

		soundCountdown = Gdx.audio.newSound(Gdx.files.internal("Sounds/Countdown.wav"));
		
		MapObjects personnages = (MapObjects)tiledMap.getLayers().get("Spawn").getObjects();
		
		rayon = Variables.PPT * Variables.WORLD_TO_BOX / 2;	
		posXInit = (personnages.get("Ball").getProperties().get("x", float.class) + personnages.get("Ball").getProperties().get("width", float.class)/2) * Variables.WORLD_TO_BOX;
		posYInit = (personnages.get("Ball").getProperties().get("y", float.class) + personnages.get("Ball").getProperties().get("height", float.class)) * Variables.WORLD_TO_BOX;
		
		balleShape = new CircleShape();
		balleShape.setRadius(rayon);	
		
		bodyDef = new BodyDef();
		bodyDef.position.set(posXInit, posYInit);
        bodyDef.type = BodyType.DynamicBody; 
        bodyDef.bullet = true;						//TRÈS IMPORTANT POUR ÉVITER L'EFFET TUNNEL À HAUTE VITESSE
        
        body = world.createBody(bodyDef);
        body.setFixedRotation(false);
        	
        fixtureDef = new FixtureDef();  
        fixtureDef.shape = balleShape; 
        fixtureDef.density = (float)(Variables.DENSITÉ/(rayon * rayon * Math.PI));
        fixtureDef.friction = 0.9f;  
        fixtureDef.restitution = 0.1f;  
        body.createFixture(fixtureDef).setUserData("Ball"); 
        
        //Création du détecteur (Important pour la flotaison)
        detecteurShape = new PolygonShape();
        detecteurShape.setAsBox(0.4f*rayon, 0.4f*rayon, new Vector2(0, 0), 0); 
		fixtureDef.shape = detecteurShape;
        fixtureDef.density = 0.0f;  
        fixtureDef.friction = 0.0f;  
        fixtureDef.restitution = 0.0f;  
		fixtureDef.isSensor = true;			
		body.createFixture(fixtureDef).setUserData("BalleDetecteur");
		
        body.setUserData("Balle");
        
        balleShape.dispose();
        detecteurShape.dispose();
	}
	
	public void activity(){ 
		if(Gdx.app.getType() == ApplicationType.Desktop)
			desktopControl();
		else if(Gdx.app.getType() == ApplicationType.Android)
			androidControl();
	
		if(restartDelay < 0)
			Variables.restart = true;
	}
	
	public void desktopControl(){
			if(Gdx.input.isKeyJustPressed(Keys.F)){
				droite = !droite;
			}
			
			if(Gdx.input.isKeyPressed(Keys.F)){
				if(droite){
					if(body.getAngularVelocity() > 0)
						body.applyTorque(-3.2f, true);
					else
						body.applyTorque(-2.0f, true);
				}
				else{
					if(body.getAngularVelocity() < 0)
						body.applyTorque(3.2f, true);
					else
						body.applyTorque(2.0f, true);
				}
	        }
			
			if(restart){
				restartDelay -= Gdx.graphics.getDeltaTime();	
				soundTrigger += Gdx.graphics.getDeltaTime();	
				if(soundTrigger >= 1){
					soundCountdown.play();
					soundTrigger = 0;
				}
			}
			else if(Gdx.input.isKeyPressed(Keys.F)){
				if(body.getLinearVelocity().len() < 3){
					restartDelay -= Gdx.graphics.getDeltaTime();
				}
				else
					restartDelay = 3f;
			}
	}
	
	public void androidControl(){
		/*
			if(Gdx.input.isTouched()){
				if(!screenTouched)
					droite = !droite;
				
				screenTouched = true;
			}
			else
				screenTouched = false;
		*/
			if(Gdx.input.isTouched()){
				if(/*droite*/ Gdx.input.getX() > Gdx.graphics.getWidth()/2){
					if(body.getAngularVelocity() > 0)
						body.applyTorque(-3.2f, true);
					else
						body.applyTorque(-2.0f, true);
				}
				else{
					if(body.getAngularVelocity() < 0)
						body.applyTorque(3.2f, true);
					else
						body.applyTorque(2.0f, true);
				}
	        }
			
			if(restart){
				restartDelay -= Gdx.graphics.getDeltaTime();	
				soundTrigger += Gdx.graphics.getDeltaTime();	
				if(soundTrigger >= 1){
					soundCountdown.play();
					soundTrigger = 0;
				}
			}
			else if(Gdx.input.isTouched()){
				if(body.getLinearVelocity().len() < 3){
					restartDelay -= Gdx.graphics.getDeltaTime();
				}
				else
					restartDelay = 3f;
			}
	}
	
	public void draw(SpriteBatch batch, TextureAtlas textureAtlas, Couleurs couleurs){
		batch.setColor(couleurs.getCouleurBalle());
		batch.draw(textureAtlas.findRegion("BallColor"), 
				this.body.getPosition().x - rayon, 
				this.body.getPosition().y - rayon,
				rayon,
				rayon,
				2 * rayon,
				2 * rayon,
				1,
				1,
				body.getAngle()*MathUtils.radiansToDegrees);
	}
	
	public void drawOmbre(SpriteBatch batch, TextureAtlas textureAtlas){
		batch.setColor(0,0,0,1);
		batch.draw(textureAtlas.findRegion("BallColor"), 
				this.body.getPosition().x - rayon + Variables.ombresX, 
				this.body.getPosition().y - rayon + Variables.ombresY, 
				2 * rayon, 
				2 * rayon);
	}
	
	public float getX(){
		return body.getPosition().x;
	}
	
	public float getY(){
		return body.getPosition().y;
	}
	
	public Vector2 getOrigine(){
		return new Vector2(posXInit, posYInit);
	}
	
	public void restart(){
		body.setLinearVelocity(0, 0);
		body.setAngularVelocity(0);
		body.setTransform(getOrigine(), body.getAngle());
		restartDelay = 3f;
		restart = false;
		droite = false;
	}
}
