package utils;

import com.badlogic.gdx.Gdx;

public class Variables {
	
	//Constantes du World
	public static float WORLD_TO_BOX = 0.05f;
	public static float BOX_TO_WORLD = 1/WORLD_TO_BOX;
	public static float BOX_STEP = 1/60f; 
	public static int BOX_VELOCITY_ITERATIONS = 6;
	public static int BOX_POSITION_ITERATIONS = 2;
	public static float GRAVITÉ = -58f;
	public static float DENSITÉ = 0.05f;
	
	//Constantes de la Tiled Map
	public static int PPT = 32;
	
	//Game Constants
	public static boolean levelComplete;
	public static String gameTitle = "Epic Ball Adventure";
	public static float fallRestartDelay = 2.136f;
	
	//Graphismes
	public static float ombresX = (float)Gdx.graphics.getWidth()/800;
	public static float ombresY =  - (float)Gdx.graphics.getHeight()/400;
	
	//Gestion des niveaux
	public static int nombreNiveaux = 5;
	public static int niveauSelectione = 1;
	public static float objectif = 70;
	public static int couleurSelectionee = 1;
	
	public static boolean début = true;
	public static boolean pause = true;
	public static boolean perdu = true;
	public static boolean gagné = true;
	public static boolean restart = false;
	public static int INTERSTITIAL_TRIGGER = 2;
	
	//Propriétés de la balle
	public static float vitesseBalle = 6.2f;
	public static float vitesseMaxBalle = 60f;
	public static float sautBalle = 220f;
	
	
	
	
}
