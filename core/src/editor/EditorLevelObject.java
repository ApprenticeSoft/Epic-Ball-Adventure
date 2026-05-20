package editor;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class EditorLevelObject {
	public enum SnapMode {
		GRID,
		FREE
	}

	private static final float ROTATION_EPSILON = 0.0001f;
	private static int nextRuntimeId = 1;

	public final int runtimeId;
	public EditorObjectType type;
	public float x;
	public float y;
	public float width;
	public float height;
	public float rotation;
	public SnapMode snapMode = SnapMode.GRID;
	public final ObjectMap<String, String> properties = new ObjectMap<String, String>();
	public final Array<Vector2> points = new Array<Vector2>();
	private boolean boundsDirty = true;
	private float localMinX;
	private float localMaxX;
	private float localMinY;
	private float localMaxY;

	public EditorLevelObject(EditorObjectType type, float x, float y, float width, float height){
		this.runtimeId = nextRuntimeId++;
		this.type = type;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void reset(EditorObjectType type, float x, float y, float width, float height){
		this.type = type;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		rotation = 0f;
		snapMode = SnapMode.GRID;
		properties.clear();
		points.clear();
		markBoundsDirty();
	}

	public void copyFrom(EditorLevelObject other){
		type = other.type;
		x = other.x;
		y = other.y;
		width = other.width;
		height = other.height;
		rotation = other.rotation;
		snapMode = other.snapMode;
		properties.clear();
		for(ObjectMap.Entry<String, String> entry : other.properties)
			properties.put(entry.key, entry.value);
		points.clear();
		for(Vector2 point : other.points)
			points.add(new Vector2(point));
		markBoundsDirty();
	}

	public EditorLevelObject copy(){
		EditorLevelObject copy = new EditorLevelObject(type, x + 32f, y + 32f, width, height);
		copy.rotation = rotation;
		copy.snapMode = snapMode;
		for(ObjectMap.Entry<String, String> entry : properties)
			copy.properties.put(entry.key, entry.value);
		for(Vector2 point : points)
			copy.points.add(new Vector2(point));
		copy.markBoundsDirty();
		return copy;
	}

	public EditorLevelObject copyExact(){
		EditorLevelObject copy = new EditorLevelObject(type, x, y, width, height);
		copy.copyFrom(this);
		return copy;
	}

	public boolean contains(float worldX, float worldY){
		if(type == EditorObjectType.POLYGON)
			return containsPolygon(worldX - x, worldY - y);
		if(type == EditorObjectType.PLATFORM)
			return distanceToPolylineSquared(worldX - x, worldY - y) <= 18f * 18f;
		if(usesRotatedRectangleGeometry())
			return containsRotatedRectangle(worldX, worldY);
		return worldX >= getMinX() && worldX <= getMaxX() && worldY >= getMinY() && worldY <= getMaxY();
	}

	public void ensureDefaultPoints(){
		if(type == EditorObjectType.POLYGON && points.size == 0)
			setPolygonVertexCount(4);
		else if(type == EditorObjectType.PLATFORM && points.size == 0){
			points.add(new Vector2(0f, 0f));
			points.add(new Vector2(width, height));
			markBoundsDirty();
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
		markBoundsDirty();
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
		markBoundsDirty();
		syncPlatformEndpointSize();
	}

	public void setPointWorldPosition(int index, float worldX, float worldY){
		if(index < 0 || index >= points.size)
			return;
		points.get(index).set(worldX - x, worldY - y);
		markBoundsDirty();
		if(type == EditorObjectType.PLATFORM)
			syncPlatformEndpointSize();
		else if(type == EditorObjectType.POLYGON)
			updateWidthHeightFromPointBounds();
	}

	public void setPlatformEnd(float endX, float endY){
		ensureDefaultPoints();
		points.peek().set(endX, endY);
		markBoundsDirty();
		syncPlatformEndpointSize();
	}

	public void replacePoints(float[] vertices){
		points.clear();
		for(int i = 0; i + 1 < vertices.length; i += 2)
			points.add(new Vector2(vertices[i], vertices[i + 1]));
		markBoundsDirty();
		if(type == EditorObjectType.PLATFORM)
			syncPlatformEndpointSize();
		else if(type == EditorObjectType.POLYGON)
			updateWidthHeightFromPointBounds();
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
		markBoundsDirty();
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
			return x + localMinX();
		if(usesRotatedRectangleGeometry())
			return minRotatedX();
		return Math.min(x, x + width);
	}

	public float getMaxX(){
		if(usesPointGeometry())
			return x + localMaxX();
		if(usesRotatedRectangleGeometry())
			return maxRotatedX();
		return Math.max(x, x + width);
	}

	public float getMinY(){
		if(usesPointGeometry())
			return y + localMinY();
		if(usesRotatedRectangleGeometry())
			return minRotatedY();
		return Math.min(y, y + height);
	}

	public float getMaxY(){
		if(usesPointGeometry())
			return y + localMaxY();
		if(usesRotatedRectangleGeometry())
			return maxRotatedY();
		return Math.max(y, y + height);
	}

	public boolean usesPointGeometry(){
		return type == EditorObjectType.POLYGON || type == EditorObjectType.PLATFORM;
	}

	public boolean usesRotatedRectangleGeometry(){
		return !usesPointGeometry() && Math.abs(rotation) > ROTATION_EPSILON;
	}

	public float getVisualDrawX(){
		if(!usesRotatedRectangleGeometry())
			return x;
		float halfWidth = width / 2f;
		float halfHeight = height / 2f;
		float angle = getVisualAngleRadians();
		return x - halfWidth + halfWidth * MathUtils.cos(angle) + halfHeight * MathUtils.sin(angle);
	}

	public float getVisualDrawY(){
		if(!usesRotatedRectangleGeometry())
			return y;
		float halfWidth = width / 2f;
		float halfHeight = height / 2f;
		float angle = getVisualAngleRadians();
		return y + halfHeight + halfWidth * MathUtils.sin(angle) - halfHeight * MathUtils.cos(angle);
	}

	public float getVisualCenterX(){
		return getVisualDrawX() + width / 2f;
	}

	public float getVisualCenterY(){
		return getVisualDrawY() + height / 2f;
	}

	public float getVisualRotationDegrees(){
		return -rotation;
	}

	public void rectangleCorners(Vector2 bottomLeft, Vector2 bottomRight, Vector2 topRight, Vector2 topLeft){
		float drawX = getVisualDrawX();
		float drawY = getVisualDrawY();
		visualLocalToWorld(0f, 0f, bottomLeft);
		visualLocalToWorld(width, 0f, bottomRight);
		visualLocalToWorld(width, height, topRight);
		visualLocalToWorld(0f, height, topLeft);
		if(!usesRotatedRectangleGeometry()){
			bottomLeft.set(drawX, drawY);
			bottomRight.set(drawX + width, drawY);
			topRight.set(drawX + width, drawY + height);
			topLeft.set(drawX, drawY + height);
		}
	}

	public Vector2 visualLocalToWorld(float localX, float localY, Vector2 out){
		float drawX = getVisualDrawX();
		float drawY = getVisualDrawY();
		if(!usesRotatedRectangleGeometry())
			return out.set(drawX + localX, drawY + localY);
		float centerX = drawX + width / 2f;
		float centerY = drawY + height / 2f;
		float dx = drawX + localX - centerX;
		float dy = drawY + localY - centerY;
		float angle = getVisualAngleRadians();
		float cos = MathUtils.cos(angle);
		float sin = MathUtils.sin(angle);
		return out.set(centerX + dx * cos - dy * sin, centerY + dx * sin + dy * cos);
	}

	public Vector2 worldToVisualLocal(float worldX, float worldY, Vector2 out){
		float drawX = getVisualDrawX();
		float drawY = getVisualDrawY();
		if(!usesRotatedRectangleGeometry())
			return out.set(worldX - drawX, worldY - drawY);
		float centerX = drawX + width / 2f;
		float centerY = drawY + height / 2f;
		float dx = worldX - centerX;
		float dy = worldY - centerY;
		float angle = -getVisualAngleRadians();
		float cos = MathUtils.cos(angle);
		float sin = MathUtils.sin(angle);
		float unrotatedX = centerX + dx * cos - dy * sin;
		float unrotatedY = centerY + dx * sin + dy * cos;
		return out.set(unrotatedX - drawX, unrotatedY - drawY);
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
		return inside || distanceToClosedPolylineSquared(localX, localY) <= 18f * 18f;
	}

	private float distanceToPolylineSquared(float localX, float localY){
		ensureDefaultPoints();
		float distance = Float.MAX_VALUE;
		for(int i = 0; i < points.size - 1; i++){
			Vector2 a = points.get(i);
			Vector2 b = points.get(i + 1);
			distance = Math.min(distance, distanceToSegmentSquared(localX, localY, a.x, a.y, b.x, b.y));
		}
		return distance;
	}

	private float distanceToClosedPolylineSquared(float localX, float localY){
		float distance = distanceToPolylineSquared(localX, localY);
		if(points.size > 2){
			Vector2 first = points.first();
			Vector2 last = points.peek();
			distance = Math.min(distance, distanceToSegmentSquared(localX, localY, last.x, last.y, first.x, first.y));
		}
		return distance;
	}

	private void updateWidthHeightFromPointBounds(){
		width = Math.max(1f, localMaxX() - localMinX());
		height = Math.max(1f, localMaxY() - localMinY());
	}

	private float getLocalMinX(){
		return localMinX();
	}

	private float getLocalMaxX(){
		return localMaxX();
	}

	private float getLocalMinY(){
		return localMinY();
	}

	private float getLocalMaxY(){
		return localMaxY();
	}

	private float localMinX(){
		updateBoundsIfNeeded();
		return localMinX;
	}

	private float localMaxX(){
		updateBoundsIfNeeded();
		return localMaxX;
	}

	private float localMinY(){
		updateBoundsIfNeeded();
		return localMinY;
	}

	private float localMaxY(){
		updateBoundsIfNeeded();
		return localMaxY;
	}

	private void updateBoundsIfNeeded(){
		if(!boundsDirty)
			return;
		ensureDefaultPoints();
		float minX = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		for(Vector2 point : points){
			minX = Math.min(minX, point.x);
			maxX = Math.max(maxX, point.x);
			minY = Math.min(minY, point.y);
			maxY = Math.max(maxY, point.y);
		}
		localMinX = minX == Float.MAX_VALUE ? 0f : minX;
		localMaxX = maxX == -Float.MAX_VALUE ? width : maxX;
		localMinY = minY == Float.MAX_VALUE ? 0f : minY;
		localMaxY = maxY == -Float.MAX_VALUE ? height : maxY;
		boundsDirty = false;
	}

	private float distanceToSegmentSquared(float px, float py, float ax, float ay, float bx, float by){
		float dx = bx - ax;
		float dy = by - ay;
		float lengthSquared = dx * dx + dy * dy;
		if(lengthSquared == 0)
			return distanceSquared(px, py, ax, ay);
		float t = ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
		t = Math.max(0f, Math.min(1f, t));
		return distanceSquared(px, py, ax + t * dx, ay + t * dy);
	}

	private float distanceSquared(float ax, float ay, float bx, float by){
		float dx = ax - bx;
		float dy = ay - by;
		return dx * dx + dy * dy;
	}

	private boolean containsRotatedRectangle(float worldX, float worldY){
		float drawX = getVisualDrawX();
		float drawY = getVisualDrawY();
		float centerX = drawX + width / 2f;
		float centerY = drawY + height / 2f;
		float dx = worldX - centerX;
		float dy = worldY - centerY;
		float inverseAngle = -getVisualAngleRadians();
		float cos = MathUtils.cos(inverseAngle);
		float sin = MathUtils.sin(inverseAngle);
		float unrotatedX = centerX + dx * cos - dy * sin;
		float unrotatedY = centerY + dx * sin + dy * cos;
		return unrotatedX >= drawX && unrotatedX <= drawX + width
				&& unrotatedY >= drawY && unrotatedY <= drawY + height;
	}

	private float minRotatedX(){
		float a = rotatedX(0f, 0f);
		float b = rotatedX(width, 0f);
		float c = rotatedX(width, height);
		float d = rotatedX(0f, height);
		return Math.min(Math.min(a, b), Math.min(c, d));
	}

	private float maxRotatedX(){
		float a = rotatedX(0f, 0f);
		float b = rotatedX(width, 0f);
		float c = rotatedX(width, height);
		float d = rotatedX(0f, height);
		return Math.max(Math.max(a, b), Math.max(c, d));
	}

	private float minRotatedY(){
		float a = rotatedY(0f, 0f);
		float b = rotatedY(width, 0f);
		float c = rotatedY(width, height);
		float d = rotatedY(0f, height);
		return Math.min(Math.min(a, b), Math.min(c, d));
	}

	private float maxRotatedY(){
		float a = rotatedY(0f, 0f);
		float b = rotatedY(width, 0f);
		float c = rotatedY(width, height);
		float d = rotatedY(0f, height);
		return Math.max(Math.max(a, b), Math.max(c, d));
	}

	private float rotatedX(float localX, float localY){
		float drawX = getVisualDrawX();
		float drawY = getVisualDrawY();
		float centerX = drawX + width / 2f;
		float centerY = drawY + height / 2f;
		float dx = drawX + localX - centerX;
		float dy = drawY + localY - centerY;
		float angle = getVisualAngleRadians();
		return centerX + dx * MathUtils.cos(angle) - dy * MathUtils.sin(angle);
	}

	private float rotatedY(float localX, float localY){
		float drawX = getVisualDrawX();
		float drawY = getVisualDrawY();
		float centerX = drawX + width / 2f;
		float centerY = drawY + height / 2f;
		float dx = drawX + localX - centerX;
		float dy = drawY + localY - centerY;
		float angle = getVisualAngleRadians();
		return centerY + dx * MathUtils.sin(angle) + dy * MathUtils.cos(angle);
	}

	private float getVisualAngleRadians(){
		return -rotation * MathUtils.degreesToRadians;
	}

	private void markBoundsDirty(){
		boundsDirty = true;
	}
}
