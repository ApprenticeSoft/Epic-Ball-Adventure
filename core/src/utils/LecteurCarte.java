package utils;

import bodies.Balle;
import bodies.Eau;
import bodies.Exit;
import bodies.Obstacle;
import bodies.ObstacleBalance;
import bodies.ObstacleBalançoire;
import bodies.ObstacleLéger;
import bodies.ObstaclePoulie;
import bodies.ObstacleRotatif;
import bodies.ObstacleSuspendu;
import bodies.Plateforme;
import bodies.Polygone;
import bodies.Spring;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.one.button.jam.Couleurs;
import com.one.button.jam.MyGdxGame;

public class LecteurCarte {

    MapObjects objects;
	public Array<Obstacle> obstacles, obstaclesOrganisés;
	public Array<Polygone> polygones;
	public Array<Plateforme> plateformes;
	public Array<Spring> springs;
	public Array<Eau> waters;
	private Array<MapObject> poulies;
	public Balle balle;
	public Exit exit;
	OrthographicCamera camera;
	World world;
	public float eauPosX, eauPosY, cameraOrigineX, cameraOrigineY;
	private Couleurs couleurs;
    
	public LecteurCarte(final MyGdxGame game, TiledMap tiledMap, World world, OrthographicCamera camera2, Couleurs couleurs){
		this.camera = camera2;
		this.world = world;
		this.couleurs = couleurs;

		cameraOrigineX = camera2.position.x;
		cameraOrigineY = camera2.position.y;
		
		System.out.println("Position camera : " + cameraOrigineX + ", " + cameraOrigineY);
		Variables.PPT =  Integer.parseInt(tiledMap.getProperties().get("tileheight").toString());
		System.out.println("Variables.PPT = " + Variables.PPT);
		
		balle = new Balle(world, camera, tiledMap);
		objects = tiledMap.getLayers().get("Objects").getObjects();
		
		poulies = new Array<MapObject>();
		springs = new Array<Spring>();
		waters = new Array<Eau>();

        obstacles = new Array<Obstacle>();    
        obstaclesOrganisés = new Array<Obstacle>();        
        for (RectangleMapObject rectangleObject : objects.getByType(RectangleMapObject.class)) {
            if(rectangleObject.getProperties().get("type") != null){
            	//Objets légers
            	if(rectangleObject.getProperties().get("type").equals("Light")){
	            	ObstacleLéger obstacle = new ObstacleLéger(world, camera2, rectangleObject, couleurs);
	                obstacles.add(obstacle);
            	}
            	//Objets rotatifs
            	else if(rectangleObject.getProperties().get("type").equals("Revolving")){
	            	ObstacleRotatif obstacle = new ObstacleRotatif(world, camera2, rectangleObject, couleurs);
	                obstacles.add(obstacle);
            	}
            	//Objets ressorts
            	else if(rectangleObject.getProperties().get("type").equals("Swing")){
	            	ObstacleBalance obstacle = new ObstacleBalance(world, camera2, rectangleObject, couleurs);
	                obstacles.add(obstacle);
            	}
            	//Balançoires
            	else if(rectangleObject.getProperties().get("type").equals("Balançoire")){
	            	ObstacleBalançoire obstacle = new ObstacleBalançoire(world, camera2, rectangleObject, couleurs);
	                obstacles.add(obstacle);
            	}
            	//Obstacle Suspendu
            	else if(rectangleObject.getProperties().get("type").equals("Suspendu")){
	            	ObstacleSuspendu obstacle = new ObstacleSuspendu(world, camera2, rectangleObject, couleurs);
	                obstacles.add(obstacle);
            	}
            	//Poulies
            	else if(rectangleObject.getProperties().get("type").equals("Poulie")){
	            	poulies.add(rectangleObject);
	            	System.out.println("poulies.size = " + poulies.size);
            	}
            	//Eau
            	else if(rectangleObject.getProperties().get("type").equals("Water")){
	            	Eau eau = new Eau(world, camera2, rectangleObject, couleurs);
	                waters.add(eau);
            	}
            	//Spring
            	else if(rectangleObject.getProperties().get("type").equals("Spring")){
	            	Spring spring = new Spring(world, camera2, rectangleObject, couleurs);
	                springs.add(spring);
            	}
            	//Exit
            	else if(rectangleObject.getProperties().get("type").equals("Exit")){
	            	exit = new Exit(world, camera2, rectangleObject, couleurs);
	                obstacles.add(exit);
            	}
            }
            else{
            	Obstacle obstacle = new Obstacle(world, camera2, rectangleObject, couleurs);
                obstacles.add(obstacle);
            }
        }
        
        //Création des poulies
        for(int i = poulies.size - 1; i > -1; i--){
        	if(poulies.get(i).getProperties().get("Groupe") != null){
        		for(int j = 0; j < poulies.size; j++){
        			if(Integer.parseInt(poulies.get(i).getProperties().get("Groupe").toString()) == Integer.parseInt(poulies.get(j).getProperties().get("Groupe").toString()) &&
        					i != j){  				
        				ObstaclePoulie obstacle = new ObstaclePoulie(world, camera, poulies.get(i), couleurs, poulies.get(j));
        				obstacles.add(obstacle);
        				
        				poulies.removeIndex(i);
        				poulies.removeIndex(j);
        				i--;
        			}
        		}
        	}	
        	else
    			System.out.println("TEST");
        }

        //Création de polygones
        polygones = new Array<Polygone>();
        for(PolygonMapObject polygonObject : objects.getByType(PolygonMapObject.class)){
        	Polygone polygone = new Polygone(world, camera2, polygonObject, couleurs);
        	polygones.add(polygone);
        }

        //Plateformes mobiles
        plateformes = new Array<Plateforme>();      
        for(PolylineMapObject polylineObject : objects.getByType(PolylineMapObject.class)){
        	Plateforme plateforme = new Plateforme(game, world, polylineObject);
        	plateformes.add(plateforme);
        	
        }
        
        //Organisation des obstacles
        for(Obstacle obstacle : obstacles){
        	if(obstacle.getClass().toString().equals("class com.gravity.ball.body.ObstacleRessort"))
        		obstaclesOrganisés.add(obstacle);
        }  
        for(Obstacle obstacle : obstacles){      	
        	if(!obstacle.getClass().toString().equals("class com.gravity.ball.body.ObstacleRessort") && !obstacle.getClass().toString().equals("class com.gravity.ball.body.Eau") && !obstacle.getClass().toString().equals("class com.gravity.ball.body.Obstacle"))
        		obstaclesOrganisés.add(obstacle);
        } 
        for(Obstacle obstacle : obstacles){
        	if(obstacle.getClass().toString().equals("class com.gravity.ball.body.Eau")){
        		obstaclesOrganisés.add(obstacle);
        	}
        } 
        for(Obstacle obstacle : obstacles){
        	if(obstacle.getClass().toString().equals("class com.gravity.ball.body.Obstacle")){
        		obstaclesOrganisés.add(obstacle);
        	}
        }  
	}
	
