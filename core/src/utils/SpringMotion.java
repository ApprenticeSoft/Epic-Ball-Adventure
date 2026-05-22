package utils;

import com.badlogic.gdx.math.Vector2;

public final class SpringMotion {
	private SpringMotion(){
	}

	public static Vector2 worldVelocity(float localPowerX, float localPowerY, float bodyAngle, Vector2 output){
		return output.set(localPowerX, localPowerY).rotateRad(bodyAngle);
	}

	public static Vector2 localDisplacement(Vector2 currentPosition, Vector2 initialPosition, float bodyAngle, Vector2 output){
		return output.set(currentPosition).sub(initialPosition).rotateRad(-bodyAngle);
	}
}
