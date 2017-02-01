package ary.com.learn;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class ChromeActivity extends AppCompatActivity implements View.OnClickListener  {

    private TextView mousePad;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private Button switchTabButton;
    private ImageButton upButton;
    private ImageButton downButton;

    Context context;
    private boolean isConnected = false;
    private boolean mouseMoved = false;
    private Socket socket;
    private PrintWriter out;

    // initializing x, y coordinates for the movement of the mouse
    private float initX = 0;
    private float initY = 0;
    private float disX = 0;
    private float disY = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chrome);

        context = this;

        backButton = (ImageButton) findViewById(R.id.backButton);
        forwardButton = (ImageButton) findViewById(R.id.forwardButton);
        switchTabButton = (Button) findViewById(R.id.lastTabButton);
        upButton = (ImageButton) findViewById(R.id.upButton);
        downButton = (ImageButton) findViewById(R.id.downButton);

        backButton.setOnClickListener(this);
        forwardButton.setOnClickListener(this);
        switchTabButton.setOnClickListener(this);
        upButton.setOnClickListener(this);
        downButton.setOnClickListener(this);

        mousePad = (TextView) findViewById(R.id.mousePad);

        mousePad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(isConnected && out!=null){
                    switch(event.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            //save X and Y positions when user touches the TextView
                            initX = event.getX();
                            initY = event.getY();
                            mouseMoved = false;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            disX = event.getX() - initX; //Mouse movement in x direction
                            disY = event.getY() - initY; //Mouse movement in y direction
                            /*set init to new position so that continuous mouse movement
                            is captured*/
                            initX = event.getX();
                            initY = event.getY();
                            if(disX != 0|| disY != 0){
                                out.println(disX +","+ disY); //send mouse movement to server
                            }
                            mouseMoved = true;
                            break;
                        case MotionEvent.ACTION_UP:
                            //consider a tap only if usr did not move mouse after ACTION_DOWN
                            if(!mouseMoved){
                                out.println(Constants.MOUSE_LEFT_CLICK);
                            }
                    }
                }
                return true;
            }
        });






    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == R.id.action_connect) {
            ConnectPhoneTask connectPhoneTask = new ConnectPhoneTask();
            connectPhoneTask.execute(Constants.SERVER_IP); //try to connect to server in another thread
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.backButton:
                if (isConnected && out!=null) {
                    out.println(Constants.BACK);
                }
                break;
            case R.id.forwardButton:
                if (isConnected && out!=null){
                    out.println(Constants.FORWARD);
                }
                break;
            case R.id.lastTabButton:
                if (isConnected && out!=null) {
                    out.println(Constants.TAB); //send "next" to server
                }
                break;
            case R.id.upButton:
                if (isConnected && out!= null){
                    out.println(Constants.UP);
                }
                break;
            case R.id.downButton:
                if (isConnected && out != null){
                    out.println(Constants.DOWN);
                }
                break;
            case R.id.rightClickButton:
                if (isConnected && out != null){
                    out.println(Constants.MOUSE_RIGHT_CLICK);
                }
                break;
        }

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(isConnected && out!=null) {
            try {
                out.println("exit"); //tell server to exit
                Toast.makeText(context, "Disconnected from Server!", Toast.LENGTH_SHORT).show();
                socket.close(); //close socket
            } catch (IOException e) {
                Log.e("remotedroid", "Error in closing socket", e);
            }
        }
    }

    public class ConnectPhoneTask extends AsyncTask<String,Void,Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            boolean result = true;
            try {
                InetAddress serverAddr = InetAddress.getByName(params[0]);
                socket = new Socket(serverAddr, Constants.SERVER_PORT);//Open socket on server IP and port
            } catch (IOException e) {
                Log.e("remotedroid", "Error while connecting", e);
                result = false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            isConnected = result;
            Toast.makeText(context,isConnected?"Connected to server!":"Error while connecting",Toast.LENGTH_LONG).show();
            try {
                if(isConnected) {
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                            .getOutputStream())), true); //create output stream to send data to server
                }
            }catch (IOException e){
                Log.e("remotedroid", "Error while creating OutWriter", e);
                Toast.makeText(context,"Error while connecting",Toast.LENGTH_LONG).show();
            }
        }
    }
}
