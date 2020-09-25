package net.wurmunlimited.forge.client;

import com.wurmonline.client.options.Options;

import java.awt.*;


public class ClientWindow {

    public static Rectangle getWindowedFullscreenSizeAndPosition() {
        final boolean maximizedWindow = Options.screenSettings.width==0 && Options.screenSettings.height==0;
        if(maximizedWindow) return null;
        return new Rectangle(Options.screenSettings.width,Options.screenSettings.height);
    }

/*   public static boolean initFullscreen() {
      if(!Options.screenSettings.maximized) {
         Options.screenSettings.fullscreen = false;
         return false;
      }
      return true;
//      if(Options.screenSettings.fullscreen && !Options.screenSettings.maximized) {
//         int w = Options.screenSettings.width;
//         int h = Options.screenSettings.height;
//         try {
//            Display.setDisplayMode(new DisplayMode(w,w));
//
//         } catch(LWJGLException e) {
//            AwakeningClientMod.severe(e.getMessage(),e);
//         }
//      }
//      return false;
   }*/
}
