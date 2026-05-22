package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DebugConfigTest {
	@Test
	public void waterTuningValuesClampToSafeRanges(){
		DebugConfig.setWaterBubbleDensityMultiplier(-2f);
		assertEquals(0f, DebugConfig.waterBubbleDensityScale(), 0.0001f);
		DebugConfig.setWaterBubbleDensityMultiplier(99f);
		assertEquals(4f, DebugConfig.waterBubbleDensityScale(), 0.0001f);

		DebugConfig.setWaterBubbleSizeMultiplier(0f);
		assertEquals(0.35f, DebugConfig.waterBubbleSizeScale(), 0.0001f);
		DebugConfig.setWaterBubbleLifetimeMultiplier(99f);
		assertEquals(2.5f, DebugConfig.waterBubbleLifetimeScale(), 0.0001f);

		DebugConfig.setWaterFoamAmount(Float.NaN);
		assertEquals(1f, DebugConfig.waterFoamAmountScale(), 0.0001f);
		DebugConfig.setWaterFoamAmount(99f);
		assertEquals(3f, DebugConfig.waterFoamAmountScale(), 0.0001f);

		DebugConfig.reset();
	}

	@Test
	public void resetClearsEditorDebugProbes(){
		DebugConfig.editorInvalidPlayProbe = true;
		DebugConfig.editorLoadLevel = 3;
		DebugConfig.resetProgress = true;
		DebugConfig.desktopBenchmark = true;
		DebugConfig.webBenchmark = true;
		DebugConfig.fixedStep = true;

		DebugConfig.reset();

		assertFalse(DebugConfig.editorInvalidPlayProbe);
		assertEquals(0, DebugConfig.editorLoadLevel);
		assertFalse(DebugConfig.resetProgress);
		assertFalse(DebugConfig.desktopBenchmark);
		assertFalse(DebugConfig.webBenchmark);
		assertFalse(DebugConfig.fixedStep);
	}

	@Test
	public void startLevelEnablesDebugMode(){
		DebugConfig.reset();

		DebugConfig.startLevel = 3;

		assertTrue(DebugConfig.isEnabled());
		DebugConfig.reset();
	}

	@Test
	public void webBenchmarkEnablesBenchmarkMode(){
		DebugConfig.reset();

		DebugConfig.webBenchmark = true;

		assertTrue(DebugConfig.isEnabled());
		assertTrue(DebugConfig.benchmarkMode());
		DebugConfig.reset();
	}
}
