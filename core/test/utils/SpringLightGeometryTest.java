package utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SpringLightGeometryTest {
	@Test
	public void distanceScalesWithSpringPowerAndClamps(){
		assertEquals(0f, SpringLightGeometry.distanceForPower(0f, 0f), 0.0001f);
		assertEquals(12f, SpringLightGeometry.distanceForPower(60f, 0f), 0.0001f);
		assertEquals(74.2586f, SpringLightGeometry.distanceForPower(300f, 0f), 0.0001f);
		assertEquals(120f, SpringLightGeometry.distanceForPower(600f, 0f), 0.0001f);
		assertEquals(120f, SpringLightGeometry.distanceForPower(900f, 0f), 0.0001f);
	}

	@Test
	public void sourceWidthProjectsSpringFaceAcrossPushDirection(){
		assertEquals(6.4f, SpringLightGeometry.sourceWidth(3.2f, 2f, 0f, 0f, 45f), 0.0001f);
		assertEquals(8f, SpringLightGeometry.sourceWidth(1.6f, 4f, 0f, 150f, 0f), 0.0001f);
		assertEquals(0f, SpringLightGeometry.sourceWidth(3.2f, 2f, 0f, 0f, 0f), 0.0001f);
	}

	@Test
	public void sourceDepthProjectsSpringFaceAlongPushDirection(){
		assertEquals(4f, SpringLightGeometry.sourceDepth(3.2f, 2f, 0f, 0f, 45f), 0.0001f);
		assertEquals(3.2f, SpringLightGeometry.sourceDepth(1.6f, 4f, 0f, 150f, 0f), 0.0001f);
		assertEquals(0f, SpringLightGeometry.sourceDepth(3.2f, 2f, 0f, 0f, 0f), 0.0001f);
	}

	@Test
	public void sourceChainSpansPerpendicularToPushDirection(){
		assertArrayEquals(new float[]{3.2f, 0f, -3.2f, 0f},
				SpringLightGeometry.sourceChain(0f, 0f, 6.4f, 0f, 45f), 0.0001f);
		assertArrayEquals(new float[]{0f, -4f, 0f, 4f},
				SpringLightGeometry.sourceChain(0f, 0f, 8f, 150f, 0f), 0.0001f);
	}

	@Test
	public void bodyLocalSourceChainStaysOnSpringSurface(){
		assertArrayEquals(new float[]{3.2f, 2.15f, -3.2f, 2.15f},
				SpringLightGeometry.bodyLocalSourceChain(3.2f, 2f, 0f, 45f, 0.15f), 0.0001f);
		assertArrayEquals(new float[]{1.75f, -4f, 1.75f, 4f},
				SpringLightGeometry.bodyLocalSourceChain(1.6f, 4f, 60f, 0f, 0.15f), 0.0001f);
	}

	@Test
	public void bodyLocalSourceChainDoesNotCounterRotateSpring(){
		float[] chain = SpringLightGeometry.bodyLocalSourceChain(3.2f, 2f, 0f, 45f, 0.15f);
		float[] rotated = rotateChain(chain, 45f);
		assertArrayEquals(new float[]{0.7425f, 3.783f, -3.783f, -0.7425f}, rotated, 0.0001f);
	}

	@Test
	public void fadeHoldsFullIntensityBeforeEasingToZero(){
		assertEquals(1f, SpringLightGeometry.fadeAlpha(0f), 0.0001f);
		assertEquals(1f, SpringLightGeometry.fadeAlpha(0.01f), 0.0001f);
		assertEquals(0.75f, SpringLightGeometry.fadeAlpha(0.255f), 0.0001f);
		assertEquals(0f, SpringLightGeometry.fadeAlpha(0.5f), 0.0001f);
		assertEquals(0f, SpringLightGeometry.fadeAlpha(2f), 0.0001f);
	}

	@Test
	public void zeroPowerHasNoDirection(){
		assertFalse(SpringLightGeometry.hasDirection(0f, 0f));
		assertTrue(SpringLightGeometry.hasDirection(-60f, 60f));
	}

	private static float[] rotateChain(float[] chain, float degrees){
		float radians = (float)Math.toRadians(degrees);
		float cos = (float)Math.cos(radians);
		float sin = (float)Math.sin(radians);
		return new float[]{
				chain[0] * cos - chain[1] * sin,
				chain[0] * sin + chain[1] * cos,
				chain[2] * cos - chain[3] * sin,
				chain[2] * sin + chain[3] * cos
		};
	}
}
