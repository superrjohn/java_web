package com.xuecheng;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

//啟動類寫在xuecheng包下,其運行會在xuecheng包和其下子包,本項目模塊名子一樣,所以會掃瞄其他模塊
@SpringBootApplication
public class ContentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContentApplication.class, args);
    }
}