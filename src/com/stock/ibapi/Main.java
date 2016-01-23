package com.stock.ibapi;

import com.ib.contracts.StkContract;
import com.ib.controller.ApiController;
import com.ib.controller.Bar;
import com.ib.controller.NewContract;
import com.ib.controller.Types;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {
    public static SimpleDateFormat END_DATE_FMT = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
    private static ApiController apiController;

    public static void main(String[] args) {
        String stockName = "uvxy";
        File outfile = new File("h://" + stockName + ".csv");
        FileOutputStream fileOutputStream = null;
        try {
            System.out.println(END_DATE_FMT.format(new Date(System.currentTimeMillis() - 24 * 3600 * 1000)));
            apiController = new ApiController(new ConnectionHandler(), new InLogger(), new OutLogger());
            apiController.connect("127.0.0.1", 7496, 1);
            fileOutputStream = new FileOutputStream(outfile);
            getHistoricalByDay(stockName, 360, fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (Exception ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        System.exit(0);
    }

    /***
     * 读取最近多少天的股票数据
     *
     * @param symbol
     * @param day
     */
    public static void getHistoricalByDay(final String symbol, long day, FileOutputStream outputStream) {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - day * 24 * 3600 * 1000;
        try {
            while (startTime < endTime) {
                getHistoricalData(symbol, startTime, outputStream);
                startTime += 24 * 3600 * 1000;
                Thread.sleep(15 * 1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * 读取交易历史数据
     *
     * @param symbol  股票交易代码
     * @param endTime 结束时间
     */
    public static void getHistoricalData(final String symbol, long endTime, final FileOutputStream outputStream) {
        StkContract stkContract = new StkContract(symbol);
        NewContract newContract = new NewContract(stkContract);
        String endDateTime = END_DATE_FMT.format(new Date(endTime));
        apiController.reqHistoricalData(newContract, endDateTime, 1, Types.DurationUnit.DAY, Types.BarSize._1_min, Types.WhatToShow.TRADES, false, new ApiController.IHistoricalDataHandler() {
            @Override
            public void historicalData(Bar bar, boolean hasGaps) {
                StringBuilder sb = new StringBuilder();
                sb.append(bar.formattedTime());
                sb.append("," + bar.high());
                sb.append("," + bar.low());
                sb.append("," + bar.open());
                sb.append("," + bar.close());
                sb.append("," + bar.volume());
                sb.append("," + bar.count());
                sb.append("," + bar.wap());
                sb.append("\r");
                try {
                    outputStream.write(sb.toString().getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void historicalDataEnd() {
                System.out.println("historicalDataEnd endDate=" + endDateTime);
            }
        });

    }
}
