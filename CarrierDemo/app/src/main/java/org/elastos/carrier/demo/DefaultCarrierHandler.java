package org.elastos.carrier.demo;

import org.elastos.carrier.AbstractCarrierHandler;
import org.elastos.carrier.Carrier;
import org.elastos.carrier.ConnectionStatus;
import org.elastos.carrier.FriendInfo;
import org.elastos.carrier.UserInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultCarrierHandler extends AbstractCarrierHandler {
    @Override
    public void onConnection(Carrier carrier, ConnectionStatus status) {
        Logger.info("Carrier connection status: " + status);

        if(status == ConnectionStatus.Connected) {
            String msg = "Friend List:";
            List<FriendInfo> friendList = CarrierHelper.getFriendList();
            if(friendList != null) {
                for(FriendInfo info: friendList) {
                    msg += "\n  " + info.getUserId();
                }
            }
            Logger.info(msg);
        }
    }

    @Override
    public void onFriendRequest(Carrier carrier, String userId, UserInfo info, String hello) {
        Logger.info("Carrier received friend request. peer UserId: " + userId);
        CarrierHelper.acceptFriend(userId, hello);
    }

    @Override
    public void onFriendAdded(Carrier carrier, FriendInfo info) {
        Logger.info("Carrier friend added. peer UserId: " + info.getUserId());
    }

    @Override
    public void onFriendConnection(Carrier carrier, String friendId, ConnectionStatus status) {
        Logger.info("Carrier friend connect. peer UserId: " + friendId + " status:" + status);
        if(status == ConnectionStatus.Connected) {
            CarrierHelper.setPeerUserId(friendId);
        } else {
            CarrierHelper.setPeerUserId(null);
        }
    }

    @Override
    public void onFriendMessage(Carrier carrier, String from, byte[] message) {
        Logger.info("Carrier receiver message from UserId: " + from
                + "\nmessage: " + new String(message));
        String[] list=new String(message).split("\n");
        if(list.length==0)
            return;

        //introduce request
        if(list[0].equals("introduce")){
            if(list.length<3)
                return;
            String address = list[2];
            if(CarrierHelper.introduced.containsKey(address))
                return;
            int n = Integer.parseInt(list[1]);
            if(n==0||n>5)
                return;
            n--;
            CarrierHelper.introduce(address,n);
            CarrierHelper.addFriend(address);
            CarrierHelper.introduced.put(address,1);
        }

        //post request
        else if(list[0].equals("post")){
            if(list.length<3)
                return;
            String userId = list[1];
            String msg = list[2];
            if (CarrierHelper.posts.containsKey(msg))
                return;
            else {
                CarrierHelper.posts.put(msg, userId);
                CarrierHelper.post(userId,msg);
            }
            Logger.info("get post from:"+ userId + "\n  "+msg);
        }

        else if(list[0].equals("pull")){
            if(list.length<2)
                return;
            String userId = list[1];
            for(Map.Entry<String,String> entry : CarrierHelper.posts.entrySet()){
                if(entry.getValue().equals(userId) || userId.isEmpty())
                    CarrierHelper.post(userId,entry.getKey());
            }

        }

        else if(list[0].equals("postAlone")){
            if(list.length<3)
                return;
            String userId = list[1];
            String msg = list[2];
            if(CarrierHelper.posts.containsKey(msg))
                return;
            CarrierHelper.posts.put(msg,userId);
            Logger.info("Refreshed new post:\n  "+msg);
        }
    }


}

