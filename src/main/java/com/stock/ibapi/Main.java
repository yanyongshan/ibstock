package com.stock.ibapi;

import com.ib.contracts.StkContract;
import com.ib.controller.ApiController;
import com.ib.controller.Bar;
import com.ib.controller.NewContract;
import com.ib.controller.Types;
import org.apache.commons.cli.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
    private static ApiController apiController;
    //股票名称
    public static String stockName;
    //保存的文件路径
    public static String savePath;
    //客户端IP
    public static String ibClientIp;
    //客户端端口
    public static int port;
    //开始时间
    public static String beginTimeStr;
    //结束时间
    public static String endTimeStr;
    //请求步长（以秒为单位）
    public static long step = 1;
    //每次请求间隔默认15秒
    public static int sleepTime = 15 * 1000;
    //请求间距长度
    public static int duration = 1;
    //请求间距单位
    public static Types.DurationUnit durationUnit;
    //K线bar大小
    public static Types.BarSize barSize;
    //
    public static HashMap<String, String> stockBarMap = new HashMap<>();

    public static void main(String[] args) {
        FileWriter fileWriter = null;
        try {
            initArguments(args);
            String[] stockList = stockName.split(",");
            if (stockList.length > 0) {
                apiController = new ApiController(new ConnectionHandler(), new InLogger(), new OutLogger());
                apiController.connect(ibClientIp, port, (int) (System.currentTimeMillis() / 1000));
                for (String stock : stockList) {
                    //创建目录
                    File path = new File(savePath);
                    if (!path.isDirectory()) {
                        boolean mkdirPath = path.mkdirs();
                        if (!mkdirPath) {
                            throw new IOException("create dir error,path：" + savePath);
                        }
                    }
                    File outfile = new File(savePath + "/" + stock + ".csv");
                    if (outfile.exists()) {
                        outfile.delete();
                    }
                    stockBarMap.clear();
                    long endTimeStamp = DATE_FMT.parse(endTimeStr).getTime();
                    long beginTimeStamp = DATE_FMT.parse(beginTimeStr).getTime();
                    fileWriter = new FileWriter(outfile);
                    while (beginTimeStamp < endTimeStamp) {
                        getHistoricalData(stock, beginTimeStamp);
                        beginTimeStamp += step;
                        Thread.sleep(sleepTime);
                    }
                    //输出到文件中
                    for (Map.Entry<String, String> entry : stockBarMap.entrySet()) {
                        fileWriter.write(entry.getValue());
                    }
                    fileWriter.flush();
                }
            } else {
                System.out.println("error stock list is empty");
            }
        } catch (ParseException pe) {
            System.out.println("run exception msg=" + pe.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
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
     * @see {http://commons.apache.org/proper/commons-cli/apidocs/index.html}
     */
    public static void initArguments(String[] args) throws ParseException {
        Options opts = new Options();
        opts.addOption(Option.builder("s").longOpt("stock").hasArg(true).desc("stock list,example:BABA").required(true).build());
        opts.addOption(Option.builder("o").longOpt("output").hasArg(true).desc("output file path").required(true).build());
        opts.addOption(Option.builder("i").longOpt("host").hasArg(true).desc("IB client host,default:127.0.0.1").required(false).build());
        opts.addOption(Option.builder("p").longOpt("port").hasArg(true).desc("IB client port,default:7496").required(false).build());
        opts.addOption(Option.builder("b").longOpt("btime").hasArg(true).desc("request begin time").required(true).build());
        opts.addOption(Option.builder("e").longOpt("etime").hasArg(true).desc("request end time,example:'20150105 23:59:59'").required(true).build());
        opts.addOption(Option.builder("w").longOpt("wait").hasArg(true).desc("request sleep time by second,default:15").required(false).build());
        opts.addOption(Option.builder("u").longOpt("unit").hasArg(true).desc("request bar size,only accept 1s,5s,10s,15s,30s,1m,2m,3m,5m,10m,15m,20m,30m,1h,4h,1d,1w").required(false).build());
        String formatstr = "java -jar ibstock.jar [-s/--stock] [-o/--output] [-b/--btime] [-e/--etime]";

        HelpFormatter formatter = new HelpFormatter();
        DefaultParser parser = new DefaultParser();
        try {
            // 处理Options和参数
            CommandLine commandLine = parser.parse(opts, args, true);
            if (commandLine.hasOption("h")) {
                formatter.printHelp("argument list:", opts);
            } else {
                stockName = commandLine.getOptionValue("s");
                savePath = commandLine.getOptionValue("o");
                ibClientIp = commandLine.getOptionValue("i", "127.0.0.1");
                port = Integer.valueOf(commandLine.getOptionValue("p", "7496"));
                beginTimeStr = commandLine.getOptionValue("b");
                endTimeStr = commandLine.getOptionValue("e");
                sleepTime = Integer.valueOf(commandLine.getOptionValue("w", "15")) * 1000;
                String unit = commandLine.getOptionValue("u", "1m");
                parseStockUnit(unit);
            }
        } catch (ParseException e) {
            // 如果发生异常，则打印出帮助信息
            formatter.printHelp(formatstr, opts);
            throw e;
        }
    }

    /***
     * 根据当请请求单元，计算请求时间间隔和间距
     *
     * @param unit
     */
    public static void parseStockUnit(String unit) {
        switch (unit) {
            case "1s":
                //请求20分钟的数据，每次1200个K线
                duration = 20 * 60;
                step = duration * 1000;
                durationUnit = Types.DurationUnit.SECOND;
                barSize = Types.BarSize._1_secs;
                break;
            case "5s":
                //请求2个小时的数据，每次1440个K线
                duration = 2 * 60 * 60;
                step = duration * 1000;
                durationUnit = Types.DurationUnit.SECOND;
                barSize = Types.BarSize._5_secs;
                break;
            case "10s":
                //请求3个小时的数据，每次1080个K线
                duration = 3 * 60 * 60;
                step = duration * 1000;
                durationUnit = Types.DurationUnit.SECOND;
                barSize = Types.BarSize._10_secs;
                break;
            case "15s":
                //请求5个小时的数据，每次1200个K线
                duration = 5 * 60 * 60;
                step = duration * 1000;
                durationUnit = Types.DurationUnit.SECOND;
                barSize = Types.BarSize._15_secs;
                break;
            case "30s":
                //请求12个小时的数据，每次1440个K线
                duration = 12 * 60 * 60;
                step = duration * 1000;
                durationUnit = Types.DurationUnit.SECOND;
                barSize = Types.BarSize._30_secs;
                break;
            case "1m":
                //请求1天数据，每次1440个K线
                duration = 1;
                step = duration * 24 * 60 * 60 * 1000;
                durationUnit = Types.DurationUnit.DAY;
                barSize = Types.BarSize._1_min;
                break;
            case "2m":
                //请求2天数据，每次1440个K线
                duration = 2;
                step = duration * 24 * 60 * 60 * 1000;
                durationUnit = Types.DurationUnit.DAY;
                barSize = Types.BarSize._2_mins;
                break;
            case "3m":
                barSize = Types.BarSize._3_mins;
                break;
            case "5m":
                barSize = Types.BarSize._5_mins;
                break;
            case "10m":
                barSize = Types.BarSize._10_mins;
                break;
            case "15m":
                barSize = Types.BarSize._15_mins;
                break;
            case "20m":
                barSize = Types.BarSize._20_mins;
                break;
            case "30m":
                barSize = Types.BarSize._30_mins;
                break;
            case "1h":
                barSize = Types.BarSize._1_hour;
                break;
            case "4h":
                barSize = Types.BarSize._4_hours;
                break;
            case "1d":
                barSize = Types.BarSize._1_day;
                break;
            case "1w":
                barSize = Types.BarSize._1_week;
                break;
        }
    }

    /***
     * 读取交易历史数据
     *
     * @param symbol  股票交易代码
     * @param endTime 结束时间
     */
    public static void getHistoricalData(final String symbol, long endTime) {
        final String endDateTime = DATE_FMT.format(new Date(endTime));
        StringBuilder request = new StringBuilder();
        request.append("send request:symbol=" + symbol);
        request.append(",endTime=" + endDateTime);
        request.append(",duration=" + duration);
        request.append(",durationUnit" + durationUnit.toString());
        request.append(",barSize=" + barSize.toString());
        System.out.println(request.toString());

        StkContract stkContract = new StkContract(symbol);
        NewContract newContract = new NewContract(stkContract);

        apiController.reqHistoricalData(newContract, endDateTime, duration, durationUnit, barSize, Types.WhatToShow.TRADES, false, new ApiController.IHistoricalDataHandler() {
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
                sb.append("\n");
                stockBarMap.put(bar.formattedTime(), sb.toString());
            }

            @Override
            public void historicalDataEnd() {
                System.out.println("handler historicalDataEnd endDate=" + endDateTime + ",stock bar length=" + stockBarMap.size());
            }
        });

    }
}
