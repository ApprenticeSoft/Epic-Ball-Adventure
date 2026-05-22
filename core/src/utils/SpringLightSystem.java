package utils;

import bodies.Spring;
import box2dLight.ChainLight;
import box2dLight.RayHandler;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class SpringLightSystem implements Disposable {
	private static final float SOURCE_OFFSET = 0.15f;

	private final World world;
	private RayHandler rayHandler;
	private final Array<ActiveSpringLight> activeLights = new Array<ActiveSpringLight>();
	private boolean disabled;
	private boolean failureLogged;

	public SpringLightSystem(World world){
		this.world = world;
		try{
			Box2DLightCompatibility.ensurePools();
			rayHandler = new RayHandler(world);
			rayHandler.setShadows(false);
			rayHandler.setBlur(true);
			rayHandler.setBlurNum(1);
			rayHandler.setAmbientLight(0f);
			rayHandler.simpleBlendFunc.set(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		}
		catch(Throwable throwable){
			disableAfterFailure(throwable);
		}
	}

	public void activate(Spring spring){
		if(disabled || rayHandler == null || spring == null || !SpringLightGeometry.hasDirection(spring.getPowerX(), spring.getPowerY()))
			return;
		try{
			LightCollisionCategories.applyToWorld(world);
			float sourceWidth = spring.lightSourceWidth();
			float distance = SpringLightGeometry.distanceForPower(spring.getPowerX(), spring.getPowerY());
			float[] chain = SpringLightGeometry.bodyLocalSourceChain(spring.getWidth(), spring.getHeight(),
					spring.getPowerX(), spring.getPowerY(), SOURCE_OFFSET);
			Color color = new Color(spring.getCouleur());
			color.a = 1f;
			removeActiveLightFor(spring.body);
			ChainLight light = new ChainLight(rayHandler, SpringLightGeometry.rayCountForSourceWidth(sourceWidth),
					color, distance, -1, chain);
			light.setContactFilter(LightCollisionCategories.SPRING_LIGHT, (short)0, LightCollisionCategories.STATIC_OCCLUDER);
			light.setSoftnessLength(distance * 0.18f);
			light.attachToBody(spring.body);
			activeLights.add(new ActiveSpringLight(spring.body, light, color));
			DebugConfig.log("spring light activated power=" + spring.getPowerX() + "," + spring.getPowerY()
					+ " width=" + sourceWidth + " distance=" + distance);
		}
		catch(Throwable throwable){
			disableAfterFailure(throwable);
		}
	}

	public void update(float delta){
		if(disabled || rayHandler == null)
			return;
		for(int i = activeLights.size - 1; i >= 0; i--){
			ActiveSpringLight activeLight = activeLights.get(i);
			activeLight.elapsed += delta;
			float alpha = SpringLightGeometry.fadeAlpha(activeLight.elapsed);
			if(alpha <= 0f){
				activeLight.light.remove();
				activeLights.removeIndex(i);
			}
			else{
				activeLight.light.setColor(activeLight.color.r, activeLight.color.g, activeLight.color.b, alpha);
			}
		}
	}

	public void render(OrthographicCamera camera){
		if(disabled || rayHandler == null || activeLights.size == 0)
			return;
		try{
			rayHandler.setCombinedMatrix(camera);
			rayHandler.updateAndRender();
		}
		catch(Throwable throwable){
			disableAfterFailure(throwable);
		}
	}

	public void resize(int backBufferWidth, int backBufferHeight){
		if(disabled || rayHandler == null)
			return;
		rayHandler.resizeFBO(Math.max(1, backBufferWidth / 4), Math.max(1, backBufferHeight / 4));
	}

	@Override
	public void dispose(){
		clearActiveLights();
		if(rayHandler != null)
			rayHandler.dispose();
	}

	private void disableAfterFailure(Throwable throwable){
		disabled = true;
		clearActiveLights();
		if(!failureLogged){
			failureLogged = true;
			DebugConfig.log("spring light disabled error=" + throwable.getClass().getName() + ": " + throwable.getMessage());
		}
	}

	private void clearActiveLights(){
		for(int i = activeLights.size - 1; i >= 0; i--){
			try{
				activeLights.get(i).light.remove();
			}
			catch(Throwable ignored){
			}
		}
		activeLights.clear();
	}

	private void removeActiveLightFor(Body springBody){
		for(int i = activeLights.size - 1; i >= 0; i--){
			ActiveSpringLight activeLight = activeLights.get(i);
			if(activeLight.springBody == springBody){
				activeLight.light.remove();
				activeLights.removeIndex(i);
			}
		}
	}

	private static class ActiveSpringLight {
		private final Body springBody;
		private final ChainLight light;
		private final Color color;
		private float elapsed;

		private ActiveSpringLight(Body springBody, ChainLight light, Color color){
			this.springBody = springBody;
			this.light = light;
			this.color = color;
		}
	}
}
