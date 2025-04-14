package hu.egyetem.vizilabdapp.utils;

import android.content.Context;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class PicassoHelper {

    /**
     * Kép betöltése egy adott URL-ről egy ImageView-ba.
     *
     * @param context   Az alkalmazás kontextusa
     * @param imageUrl  A kép URL-je
     * @param imageView Az a ImageView, ahová a kép betöltésre kerül
     */
    public static void loadImage(Context context, String imageUrl, ImageView imageView) {
        Picasso.get()
                .load(imageUrl)         // A kép URL-je
                .placeholder(android.R.drawable.ic_menu_gallery) // Helyőrző kép, amíg az letöltés folyamatban van
                .error(android.R.drawable.ic_delete) // Hibakép, ha a letöltés nem sikerül
                .into(imageView);       // Töltse be a megadott ImageView-ba
    }

    /**
     * Kép betöltése adott szélesség és magasság beállításával.
     *
     * @param context   Az alkalmazás kontextusa
     * @param imageUrl  A kép URL-je
     * @param imageView Az a ImageView, ahová a kép betöltésre kerül
     * @param width     A kívánt szélesség pixelben
     * @param height    A kívánt magasság pixelben
     */
    public static void loadImageWithSize(Context context, String imageUrl, ImageView imageView, int width, int height) {
        Picasso.get()
                .load(imageUrl)
                .resize(width, height)  // Kép átméretezése a megadott méretekre
                .centerCrop()           // A kép középre illesztése
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(imageView);
    }
}