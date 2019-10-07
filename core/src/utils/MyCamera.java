package utils;

import bodies.Balle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;

public class MyCamera extends  OrthographicCamera{
	
	float posX, posY;
	
	public MyCamera(){
		super();
	}
	
	public void mouvement(Balle balle, TiledMap tiledMap){
		//Positionnement par rapport à la balle		
		if(this.position.x < balle.getX() - Gdx.graphics.getWidth() * Variables.WORLD_TO_BOX/10)
			posX = balle.getX() - Gdx.graphics.getWidth() * Variables.WORLD_TO_BOX/10;
		else if(this.position.x > balle.getX() + Gdx.graphics.getWidth() * Variables.WORLD_TO_BOX/10)
			posX = balle.getX() + Gdx.graphics.getWidth() * Variables.WORLD_TO_BOX/10;
		if(this.position.y < balle.getY() - Gdx.graphics.getHeight() * Variables.WORLD_TO_BOX/10)
			//this.position.set(this.position.x,balle.getY() - Gdx.graphics.getHeight() * Variables.WORLD_TO_BOX/10,0);
			posY = balle.getY() - Gdx.graphics.getHeight() * Variables.WORLD_TO_BOX/10;
		else if(this.position.y > balle.getY() + Gdx.graphics.getHeight() * Variables.WORLD_TO_BOX/10)
			//this.position.set(this.position.x,balle.getY() + Gdx.graphics.getHeight() * Variables.WORLD_TO_BOX/10,0);
			posY = balle.getY() + Gdx.graphics.getHeight() * Variables.WORLD_TO_BOX/10;
		
		this.position.interpolate(new Vector3(posX,posY,0), 0.45f, Interpolation.fade); //Mouvement transitoire de la caméra
		
		//Positionnement par rapport au niveau
		if(this.position.x + this.viewportWidth/2 > ((float)(tiledMap.getProperties().get("width", Integer.class)*Variables.PPT))*Variables.WORLD_TO_BOX)
			this.position.set(((float)(tiledMap.getProperties().get("width", Integer.class)*Variables.PPT))*Variables.WORLD_TO_BOX - this.viewportWidth/2, this.position.y, 0);
		else if(this.position.x - this.viewportWidth/2 < 0)
			this.position.set(this.viewportWidth/2, this.position.y, 0);
		if(this.position.y + this.viewportHeight/2 > ((float)(tiledMap.getProperties().get("height", Integer.class)*Variables.PPT))*Variables.WORLD_TO_BOX)
			this.position.set(this.position.x, ((float)(tiledMap.getProperties().get("height", Integer.class)*Variables.PPT))*Variables.WORLD_TO_BOX - this.viewportHeight/2, 0);
		else if(this.position.y - this.viewportHeight/2 < 0)
			this.position.set(this.position.x, this.viewportHeight/2, 0);
		
	}
}