	public Array<Obstacle> getObstacles(){
		return obstacles;
	}
	
	public Array<Plateforme> getPlateformes(){
		return plateformes;
	}
	
	public void draw(SpriteBatch batch, TextureAtlas textureAtlas){
		//drawOmbre(batch, textureAtlas);
		drawPlateforme(batch, textureAtlas);
		drawBalle(batch, textureAtlas);
		drawSpring(batch, textureAtlas);
		drawObstacle(batch, textureAtlas);
		drawWater(batch, textureAtlas);
	}
	
	public void drawBalle(SpriteBatch batch, TextureAtlas textureAtlas){  
        balle.draw(batch, textureAtlas, couleurs);        
	}		
	
	public void drawPlateforme(SpriteBatch batch, TextureAtlas textureAtlas){  
        for(Plateforme plateforme : plateformes){
        	plateforme.draw(batch, textureAtlas, couleurs);
        }         
	}	
	
	public void drawSpring(SpriteBatch batch, TextureAtlas textureAtlas){
        for(Spring spring : springs){
        	spring.draw(batch, textureAtlas);
        }         
	}
	
	public void drawWater(SpriteBatch batch, TextureAtlas textureAtlas){
        for(Eau water : waters){
        	water.draw(batch, textureAtlas);
        }         
	}
	
	public void drawObstacle(SpriteBatch batch, TextureAtlas textureAtlas){
		 for(Obstacle obstacle : obstaclesOrganisés)
			 obstacle.draw(batch, textureAtlas);
		/*
        for(Obstacle obstacle : obstacles){
        	if(obstacle.getClass().toString().equals("class com.gravity.ball.body.ObstacleRessort")){
        		obstacle.draw(batch, textureAtlas);
        	}
        }  
        for(Obstacle obstacle : obstacles){      	
        	if(!obstacle.getClass().toString().equals("class com.gravity.ball.body.ObstacleRessort") && !obstacle.getClass().toString().equals("class com.gravity.ball.body.Eau"))
            	obstacle.draw(batch, textureAtlas);
        } 
        for(Obstacle obstacle : obstacles){
        	if(obstacle.getClass().toString().equals("class com.gravity.ball.body.Eau")){
        		obstacle.draw(batch, textureAtlas);
        	}
        }  
        */   
	}
	
	public void drawPolygone(PolygonSpriteBatch batch, MyCamera camera){  
        for(Polygone polygone : polygones){
        	polygone.setPos(cameraOrigineX - camera.position.x, cameraOrigineY - camera.position.y);
        	polygone.draw(batch, couleurs);
        }         
	}
	
	public void drawPolygoneOmbre(PolygonSpriteBatch batch, MyCamera camera){  
        for(Polygone polygone : polygones){
        	polygone.setPos(cameraOrigineX - camera.position.x + Variables.ombresX, cameraOrigineY - camera.position.y + Variables.ombresY);
        	polygone.draw(batch, couleurs);
        }         
	}
	
	public void drawOmbre(SpriteBatch batch, TextureAtlas textureAtlas){
		balle.drawOmbre(batch, textureAtlas);
		
		for(Plateforme plateforme : plateformes){
        	plateforme.drawOmbre(batch, textureAtlas);
        } 
		
		for(Obstacle obstacle : obstacles){
        	obstacle.drawOmbre(batch, textureAtlas);
        } 
	}
	
	public void activity(){
		for(Plateforme plateforme : plateformes)
			plateforme.déplacement();
		for(Spring spring : springs)
			spring.activity();
		for(Obstacle obstacle : obstacles)
        	obstacle.activity();
		for(Eau water : waters)
        	water.activity();
		balle.activity();
	}
	
	public void restart(){
		Variables.fallRestartDelay = 2.136f;
		balle.restart();
		for(Spring spring : springs)
			spring.initiate();
		for(Obstacle obstacle : obstacles)
        	obstacle.initiate();
	}
}
