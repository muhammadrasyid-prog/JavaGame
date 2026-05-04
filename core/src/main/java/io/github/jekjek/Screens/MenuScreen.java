package io.github.jekjek.Screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.jekjek.Main;

public class MenuScreen implements Screen {
    Main game;

    public MenuScreen(Main game){
        this.game = game;
    }

    @Override
    public void render(float delta){
        ScreenUtils.clear(0, 0, 0, 1);

        // justTouched() hanya terpicu SEKALI per klik
        if(Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)){
            game.setScreen(new GameScreen(game));
        }
    }

    public void show(){}
    public void resize(int w,int h){}
    public void pause(){}
    public void resume(){}
    public void hide(){}
    public void dispose(){}
}
