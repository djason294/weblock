package org.elastos.carrier.demo;

import android.content.Context;

import org.elastos.carrier.Carrier;
import org.elastos.carrier.CarrierHandler;
import org.elastos.carrier.FriendInfo;
import org.elastos.carrier.UserInfo;
import org.elastos.carrier.demo.session.CarrierSessionHelper;
import org.elastos.carrier.demo.Transaction;
import java.security.MessageDigest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CarrierHelper {
    private CarrierHelper() {}
    public static HashMap<String,String> posts;
    public static HashMap<String,Integer> introduced;
    public static Wallet wallet1 = new Wallet("ENPazHBvXd3BfA5MFnH5Nz484CkZeyu74T",
            "026B201245FC6BD642C1D6198162F917D107AE855E1C1EF7F7711E74D65EFE7E3C",
            "7A355B46A676474775BEB8795AD7093D9508A9441097EA105CEEDDA389A298C0");
    public static Wallet wallet2 = new Wallet("ELoVReV2FTzrEcxpMSoH3kHzsUaCrHCP4C",
            "0365B7170F3B21AEA6A82D39884E2B6ACE914D1848FE77B13382FD89230ECE3D25",
            "1C43119EC461196610B773C8FA4E8A778E4922A08E0C7F0D5F90BE3654542E35");
    public  static  Wallet curWallet = wallet1;
    public  static MyListViewAdapter adapter;

    public static void startCarrier(Context context) {
        try {
            String dir = context.getFilesDir().getAbsolutePath();
            Carrier.Options options = new DefaultCarrierOptions(dir);
            CarrierHandler handler = new DefaultCarrierHandler();

            Carrier.initializeInstance(options, handler);
            Carrier carrier = Carrier.getInstance();

            String addr = carrier.getAddress();
            Logger.info("Carrier Address: " + addr);

            String userID = carrier.getUserId();
            Logger.info("Carrier UserId: " + userID);

            carrier.start(1000);
            posts = new HashMap<>();
            Logger.info("start carrier.");
        } catch (Exception e) {
            Logger.error("Failed to start carrier.", e);
        }
    }

    public static void stopCarrier() {
        Carrier carrier = Carrier.getInstance();
        if(carrier != null) {
            carrier.kill();
            Logger.info("stop carrier.");
        }
    }

    public static String getAddress() {
        String addr = null;
        try {
            addr = Carrier.getInstance().getAddress();
        } catch (Exception e) {
            Logger.error("Failed to get address.", e);
        }
        return addr;
    }

    public static List<FriendInfo> getFriendList() {
        List<FriendInfo> friendList = null;
        try {
            friendList = Carrier.getInstance().getFriends();
        } catch (Exception e) {
            Logger.error("Failed to get friend list.", e);
        }
        return friendList;
    }

    public static void addFriend(String peerAddr) {
        try {
            String userId = Carrier.getIdFromAddress(peerAddr);
            if(Carrier.getInstance().isFriend(userId)) {
                Logger.info("Carrier ignore to add friend address: " + peerAddr);
                return;
            }

            Carrier.getInstance().addFriend(peerAddr, CARRIER_HELLO_AUTH);
            Logger.info("Carrier add friend address: " + peerAddr);
        } catch (Exception e) {
            Logger.error("Failed to add friend.", e);
        }
        return;
    }

    public static void acceptFriend(String peerUserId, String hello) {
        try {
            if (hello.equals(CARRIER_HELLO_AUTH) == false) {
                Logger.error("Ignore to accept friend, not expected.");
                Logger.error("hello:"+hello);
                return;
            }

            Carrier.getInstance().acceptFriend(peerUserId);
            Logger.info("Carrier accept friend UserId: " + peerUserId);
        } catch (Exception e) {
            Logger.error("Failed to add friend.", e);
        }
    }


    public static void sendMessage(String message) {
        if(sPeerUserId == null) {
            Logger.error("Failed to send message, friend not found.");
            return;
        }

        try {
            Carrier.getInstance().sendFriendMessage(sPeerUserId, message);
            CarrierHelper.post(CarrierHelper.curWallet.address,message);
            Logger.info("Carrier send message to UserId: " + sPeerUserId
                    + "\nmessage: " + message);
        } catch (Exception e) {
            Logger.error("Failed to send message.", e);
        }
    }

    public static void sendMessage(String peerUserId, String message) {
        if(peerUserId == null) {
            Logger.error("Failed to send message, friend not found.");
            return;
        }

        try {
            Carrier.getInstance().sendFriendMessage(peerUserId, message);
            Logger.info("Carrier send message to UserId: " + peerUserId
                    + "\nmessage: " + message);
        } catch (Exception e) {
            Logger.error("Failed to send message.", e);
        }
    }

    public static void sendRequest(String peerUserId,String arg1,String arg2,String arg3){

    }


    public  static void introduce(String address, int n){

        List<FriendInfo> friendList = CarrierHelper.getFriendList();
        if(friendList != null) {
            for(FriendInfo info: friendList) {
                CarrierHelper.sendMessage(info.getUserId(),"introduce\n"+String.valueOf(n)+"\n"+address);
            }
        }

    }

    public  static void post(String userId,String msg){
        CarrierHelper.posts.put(msg,userId);
        List<FriendInfo> friendList = CarrierHelper.getFriendList();
        if(friendList != null) {
            for(FriendInfo info: friendList) {
                CarrierHelper.sendMessage(info.getUserId(),"post\n"+userId+"\n"+msg);
            }
        }
    }

    public  static void pull(String userId){
        List<FriendInfo> friendList = CarrierHelper.getFriendList();
        if(friendList != null) {
            for(FriendInfo info: friendList) {
                CarrierHelper.sendMessage(info.getUserId(),"pull\n"+userId);
            }
        }
    }

    public  static void tip(String address,double amount){
        Wallet wallet = CarrierHelper.curWallet;
        Transaction transaction = new Transaction(address, wallet.address, wallet.secKey, amount, "");
        try {
            transaction.Send();
        }
        catch(Exception e){
            Logger.info("transaction send faild:"+e);
        }
    }

    public  static String recordOnBlock(String msg){
        Wallet wallet = CarrierHelper.curWallet;
        Transaction transaction = new Transaction(wallet.address, wallet.address, wallet.secKey, 0,String.valueOf(msg.hashCode()));
        try {
            String tid = transaction.Send();
            return tid;
        }
        catch(Exception e){
            Logger.info("transaction send faild:"+e);
        }
        return null;
    }

    public static boolean verifyPost(String tid,String post,String address){
        try{
            Map txn = Transaction.GetTxById(tid);
            if(txn==null)
                return false;

            if(!txn.get("RxAddress").equals(address))
                return  false;
            String hex = txn.get("hexMetadata").toString();

            return  Integer.parseInt(hex)== post.hashCode();

        }
        catch (Exception e){
            Logger.info("verify failed:"+e);
        }
        return false;
    }
    public  static void postAlone(String to, String userId,String msg){

        CarrierHelper.sendMessage(to,"postAlone\n"+userId+"\n"+msg);

    }

    public static void setPeerUserId(String peerUserId) {
        sPeerUserId = peerUserId;
    }

    public static String getPeerUserId() {
        return sPeerUserId;
    }

    private static String sPeerUserId = null;

    private static final String CARRIER_HELLO_AUTH = "auto-auth";
}

final class Wallet{
    String address;
    String pubKey;
    String secKey;

    public Wallet(String address, String pubKey, String secKey) {
        this.address = address;
        this.pubKey = pubKey;
        this.secKey = secKey;
    }
}
