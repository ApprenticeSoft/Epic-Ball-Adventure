package utils;

import bodies.Eau;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ShortArray;

final class WaterSurfaceRenderer {
	private static final int VERTEX_SIZE = 5;
	private static final float MIN_VISIBLE_DEPTH = 0.08f;

	private final EarClippingTriangulator triangulator = new EarClippingTriangulator();
	private final Vector2 worldPoint = new Vector2();
	private float[] polygonVertices = new float[0];
	private float[] renderVertices = new float[0];

	void draw(PolygonSpriteBatch batch, TextureAtlas textureAtlas, Eau water,
			WaterSplashSystem.WaterSurfaceSimulation simulation){
		TextureRegion region = textureAtlas.findRegion("WhiteSquare");
		if(region == null)
			return;
		int coordCount = buildLocalPolygonVertices(water.width, water.height, simulation, ensurePolygonCapacity(water,
				simulation));
		ShortArray triangles = triangulator.computeTriangles(polygonVertices, 0, coordCount);
		int renderCount = buildRenderVertices(water, region, coordCount);
		batch.draw(region.getTexture(), renderVertices, 0, renderCount, triangles.items, 0, triangles.size);
		batch.setColor(1f, 1f, 1f, 1f);
	}

	private float[] ensurePolygonCapacity(Eau water, WaterSplashSystem.WaterSurfaceSimulation simulation){
		int sampleCount = simulation == null
				? WaterSplashSystem.calculateSurfaceSampleCount(water.width)
				: simulation.sampleCount;
		int coordCapacity = (sampleCount + 2) * 2;
		if(polygonVertices.length < coordCapacity)
			polygonVertices = new float[coordCapacity];
		if(renderVertices.length < (coordCapacity / 2) * VERTEX_SIZE)
			renderVertices = new float[(coordCapacity / 2) * VERTEX_SIZE];
		return polygonVertices;
	}

	private int buildRenderVertices(Eau water, TextureRegion region, int coordCount){
		Color color = water.getCouleur();
		float packedColor = color == null ? Color.WHITE_FLOAT_BITS : color.toFloatBits();
		float halfWidth = water.width;
		float minY = -water.height;
		float maxY = water.height + maxSurfaceLift(water.height);
		float safeWidth = Math.max(0.0001f, halfWidth * 2f);
		float safeHeight = Math.max(0.0001f, maxY - minY);
		float u = region.getU();
		float u2 = region.getU2();
		float v = region.getV();
		float v2 = region.getV2();
		int vertexIndex = 0;
		for(int i = 0; i < coordCount; i += 2){
			float localX = polygonVertices[i];
			float localY = polygonVertices[i + 1];
			Vector2 transformedPoint = water.body.getWorldPoint(worldPoint.set(localX, localY));
			renderVertices[vertexIndex++] = transformedPoint.x;
			renderVertices[vertexIndex++] = transformedPoint.y;
			renderVertices[vertexIndex++] = packedColor;
			renderVertices[vertexIndex++] = MathUtils.lerp(u, u2, (localX + halfWidth) / safeWidth);
			renderVertices[vertexIndex++] = MathUtils.lerp(v2, v, (localY - minY) / safeHeight);
		}
		return vertexIndex;
	}

	static int buildLocalPolygonVertices(float halfWidth, float halfHeight,
			WaterSplashSystem.WaterSurfaceSimulation simulation, float[] out){
		int sampleCount = simulation == null
				? WaterSplashSystem.calculateSurfaceSampleCount(halfWidth)
				: simulation.sampleCount;
		if(out.length < (sampleCount + 2) * 2)
			throw new IllegalArgumentException("Output array is too small for water surface polygon.");
		int index = 0;
		out[index++] = -halfWidth;
		out[index++] = -halfHeight;
		out[index++] = halfWidth;
		out[index++] = -halfHeight;
		for(int i = sampleCount - 1; i >= 0; i--){
			float localX = simulation == null ? sampleLocalX(halfWidth, sampleCount, i) : simulation.localX(i);
			float displacement = simulation == null ? 0f : simulation.displacement(i);
			out[index++] = localX;
			out[index++] = clampSurfaceY(halfHeight, displacement);
		}
		return index;
	}

	static float clampSurfaceY(float halfHeight, float displacement){
		float minSurfaceY = halfHeight - Math.min(halfHeight * 0.65f, Math.max(MIN_VISIBLE_DEPTH, 1f));
		float maxSurfaceY = halfHeight + maxSurfaceLift(halfHeight);
		return MathUtils.clamp(halfHeight + displacement, minSurfaceY, maxSurfaceY);
	}

	private static float sampleLocalX(float halfWidth, int sampleCount, int index){
		if(sampleCount <= 1)
			return 0f;
		return -halfWidth + (halfWidth * 2f) * index / (sampleCount - 1);
	}

	private static float maxSurfaceLift(float halfHeight){
		return Math.min(Math.max(0.25f, halfHeight * 0.45f), 1.25f);
	}
}
