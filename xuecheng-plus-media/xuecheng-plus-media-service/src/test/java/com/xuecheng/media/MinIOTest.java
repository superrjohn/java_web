package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MinIOTest {
    static MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public void test_MinIO() throws Exception {
        //創建桶,有則不創
        boolean found =
                minioClient.bucketExists(BucketExistsArgs.builder().bucket("testbuckt").build());
        if (!found) {
            // Make a new bucket called 'asiatrip'.
            minioClient.makeBucket(MakeBucketArgs.builder().bucket("testbuckt").build());
        } else {
            System.out.println("Bucket 'testbuckt' already exists.");

            //根据扩展名取出mimeType
            ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".jpg");
            String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流

            if (extensionMatch != null) {
                mimeType = extensionMatch.getMimeType();
            }

            //上傳文件的參數信息
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket("testbuckt") //你建立的桶名稱
                    .filename("D:\\Develop\\jpgTest\\CMDDDDDDDDDDD.jpg") //指定本地文件路徑
                    //.object("CMDDDDDDDDDDD.jpg")//對象名 在桶下存儲該文件
                    .object("test/01/CMDDDDDDDDDDD.jpg")//對象名 文在子目錄下
                    .contentType(mimeType) //默认根据扩展名确定文件内容类型
                    .build();
            //上傳文件
            minioClient.uploadObject(uploadObjectArgs);

        }
    }

    //刪除文件
    @Test
    public void delete_MinIO() throws Exception {
        //刪除參數的信息
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket("testbuckt").object("CMDDDDDDDDDDD.jpg").build();

        //刪除文件
        minioClient.removeObject(removeObjectArgs);
    }

    //查詢文件,從minio中下載
    @Test
    public void getFile() throws Exception {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket("testbuckt").object("test/01/CMDDDDDDDDDDD.jpg").build();
        //查詢遠程服務獲得到一個流對象
        FilterInputStream inputStream = minioClient.getObject(getObjectArgs); //輸入流
        //指定輸出流
        FileOutputStream outputStream = new FileOutputStream(("D:\\Develop\\jpgTest\\1ACMDDDDDDDDDDD.jpg"));
        //將輸入流COPY到輸出流
        IOUtils.copy(inputStream, outputStream);
    }

    //將分塊文件上傳到minio
    @Test
    public void uploadChunk() throws Exception {
        for (int i = 0; i < 5; i++) {
            //上傳文件的參數信息
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket("testbuckt") //你建立的桶名稱
                    .filename("D:\\Develop\\chunk\\" + i) //指定本地文件路徑
                    //.object("CMDDDDDDDDDDD.jpg")//對象名 在桶下存儲該文件
                    .object("chunk/" + i)//對象名 文在子目錄下
                    //.contentType(mimeType) //默认根据扩展名确定文件内容类型
                    .build();
            //上傳文件
            minioClient.uploadObject(uploadObjectArgs);
            System.out.println("上傳分塊" + i + "成功");
        }

    }

    //調用minio接口合併分塊
    @Test
    public void testMerger() throws Exception {
        //用stream流,從0開始到limit遍歷,再用map映射到bucket中的源文件,再轉為List
      List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(5)
              .map(i -> ComposeSource.builder().bucket("testbuckt").object("chunk/" + i).build())
              .collect(Collectors.toList());

      //指定合併後的objectName等信息
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket("testbuckt")
                .object("merge01.webm")
                .sources(sources)//指定源文件
                .build();
        //合併文件,minio的composeObject方法默認的分塊大小為5M
        minioClient.composeObject(composeObjectArgs);

    }
    //批量清理分塊文件
}
