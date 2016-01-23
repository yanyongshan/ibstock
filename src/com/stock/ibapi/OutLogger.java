package com.stock.ibapi;

import com.ib.controller.ApiConnection;

/**
 * Created by yanyongshan on 2016/1/17.
 */
public class OutLogger implements ApiConnection.ILogger{
    @Override
    public void log(String valueOf) {
        //System.out.println("outlog:"+valueOf);
    }
}
