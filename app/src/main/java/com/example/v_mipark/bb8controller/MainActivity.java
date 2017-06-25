package com.example.v_mipark.bb8controller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.orbotix.ConvenienceRobot;
import com.orbotix.le.DiscoveryAgentLE;
import com.orbotix.common.DiscoveryException;
import com.orbotix.common.Robot;
import com.orbotix.common.RobotChangedStateListener;
import com.orbotix.le.RobotLE;
import com.orbotix.le.RobotRadioDescriptor;

public class MainActivity extends AppCompatActivity implements RobotChangedStateListener {
    private ConvenienceRobot mRobot;

    private float mSpeed = 0.7f;
    private TextView mLogView;
    private Button mButtonMoveUp;
    private Button mButtonMoveDown;
    private Button mButtonMoveLeft;
    private Button mButtonMoveRight;

    // Zero Heading Mode
    private boolean mIsZeroHeadingMode = false;
    private ToggleButton mButtonAim;
    private float mCurrentDirection = 0f;
    private DiscoveryAgentLE mDiscoveryAgent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        mDiscoveryAgent = DiscoveryAgentLE.getInstance();
        mDiscoveryAgent.addRobotStateListener( this );

        mLogView = (TextView) findViewById(R.id.textview_log);
        mButtonMoveUp = (Button) findViewById(R.id.button_move_up);
        mButtonMoveUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    move(0f, mSpeed);
                } else if (action == MotionEvent.ACTION_UP)
                    moveStop();
                return false;
            }
        });
        mButtonMoveDown = (Button) findViewById(R.id.button_move_down);
        mButtonMoveDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    move(180f, mSpeed);
                } else if (action == MotionEvent.ACTION_UP)
                    moveStop();
                return false;
            }
        });
        mButtonMoveLeft = (Button) findViewById(R.id.button_move_left);
        mButtonMoveLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    if (mIsZeroHeadingMode) {
                        mCurrentDirection = mCurrentDirection + 1.0f;
//                        mRobot.rotate(mCurrentDirection);
                        move(mCurrentDirection, 0.0f);
                    } else {
                        move(270f, mSpeed);
                    }
                } else if (action == MotionEvent.ACTION_UP)
                    moveStop();
                return false;
            }
        });
        mButtonMoveRight = (Button) findViewById(R.id.button_move_right);
        mButtonMoveRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    if (mIsZeroHeadingMode) {
                        mCurrentDirection = mCurrentDirection - 1.0f;
//                        mRobot.rotate(mCurrentDirection);
                        move(mCurrentDirection, 0.0f);
                    } else {
                        move(90f, mSpeed);
                    }
                } else if (action == MotionEvent.ACTION_UP)
                    moveStop();
                return false;
            }
        });
        mButtonAim = (ToggleButton) findViewById(R.id.button_aim);
        mButtonAim.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mIsZeroHeadingMode = isChecked;
                if(mIsZeroHeadingMode) {
                    addLog("Zero heading started");
                    mRobot.setLed(0.0f, 0.0f, 0.0f);
                    mRobot.setBackLedBrightness(1.0f);
                    move(0.0f, 0.0f);
                    mCurrentDirection = 0.0f;
                } else {
                    mRobot.setZeroHeading();
                    mRobot.setBackLedBrightness(0.0f);
                    addLog("Zero heading ended");
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        RobotRadioDescriptor robotRadioDescriptor = new RobotRadioDescriptor();
        robotRadioDescriptor.setNamePrefixes(new String[]{"BB-"});
        mDiscoveryAgent.setRadioDescriptor(robotRadioDescriptor);

        if (mRobot == null && !mDiscoveryAgent.isDiscovering()){
            try {
                addLog("Start Discovery");

                mDiscoveryAgent.startDiscovery(getApplicationContext());
            } catch( DiscoveryException e ) {
                addLog("Can't connect to BB-8:" + e.toString());
            }
        }
    }

    @Override
    protected void onStop() {
        if (mRobot != null) {
            mRobot.disconnect();
        }
        if (mDiscoveryAgent.isDiscovering()) {
            mDiscoveryAgent.stopDiscovery();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDiscoveryAgent.addRobotStateListener(null);
    }

    @Override
    public void handleRobotChangedState(Robot robot, RobotChangedStateNotificationType type) {
        switch (type) {
            case Online:

                mDiscoveryAgent.stopDiscovery();

                //If robot uses Bluetooth LE, Developer Mode can be turned on.
                //This turns off DOS protection. This generally isn't required.
                addLog("Found BB-8");
                if( robot instanceof RobotLE) {
                    ( (RobotLE) robot ).setDeveloperMode( true );
                }

                //Save the robot as a ConvenienceRobot for additional utility methods
                mRobot = new ConvenienceRobot(robot);

                break;
            case Disconnected:
                break;
            default:
                addLog("unknown type " + type.toString());
        }
    }

    private void move(float direction, float speed){
        addLog("Move [" + direction + "," + speed + "]");
        mRobot.drive(direction, speed);
    }

    private void moveStop() {
        addLog("Stop");
        mRobot.stop();
    }

    private void addLog(String s) {
        mLogView.setText(mLogView.getText() + s + "\n");
    }

}
