package editor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
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
	public final Array<Vector2> points = new Array<Vector2>();

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
		for(Vector2 point : points)
			copy.points.add(new Vector2(point));
		return copy;
	}

	public boolean contains(float worldX, float worldY){
		if(type == EditorObjectType.POLYGON)
			return containsPolygon(worldX - x, worldY - y);
		if(type == EditorObjectType.PLATFORM)
			return distanceToPolyline(worldX - x, worldY - y) <= 18f;
		return worldX >= getMinX() && worldX <= getMaxX() && worldY >= getMinY() && worldY <= getMaxY();
	}

	public void ensureDefaultPoints(){
		if(type == EditorObjectType.POLYGON && points.size == 0)
			setPolygonVertexCount(4);
		else if(type == EditorObjectType.PLATFORM && points.size == 0){
			points.add(new Vector2(0f, 0f));
			points.add(new Vector2(width, height));
		}
	}

	public void setPolygonVertexCount(int count){
		count = Math.max(3, Math.min(8, count));
		float minWorldX = points.size == 0 ? x : getMinX();
		float minWorldY = points.size == 0 ? y : getMinY();
		float maxWorldX = points.size == 0 ? x + width : getMaxX();
		float maxWorldY = points.size == 0 ? y + height : getMaxY();
		float boundsWidth = Math.max(32f, maxWorldX - minWorldX);
		float boundsHeight = Math.max(32f, maxWorldY - minWorldY);
		x = minWorldX;
		y = minWorldY;
		width = boundsWidth;
		height = boundsHeight;
		points.clear();
		float centerX = boundsWidth / 2f;
		float centerY = boundsHeight / 2f;
		float radiusX = boundsWidth / 2f;
		float radiusY = boundsHeight / 2f;
		for(int i = 0; i < count; i++){
			double angle = Math.PI / 2.0 - (Math.PI * 2.0 * i / count);
			points.add(new Vector2(centerX + (float)Math.cos(angle) * radiusX,
					centerY + (float)Math.sin(angle) * radiusY));
		}
	}

	public void setPlatformPointCount(int count){
		count = Math.max(2, Math.min(8, count));
		ensureDefaultPoints();
		Vector2 start = new Vector2(points.first());
		Vector2 end = new Vector2(points.peek());
		points.clear();
		for(int i = 0; i < count; i++){
			float alpha = count == 1 ? 0f : (float)i / (float)(count - 1);
			points.add(new Vector2(start.x + (end.x - start.x) * alpha, start.y + (end.y - start.y) * alpha));
		}
		syncPlatformEndpointSize();
	}

	public void setPointWorldPosition(int index, float worldX, float worldY){
		if(index < 0 || index >= points.size)
			return;
		points.get(index).set(worldX - x, worldY - y);
		if(type == EditorObjectType.PLATFORM)
			syncPlatformEndpointSize();
		else if(type == EditorObjectType.POLYGON)
			updateWidthHeightFromPointBounds();
	}

	public void setPlatformEnd(float endX, float endY){
		ensureDefaultPoints();
		points.peek().set(endX, endY);
		syncPlatformEndpointSize();
	}

	public void scalePointBounds(float newWidth, float newHeight){
		if(points.size == 0)
			return;
		float minX = getLocalMinX();
		float minY = getLocalMinY();
		float oldWidth = Math.max(1f, getLocalMaxX() - minX);
		float oldHeight = Math.max(1f, getLocalMaxY() - minY);
		float scaleX = Math.max(1f, newWidth) / oldWidth;
		float scaleY = Math.max(1f, newHeight) / oldHeight;
		for(Vector2 point : points){
			point.x = minX + (point.x - minX) * scaleX;
			point.y = minY + (point.y - minY) * scaleY;
		}
		width = Math.max(1f, newWidth);
		height = Math.max(1f, newHeight);
		if(type == EditorObjectType.PLATFORM)
			syncPlatformEndpointSize();
	}

	public void syncPlatformEndpointSize(){
		if(points.size == 0)
			return;
		Vector2 end = points.peek();
		width = end.x;
		height = end.y;
	}

	public float[] pointVertices(){
		ensureDefaultPoints();
		float[] vertices = new float[points.size * 2];
		for(int i = 0; i < points.size; i++){
			vertices[i * 2] = points.get(i).x;
			vertices[i * 2 + 1] = points.get(i).y;
		}
		return vertices;
	}

	public float getMinX(){
		if(usesPointGeometry())
			return x + getLocalMinX();
		return Math.min(x, x + width);
	}

	public float getMaxX(){
		if(usesPointGeometry())
			return x + getLocalMaxX();
		return Math.max(x, x + width);
	}

	public float getMinY(){
		if(usesPointGeometry())
			return y + getLocalMinY();
		return Math.min(y, y + height);
	}

	public float getMaxY(){
		if(usesPointGeometry())
			return y + getLocalMaxY();
		return Math.max(y, y + height);
	}

	private boolean usesPointGeometry(){
		return type == EditorObjectType.POLYGON || type == EditorObjectType.PLATFORM;
	}

	private boolean containsPolygon(float localX, float localY){
		ensureDefaultPoints();
		boolean inside = false;
		for(int i = 0, j = points.size - 1; i < points.size; j = i++){
			Vector2 pi = points.get(i);
			Vector2 pj = points.get(j);
			if(((pi.y > localY) != (pj.y > localY))
					&& localX < (pj.x - pi.x) * (localY - pi.y) / (pj.y - pi.y) + pi.x)
				inside = !inside;
		}
		return inside || distanceToClosedPolyline(localX, localY) <= 18f;
	}

	private float distanceToPolyline(float localX, float localY){
		ensureDefaultPoints();
		float distance = Float.MAX_VALUE;
		for(int i = 0; i < points.size - 1; i++){
			Vector2 a = points.get(i);
			Vector2 b = points.get(i + 1);
			distance = Math.min(distance, distanceToSegment(localX, localY, a.x, a.y, b.x, b.y));
		}
		return distance;
	}

	private float distanceToClosedPolyline(float localX, float localY){
		float distance = distanceToPolyline(localX, localY);
		if(points.size > 2){
			Vector2 first = points.first();
			Vector2 last = points.peek();
			distance = Math.min(distance, distanceToSegment(localX, localY, last.x, last.y, first.x, first.y));
		}
		return distance;
	}

	private void updateWidthHeightFromPointBounds(){
		width = Math.max(1f, getLocalMaxX() - getLocalMinX());
		height = Math.max(1f, getLocalMaxY() - getLocalMinY());
	}

	private float getLocalMinX(){
		ensureDefaultPoints();
		float min = Float.MAX_VALUE;
		for(Vector2 point : points)
			min = Math.min(min, point.x);
		return min == Float.MAX_VALUE ? 0f : min;
	}

	private float getLocalMaxX(){
		ensureDefaultPoints();
		float max = -Float.MAX_VALUE;
		for(Vector2 point : points)
			max = Math.max(max, point.x);
		return max == -Float.MAX_VALUE ? width : max;
	}

	private float getLocalMinY(){
		ensureDefaultPoints();
		float min = Float.MAX_VALUE;
		for(Vector2 point : points)
			min = Math.min(min, point.y);
		return min == Float.MAX_VALUE ? 0f : min;
	}

	private float getLocalMaxY(){
		ensureDefaultPoints();
		float max = -Float.MAX_VALUE;
		for(Vector2 point : points)
			max = Math.max(max, point.y);
		return max == -Float.MAX_VALUE ? height : max;
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
