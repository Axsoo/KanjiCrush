package com.example.kanjicrush2;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Declare object for the gridView
    private GridView mGridView;
    public static final String msg = "Android : ";

    // Game-specific variables
    private static final int[][] mLevelDimensions = {{3,4},{3,6},{6,4},{6,5},{6,6}};
    private static int ROWS;
    private static int COLUMNS;
    private static int DIMENSIONS;

    // non-constant variables
    private static int mColumnWidth, mColumnHeight;
    private static int mLevel = 0;
    private static int mNumWords = 4;

    // Declaration the local lists
    private static String[] mJukuList;
    private static String[] mKanjiList;
    private static int[] mButtonStateList;

    private void advanceLevel(){
        Log.d(msg, "The board is complete, next level can be loaded");
        if (mLevel < 4){
            mLevel++;
        } else {
            // reset if limit reached
            mLevel = 0;
        }
        mNumWords = 4 + mLevel*2;
        setRowsAndColumns();
        initLists();
        initView();
        initText();
        setDimensions();
    }

    /** Checks for completed lines and updates status */
    private void checkForCompletedLine(){
        Log.d(msg,"checkForCompletedLine() called:");
        for (int i = 0; i < mKanjiList.length/3; i++){
            String candidate;
            if (i >= COLUMNS){
                //Log.d(msg,"Second row");
                candidate = mKanjiList[i+(2*COLUMNS)] + mKanjiList[i+(3*COLUMNS)] + mKanjiList[i+(4*COLUMNS)];
            } else {
                //Log.d(msg,"First row");
                candidate = mKanjiList[i] + mKanjiList[i+COLUMNS] + mKanjiList[i+2*COLUMNS];
            }
            //Log.d(msg,candidate);
            for (String juku : mJukuList) {
                if (candidate.equals(juku)) {
                    if (i >= COLUMNS) {
                        mButtonStateList[i + (2 * COLUMNS)] = 2;
                        mButtonStateList[i + (3 * COLUMNS)] = 2;
                        mButtonStateList[i + (4 * COLUMNS)] = 2;
                    } else {
                        mButtonStateList[i] = 2;
                        mButtonStateList[i + COLUMNS] = 2;
                        mButtonStateList[i + 2 * COLUMNS] = 2;
                    }
                    // One more line is completed
                    updateButtonBackgrounds();
                    checkForCompletedBoard();
                }
            }
        }
    }

    /** Checks for completed board */
    private void checkForCompletedBoard() {
        Log.d(msg,"checkForCompletedBoard() called");
        int numCompleted = 0;
        for (int i = 0; i < DIMENSIONS; i++) {
            if (mButtonStateList[i] == 2) numCompleted++;
        }
        if (numCompleted == DIMENSIONS){
            // The board is complete
            advanceLevel();
        }
    }

    /** Checks for 2 selected chips */
    private void checkForSelectedChips() {
        Log.d(msg, "checkForSelectedChips() called");
        int count = 0;
        for (int state : mButtonStateList) {
            if (state == 1) {
                count++;
                if (count > 1) {
                    swapChips();
                }
            }
        }
        updateButtonBackgrounds();
    }

    /** Makes an ArrayList of button-objects from context and sends to our Adapter */
    private void display(Context context) {
        Log.d(msg,"display() called");
        // This sends the view to the adapter which actually puts it on the screen
        mGridView.setAdapter(new CustomAdapter(getButtons(context), mColumnWidth, mColumnHeight));
    }

    /** Initialize the buttons and put them in a list, which is returned */
    private ArrayList<Button> getButtons(Context context) {
        Log.d(msg,"getButtons() called");
        ArrayList<Button> buttons = new ArrayList<>();
        for (int i = 0; i < DIMENSIONS; i++) {
            if (ROWS/3 > 1) {
                // Add extra row of empty invisible buttons
                if (i == DIMENSIONS/2){
                    Log.d(msg,"i = " + i);
                    Log.d(msg,"we are in the beginning of the middle row");
                    Button button= new Button(context);
                    button.setVisibility(Button.INVISIBLE);
                    button.setEnabled(false);
                    for(int j=0; j < COLUMNS; j++){
                        Log.d(msg,"Adding button + " + j);
                        buttons.add(button);
                    }
                }
            }
            final Button button= new Button(context);
            button.setId(i);

            // Set default text
            String button_text = mKanjiList[button.getId()];
            button.setText(button_text);
            button.setTextColor(Color.parseColor("#FFFFFFFF"));
            button.setTextSize(35);

            // set selector
            button.setBackgroundResource(R.drawable.button_selector_default);

            /* TODO: make the listener a separate class */
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(msg, "onClick() called @ id " + button.getId());
                    if (mButtonStateList[button.getId()] == 0) mButtonStateList[button.getId()] = 1;
                    else mButtonStateList[button.getId()] = 0;
                    //logStateList();
                    checkForSelectedChips();
                }
            });
            buttons.add(button);
        }
        return buttons;
    }

    /** Returns the list of the juku */
    private String[] getJukuList(int numWords) {
        InputStream inputStream = getResources().openRawResource(R.raw.jukugo);
        CSVFile mCSVFile = new CSVFile(inputStream);

        // Get the list of words as string array
        List myList = mCSVFile.read();
        String[] myStringList = (String[]) myList.get(0);

        // Get 3 random words
        String[] mRandomList = new String[numWords];
        Random random = new Random();
        for (int i=0; i < numWords; i++) {
            mRandomList[i] = myStringList[random.nextInt(myStringList.length)];
            /* TODO: check for duplicates in mRandomList when adding next random word */
        }
        return mRandomList;
    }

    /* TODO: make getKanjiList more elegant */
    /** Breaks up the 3-char words into a list of single-char strings */
    private String[] getKanjiList(String[] jukuList) {
        mKanjiList = new String[jukuList.length*3];
        for (int i = 0; i < mKanjiList.length; i+=3) {
            for(int j = 0; j < jukuList[0].length(); j++) {
                mKanjiList[i+j] = String.valueOf(jukuList[i/jukuList[0].length()].charAt(j));
            }
        }
        return mKanjiList;
    }

    /** Returns the height of the status bar */
    private int getStatusBarHeight(Context context){
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height",
                "dimen",
                "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void initDebugButton() {
        Button debugButton;
        debugButton = findViewById(R.id.btnSubmit);
        debugButton.setText(R.string.lower_button_text);
        debugButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Log.d(msg, "debug button pressed");
                advanceLevel();
            }
        });
    }

    /** Create and assign the lists*/
    private void initLists() {
        Log.d(msg, "initLists() called");
        mJukuList = getJukuList(mNumWords);
        for( int i=0; i < mJukuList.length; i++){
            Log.d(msg, "Juku " + i + " is: " + mJukuList[i]);
        }
        mKanjiList = getKanjiList(mJukuList);
        for( int i=0; i < mKanjiList.length; i++){
            Log.d(msg, "Kanji " + i + " is: " + mKanjiList[i]);
        }
        // Set all states to zero at the start
        mButtonStateList = new int[DIMENSIONS];
        for( int i=0; i < DIMENSIONS; i++) {
            mButtonStateList[i] = 0;
        }
        scrambleKanjiList();
    }

    /** Add text to the textView at the top */
    private void initText() {
        TextView levelTextView = findViewById(R.id.levelText);
        String levelAnnotation = "Level: " + (mLevel + 1) + "/5";
        levelTextView.setTextColor(Color.parseColor("#FFFFFFFF"));
        levelTextView.setText(levelAnnotation);
    }

    /** Assign the main GridView */
    private void initView() {
        Log.d(msg, "initView() called");
        mGridView = findViewById(R.id.gridView);
        mGridView.setNumColumns(COLUMNS);
    }

    // Debugging the state list
    private void logStateList() {
        StringBuilder stateStr = new StringBuilder();
        for(int s : mButtonStateList){
            stateStr.append(s);
        }
        Log.d(msg, stateStr.toString());
    }

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(msg, "The onCreate() event");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRowsAndColumns();
        initLists();
        initView();
        initText();
        initDebugButton();
        setDimensions(); // set COLUMNS, ROWS, DIMENSIONS, mColumnWidth and mColumnHeight
    }

    /** Scramble the list */
    private void scrambleKanjiList() {
        int index;
        String temp;
        Random random = new Random();

        for (int i = mKanjiList.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            temp = mKanjiList[index];
            mKanjiList[index] = mKanjiList[i];
            mKanjiList[i] = temp;
        }
    }

    /** Sets dimensions buttons and calls display()*/
    private void setDimensions() {
        Log.d(msg, "setDimensions() called");
        ViewTreeObserver vto = mGridView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d(msg, "onGlobalLayout called");
                mGridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int displayWidth = mGridView.getMeasuredWidth() - 100; // -140 to account for style margins
                int displayHeight = mGridView.getMeasuredHeight();

                int statusbarHeight = getStatusBarHeight(getApplicationContext());
                int requiredHeight = displayHeight - statusbarHeight;

                mColumnWidth = displayWidth / COLUMNS;

                if (ROWS/3 > 1) {
                    mColumnHeight = requiredHeight / (ROWS + 1); // +1 for a bit of extra space
                } else {
                    mColumnHeight = requiredHeight / ROWS; // +1 for a bit of extra space
                }

                Log.d(msg, "Column dimensions: " + (mColumnWidth) + "x" + (mColumnHeight));
                display(getApplicationContext());
            }
        });
    }

    private void setRowsAndColumns() {
        Log.d(msg, "setRowsAndColumns() called");
        ROWS = mLevelDimensions[mLevel][0];
        COLUMNS = mLevelDimensions[mLevel][1];
        DIMENSIONS = ROWS*COLUMNS;
    }

    private void swapChips(){
        Log.d(msg, "swapChips() called");
        //Find first
        for(int i = 0; i < mButtonStateList.length; i++){
            if(mButtonStateList[i] == 1){
                String tmp_str = mKanjiList[i];
                // Find second
                for(int j = i + 1; j < mButtonStateList.length; j++){
                    if(mButtonStateList[j] == 1){
                        mButtonStateList[i] = 0;
                        mButtonStateList[j] = 0;

                        mKanjiList[i] = mKanjiList[j];
                        mKanjiList[j] = tmp_str;

                        int k = i; // Final integer variable for first kanji
                        int l = j; //Final integer variable for second kanji

                        Log.d(msg, "Starting animation");

                        // Animation step
                        Button button1 = (Button)findViewById(i);
                        Button button2 = (Button)findViewById(j);
                        final Animation animation1 = new TranslateAnimation(0, (button2.getX() - button1.getX()), 0, (button2.getY() - button1.getY()));
                        final Animation animation2 = new TranslateAnimation(0, (button1.getX() - button2.getX()), 0, (button1.getY() - button2.getY()));
                        animation1.setDuration(300);
                        animation2.setDuration(300);
                        button1.startAnimation(animation1);
                        button2.startAnimation(animation2);

                        // Setting the new text at the end anc cancelling
                        mGridView.postOnAnimationDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(msg, "run() called");
                                // Set text
                                button1.setText(mKanjiList[k]);
                                button2.setText(mKanjiList[l]);
                                checkForCompletedLine();

                                // Cancel animation to avoid flicker
                                animation1.cancel();
                                animation2.cancel();
                            }
                        }, 300);
                    }
                }
            }
        }
    }

    private void updateButtonBackgrounds(){
        Log.d(msg, "updateButtonBackgrounds() called");
        for (int i = 0; i < DIMENSIONS; i++){
            Button uButton = findViewById(i); //local var for updating
            //Log.d(msg, "Button with id: " + uButton.getId() + " has state: " + mButtonStateList[i]);
            if(mButtonStateList[i] == 0) {
                uButton.setBackgroundResource(R.drawable.button_selector_default);
            }
            else if (mButtonStateList[i] == 1) {
                uButton.setBackgroundResource(R.drawable.button_selector_selected);
            }
            else if (mButtonStateList[i] == 2) {
                uButton.setBackgroundResource(R.drawable.button_selector_completed);
                uButton.setEnabled(false);
            }
        }
    }
}
