package com.one.button.jam;

import screen.LoadingScreen;
import utils.Data;

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
		
		this.setScreen(new LoadingScreen(this));
	}

	@Override
	public void render () {
		super.render();
	}
	
	@Override
	public void dispose () {

	}
}
