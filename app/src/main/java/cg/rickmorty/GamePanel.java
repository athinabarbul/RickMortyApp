package cg.rickmorty;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static cg.rickmorty.R.drawable;


public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {

    public static final int WIDTH = 856;
    public static final int HEIGHT = 480;
    public static final int MOVESPEED = -5;
    private long missileStartTime;
    private MainThread thread;
    private Background bg;
    private Player player;
    private ArrayList<Missile> missiles;
    private ArrayList<TopBorder> topBorders;
    private ArrayList<BotBorder> botBorders;
    private Random rand = new Random();
    private int maxBorderHeight;
    private int minBorderHeight;
    private int progressDenom = 20;
    private boolean topDown = true;
    private boolean botDown = true;
    private boolean newGameCreated;
    private SoundPlayer sound;
    private Explosion explosion;
    private long startReset;
    private boolean reset;
    private boolean dissapear;
    private boolean started;


    public GamePanel(Context context) {
        super(context);


        //add the callback to the surfaceholder to intercept events
        getHolder().addCallback(this);

        //thread = new MainThread(getHolder(), this);

        //make gamePanel focusable so it can handle events
        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        int counter = 0;
        while (retry && counter < 1000) {
            try {
                counter++;
                thread.setRunning(false);
                thread.join();
                retry = false;
                thread = null;

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        bg = new Background(BitmapFactory.decodeResource(getResources(), drawable.background));
        player = new Player(BitmapFactory.decodeResource(getResources(), drawable.rick), 115, 90, 3);

        sound = new SoundPlayer(getContext());
        missiles = new ArrayList<Missile>();
        missileStartTime = System.nanoTime();
        topBorders = new ArrayList<>();
        botBorders = new ArrayList<>();


        thread = new MainThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();

    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            if (!player.getPlaying() && newGameCreated && reset) {
                player.setPlaying(true);
                player.setUp(true);
            }
            if (player.getPlaying()) {

                if (!started) started = true;
                reset = false;
                player.setUp(true);
            }
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {

            player.setUp(false);
            return true;
        }
        return super.onTouchEvent(event);
    }

    public boolean collision(GameObject a, GameObject b) {
        if (Rect.intersects(a.getRectangle(), b.getRectangle())) {
            return true;
        }
        return false;
    }

    public void update() throws IOException {

        if (player.getPlaying()) {

            if (botBorders.isEmpty()) {
                player.setPlaying(false);
                return;
            }
            if (topBorders.isEmpty()) {
                player.setPlaying(false);
                return;
            }

            bg.update();
            player.update();

            maxBorderHeight = 30 + player.getScore() / progressDenom;

            if (maxBorderHeight > HEIGHT / 4)
                maxBorderHeight = HEIGHT / 4;

            minBorderHeight = 5 + player.getScore() / progressDenom;


            //check bottom border collision
            for (int i = 0; i < botBorders.size(); i++) {
                if (collision(botBorders.get(i), player)) {

                    sound.playOverSound();
                    player.setPlaying(false);
                }

            }

            //check top border collision
            for (int i = 0; i < topBorders.size(); i++) {
                if (collision(topBorders.get(i), player)) {

                    sound.playOverSound();
                    player.setPlaying(false);
                }
            }


            //update top border
            this.updateTopBorder();
            //update bottom border
            this.updateBottomBorder();


            //add missiles on timer
            long missileElapsed = (System.nanoTime() - missileStartTime) / 1000000;
            if (missileElapsed > (2000 - player.getScore() / 4)) {

                System.out.println("making missile");
                //first missile always goes down the middle
                if (missiles.size() == 0) {
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(), drawable.
                            pickle), WIDTH + 10, (int) (rand.nextDouble() * (HEIGHT - (maxBorderHeight * 2)) + maxBorderHeight), 65, 80, player.getScore(), 10));
                } else {

                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(), drawable.pickle),
                            WIDTH + 10, (int) (rand.nextInt(HEIGHT)), 65, 80, player.getScore(), 10));
//                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(), drawable.pickle2),
//                            WIDTH + 10, (int) (rand.nextInt(HEIGHT)), 64, 80, player.getScore(), 10));
                }

                //reset timer
                missileStartTime = System.nanoTime();
            }
            //loop through every missile and check collision and remove
            for (int i = 0; i < missiles.size(); i++) {

                //update missile
                missiles.get(i).update();

                if (collision(missiles.get(i), player)) {

                    sound.playHitSound();
                    missiles.remove(i);
                    player.setPlaying(false);
                    break;
                }
                //remove missile if it is way off the screen
                if (missiles.get(i).getX() < -100) {
                    missiles.remove(i);
                    break;
                }
            }


        } else {
            player.resetDY();
            if (!reset) {
                newGameCreated = false;
                startReset = System.nanoTime();
                reset = true;
                dissapear = true;
                explosion = new Explosion(BitmapFactory.decodeResource(getResources(), drawable.explosion), player.getX(),
                        player.getY() - 30, 100, 100, 25);
            }

            explosion.update();
            long resetElapsed = (System.nanoTime() - startReset) / 1000000;

            if (resetElapsed > 2500 && !newGameCreated) {
                newGame();
            }
        }


    }


    @SuppressLint("MissingSuperCall")
    @Override
    public void draw(Canvas canvas) {
        final float scaleFactorX = getWidth() / (WIDTH * 1.f);
        final float scaleFactorY = getHeight() / (HEIGHT * 1.f);
        if (canvas != null) {
            final int savedState = canvas.save();
            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);
            if (!dissapear) {
                player.draw(canvas);
            }
            //draw missiles
            for (Missile m : missiles) {
                m.draw(canvas);
            }


            //draw topborder

            for (TopBorder tb : topBorders) {
                tb.draw(canvas);
            }


            //draw bottomborder

            for (BotBorder bb : botBorders) {
                bb.draw(canvas);
            }
            //draw explosion
            if (started) {
                explosion.draw(canvas);
            }
            drawText(canvas);
            canvas.restoreToCount(savedState);
        }
    }

    public void updateBottomBorder() {
        if (player.getScore() % 50 == 0) {
            botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), drawable.brick),
                    botBorders.get(botBorders.size() - 1).getX() + 20, (int) ((rand.nextDouble()
                    * maxBorderHeight) + (HEIGHT - maxBorderHeight))));
        }
        //update bottom border
        for (int i = 0; i < botBorders.size(); i++) {
            botBorders.get(i).update();

            //if border is moving off screen, remove it and add a corresponding new one
            if (botBorders.get(i).getX() < -20) {
                botBorders.remove(i);


                //determine if border will be moving up or down
                if (botBorders.get(botBorders.size() - 1).getY() <= HEIGHT - maxBorderHeight) {
                    botDown = true;
                }
                if (botBorders.get(botBorders.size() - 1).getY() >= HEIGHT - minBorderHeight) {
                    botDown = false;
                }

                if (botDown) {
                    botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), drawable.brick
                    ), botBorders.get(botBorders.size() - 1).getX() + 20, botBorders.get(botBorders.size() - 1
                    ).getY() + 1));
                } else {
                    botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), drawable.brick
                    ), botBorders.get(botBorders.size() - 1).getX() + 20, botBorders.get(botBorders.size() - 1
                    ).getY() - 1));
                }
            }
        }

    }

    public void updateTopBorder() {
        if (player.getScore() % 40 == 0) {
            topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(), drawable.brick
            ), topBorders.get(topBorders.size() - 1).getX() + 20, 0, (int) ((rand.nextDouble() * (maxBorderHeight
            )) + 1)));

        }

        for (int i = 0; i < topBorders.size(); i++) {
            topBorders.get(i).update();
            if (topBorders.get(i).getX() < -20) {
                topBorders.remove(i);

                if (topBorders.get(topBorders.size() - 1).getHeight() >= maxBorderHeight) {
                    topDown = false;

                }
                if (topBorders.get(topBorders.size() - 1).getHeight() <= minBorderHeight) {
                    topDown = true;

                }

                if (topDown) {
                    topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),
                            drawable.brick), topBorders.get(topBorders.size() - 1).getX() + 20,
                            0, topBorders.get(topBorders.size() - 1).getHeight() + 1));

                } else {
                    topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),
                            drawable.brick), topBorders.get(topBorders.size() - 1).getX() + 20,
                            0, topBorders.get(topBorders.size() - 1).getHeight() - 1));
                }

            }

        }
    }


    public void newGame() {
        dissapear = false;


        botBorders.clear();
        topBorders.clear();
        missiles.clear();


        minBorderHeight = 5;
        maxBorderHeight = 30;

        player.resetDY();
//            player.resetScore();
        player.setY(HEIGHT / 2);

        if (player.getScore() > getRecord()) {
            setRecord(player.getScore());

        }

        player.resetScore();
        //create initial borders

        //initial top border
        for (int i = 0; i * 20 < WIDTH + 40; i++) {
            //first top border create
            if (i == 0) {
                topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(), drawable.brick
                ), i * 20, 0, 10));
            } else {
                topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(), drawable.brick
                ), i * 20, 0, topBorders.get(i - 1).getHeight() + 1));
            }
        }
        //initial bottom border
        for (int i = 0; i * 20 < WIDTH + 40; i++) {
            //first border ever created
            if (i == 0) {
                botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), drawable.brick)
                        , i * 20, HEIGHT - minBorderHeight));
            }
            //adding borders until the initial screen is filed
            else {
                botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), drawable.brick),
                        i * 20, botBorders.get(i - 1).getY() - 1));
            }
        }

        newGameCreated = true;


    }

    public void drawText(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        canvas.drawText("DISTANCE: " + (player.getScore() * 3), 10, HEIGHT - 10, paint);
        canvas.drawText("BEST: " + (getRecord() * 3), WIDTH - 215, HEIGHT - 10, paint);

        if (!player.getPlaying() && newGameCreated && reset) {
            Paint paint1 = new Paint();
            paint1.setColor(Color.WHITE);
            paint1.setTextSize(40);
            paint1.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            canvas.drawText("PRESS TO START", WIDTH / 2 - 50, HEIGHT / 2, paint1);

            paint1.setTextSize(20);
            canvas.drawText("PRESS AND HOLD TO GO UP", WIDTH / 2 - 50, HEIGHT / 2 + 20, paint1);
            canvas.drawText("RELEASE TO GO DOWN", WIDTH / 2 - 50, HEIGHT / 2 + 40, paint1);
        }
    }

    public int getRecord() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        return prefs.getInt("record", 0);
    }

    public void setRecord(int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("record", value);
        editor.commit();
    }


}















