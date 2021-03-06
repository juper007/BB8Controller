package com.example.v_mipark.bb8controller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.orbotix.ConvenienceRobot;
import com.orbotix.common.DiscoveryAgentEventListener;
import com.orbotix.le.DiscoveryAgentLE;
import com.orbotix.common.DiscoveryException;
import com.orbotix.common.Robot;
import com.orbotix.common.RobotChangedStateListener;
import com.orbotix.le.RobotLE;
import com.orbotix.le.RobotRadioDescriptor;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ConvenienceRobot mRobot;

    private float mSpeed = 0.3f;
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
    private boolean mLedOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
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
                setZeroHeader(isChecked);
            }
        });
    }

    private void setZeroHeader(boolean isChecked) {
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

    private DiscoveryAgentEventListener _discoveryAgentEventListener = new DiscoveryAgentEventListener() {
        @Override
        public void handleRobotsAvailable(List<Robot> robots) {
            addLog("Found " + robots.size() + " robots");

            for (Robot robot : robots) {
                addLog(robot.getName());
            }
        }
    };

    private RobotChangedStateListener _robotStateListener = new RobotChangedStateListener() {
        @Override
        public void handleRobotChangedState(Robot robot, RobotChangedStateNotificationType type) {
            switch (type) {
                case Online:

                    mDiscoveryAgent.stopDiscovery();
                    addLog("Found BB-8");
                    if( robot instanceof RobotLE) {
                        ( (RobotLE) robot ).setDeveloperMode( true );
                    }

                    mRobot = new ConvenienceRobot(robot);
                    mRobot.setLed(0f, 0f, 0f);
                    mLedOn = false;
                    break;
                case Disconnected:
                    break;
                case Connecting:
                    addLog("Connecting to BB-8");
                    break;
                case Connected:
                    addLog("Connected to BB-8");
                    break;
                case FailedConnect:
                    addLog("Failed to Connect to BB-8");
                    break;
                default:
                    addLog("unknown type " + type.toString());
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        mDiscoveryAgent = DiscoveryAgentLE.getInstance();
        mDiscoveryAgent.addDiscoveryListener(_discoveryAgentEventListener);
        mDiscoveryAgent.addRobotStateListener(_robotStateListener);

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
            mDiscoveryAgent.removeDiscoveryListener(_discoveryAgentEventListener);
            mDiscoveryAgent.removeRobotStateListener(_robotStateListener);
            mDiscoveryAgent = null;
        }
        super.onStop();
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
                event.getAction() == MotionEvent.ACTION_MOVE) {

            // Process all historical movement samples in the batch
            final int historySize = event.getHistorySize();

            // Process the movements starting from the
            // earliest historical position in the batch
            for (int i = 0; i < historySize; i++) {
                // Process the event at historical position i
                processJoystickInput(event, i);
            }

            // Process the current movement sample in the batch (position -1)
            processJoystickInput(event, -1);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private void processJoystickInput(MotionEvent event, int historyPos) {
        InputDevice mInputDevice = event.getDevice();

        float x = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_X, historyPos);
        if (x == 0) x = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_Z, historyPos);

        float y = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_Y, historyPos);
        if (y == 0) y = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_RZ, historyPos);

        x = Math.round(x * 100) / 100.0f;
        y = Math.round(y * 100) / 100.0f;

        float angle = (float) Math.round(((Math.toDegrees(Math.atan2(y, x)) + 450) % 360) * 100) / 100.0f ;
        float distance = (float) Math.round(Math.sqrt(x*x + y*y) * 100) / 100.0f;

        if (x == 0.0f && y == 0.0f) {
            angle = 0;
            distance = 0;
            moveStop();
        } else {
            move(angle, mSpeed);
        }

        addLog("Joystick Move : \nAngle - " + angle + ",\nDistance - " + distance + "\n[" + x + "," + y + "]");
    }

    private float getCenteredAxis(MotionEvent event, InputDevice device, int axis, int historyPos) {
        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        if (range != null) {
            final float flat = range.getFlat();
            final float value = historyPos < 0 ? event.getAxisValue(axis): event.getHistoricalAxisValue(axis, historyPos);

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDiscoveryAgent.addRobotStateListener(null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int source = event.getSource();
        if ((source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            if (event.getRepeatCount() == 0) {
                switch (keyCode) {
                    case (ButtonMap.BUTTON_A) :
                        addLog("Game Pad Key Down : A Button");
                        mSpeed = 0.7f;
                        break;
                    case (ButtonMap.BUTTON_B) :
                        addLog("Game Pad Key Down : B Button");
                        mSpeed = 1.0f;
                        break;
                    case (ButtonMap.BUTTON_X) :
                        addLog("Game Pad Key Down : X Button");
                        setZeroHeader(true);
                        break;
                    case (ButtonMap.BUTTON_Y) :
                        addLog("Game Pad Key Down : Y Button");
                        break;
                    case (ButtonMap.BUTTON_VIEW) :
                        addLog("Game Pad Key Down : View Button");
                        break;
                    case (ButtonMap.BUTTON_MENU) :
                        addLog("Game Pad Key Down : Select Button");
                        break;
                    case (ButtonMap.BUTTON_XBOX) :
                        addLog("Game Pad Key Down : Xbox Button");
                        break;
                    case (ButtonMap.BUTTON_LEFT_BUMPER) :
                        addLog("Game Pad Key Down : Left bumper Button");
                        break;
                    case (ButtonMap.BUTTON_RIGHT_BUMPER) :
                        addLog("Game Pad Key Down : Right bumper Button");
                        break;
                    case (ButtonMap.BUTTON_LEFT_STICK) :
                        addLog("Game Pad Key Down : Left stick Button");
                        break;
                    case (ButtonMap.BUTTON_RIGHT_STICK) :
                        addLog("Game Pad Key Down : Right stick Button");
                        break;
                    default:
                        addLog("Game Pad Key Down : Unknown(" + keyCode + ", from " + source + ")");
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        int source = event.getSource();
        if ((source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            if (event.getRepeatCount() == 0) {
                switch (keyCode) {
                    case (ButtonMap.BUTTON_A) :
                        addLog("Game Pad Key Up : A Button");
                        mSpeed = 0.3f;
                        break;
                    case (ButtonMap.BUTTON_B) :
                        addLog("Game Pad Key Up : B Button");
                        mSpeed = 0.3f;
                        break;
                    case (ButtonMap.BUTTON_X) :
                        addLog("Game Pad Key Up : X Button");
                        setZeroHeader(false);
                        break;
                    case (ButtonMap.BUTTON_Y) :
                        addLog("Game Pad Key Up : Y Button");
                        randomLEDToggle();
                        break;
                    case (ButtonMap.BUTTON_VIEW) :
                        addLog("Game Pad Key Up : View Button");
                        break;
                    case (ButtonMap.BUTTON_MENU) :
                        addLog("Game Pad Key Up : Select Button");
                        break;
                    case (ButtonMap.BUTTON_XBOX) :
                        addLog("Game Pad Key Up : Xbox Button");
                        break;
                    case (ButtonMap.BUTTON_LEFT_BUMPER) :
                        addLog("Game Pad Key Up : Left bumper Button");
                        break;
                    case (ButtonMap.BUTTON_RIGHT_BUMPER) :
                        addLog("Game Pad Key Up : Right bumper Button");
                        break;
                    case (ButtonMap.BUTTON_LEFT_STICK) :
                        addLog("Game Pad Key Up : Left stick Button");
                        break;
                    case (ButtonMap.BUTTON_RIGHT_STICK) :
                        addLog("Game Pad Key Up : Right stick Button");
                        break;
                    default:
                        addLog("Game Pad Key Up : Unknown(" + keyCode + ", from " + source + ")");
                }
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    private void randomLEDToggle() {
        if (mRobot != null) {
            if (mLedOn) {
                mRobot.setLed(0f, 0f, 0f);
                mLedOn = false;
                addLog("LED OFF");
            } else {
                float ledR = (float)Math.random();
                float ledG = (float)Math.random();
                float ledB = (float)Math.random();

                mRobot.setLed(ledR, ledG, ledB);
                mLedOn = true;
                addLog(String.format("LED ON - %.2f, %.2f, %.2f", ledR, ledG, ledB));
            }
        }
    }

    private void move(float direction, float speed) {
        if (mRobot != null) {
            addLog("Move [" + direction + "," + speed + "]");
            mRobot.drive(direction, speed);
        }
    }

    private void moveStop() {
        if (mRobot != null) {
            addLog("Stop");
            mRobot.stop();
        }
    }

    private void addLog(String s) {
        mLogView.setText(s + "\n");
    }

}
