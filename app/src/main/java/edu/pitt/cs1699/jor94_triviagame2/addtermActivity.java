/*
* By: Joshua Rodstein
* Assignment1 - CS1699
* PItt: jor94@pitt.edu
* ID: 4021607
*
* */


package edu.pitt.cs1699.jor94_triviagame2;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.FileOutputStream;
import java.io.IOException;

public class addtermActivity extends Activity {
    DatabaseHelper db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addterm);
        db = new DatabaseHelper(this);
    }


    public void ok_onClick(View view){
        EditText term = (EditText) findViewById(R.id.addterm_edittext);
        EditText def = (EditText) findViewById(R.id.adddef_edittext);

        String termString = term.getText().toString();
        String defString = def.getText().toString();

        if(!termString.equals("") && !defString.equals("")){
           db.addTermToFB(new TermAndDef(termString, defString));
        }

        finish();
    }


}
