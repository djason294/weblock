package org.elastos.carrier.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.elastos.carrier.Carrier;
import org.elastos.carrier.demo.session.CarrierSessionHelper;
import org.elastos.carrier.demo.session.CarrierSessionInfo;
import org.elastos.carrier.session.ManagerHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSessionThread = new HandlerThread("CarrierHandleThread");
        mSessionThread.start();

        setContentView(R.layout.activity_main);

        /****************************************************/
        CarrierHelper.startCarrier(this);
        CarrierSessionHelper.initSessionManager(mSessionManagerHandler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        CarrierSessionHelper.cleanupSessionManager();
        CarrierHelper.stopCarrier();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK) {
            Log.d(Logger.TAG,"COULD NOT GET A GOOD RESULT.");
            if(data == null) {
                return;
            }
            String result = data.getStringExtra("com.blikoon.qrcodescanner.error_decoding_image");
            if(result == null) {
                return;
            }

            showError("QR Code could not be scanned.");
        }

        if(requestCode == REQUEST_CODE_QR_SCAN) {
            if(data==null)
                return;
            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
            Log.d(Logger.TAG,"Scan result:"+ result);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Find Address");
            builder.setMessage(result);
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            });
            builder.setPositiveButton("Add Friend", (dialog, which) -> {
                CarrierHelper.addFriend(result);
            });
            builder.create().show();
        }
    }

    private void showAddress() {
        try {
            String address = CarrierHelper.getAddress();
            Log.i(Logger.TAG, "show address: " + address);
            HashMap<EncodeHintType, ErrorCorrectionLevel> hintMap = new HashMap<>();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            BitMatrix matrix = new MultiFormatWriter().encode(address, BarcodeFormat.QR_CODE, 512, 512, hintMap);

            //converting bitmatrix to bitmap
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];
            // All are 0, or black, by default
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            ImageView image = new ImageView(this);
            image.setImageBitmap(bitmap);

            TextView txt = new TextView(this);
            txt.setText(address);

            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.addView(image);
            root.addView(txt);
            ViewGroup.MarginLayoutParams txtLayout = (ViewGroup.MarginLayoutParams) txt.getLayoutParams();
            txtLayout.setMargins(100, 100, 100, 100);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("My Address");
            builder.setPositiveButton("OK", (dialog, which) -> {
                dialog.dismiss();
            });
            builder.setView(root);
            builder.create().show();
        } catch (Exception e) {
            Logger.error("Failed to show address.", e);
        }
    }

    private void scanAddress() {
        Intent i = new Intent(MainActivity.this, QrCodeActivity.class);
        startActivityForResult( i,REQUEST_CODE_QR_SCAN);
    }

    private String getAddressFromTmp() {
        String content = readFile(new File("/data/local/tmp/debug-carrier"));
        return content;
    }

    private void sendMessage() {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }

        String msg = "Message " + mMsgCounter.getAndIncrement();
        CarrierHelper.sendMessage(msg);
    }

    private void createSession() {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }
        if(mCarrierSessionInfo != null) {
            showError("Session has been created.");
            return;
        }

        mCarrierSessionInfo = CarrierSessionHelper.newSessionAndStream(CarrierHelper.getPeerUserId());
        if(mCarrierSessionInfo == null) {
            Log.e(Logger.TAG, "Failed to new session.");
            return;
        }
        boolean wait = mCarrierSessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_INITIALIZED, 10000);
        if(wait == false) {
            deleteSession();
            Logger.error("Failed to wait session initialize.");
            return;
        }

        CarrierSessionHelper.requestSession(mCarrierSessionInfo);
        wait = mCarrierSessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_REQUEST_COMPLETED, 30000);
        if(wait == false) {
            deleteSession();
            Logger.error("Failed to wait session request.");
            return;
        }

        CarrierSessionHelper.startSession(mCarrierSessionInfo);
    }

    private void sendSessionData() {
        if(mCarrierSessionInfo == null) {
            showError("Friend is not online.");
            return;
        }
        boolean connected = mCarrierSessionInfo.mSessionState.isMasked(CarrierSessionInfo.SessionState.SESSION_STREAM_CONNECTED);
        if(connected == false) {
            showError("Session is not connected.");
            return;
        }

        String msg = "Stream Message " + mMsgCounter.getAndIncrement();
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, msg.getBytes());
    }

    private void deleteSession() {
        if(mCarrierSessionInfo == null) {
            return;
        }

        CarrierSessionHelper.closeSession(mCarrierSessionInfo);
        mCarrierSessionInfo = null;
    }

    private void openPFServer(String ipaddr, String port) {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }
        if(mCarrierSessionInfo == null) {
            showError("Session has not been created.");
            return;
        }
        if(mCarrierSessionInfo.mStream == null) {
            showError("Stream has not been created.");
            return;
        }

        CarrierSessionHelper.addServer(mCarrierSessionInfo, ipaddr, port);
        Logger.info("Add server. ipaddr=" + ipaddr + " port=" + port);
    }

    private void openPFClient(String ipaddr, String port) {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }
        if(mCarrierSessionInfo == null) {
            showError("Session has not been created.");
            return;
        }
        if(mCarrierSessionInfo.mStream == null) {
            showError("Stream has not been created.");
            return;
        }
        if(mCarrierSessionInfo.mPortForwarding > 0) {
            showError("PortForwarding has not been created.");
            return;
        }

        mCarrierSessionInfo.mPortForwarding = CarrierSessionHelper.openPortForwarding(mCarrierSessionInfo.mStream, ipaddr, port);
        if(mCarrierSessionInfo.mPortForwarding <= 0) {
            Logger.error("Failed to open port forwarding. retval=" + mCarrierSessionInfo.mPortForwarding);
            return;
        }

        Logger.info("Open port forwarding. id=" + mCarrierSessionInfo.mPortForwarding  + " ipaddr=" + ipaddr + " port=" + port);
    }

    private void closePF() {
        if(mCarrierSessionInfo == null) {
            return;
        }

        CarrierSessionHelper.closePortForwarding(mCarrierSessionInfo.mStream, mCarrierSessionInfo.mPortForwarding);
        mCarrierSessionInfo.mPortForwarding = -1;

        CarrierSessionHelper.removeServer(mCarrierSessionInfo);
    }

    private void openPFPeerServer(String ipaddr, String port) {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }
        if(mCarrierSessionInfo == null) {
            showError("Session has not been created.");
            return;
        }
        if(mCarrierSessionInfo.mStream == null) {
            showError("Stream has not been created.");
            return;
        }

        String ipport = "addServer:" + ipaddr + ":" + port;
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, ipport.getBytes());
        Logger.info("Add peer server. ipaddr=" + ipaddr + " port=" + port);
    }

    private void openChannel() {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }
        if(mCarrierSessionInfo == null) {
            showError("Session has not been created.");
            return;
        }
        if(mCarrierSessionInfo.mStream == null) {
            showError("Stream has not been created.");
            return;
        }
        if(mCarrierSessionInfo.mChannel > 0) {
            showError("Channel has not been created.");
            return;
        }

        mCarrierSessionInfo.mChannel = CarrierSessionHelper.openChannel(mCarrierSessionInfo.mStream,"channel-0");
        if(mCarrierSessionInfo.mChannel <= 0) {
            Logger.error("Failed to open channel. retval=" + mCarrierSessionInfo.mChannel);
            return;
        }
        boolean wait = mCarrierSessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_CHANNEL_OPENED, 10000);
        if(wait == false) {
            closeChannel();
            Logger.error("Failed to wait channel open.");
            return;
        }
    }

    private void sendChannelData() {
        if(mCarrierSessionInfo == null) {
            showError("Friend is not online.");
            return;
        }
        if(mCarrierSessionInfo.mChannel <= 0) {
            showError("Channel is not opened.");
            return;
        }

        String msg = "Channel Message " + mMsgCounter.getAndIncrement();
        CarrierSessionHelper.sendChannelData(mCarrierSessionInfo.mStream, mCarrierSessionInfo.mChannel, msg.getBytes());
    }

    private void closeChannel() {
        if(mCarrierSessionInfo == null) {
            return;
        }

        CarrierSessionHelper.closeChannel(mCarrierSessionInfo.mStream, mCarrierSessionInfo.mChannel);
        mCarrierSessionInfo.mChannel = -1;
    }

    private ManagerHandler mSessionManagerHandler = new ManagerHandler() {
        @Override
        public void onSessionRequest(Carrier carrier, String from, String sdp) {
            CarrierSessionInfo sessionInfo = CarrierSessionHelper.newSessionAndStream(CarrierHelper.getPeerUserId());
            if(sessionInfo == null) {
                Logger.error("Failed to new session.");
                return;
            }
            boolean wait = sessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_INITIALIZED, 10000);
            if(wait == false) {
                deleteSession();
                Logger.error("Failed to wait session initialize.");
                return;
            }

            CarrierSessionHelper.replyRequest(sessionInfo);
            wait = sessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_TRANSPORTREADY, 10000);
            if(wait == false) {
                deleteSession();
                Logger.error("Failed to wait session initialize.");
                return;
            }

            sessionInfo.mSdp = sdp;
            CarrierSessionHelper.startSession(sessionInfo);

            mCarrierSessionInfo = sessionInfo;
        }
    };

    private void showError(String msg) {
        if(Looper.myLooper() != Looper.getMainLooper()) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                showError(msg);
            });
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage(msg);
        builder.setNegativeButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.create().show();
    }

    private static String getLocalIpAddress() {
        String ipaddr = null;
        try {
            Enumeration<NetworkInterface> enum_ni = NetworkInterface.getNetworkInterfaces();
            while(enum_ni.hasMoreElements()) {
                NetworkInterface net_if = enum_ni.nextElement();
                String net_name = net_if.getName();
                Enumeration<InetAddress> enum_ia = net_if.getInetAddresses();
                while(enum_ia.hasMoreElements()) {
                    InetAddress ia = enum_ia.nextElement();
                    if (ia.isSiteLocalAddress() == false
                    || ia.isLoopbackAddress() == true
                    || ia.isLinkLocalAddress() == true ) {
                        continue;
                    }

                    if(net_name.startsWith("eth")) {
                        ipaddr = ia.getHostAddress();
                    } else if(ipaddr == null
                            && net_name.startsWith("wlan")) {
                        ipaddr = ia.getHostAddress();
                    }

                    Logger.info("get device name=" + net_name + " ipaddr=" + ipaddr);
                }
            }
        } catch (SocketException ex) {
            Logger.error(ex.toString());
        }

        return ipaddr;
    }

    public static String readFile(File file) {
        if(file.exists() == false) {
            Logger.info(file.getAbsolutePath() + " is not exists.");
            return null;
        }

        InputStreamReader input_reader = null;
        BufferedReader buf_reader = null;
        try {
            input_reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            buf_reader = new BufferedReader(input_reader);
            String line;
            StringBuffer result = new StringBuffer();
            while ((line = buf_reader.readLine()) != null) {
                if(result.length() > 0) {
                    result.append('\n');
                }

                result.append(line);
            }
            return result.toString();
        } catch (Exception e) {
            Logger.error("Failed to read file: " + file.getAbsolutePath(), e);
        } finally {
            try {
                if (buf_reader != null) buf_reader.close();
                if (input_reader != null) input_reader.close();
            } catch (Exception e) {
            }
        }

        return null;
    }

    private HandlerThread mSessionThread;
    private CarrierSessionInfo mCarrierSessionInfo;
    private AtomicInteger mMsgCounter = new AtomicInteger(0);
    private static final int REQUEST_CODE_QR_SCAN = 101;
}