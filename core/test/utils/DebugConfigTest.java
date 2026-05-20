package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
