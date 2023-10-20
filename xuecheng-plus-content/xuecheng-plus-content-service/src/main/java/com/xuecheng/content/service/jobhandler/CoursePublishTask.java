package com.xuecheng.content.service.jobhandler;

import com.xuecheng.content.feignclient.CourseIndex;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.exception.XueChengPlusException;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @author Mr.M
 * @version 1.0
 * @description 課程發布任務類
 * @date 2022/9/22 10:16
 */
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {
    @Autowired
    CoursePublishService coursePublishService;

    @Autowired
    SearchServiceClient searchServiceClient;

    @Autowired
    CoursePublishMapper coursePublishMapper;

    //任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex="+shardIndex+",shardTotal="+shardTotal);
        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex,shardTotal,"course_publish",30,60);
    }


    //课程发布任务处理
    @Override
    public boolean execute(MqMessage mqMessage) {
        //從mq拿到課程id
        Long courseId = Long.parseLong(mqMessage.getBusinessKey1());

        //課程靜態化上傳到minio
        generateCourseHtml(mqMessage, courseId);
        //向elasticsearch寫索引數據
        saveCourseIndex(mqMessage,courseId);

        //向redis寫緩存
        saveCourseCache(mqMessage,courseId);

        //返回true表示任務完成
        return true;
    }

    private void saveCourseCache(MqMessage mqMessage, Long courseId) {
       // int i =1/0;
        //消息id
        Long taskId = mqMessage.getId();
        //這是實現抽象類方法,指針指向抽象類,抽象類中又注入service,所以這用this可以拿到service
        MqMessageService mqMessageService = this.getMqMessageService();
        //查詢數據庫看執行階段
        int stageThree= mqMessageService.getStageThree(taskId);
        if (stageThree > 0) {
            log.debug("緩存任務完成,不用處理");
            return;
        }

        //開始緩存任務

        //任務處理完成改寫任務狀態3為完成
        mqMessageService.completedStageThree(taskId);
    }

    //生成課程靜態頁面并上傳到文件系統
    private void generateCourseHtml(MqMessage mqMessage, long courseId) {
        //消息id
        Long taskId = mqMessage.getId();
        //這是實現抽象類方法,指針指向抽象類,抽象類中又注入service,所以這用this可以拿到service
        MqMessageService mqMessageService = this.getMqMessageService();
        //查詢數據庫看執行階段
        int stageOne = mqMessageService.getStageOne(taskId);
        if (stageOne > 0) {
            log.debug("課程靜態代任務完成,不用處理");
            return;
        }

        //開始進行靜態化頁面,生成html
        File file = coursePublishService.generateCourseHtml(courseId);
        //將html上傳到minio
        coursePublishService.uploadCourseHtml(courseId,file);
        //任務處理完成改寫任務狀態1為完成
        mqMessageService.completedStageOne(taskId);
    }

    //保存课程索引信息
    public void saveCourseIndex(MqMessage mqMessage, long courseId) {
        //消息id
        Long taskId = mqMessage.getId();
        //這是實現抽象類方法,指針指向抽象類,抽象類中又注入service,所以這用this可以拿到service
        MqMessageService mqMessageService = this.getMqMessageService();
        //查詢數據庫看執行階段
        int stageTwo = mqMessageService.getStageTwo(taskId);
        if (stageTwo > 0) {
            log.debug("課程索引任務完成,不用處理");
            return;
        }

        //調用搜索服務添加索引
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        Boolean add = searchServiceClient.add(courseIndex);
        if(!add){
            XueChengPlusException.cast("遠程調用失敗");
        }
        //任務處理完成改寫任務狀態2為完成
        mqMessageService.completedStageTwo(taskId);
    }

}
