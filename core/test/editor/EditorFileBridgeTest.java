package editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class EditorFileBridgeTest {
	@Test
	public void sanitizeAddsExtensionAndRemovesPathSeparators(){
		assertEquals("Level 1.tmx", EditorFileBridge.sanitize("Level 1"));
		assertEquals(".._Unsafe_Level.tmx", EditorFileBridge.sanitize("../Unsafe\\Level.tmx"));
		assertEquals("Editor Level.tmx", EditorFileBridge.sanitize(" "));
	}
}
