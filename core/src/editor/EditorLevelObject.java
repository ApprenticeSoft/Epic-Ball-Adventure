package editor;

import com.badlogic.gdx.utils.ObjectMap;

public class EditorLevelObject {
	private static int nextRuntimeId = 1;

	public final int runtimeId;
	public EditorObjectType type;
	public float x;
	public float y;
	public float width;
	public float height;
	public float rotation;
	public final ObjectMap<String, String> properties = new ObjectMap<String, String>();

	public EditorLevelObject(EditorObjectType type, float x, float y, float width, float height){
		this.runtimeId = nextRuntimeId++;
		this.type = type;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public EditorLevelObject copy(){
		EditorLevelObject copy = new EditorLevelObject(type, x + 32f, y + 32f, width, height);
		copy.rotation = rotation;
		for(ObjectMap.Entry<String, String> entry : properties)
			copy.properties.put(entry.key, entry.value);
		return copy;
	}

	public boolean contains(float worldX, float worldY){
		if(type == EditorObjectType.PLATFORM)
			return distanceToSegment(worldX, worldY, x, y, x + width, y + height) <= 18f;
		return worldX >= x && worldX <= x + width && worldY >= y && worldY <= y + height;
	}

	private float distanceToSegment(float px, float py, float ax, float ay, float bx, float by){
		float dx = bx - ax;
		float dy = by - ay;
		float lengthSquared = dx * dx + dy * dy;
		if(lengthSquared == 0)
			return distance(px, py, ax, ay);
		float t = ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
		t = Math.max(0f, Math.min(1f, t));
		return distance(px, py, ax + t * dx, ay + t * dy);
	}

	private float distance(float ax, float ay, float bx, float by){
		float dx = ax - bx;
		float dy = ay - by;
		return (float)Math.sqrt(dx * dx + dy * dy);
	}
}
