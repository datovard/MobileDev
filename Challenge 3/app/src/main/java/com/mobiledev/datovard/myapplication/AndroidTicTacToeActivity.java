package com.mobiledev.datovard.myapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class AndroidTicTacToeActivity extends AppCompatActivity {

    private TicTacToeGame mGame;

    private Button mBoardButtons[];

    private TextView mInfoTextView;

    private TextView mHumanWinsTextView;
    private TextView mTiesWinsTextView;
    private TextView mAndroidWinsTextView;

    private int mHumanWins;
    private int mTiesWins;
    private int mAndroidWins;
    private boolean mGoFirst;

    private boolean mGameOver;
    private boolean mSoundOn;

    private SharedPreferences mPrefs;

    static final int DIALOG_SETTINGS = 0;
    static final int DIALOG_QUIT_ID = 1;
    static final int DIALOG_ABOUT = 2;
    static final int DIALOG_RESET = 3;

    private BoardView mBoardView;

    MediaPlayer mHumanMediaPlayer;
    MediaPlayer mComputerMediaPlayer;

    // Listen for touches on the board
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {

            // Determine which cell was touched
            int col = (int) event.getX() / mBoardView.getBoardCellWidth();
            int row = (int) event.getY() / mBoardView.getBoardCellHeight();
            int pos = row * 3 + col;

            if (!mGameOver && setMove(TicTacToeGame.HUMAN_PLAYER, pos))	{
                mHumanMediaPlayer.start();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        int winner = mGame.checkForWinner();

                        // If no winner yet, let the computer make a move
                        if (winner == 0) {
                            mInfoTextView.setText(R.string.turn_computer);
                            int move = mGame.getComputerMove();
                            setMove(TicTacToeGame.COMPUTER_PLAYER, move);
                            mComputerMediaPlayer.start();
                            winner = mGame.checkForWinner();
                        }

                        if (winner == 0) {
                            mInfoTextView.setText(R.string.turn_human);
                        } else if (winner == 1){
                            mGameOver = true;
                            mInfoTextView.setText(R.string.result_tie);
                            mTiesWins++;
                            mTiesWinsTextView.setText(String.valueOf(mTiesWins));
                        }
                        else if (winner == 2) {
                            mGameOver = true;
                            mHumanWins++;
                            mHumanWinsTextView.setText(String.valueOf(mHumanWins));
                            String defaultMessage = getResources().getString(R.string.result_human_wins);
                            mInfoTextView.setText(mPrefs.getString("victory_message", defaultMessage));
                        }
                        else {
                            mGameOver = true;
                            mInfoTextView.setText(R.string.result_computer_wins);
                            mAndroidWins++;
                            mAndroidWinsTextView.setText(String.valueOf(mAndroidWins));
                        }
                    }
                }, 1000);

                return true;
            }

            // So we aren't notified of continued events when finger is moved
            return false;
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putCharArray("board", mGame.getBoardState());
        outState.putBoolean("mGameOver", mGameOver);
        outState.putCharSequence("info", mInfoTextView.getText());

        outState.putInt("mHumanWins", Integer.valueOf(mHumanWins));
        outState.putInt("mAndroidWins", Integer.valueOf(mAndroidWins));
        outState.putInt("mTiesWins", Integer.valueOf(mTiesWins));
        outState.putBoolean("mGoFirst", mGoFirst);

        //outState.putChar("mGoFirst", mInfoTextView);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_tic_tac_toe);

        mGame = new TicTacToeGame();
        mBoardView = (BoardView) findViewById(R.id.board);
        mBoardView.setGame(mGame);

        // Listen for touches on the board
        mBoardView.setOnTouchListener(mTouchListener);

        mInfoTextView         = (TextView) findViewById(R.id.information);
        mHumanWinsTextView    = (TextView) findViewById(R.id.human_wins);
        mTiesWinsTextView     = (TextView) findViewById(R.id.ties_wins);
        mAndroidWinsTextView  = (TextView) findViewById(R.id.android_wins);

        // Restore the scores
        mPrefs       = getSharedPreferences("ttt_prefs", MODE_PRIVATE);
        mHumanWins   = mPrefs.getInt("mHumanWins", 0);
        mAndroidWins = mPrefs.getInt("mAndroidWins", 0);
        mTiesWins    = mPrefs.getInt("mTiesWins", 0);

        mSoundOn = mPrefs.getBoolean("sound", true);
        String difficultyLevel = mPrefs.getString("difficulty_level",
                getResources().getString(R.string.difficulty_harder));
        if (difficultyLevel.equals(getResources().getString(R.string.difficulty_easy)))
            mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Easy);
        else if (difficultyLevel.equals(getResources().getString(R.string.difficulty_harder)))
            mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Harder);
        else
            mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Expert);


        mGoFirst = true;

        if( savedInstanceState == null )
        {
            startNewGame();
        } else
        {
            // Restore the game's state
            mGame.setBoardState(savedInstanceState.getCharArray("board"));
            mGameOver = savedInstanceState.getBoolean("mGameOver");
            mInfoTextView.setText(savedInstanceState.getCharSequence("info"));

            mHumanWins   = savedInstanceState.getInt("mHumanWins");
            mAndroidWins = savedInstanceState.getInt("mAndroidWins");
            mTiesWins    = savedInstanceState.getInt("mTiesWins");
            mGoFirst     = savedInstanceState.getBoolean("mGoFirst");
        }
        displayScores();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Save the current scores
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putInt("mHumanWins", mHumanWins);
        ed.putInt("mAndroidWins", mAndroidWins);
        ed.putInt("mTiesWins", mTiesWins);
        ed.commit();
    }


    private void displayScores() {
        mHumanWinsTextView.setText(Integer.toString(mHumanWins));
        mAndroidWinsTextView.setText(Integer.toString(mAndroidWins));
        mTiesWinsTextView.setText(Integer.toString(mTiesWins));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mGame.setBoardState(savedInstanceState.getCharArray("board"));
        mGameOver = savedInstanceState.getBoolean("mGameOver");
        mInfoTextView.setText(savedInstanceState.getCharSequence("info"));
        //mGoFirst = savedInstanceState.getChar("mGoFirst");

        mHumanWins   = savedInstanceState.getInt("mHumanWins");
        mAndroidWins = savedInstanceState.getInt("mAndroidWins");
        mTiesWins    = savedInstanceState.getInt("mTiesWins");
    }

    @Override
    protected void onResume() {
        super.onResume();

        mHumanMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.x_sound);
        mComputerMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.o_sound);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mHumanMediaPlayer.release();
        mComputerMediaPlayer.release();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.new_game:
                startNewGame();
                return true;
            case R.id.settings:
                startActivityForResult(new Intent(this, Settings.class), 0);
                return true;
            case R.id.quit:
                showDialog(DIALOG_QUIT_ID);
                return true;
            case R.id.about:
                showDialog(DIALOG_ABOUT);
                return true;
            case R.id.reset:
                showDialog(DIALOG_RESET);
                return true;
        }

        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
            case DIALOG_QUIT_ID:
                // Create the quit confirmation dialog

                builder.setMessage(R.string.quit_question)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                AndroidTicTacToeActivity.this.finish();
                            }
                        })
                        .setNegativeButton(R.string.no, null);
                dialog = builder.create();
                break;
            case DIALOG_ABOUT:
                Context context = getApplicationContext();
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.about_dialog, null);
                builder.setView(layout);
                builder.setPositiveButton("OK", null);
                dialog = builder.create();
                break;
            case DIALOG_RESET:
                // Save the current scores
                SharedPreferences.Editor ed = mPrefs.edit();
                ed.putInt("mHumanWins", 0);
                ed.putInt("mAndroidWins", 0);
                ed.putInt("mTiesWins", 0);
                ed.commit();

                mHumanWins = mAndroidWins = mTiesWins = 0;
                displayScores();
                break;
        }
        return dialog;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RESULT_CANCELED) {
            // Apply potentially new settings

            mSoundOn = mPrefs.getBoolean("sound", true);

            String difficultyLevel = mPrefs.getString("difficulty_level",
                    getResources().getString(R.string.difficulty_harder));

            if (difficultyLevel.equals(getResources().getString(R.string.difficulty_easy)))
                mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Easy);
            else if (difficultyLevel.equals(getResources().getString(R.string.difficulty_harder)))
                mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Harder);
            else
                mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Expert);
        }
    }



    private void startNewGame()
    {
        mGame.clearBoard();
        mBoardView.invalidate();   // Redraw the board
        mGameOver = false;
        if( mGoFirst )
        {
            mInfoTextView.setText(R.string.first_human);
        }
        else
        {
            int move = mGame.getComputerMove();
            setMove(TicTacToeGame.COMPUTER_PLAYER, move);
            mInfoTextView.setText(R.string.turn_human);
        }
        mGoFirst = !mGoFirst;
    }

    private boolean setMove(char player, int location) {
        if (this.mGame.setMove(player, location)) {
            mBoardView.invalidate();   // Redraw the board
            return true;
        }
        return false;
    }
}
