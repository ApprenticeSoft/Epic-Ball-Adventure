package utils;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

public class OrthogonalTiledMapRendererWithSprites  extends OrthogonalTiledMapRenderer{
	float unitScale = 1;
	
	public OrthogonalTiledMapRendererWithSprites(TiledMap map, Batch batch) {
		super(map, batch);
	}

	public OrthogonalTiledMapRendererWithSprites (TiledMap map, float unitScale, Batch batch) {
		super(map, unitScale, batch);
		
		this.unitScale = unitScale;
	}

	
	//Permet d'afficher des objets que nous allons rajouter dans une couche "objet" de façon procédurale
    @Override
    public void renderObject(MapObject object) {
        if(object instanceof TextureMapObject) {
            TextureMapObject textureObj = (TextureMapObject) object;
                //batch.draw(textureObj.getTextureRegion(), textureObj.getX(), textureObj.getY());
                batch.draw(textureObj.getTextureRegion(), 
                			textureObj.getX(), 
                			textureObj.getY(), 
                			textureObj.getTextureRegion().getRegionWidth() * unitScale, 
                			textureObj.getTextureRegion().getRegionHeight() * unitScale);
        }
    }
	
}
