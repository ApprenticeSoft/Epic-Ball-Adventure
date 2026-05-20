package utils;

import bodies.Balle;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;

public class MyCamera extends  OrthographicCamera{

	float posX, posY;
	private final Vector3 target = new Vector3();

	public MyCamera(){
		super();
	}

	public void mouvement(Balle balle, TiledMap tiledMap){
		mouvement(balle, tiledMap, Variables.BOX_STEP);
	}

	public void mouvement(Balle balle, TiledMap tiledMap, float delta){
		if(posX == 0 && posY == 0){
			posX = this.position.x;
			posY = this.position.y;
		}

		float deadZoneX = this.viewportWidth / 10f;
		float deadZoneY = this.viewportHeight / 10f;

		if(this.position.x < balle.getX() - deadZoneX)
			posX = balle.getX() - deadZoneX;
		else if(this.position.x > balle.getX() + deadZoneX)
			posX = balle.getX() + deadZoneX;
		if(this.position.y < balle.getY() - deadZoneY)
			posY = balle.getY() - deadZoneY;
		else if(this.position.y > balle.getY() + deadZoneY)
			posY = balle.getY() + deadZoneY;

		float alpha = 1f - (float)Math.pow(1f - 0.45f, Math.max(delta, 0f) / Variables.BOX_STEP);
		if(alpha < 0f)
			alpha = 0f;
		else if(alpha > 1f)
			alpha = 1f;
		target.set(posX, posY, 0);
		this.position.interpolate(target, alpha, Interpolation.fade);

		clampToLevel(tiledMap);
		posX = this.position.x;
		posY = this.position.y;
	}

	private void clampToLevel(TiledMap tiledMap){
		float levelWidth = ((float)(tiledMap.getProperties().get("width", Integer.class) * Variables.PPT)) * Variables.WORLD_TO_BOX;
		float levelHeight = ((float)(tiledMap.getProperties().get("height", Integer.class) * Variables.PPT)) * Variables.WORLD_TO_BOX;

		float clampedX;
		if(levelWidth <= this.viewportWidth)
			clampedX = levelWidth / 2f;
		else if(this.position.x + this.viewportWidth / 2f > levelWidth)
			clampedX = levelWidth - this.viewportWidth / 2f;
		else if(this.position.x - this.viewportWidth / 2f < 0)
			clampedX = this.viewportWidth / 2f;
		else
			clampedX = this.position.x;

		float clampedY;
		if(levelHeight <= this.viewportHeight)
			clampedY = levelHeight / 2f;
		else if(this.position.y + this.viewportHeight / 2f > levelHeight)
			clampedY = levelHeight - this.viewportHeight / 2f;
		else if(this.position.y - this.viewportHeight / 2f < 0)
			clampedY = this.viewportHeight / 2f;
		else
			clampedY = this.position.y;

		this.position.set(clampedX, clampedY, 0);

	}
}
