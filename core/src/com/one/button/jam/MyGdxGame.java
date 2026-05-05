package com.one.button.jam;

import screen.LoadingScreen;
import utils.Data;
import utils.DebugConfig;
import utils.Variables;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MyGdxGame extends Game implements ApplicationListener {
	public SpriteBatch batch;
	public AssetManager assets;

	@Override
	public void create () {
		//Data.Load();
		//Data.setLevel(1);

		batch = new SpriteBatch();
		assets = new AssetManager();
		if(DebugConfig.isEnabled()){
			Variables.niveauSelectione = Math.max(1, DebugConfig.startLevel);
			DebugConfig.log("game create startLevel=" + Variables.niveauSelectione
					+ " autoAdvance=" + DebugConfig.autoAdvanceLevels
					+ " delay=" + DebugConfig.autoAdvanceDelay);
		}

		this.setScreen(new LoadingScreen(this));
	}

	@Override
	public void render () {
		super.render();
	}

	@Override
	public void dispose () {
		if(getScreen() != null)
			getScreen().dispose();
		if(batch != null)
			batch.dispose();
		if(assets != null)
			assets.dispose();
	}
}
