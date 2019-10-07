package com.one.button.jam.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.one.button.jam.MyGdxGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.samples = 4; 				//Antialiasing
		config.title = "Epic Ball Adventure";
	    config.width = 1024;
	    config.height = 720;
		new LwjglApplication(new MyGdxGame(), config);
	}
}
