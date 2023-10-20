package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CourseTeacherService;
import com.xuecheng.exception.XueChengPlusException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Autowired
    CourseTeacherMapper courseTeacherMapper;

    @Autowired
    TeachplanMapper teachplanMapper;


    @Override
    public PageResult<CourseBase> queryCourseBaseList(Long companyId,PageParams pageParams, QueryCourseParamsDto courseParamsDto) {

        //拼接查詢條件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //根據名稱模糊查詢,在sql中拼接course_base.name like '%值%',先判斷name是否為空,有值就開始拼接
        queryWrapper.like(StringUtils.isNotEmpty(courseParamsDto.getCourseName()), CourseBase::getName, courseParamsDto.getCourseName());
        //根據課程審核狀態查詢course_base.audit_status = ?
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getAuditStatus()), CourseBase::getAuditStatus, courseParamsDto.getAuditStatus());
        //按課程發布狀況查詢
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getPublishStatus()), CourseBase::getStatus, courseParamsDto.getPublishStatus());
        //根據培訓機構ID拼接查詢條件
        queryWrapper.eq(CourseBase::getCompanyId,companyId);
        //創建page分頁參數對象,參數:當前頁碼,每頁記錄數
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        //開始進行分頁查詢
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        //數據列表
        List<CourseBase> items = pageResult.getRecords();
        //總記錄數
        long total = pageResult.getTotal();
        //List<T> items, long counts, long page,long pageSize
        PageResult<CourseBase> courseBasePageResult = new PageResult<>(items, total, pageParams.getPageNo(), pageParams.getPageSize());
        return courseBasePageResult;
    }

    @Transactional//事務
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
        //參數檢驗,由框架完成

        //向課程基本信息表course_base寫入數據
        CourseBase courseBaseNew = new CourseBase();
        //將傳入頁面的參數放到courseBaseNew對象
        BeanUtils.copyProperties(dto, courseBaseNew);//只要屬性名一致就能拷貝,如果空值也會拷貝
        courseBaseNew.setCompanyId(companyId);
        courseBaseNew.setCreateDate(LocalDateTime.now());
        //審核狀況默認為未提交
        courseBaseNew.setAuditStatus("202002");
        //審核狀況默認為未發佈
        courseBaseNew.setStatus("203001");
        //插入數據
        int insert = courseBaseMapper.insert(courseBaseNew);
        if (insert <= 0) {
            throw new RuntimeException("添加課程失敗");// insert<=0就明沒值
        }
        //向課程營銷表course_market寫入數據
        CourseMarket courseMarketNew = new CourseMarket();
        //將頁面輸入的數據拷貝到courseMarketNew
        BeanUtils.copyProperties(dto, courseMarketNew);
        //拿課程ID,當新增課程成功就會有id
        Long courseId = courseBaseNew.getId();
        courseMarketNew.setId(courseId);
        //保存營銷信息
        saveCourseMarket(courseMarketNew);
        //從數據庫查詢課程詳細信息,包括兩部份
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    //查詢課程信息
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {
        //從課程基本信息表查詢
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            return null;
        }
        //從課程營銷表查詢
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //組裝一起
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        //通過courseCategoryMapper查詢分類信息,將分類名稱放在courseBaseInfoDto對象
        CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setStName(courseCategoryBySt.getName());
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName());
        return courseBaseInfoDto;
    }

    //修改課程
    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        //拿到課程id
        Long courseId = editCourseDto.getId();
        //查詢課程ID
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            XueChengPlusException.cast("課程不存在"); //課程不存在,所以沒法修改
        }
        //數據合法性校驗,模塊對象已有非空判斷
        //根據具體的業務邏輯都寫在service
        //本機構只能修改本機構的課程
        if (!companyId.equals(courseBase.getCompanyId())) {
            XueChengPlusException.cast("本機構只能修改本機構的課程");
        }
        //封裝數據,將傅入來的數據覆蓋舊的數據
        BeanUtils.copyProperties(editCourseDto, courseBase);
        //修改時間
        courseBase.setChangeDate(LocalDateTime.now());
        //更新數據庫
        int i = courseBaseMapper.updateById(courseBase);
        if (i <= 0) {
            XueChengPlusException.cast("修改課程失敗");
        }
        //更新營銷信息
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(editCourseDto, courseMarket);
        saveCourseMarket(courseMarket);
        //查詢課程信息
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    //删除课程接口
    @Transactional
    @Override
    public void delectCourse(Long companyId, Long courseId) {
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (!companyId.equals(courseBase.getCompanyId()))
            XueChengPlusException.cast("只允许删除本机构的课程");
        // 删除课程教师信息
        LambdaQueryWrapper<CourseTeacher> teacherLambdaQueryWrapper = new LambdaQueryWrapper<>();
        teacherLambdaQueryWrapper.eq(CourseTeacher::getCourseId, courseId);
        courseTeacherMapper.delete(teacherLambdaQueryWrapper);
        // 删除课程计划
        LambdaQueryWrapper<Teachplan> teachplanLambdaQueryWrapper = new LambdaQueryWrapper<>();
        teachplanLambdaQueryWrapper.eq(Teachplan::getCourseId, courseId);
        teachplanMapper.delete(teachplanLambdaQueryWrapper);
        // 删除营销信息
        courseMarketMapper.deleteById(courseId);
        // 删除课程基本信息
        courseBaseMapper.deleteById(courseId);
    }


    //單獨寫一個方法保存營銷信息,邏輯:存在則更新,不存在則添加
    private int saveCourseMarket(CourseMarket courseMarketNew) {
        //參數的合法性校驗
        String charge = courseMarketNew.getCharge();
        if (StringUtils.isEmpty(charge)) {
            throw new RuntimeException("收費規則為空");
        }
        //如果課程收費,價格沒有填也要拋異常
        if (charge.equals("201001")) {
            if (courseMarketNew.getCharge() == null || courseMarketNew.getPrice().floatValue() <= 0) {
                throw new RuntimeException("課程價格不能為空且要大於0");
            }
        }
        //從數據庫中查詢營銷信息,存在則更新,不存在則添加
        Long id = courseMarketNew.getId();//主鍵,新增課程id當主鍵
        CourseMarket courseMarket = courseMarketMapper.selectById(id);
        if (courseMarket == null) {
            //插入數據
            int insert = courseMarketMapper.insert(courseMarketNew);
            return insert;
        } else {
            //將courseMarketNew拷貝到courseMarket
            BeanUtils.copyProperties(courseMarketNew, courseMarket);
            courseMarket.setId(courseMarketNew.getId());
            //更新
            int i = courseMarketMapper.updateById(courseMarket);
            return i;
        }

    }
}
