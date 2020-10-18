package jiahaoliu.example.broadcasttree.Util;

import jiahaoliu.example.broadcasttree.BroadcastObserver;
import jiahaoliu.example.broadcasttree.BroadcastRule;
import jiahaoliu.example.broadcasttree.MsgBroadcastInitializer;
import jiahaoliu.example.broadcasttree.ResultTransferObserver;

public class Resetter {

    public static void setStaticFieldToDefault() {
        BroadcastObserver.broadcastCycle = true;
        BroadcastObserver.broadcastNum = 0;
        BroadcastRule.initialed = false;
    }
}
