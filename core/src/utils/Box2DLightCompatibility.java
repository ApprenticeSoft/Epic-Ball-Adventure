package utils;

import box2dLight.Spinor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

public final class Box2DLightCompatibility {
	private static boolean initialized;

	private Box2DLightCompatibility(){
	}

	public static void ensurePools(){
		if(initialized)
			return;
		initialized = true;
		Pools.set(Vector2.class, new Pool<Vector2>(8, 256) {
			@Override
			protected Vector2 newObject(){
				return new Vector2();
			}

			@Override
			protected void reset(Vector2 object){
				object.setZero();
			}
		});
		Pools.set(FloatArray.class, new Pool<FloatArray>(4, 64) {
			@Override
			protected FloatArray newObject(){
				return new FloatArray();
			}

			@Override
			protected void reset(FloatArray object){
				object.clear();
			}
		});
		Pools.set(Spinor.class, new Pool<Spinor>(8, 256) {
			@Override
			protected Spinor newObject(){
				return new Spinor();
			}

			@Override
			protected void reset(Spinor object){
				object.set(1f, 0f);
			}
		});
	}
}
