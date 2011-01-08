package org.apelikecoder.bulgariankeyboard2;

import android.content.res.XmlResourceParser;
import android.util.Log;
import android.view.KeyCharacterMap;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 1/3/11
 * Time: 4:08 PM
 */
public class Mapper {
    private static final String TAG = "Mapper";

    private HashMap<Character, Character> map;
    private HashMap<String, Character> composers;
    private KeyCharacterMap charMap;

    public Mapper(XmlResourceParser xrp) {
        charMap = KeyCharacterMap.load(KeyCharacterMap.BUILT_IN_KEYBOARD);

        map = new HashMap<Character, Character>();
        composers = new HashMap<String, Character>();
        try {
            int current = xrp.getEventType();
            while (current != XmlResourceParser.END_DOCUMENT) {
                if (current == XmlResourceParser.START_TAG) {
                    String tag = xrp.getName();
                    if (tag != null) {
                        String key, value;
                        key = xrp.getAttributeValue(null, "name");
                        value = xrp.getAttributeValue(null, "value");
                        if (tag.equals("key"))
                            map.put(key.charAt(0), value.charAt(0));
                        else if (tag.equals("compose"))
                            composers.put(key, value.charAt(0));
                    }
                }
                xrp.next();
                current = xrp.getEventType();
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XML parsing failure");
        } catch (IOException e) {
            Log.e(TAG, "XML IOException");
        }
//        for (Character c : map.keySet())
//            System.out.println(c + " (" + (int)c + ") " + map.get(c));
//        for (String s : composers.keySet())
//            System.out.println(s + " " + composers.get(s));
    }

    public char getComposed(String composer) {
//        System.out.println(composers.get(composer) + " " + composer);
        Character c = composers.get(composer);
        return c != null ? c : 0;
    }

    public char getMappedChar(int keyCode) {
        int c = charMap.get(keyCode, 0);
        Character m = map.get((char) c);
        //System.out.println("mmmmm " + keyCode + " " + m + " " + c);
        return m != null ? m : 0;
    }
}

