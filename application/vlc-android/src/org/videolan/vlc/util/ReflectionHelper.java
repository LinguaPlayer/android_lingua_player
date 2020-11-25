package org.videolan.vlc.util;

import org.videolan.libvlc.MediaPlayer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ReflectionHelper {
    public static MediaPlayer.TrackDescription instantiateTrackDescription(int id, String name){
        Constructor<MediaPlayer.TrackDescription> constructor;
        try {
            constructor = MediaPlayer.TrackDescription.class.getDeclaredConstructor(int.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(id, name);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
