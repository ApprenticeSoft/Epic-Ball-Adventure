package utils;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public final class LightCollisionCategories {
	public static final short STATIC_OCCLUDER = 0x0001;
	public static final short DYNAMIC_NON_OCCLUDER = 0x0002;
	public static final short SPRING_LIGHT = 0x0004;
	public static final short ALL = -1;

	private LightCollisionCategories(){
	}

	public static void applyToWorld(World world){
		if(world == null)
			return;
		Array<Body> bodies = new Array<Body>();
		world.getBodies(bodies);
		for(Body body : bodies)
			applyToBody(body);
	}

	public static void applyToBody(Body body){
		if(body == null)
			return;
		short category = body.getType() == BodyType.StaticBody ? STATIC_OCCLUDER : DYNAMIC_NON_OCCLUDER;
		for(Fixture fixture : body.getFixtureList())
			setCategory(fixture, category, ALL);
	}

	private static void setCategory(Fixture fixture, short categoryBits, short maskBits){
		Filter filter = fixture.getFilterData();
		filter.categoryBits = categoryBits;
		filter.maskBits = maskBits;
		filter.groupIndex = 0;
		fixture.setFilterData(filter);
	}
}
