package com.stock.ibapi;

import com.ib.controller.ApiConnection;

/**
 * Created by yanyongshan on 2016/1/17.
 */
public class InLogger implements ApiConnection.ILogger{
    @Override
    public void log(String valueOf) {
        //System.out.println("inlog:"+valueOf);
    }
}
