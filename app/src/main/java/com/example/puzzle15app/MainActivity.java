package com.example.puzzle15app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Button[][] buttons = new Button[4][4]; //buttons
    private final int N = 4;
    private int x;
    private int y;
    private final List<String> values = new ArrayList<>();
    private int counter;  // count move
    private SharedPreferences pref; //shared preference
    private Chronometer chronometer; // time
    long pauseOffset = 0;
    boolean timeIsSave = false;
    private int record;
    TextView stepsView;
    MyShared saveRecords;


    // shared preference take data before the onCreate()
    // onCreate()
    // give data to onStop()
    // take data to onResume()


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Make status bar transparent
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getResources().getColor(android.R.color.transparent));

        pref = this.getSharedPreferences("Puzzle15", Context.MODE_PRIVATE);  // create shared preferences
        saveRecords = MyShared.getInstance(this);

        counter = pref.getInt("Count", 0);

        chronometer = findViewById(R.id.timer);
        chronometer.start();

        initData();  // give numbers to list
        initViews(); // take buttons sequence
        shuffle();
        loadData();  // give numbers to buttons

        findViewById(R.id.restart).setOnClickListener(v -> {restarter();});//Restart

        findViewById(R.id.pauseBtn).setOnClickListener(v -> { //Pause the game
            chronometer.stop();
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            pref.edit().putLong("Timer", pauseOffset).apply();
            timeIsSave = true;

            findViewById(R.id.pauseScreen).setVisibility(View.VISIBLE);
            findViewById(R.id.container).setVisibility(View.INVISIBLE);
            findViewById(R.id.topMenu).setVisibility(View.INVISIBLE);
        });


        findViewById(R.id.cancelBtn).setOnClickListener(v -> {   //Resume game
            findViewById(R.id.pauseScreen).setVisibility(View.INVISIBLE);
            findViewById(R.id.container).setVisibility(View.VISIBLE);
            findViewById(R.id.topMenu).setVisibility(View.VISIBLE);

            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
        });


        findViewById(R.id.homeBtn).setOnClickListener(v -> finish()); // go home screen

        findViewById(R.id.returnBtn).setOnClickListener(v -> {
            findViewById(R.id.pauseScreen).setVisibility(View.INVISIBLE);
            findViewById(R.id.container).setVisibility(View.VISIBLE);
            findViewById(R.id.topMenu).setVisibility(View.VISIBLE);
            restarter();
        });


        findViewById(R.id.win_restart).setOnClickListener(v -> {  // restart on Win dialog
            findViewById(R.id.container).setVisibility(View.VISIBLE);
            findViewById(R.id.topMenu).setVisibility(View.VISIBLE);
            findViewById(R.id.winView).setVisibility(View.GONE);

            restarter();
        });

        findViewById(R.id.win_home).setOnClickListener(v -> finish()); // go home on Win dialog


        stepsView = findViewById(R.id.steps);
    }


    private void shuffle() {  // just shuffle buttons and check is possible to solve
        Collections.shuffle(values);

        int[][] matrix = new int[4][4];
        int index = 0;
        int m = 4;

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < m; j++) {

                if (values.get(index).equals("")) {
                    matrix[i][j] = 0;
                } else matrix[i][j] = Integer.parseInt(values.get(index));

                ++index;
            }
        }

        // check if it is not  possible shuffle again and again
        if (!isSolvable(matrix)) shuffle();
    }

    @SuppressLint("SetTextI18n")
    void initViews() {

        TextView steps = findViewById(R.id.steps);
        steps.setText(String.valueOf(counter));

        RelativeLayout container = findViewById(R.id.container);
        int length = container.getChildCount();  // 16

        for (int i = 0; i < length; i++) {
            Button currBtn = (Button) container.getChildAt(i);
            buttons[i / 4][i % 4] = currBtn;

            currBtn.setTag(new Point(i / 4, i % 4));
            currBtn.setOnClickListener(v -> {

                Button clickedBtn = (Button) v;
                Point point = (Point) clickedBtn.getTag();

                boolean canMove = (x == point.x && (Math.abs(y - point.y) == 1)) //check there is empty space next to it
                        || (y == point.y && (Math.abs(x - point.x) == 1));


                if (canMove) {
                    ++counter;
                    steps.setText(String.valueOf(counter));

                    buttons[x][y].setText(clickedBtn.getText());
                    clickedBtn.setText("0");
                    Button btn = buttons[x][y];
                    btn.setVisibility(View.VISIBLE);
                    clickedBtn.setVisibility(View.INVISIBLE);

                    x = point.x;
                    y = point.y;

                    if (x == 3 && y == 3) { //check win When last button is invisible
                        if (checkWin()) {
                            chronometer.stop();

                            saveRecords.saveMoveCount(counter);
                            ((TextView) findViewById(R.id.recordText)).setText("Score is: " + saveRecords.getResults()[0]);


                            findViewById(R.id.container).setVisibility(View.GONE);
                            findViewById(R.id.topMenu).setVisibility(View.INVISIBLE);
                            findViewById(R.id.winView).setVisibility(View.VISIBLE);

                        }
                    }
                }


            });

        }
    }

    private void initData() {
        for (int i = 1; i < 16; i++) {
            values.add(String.valueOf(i));
        }
        values.add("0");
    }

    private void loadData() {
        for (int i = 0; i < 16; i++) {
            if (values.get(i).equals("0")) {
                x = i / 4;
                y = i % 4;
                buttons[i / 4][i % 4].setVisibility(View.INVISIBLE);
            }

            buttons[i / 4][i % 4].setText(values.get(i));
        }
    }

    private boolean checkWin() {
        RelativeLayout buttonsContainer = findViewById(R.id.container);

        for (int i = 1; i < 16; i++) {
            Button currentButton = (Button) buttonsContainer.getChildAt(i - 1);

            if (!currentButton.getText().equals(String.valueOf(i))) return false;
        }

        return true;
    }

    void clearButton() {
        for (int i = 0; i < 16; i++) {
            if (i / 4 == x && i % 4 == y) {
                buttons[x][y].setVisibility(View.INVISIBLE);
            } else {
                buttons[i / 4][i % 4].setVisibility(View.VISIBLE);
            }
        }

        counter = 0;
        ((TextView) findViewById(R.id.steps)).setText("0");
    }


    private void restarter() {
        values.clear();
        initData();
        shuffle();
        loadData();
        clearButton();

        sharedPreferencesCleaner();


        chronometer.setBase(SystemClock.elapsedRealtime());
        pauseOffset = 0;
        chronometer.start();
    }


    @Override
    protected void onResume() {

        boolean doOrDont = getIntent().getBooleanExtra("SAVE", false);

        if (!doOrDont) {
            ((TextView) findViewById(R.id.steps)).setText("0");

            sharedPreferencesCleaner();
//            pref.edit().clear().apply();
        }

        counter = pref.getInt("Count", 0);  // move
        record = pref.getInt("Record", 0);  // time

        String[] numbers = pref.getString("Numbers", "").split("#"); //sequence of button

        if (numbers.length != 1) {
            for (int i = 0; i < 16; i++) {
                if (numbers[i].equals("0")) {
                    buttons[i / 4][i % 4].setText("0");
                    buttons[i / 4][i % 4].setVisibility(View.INVISIBLE);

                    x = i / 4;
                    y = i % 4;
                } else {
                    buttons[i / 4][i % 4].setText(numbers[i]);
                    buttons[i / 4][i % 4].setVisibility(View.VISIBLE);
                }
            }


            pauseOffset = pref.getLong("Timer", 0);

            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        if (!timeIsSave) {
            chronometer.stop();
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            pref.edit().putLong("Timer", pauseOffset).apply();
        }

        StringBuilder numbers = new StringBuilder();

        for (int i = 0; i < 16; i++) {
            if (buttons[i / 4][i % 4].getText().equals("0")) numbers.append("0").append("#");
            else numbers.append(buttons[i / 4][i % 4].getText()).append("#");
        }

        values.clear();


        pref.edit().putString("Numbers", numbers.toString()).apply(); //sequence of button
        pref.edit().putInt("Count", counter).apply(); // move
        pref.edit().putInt("Record", record).apply(); //time

        super.onPause();
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("Steps", counter);

        chronometer.stop();
        pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();

        outState.putLong("Time", pauseOffset);

        StringBuilder lastPositions = new StringBuilder();

        for (int i = 0; i < 16; i++) {
            if (buttons[i / 4][i % 4].getText().equals("0")) lastPositions.append("0").append("#");
            else lastPositions.append(buttons[i / 4][i % 4].getText()).append("#");
        }

        outState.putString("LastPositions", lastPositions.toString());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        int steps = savedInstanceState.getInt("Steps", -1);

        if (steps != -1) {
            Log.d("Steps", ""+steps);

            counter = steps;
            stepsView.setText(String.valueOf(counter));

            Log.d("Qaniy", ""+((TextView) findViewById(R.id.steps)).getText());

            String[] lastPositions = savedInstanceState.getString("LastPositions", "").split("#");

            for (int i = 0; i < 16; i++) {
                if (lastPositions[i].equals("0")) {
                    buttons[i / 4][i % 4].setText("0");
                    buttons[i / 4][i % 4].setVisibility(View.INVISIBLE);

                    x = i / 4;
                    y = i % 4;
                } else {
                    buttons[i / 4][i % 4].setText(lastPositions[i]);
                    buttons[i / 4][i % 4].setVisibility(View.VISIBLE);
                }
            }

            pauseOffset = savedInstanceState.getLong("Time", 0);

            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
        }

        super.onRestoreInstanceState(savedInstanceState);
    }

    void sharedPreferencesCleaner() {
        SharedPreferences.Editor editor = pref.edit();

        editor.remove("Count");
        editor.remove("Numbers");
        editor.remove("Timer");

        editor.apply();
    }



    int getInvCount(int[] arr) {
        int inv_count = 0;
        for (int i = 0; i < N * N - 1; i++) {
            for (int j = i + 1; j < N * N; j++) {
                if (arr[j] != 0 && arr[i] != 0
                        && arr[i] > arr[j])
                    inv_count++;
            }
        }
        return inv_count;
    }

    boolean isSolvable(int[][] puzzle) {
        // Count inversions in given puzzle
        int[] arr = new int[N * N];
        int k = 0;
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++)
                arr[k++] = puzzle[i][j];

        int invCount = getInvCount(arr);

        // If grid is odd, return true if inversion
        // count is even.
        int pos = findXPosition(puzzle);
        if (pos % 2 == 1)
            return invCount % 2 == 0;
        else
            return invCount % 2 == 1;
    }

    int findXPosition(int[][] puzzle) {
        // start from bottom-right corner of matrix
        for (int i = N - 1; i >= 0; i--)
            for (int j = N - 1; j >= 0; j--)
                if (puzzle[i][j] == 0)
                    return N - i;
        return -1;
    }
}