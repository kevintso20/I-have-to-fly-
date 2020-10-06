package com.example.dodgingbirds;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable{

    private Thread thread;
    private Boolean isPlayng , isGameOver = false;
    private Background background1 , background2;
    private int screenX , screenY , score = 0;
    private Paint paint;
    public static float screenRatioX, screenRatioY;
    private Flight flight;
    private List<Bullet> bullets;
    private Bird[] birds;
    private Random random;
    private SharedPreferences sharedPreferences ;
    private GameActivity activity;

    public GameView(GameActivity activity , int screenX, int screenY) {
        super(activity);

        this.activity = activity;
        sharedPreferences =  activity.getSharedPreferences("game", Context.MODE_PRIVATE);
        this.screenX = screenX;
        this.screenY = screenY;
        screenRatioX = 1920f / screenX;
        screenRatioY = 1080f / screenY;


        background1 = new Background(screenX , screenY , getResources());
        background2 = new Background(screenX , screenY , getResources());

        flight  = new Flight(this,screenY ,getResources());

        bullets = new ArrayList<>();

        background2.x = screenX;

        paint = new Paint();
        paint.setTextSize(128);
        paint.setColor(Color.WHITE);

        birds = new Bird[4];

        for(int i = 0; i < 4; i++ ){
            Bird bird = new Bird(getResources());
            birds[i] = bird;
        }

        random = new Random();

        hideNavigationBar(this.activity);

    }

    @Override
    public void run() {
        while (isPlayng){
            update();
            draw();
            sleep();
        }
    }

    public void resume(){
        isPlayng = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause(){
        try {
            isPlayng = false;
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void update(){
        background1.x -= 10 * screenRatioX;
        background2.x -= 10 * screenRatioX;

        if(background1.x + background1.background.getWidth() < 0 ){
            background1.x = screenX;
        }

        if(background2.x + background2.background.getWidth() < 0 ){
            background2.x = screenX;
        }

        if (flight.isGoingUp)
            flight.y -= 30 * screenRatioY;
        else
            flight.y += 30 * screenRatioY;

        if (flight.y < 0)
            flight.y = 0;

        if (flight.y >= screenY - flight.height)
            flight.y = screenY - flight.height;

        List<Bullet> thrash = new ArrayList<>();

        for(Bullet bullet : bullets){
            if(bullet.x > screenX) { thrash.add(bullet); }

            bullet.x += 50 * screenRatioX;

            for(Bird bird : birds){
                if(Rect.intersects(bird.getCollisionShape() , bullet.getCollisionShape()) ){
                    score++;
                    bird.x = -500;
                    bullet.x = screenX + 500;
                    bird.wasShot = true;
                }
            }
        }

        for(Bullet bullet : thrash) {
            bullets.remove(bullet);
        }

        for(Bird bird : birds){
            bird.x -= bird.speed;
            if(bird.x + bird.width < 0){

                if(!bird.wasShot){
                    isGameOver = true;
                    return;
                }

                int bound = (int) (30 * screenRatioX);
                bird.speed = random.nextInt(bound);

                if(bird.speed < 10 * screenRatioX) bird.speed = (int) (10 * screenRatioX);

                bird.x = screenX;
                bird.y = random.nextInt(screenY - bird.height);

                bird.wasShot = false;
            }
            if(Rect.intersects(bird.getCollisionShape() , flight.getCollisionShape())){
                isGameOver = true;
            }
        }

}


    private void draw(){
        if(getHolder().getSurface().isValid()){
            Canvas canvas = getHolder().lockCanvas();
            canvas.drawBitmap(background1.background , background1.x, background1.y,paint);
            canvas.drawBitmap(background2.background , background2.x, background2.y,paint);

            for(Bird bird : birds){
                canvas.drawBitmap(bird.getbird(), bird.x, bird.y ,paint);
            }

            canvas.drawText(score + "", screenX / 2f , 164 , paint);

            if(isGameOver){
                isPlayng = false;
                canvas.drawBitmap(flight.getDead() , flight.x , flight.y ,paint);
                paint.setTextSize(150);
                paint.setColor(Color.BLACK);
                canvas.drawText( "Game Over", screenX / 3 , screenY / 2f , paint);
                getHolder().unlockCanvasAndPost(canvas);
                saveIfHightScore();
                waitBeforeExiting();
                return;
            }

            canvas.drawBitmap(flight.getFlight(), flight.x, flight.y ,paint);

            for (Bullet bullet : bullets) canvas.drawBitmap(bullet.bullet , bullet.x , bullet.y , paint);

            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    private void waitBeforeExiting() {
        try {
            Thread.sleep(2750);
            activity.startActivity(new Intent(activity,MainActivity.class));
            activity.finish();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void saveIfHightScore() {
        if(sharedPreferences.getInt("highscore" , 0 ) < score){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("highscore" , score);
            editor.apply();
        }
    }

    private void sleep(){
        try {
            Thread.sleep(17);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getX() < screenX / 2) {
                    flight.isGoingUp = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                flight.isGoingUp = false;
                if (event.getX() > screenX / 2)
                    flight.tooShoot++;
                break;
        }

        return true;
    }

    public void newBullet() {
        Bullet bullet =  new Bullet(getResources());
        bullet.x = flight.x + flight.width;
        bullet.y = flight.y + (flight.height / 2);
        bullets.add(bullet);
    }


    public void hideNavigationBar(GameActivity activity){
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

    }


}
