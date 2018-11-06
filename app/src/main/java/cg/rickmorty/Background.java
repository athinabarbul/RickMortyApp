package cg.rickmorty;

import android.graphics.Bitmap;
import android.graphics.Canvas;

class Background {

    private Bitmap image;
    private int x, y, dx;

    Background(Bitmap res) {

        image = res;
        dx = GamePanel.MOVESPEED;
    }

    void update() {
        x += dx;
        if (x < -GamePanel.WIDTH) {
            x = 0;
        }
    }

    void draw(Canvas canvas) {
        canvas.drawBitmap(image, x, y, null);
        if (x < 0) {
            canvas.drawBitmap(image, x + GamePanel.WIDTH, y, null);
        }
    }


}