package utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public final class SpringLightGeometry {
	public static final float FADE_SECONDS = 0.5f;
	public static final float FULL_INTENSITY_SECONDS = 0.01f;
	private static final float POWER_REFERENCE = 60f;
	private static final float DISTANCE_AT_REFERENCE = 12f;
	private static final float POWER_DISTANCE_EXPONENT = 1.35f;
	private static final float MIN_DISTANCE = 4f;
	private static final float MAX_DISTANCE = 120f;
	private static final int MIN_RAYS = 12;
	private static final int MAX_RAYS = 64;
	private static final float RAYS_PER_WORLD_UNIT = 8f;

	private SpringLightGeometry(){
	}

	public static float powerMagnitude(float powerX, float powerY){
		return (float)Math.sqrt(powerX * powerX + powerY * powerY);
	}

	public static boolean hasDirection(float powerX, float powerY){
		return powerMagnitude(powerX, powerY) > 0.001f;
	}

	public static float distanceForPower(float powerX, float powerY){
		float magnitude = powerMagnitude(powerX, powerY);
		if(magnitude <= 0.001f)
			return 0f;
		float scaledPower = magnitude / POWER_REFERENCE;
		float distance = MIN_DISTANCE + (DISTANCE_AT_REFERENCE - MIN_DISTANCE)
				* (float)Math.pow(scaledPower, POWER_DISTANCE_EXPONENT);
		return MathUtils.clamp(distance, MIN_DISTANCE, MAX_DISTANCE);
	}

	public static float fadeAlpha(float elapsedSeconds){
		if(elapsedSeconds <= FULL_INTENSITY_SECONDS)
			return 1f;
		float fadeProgress = (elapsedSeconds - FULL_INTENSITY_SECONDS) / (FADE_SECONDS - FULL_INTENSITY_SECONDS);
		fadeProgress = MathUtils.clamp(fadeProgress, 0f, 1f);
		return MathUtils.clamp(1f - fadeProgress * fadeProgress, 0f, 1f);
	}

	public static int rayCountForSourceWidth(float sourceWidth){
		return MathUtils.clamp(MathUtils.ceil(Math.max(0f, sourceWidth) * RAYS_PER_WORLD_UNIT), MIN_RAYS, MAX_RAYS);
	}

	public static float sourceWidth(float halfWidth, float halfHeight, float bodyAngle, float powerX, float powerY){
		Vector2 direction = direction(powerX, powerY);
		if(direction.isZero())
			return 0f;
		Vector2 perpendicular = new Vector2(-direction.y, direction.x);
		float cos = MathUtils.cos(bodyAngle);
		float sin = MathUtils.sin(bodyAngle);
		Vector2 localX = new Vector2(cos, sin);
		Vector2 localY = new Vector2(-sin, cos);
		return 2f * (Math.abs(perpendicular.dot(localX)) * halfWidth
				+ Math.abs(perpendicular.dot(localY)) * halfHeight);
	}

	public static float sourceDepth(float halfWidth, float halfHeight, float bodyAngle, float powerX, float powerY){
		Vector2 direction = direction(powerX, powerY);
		if(direction.isZero())
			return 0f;
		float cos = MathUtils.cos(bodyAngle);
		float sin = MathUtils.sin(bodyAngle);
		Vector2 localX = new Vector2(cos, sin);
		Vector2 localY = new Vector2(-sin, cos);
		return 2f * (Math.abs(direction.dot(localX)) * halfWidth
				+ Math.abs(direction.dot(localY)) * halfHeight);
	}

	public static float bodyLocalSourceWidth(float halfWidth, float halfHeight, float powerX, float powerY){
		Vector2 localDirection = direction(powerX, powerY);
		if(localDirection.isZero())
			return 0f;
		return sourceWidthForLocalDirection(halfWidth, halfHeight, localDirection);
	}

	public static float bodyLocalSourceDepth(float halfWidth, float halfHeight, float powerX, float powerY){
		Vector2 localDirection = direction(powerX, powerY);
		if(localDirection.isZero())
			return 0f;
		return sourceDepthForLocalDirection(halfWidth, halfHeight, localDirection);
	}

	public static float[] bodyLocalSourceChain(float halfWidth, float halfHeight,
			float powerX, float powerY, float sourceOffset){
		Vector2 localDirection = direction(powerX, powerY);
		if(localDirection.isZero())
			return new float[]{0f, 0f, 0f, 0f};
		float sourceWidth = sourceWidthForLocalDirection(halfWidth, halfHeight, localDirection);
		float sourceDepth = sourceDepthForLocalDirection(halfWidth, halfHeight, localDirection);
		Vector2 center = new Vector2(localDirection).scl(sourceDepth * 0.5f + sourceOffset);
		Vector2 perpendicular = new Vector2(-localDirection.y, localDirection.x).scl(sourceWidth * 0.5f);
		return new float[]{
				center.x - perpendicular.x, center.y - perpendicular.y,
				center.x + perpendicular.x, center.y + perpendicular.y
		};
	}

	public static float[] sourceChain(float centerX, float centerY, float sourceWidth, float powerX, float powerY){
		Vector2 direction = direction(powerX, powerY);
		if(direction.isZero())
			return new float[]{centerX, centerY, centerX, centerY};
		Vector2 perpendicular = new Vector2(-direction.y, direction.x).scl(sourceWidth * 0.5f);
		return new float[]{
				centerX - perpendicular.x, centerY - perpendicular.y,
				centerX + perpendicular.x, centerY + perpendicular.y
		};
	}

	private static Vector2 direction(float powerX, float powerY){
		Vector2 direction = new Vector2(powerX, powerY);
		if(direction.len2() <= 0.000001f)
			return Vector2.Zero.cpy();
		return direction.nor();
	}

	private static float sourceWidthForLocalDirection(float halfWidth, float halfHeight, Vector2 localDirection){
		Vector2 perpendicular = new Vector2(-localDirection.y, localDirection.x);
		return 2f * (Math.abs(perpendicular.x) * halfWidth
				+ Math.abs(perpendicular.y) * halfHeight);
	}

	private static float sourceDepthForLocalDirection(float halfWidth, float halfHeight, Vector2 localDirection){
		return 2f * (Math.abs(localDirection.x) * halfWidth
				+ Math.abs(localDirection.y) * halfHeight);
	}
}
