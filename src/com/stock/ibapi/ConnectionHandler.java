package com.stock.ibapi;

import com.ib.controller.ApiController;

import java.util.ArrayList;

/**
 * IB客户端连接回调
 * Created by yanyongshan on 2016/1/17.
 */
public class ConnectionHandler implements ApiController.IConnectionHandler {
    @Override
    public void connected() {
        System.out.println("ib client connected...");
    }

    @Override
    public void disconnected() {
        System.out.println("ib client disconnected...");
    }

    @Override
    public void accountList(ArrayList<String> list) {

    }

    @Override
    public void error(Exception e) {
        e.printStackTrace();
        System.out.println("error exception msg=" + e.getMessage());
    }

    @Override
    public void message(int id, int errorCode, String errorMsg) {
        System.out.println("message request id=" + id + ",errorCode=" + errorCode + ",errorMsg=" + errorMsg);
    }

    @Override
    public void show(String string) {
        System.out.println("show string=" + string);
    }
}
