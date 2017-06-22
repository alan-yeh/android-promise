package cn.yerl.android.promise.http.logger;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Http 文件日志
 * Created by Alan Yeh on 2016/12/27.
 */
public class FileLogger extends BaseLogger {
    private final File logPath;
    private final SimpleDateFormat logFolderFormatter = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat logFilenameFormatter = new SimpleDateFormat("HH");

    /**
     * 创建文件日志
     * @param logPath 日志保存地址
     */
    public FileLogger(File logPath){
        if (logPath == null){
            throw new IllegalArgumentException("PromiseHttp: logPath不能为空");
        }
        this.logPath = logPath;
        if (!logPath.exists()){
            logPath.mkdirs();
        }
    }

    @Override
    void writeInfo(String info) {
        writeString(info);
    }

    @Override
    void writeError(String error) {
        writeString(error);
    }

    void writeString(String content){
        Date date = new Date();
        String folderName = logFolderFormatter.format(date);
        File folder = new File(logPath.getPath() + File.separator + folderName);
        if (! folder.exists()){
            folder.mkdirs();
        }

        try{
            RandomAccessFile randomAccessFile = new RandomAccessFile(new File(folder.getPath() + File.separator + logFilenameFormatter.format(date) + ".info"), "rw");
            randomAccessFile.seek(randomAccessFile.length());
            randomAccessFile.write(content.getBytes("UTF-8"));
            randomAccessFile.close();
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

}
