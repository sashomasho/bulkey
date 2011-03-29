package org.apelikecoder.bulgariankeyboard2;

import android.app.TabActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TabHost;

public class StartMeUp extends TabActivity  {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);

        TabHost tabHost = getTabHost();
        
        LayoutInflater.from(this).inflate(R.layout.main, tabHost.getTabContentView(), true);

        tabHost.addTab(tabHost.newTabSpec("Setup")
                .setIndicator("Setup"/*,
                        getResources().getDrawable(android.R.drawable.ic_menu_preferences)*/)
                .setContent(R.id.tab1));
        tabHost.addTab(tabHost.newTabSpec("Help")
                .setIndicator("Help"/*,
                        getResources().getDrawable(android.R.drawable.ic_menu_help)*/)
                .setContent(R.id.tab2));

        Button btn = (Button) findViewById(R.id.btnGoToSettings);
        btn.requestFocus();
        btn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    //Toast.makeText(StartMeUp.this, R.string.input_method_nointent, Toast.LENGTH_LONG).show();
                }
            }
        });

        btn = (Button)findViewById(R.id.btnInputMethod);
        btn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                InputMethodManager mgr = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                mgr.showInputMethodPicker();
            }
        });
    }
}
