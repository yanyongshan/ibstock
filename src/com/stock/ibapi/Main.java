package com.stock.ibapi;

import com.ib.contracts.StkContract;
import com.ib.controller.ApiController;
import com.ib.controller.Bar;
import com.ib.controller.NewContract;
import com.ib.controller.Types;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {
    public static SimpleDateFormat END_DATE_FMT = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
    private static ApiController apiController;
    //股票名称
    public static String stockName;
    //保存的文件路径
    public static String savePath;
    //客户端IP
    public static String clientIp;
    //客户端端口
    public static int port;
    //开始时间
    public static String startTime;
    //结束时间
    public static String endTime;
    //每次请求间隔默认15秒
    public static int sleepTime = 15 * 1000;

    public static void main(String[] args) {
        FileOutputStream fileOutputStream = null;
        try {
            checkArguments(args);
            //创建目录
            File path = new File(savePath);
            if (!path.isDirectory()) {
                boolean mkdirPath = path.mkdirs();
                if (!mkdirPath) {
                    throw new IOException("创建目录失败，目录路径：" + savePath);
                }
            }
            File outfile = new File(savePath + "/" + stockName + ".csv");
            System.out.println(END_DATE_FMT.format(new Date(System.currentTimeMillis() - 24 * 3600 * 1000)));
            apiController = new ApiController(new ConnectionHandler(), new InLogger(), new OutLogger());
            apiController.connect(clientIp, port, 1);
            fileOutputStream = new FileOutputStream(outfile);
            getHistoricalByDay(stockName, 360, fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("run error:" + e.getMessage());
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
     * 初始化检查参数列表
     *
     * @param args 参数列表
     */
    public static void checkArguments(String[] args) {
        Arguments arguments = new Arguments(args);
        stockName = arguments.getString("strockName");
        if (stockName == null) {
            throw new IllegalArgumentException("参数错误：要求参数-strockName必填");
        }
        savePath = arguments.getString("savePath");
        if (savePath == null) {
            throw new IllegalArgumentException("参数错误：要求参数-savePath必填");
        }
        clientIp = arguments.getString("clientIp", "127.0.0.1");
        port = arguments.getInteger("port", 7496);
        startTime = arguments.getString("startTime");
        if (startTime == null) {
            throw new IllegalArgumentException("参数错误：要求参数-startTime必填");
        }
        endTime = arguments.getString("endTime");
        if (endTime == null) {
            throw new IllegalArgumentException("参数错误：要求参数-endTime必填");
        }
        //每次请求间隔默认15秒
        sleepTime = arguments.getInteger("sleepTime", 15 * 1000);
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
        final String endDateTime = END_DATE_FMT.format(new Date(endTime));
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
