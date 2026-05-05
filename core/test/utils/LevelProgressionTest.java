package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LevelProgressionTest {
	@Test
	void progressesThroughConfiguredLevels(){
		int maxLevel = 5;
		int level = 1;

		assertTrue(LevelProgression.hasNextLevel(level, maxLevel));
		level = LevelProgression.nextLevel(level, maxLevel);
		assertEquals(2, level);

		level = LevelProgression.nextLevel(level, maxLevel);
		assertEquals(3, level);

		level = LevelProgression.nextLevel(level, maxLevel);
		assertEquals(4, level);

		level = LevelProgression.nextLevel(level, maxLevel);
		assertEquals(5, level);
		assertFalse(LevelProgression.hasNextLevel(level, maxLevel));
		assertEquals(5, LevelProgression.nextLevel(level, maxLevel));
	}

	@Test
	void transitionTimingCannotStallPastDuration(){
		assertFalse(LevelProgression.transitionComplete(1.34f, 1.35f));
		assertTrue(LevelProgression.transitionComplete(1.35f, 1.35f));
		assertTrue(LevelProgression.transitionComplete(2f, 1.35f));
	}

	@Test
	void clampsTransitionProgress(){
		assertEquals(0f, LevelProgression.transitionProgress(-1f, 1.35f));
		assertEquals(0.5f, LevelProgression.transitionProgress(0.675f, 1.35f), 0.0001f);
		assertEquals(1f, LevelProgression.transitionProgress(2f, 1.35f));
		assertEquals(1f, LevelProgression.transitionProgress(0f, 0f));
	}
}
