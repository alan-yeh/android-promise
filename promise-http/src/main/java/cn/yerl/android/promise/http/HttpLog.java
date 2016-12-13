package cn.yerl.android.promise.http;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


/**
 * 文件日志类，联网成功失败打印出日志信息
 * Created by chenxiaoxu on 2016/5/20.
 */
class HttpLog {
    private static HttpLog logger;

    public static void Log(PromiseRequest request){

    }

    public static void Log(PromiseResponse response){

    }


//    /**
//     * 写回调信息
//     */
//    public void writeMsg(String systemDate, String code, String url, PromiseRequest params, String result){
//
//        if(builder.length() > 0) {
//            builder.delete(0, builder.length() - 1);
//        }
//        if(params == null){
//            paramsStr = "null";
//        }else{
////            paramsStr = params.getParamsString();
//        }
//        builder.append(" ** ")
//                .append("SystemDate : "+ systemDate +" ; ")
//                .append("ResultCode : "+ code +" ; ")
//                .append("Url : "+ url +" ; ")
//                .append("Params : "+ paramsStr)
//                .append("Result : " + result + " ; ");
//        writeContent(builder.toString());
//
//    }
//
//    /**
//     * 写入内容
//     * @param content
//     */
//    private void writeContent(String content){
//        if(logFileName != null){
//            try {
//                if(bufferedWriter == null){
//                    writer = new FileWriter(logFileName);
//                    bufferedWriter = new BufferedWriter(writer);
//                }
//                bufferedWriter.newLine();
//                bufferedWriter.write(content);
//                bufferedWriter.flush();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }else{
//            createFileDir(mContext);
//        }
//    }
//
//    /**
//     * 得到当前包名
//     * @return 包名
//     */
//    private String getApkPackName(Context context){
//        PackageInfo info;
//        String packName = null;
//        try {
//            info = context.getPackageManager().getPackageInfo(context.getPackageName(),0);
//            packName = info.packageName;
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        return packName;
//    }
}
