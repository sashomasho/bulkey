package org.apelikecoder.bulgariankeyboard2;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.Keyboard;
import android.util.Log;

public class MyKeyboard extends LatinKeyboard {
    
    private static final String TAG_KEY = "Key";
    public static final int NEXT_KEYBOARD = -12;

    private int extraheight;

    public MyKeyboard(Context context, int xmlLayoutResId, int mode, boolean showArrows) {
        super(context, xmlLayoutResId, mode);
        if (showArrows) {
            final Resources res = context.getResources();
            for (Key k : getKeys()) {
                if ((k.edgeFlags & Keyboard.EDGE_BOTTOM) != 0)
                    k.edgeFlags ^= Keyboard.EDGE_BOTTOM;
            }
            Row r = createRowFromXml(res, res.getXml(R.xml.arrows_row));
            loadArrowRow(context, res.getXml(R.xml.arrows_row), r);
        }
    }

    private void loadArrowRow(Context context, XmlResourceParser parser, Row row) {
        boolean inKey = false;
        int x = 0;
        Key key = null;
        Resources res = context.getResources();
        
        try {
            int event;
            while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    String tag = parser.getName();
                    if (TAG_KEY.equals(tag)) {
                        inKey = true;
                        key = createKeyFromXml(res, row, x, super.getHeight(), parser);
                        getKeys().add(key);
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKey) {
                        inKey = false;
                        x += key.gap + key.width;
                        extraheight = Math.max(extraheight, key.height);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("XXXX", "Parse error:" + e);
            e.printStackTrace();
        }
    }

    @Override
    public int getHeight() {
        return super.getHeight() + extraheight;
    }
}
