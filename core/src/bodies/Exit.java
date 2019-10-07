package bodies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.World;
import com.one.button.jam.Couleurs;

public class Exit extends Obstacle{

	float glowAlpha = 1;
	
	public Exit(World world, Camera camera, MapObject rectangleObject, Couleurs couleurs) {
		super(world, camera, rectangleObject, couleurs);
		body.setUserData("Exit");
		body.getFixtureList().get(0).setSensor(true);
		body.getFixtureList().get(0).setUserData("Exit");
		
		this.couleur = couleurs.getCouleurExit();	
	}
	

	@Override
	public void draw(SpriteBatch batch, TextureAtlas textureAtlas){
		batch.setColor(couleur.r, couleur.g, couleur.b, 0.8f);
		batch.draw(textureAtlas.findRegion("Exit_background"), 
				this.body.getPosition().x - width, 
				this.body.getPosition().y - height,
				width,
				height,
				2 * width,
				2 * height,
				1,
				1,
				body.getAngle()*MathUtils.radiansToDegrees);
		

		batch.setColor(couleur.r, couleur.g, couleur.b, (float)(1 + Math.cos(glowAlpha += 4*Gdx.graphics.getDeltaTime()))/2);
		batch.draw(textureAtlas.findRegion("Exit_glow"), 
				this.body.getPosition().x - width * ((float)textureAtlas.findRegion("Exit_glow").getRegionWidth() / (float)textureAtlas.findRegion("Exit_background").getRegionWidth()), 
				this.body.getPosition().y - height * ((float)textureAtlas.findRegion("Exit_glow").getRegionWidth() / (float)textureAtlas.findRegion("Exit_background").getRegionWidth()),
				width * ((float)textureAtlas.findRegion("Exit_glow").getRegionWidth() / (float)textureAtlas.findRegion("Exit_background").getRegionWidth()),
				height * ((float)textureAtlas.findRegion("Exit_glow").getRegionWidth() / (float)textureAtlas.findRegion("Exit_background").getRegionWidth()),
				2 * width * ((float)textureAtlas.findRegion("Exit_glow").getRegionWidth() / (float)textureAtlas.findRegion("Exit_background").getRegionWidth()),
				2 * height * ((float)textureAtlas.findRegion("Exit_glow").getRegionWidth() / (float)textureAtlas.findRegion("Exit_background").getRegionWidth()),
				1,
				1,
				body.getAngle()*MathUtils.radiansToDegrees);
	}

}
