package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.exception.XueChengPlusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


@Slf4j
@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachplanDtos;
    }

    @Transactional
    @Override
    public void saveTeachplan(SaveTeachplanDto teachplanDto) {

        //课程计划id
        Long id = teachplanDto.getId();
        //修改课程计划,沒有id說明是新增,有則修改
        if (id != null) {
            Teachplan teachplan = teachplanMapper.selectById(id);
            BeanUtils.copyProperties(teachplanDto, teachplan);
            teachplanMapper.updateById(teachplan);
        } else {
            //取出同父同级别的课程计划数量
            int count = getTeachplanCount(teachplanDto.getCourseId(), teachplanDto.getParentid());
            Teachplan teachplanNew = new Teachplan();
            //设置排序号
            teachplanNew.setOrderby(count + 1);
            BeanUtils.copyProperties(teachplanDto, teachplanNew);

            teachplanMapper.insert(teachplanNew);

        }

    }


    /**
     * @param courseId 课程id
     * @param parentId 父课程计划id
     * @return int 最新排序号
     * @description 获取最新的排序号
     * @author Mr.M
     * @date 2022/9/9 13:43
     */
    private int getTeachplanCount(long courseId, long parentId) {
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, courseId);
        queryWrapper.eq(Teachplan::getParentid, parentId);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count;
    }

    /**
     * @param teachPlanId
     * @description刪除課程
     */
    @Transactional
    @Override
    public void removeTeachPlan(Long teachPlanId) {
        //取出课程计划
        Teachplan teachplan = teachplanMapper.selectById(teachPlanId);
        if (teachplan == null)
            XueChengPlusException.cast("课程计划不存在。");

        //课程id
        Long courseId = teachplan.getCourseId();

        //课程信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        //审核状态
        String auditStatus = courseBase.getAuditStatus();

        //只有当课程是未提交时方可删除
        if (!"202002".equals(auditStatus)) {
            XueChengPlusException.cast("删除失败，课程审核状态是未提交时方可删除。");
        }

        //如果课程计划为第一级分类，下边没有子课程计划方可删除
        Integer grade = teachplan.getGrade();
        if (grade == 1) {
            //查詢課程計劃(Teachplan)表中,id(大節點)中有多少個小節點
            Integer count = teachplanMapper.selectCount(new LambdaQueryWrapper<Teachplan>().eq(Teachplan::getParentid, teachplan.getId()));
            if (count > 0) {
                XueChengPlusException.cast("删除失败，课程计划下有子课程计划。");
            }
        }

        //删除课程计划
        teachplanMapper.deleteById(teachPlanId);
        //删除课程计划与媒资的关联信息
        teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getTeachplanId, teachplan.getId()));
    }

    @Override
    public void moveTeachPlan(Long teachPlanId, String moveType) {

        //课程计划
        Teachplan teachplan = teachplanMapper.selectById(teachPlanId);

        //查询同级别的课程计划
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, teachplan.getCourseId()).eq(Teachplan::getParentid, teachplan.getParentid());

        List<Teachplan> teachplans = teachplanMapper.selectList(queryWrapper);

        //如果同级别只有一个课程计划，什么也不处理
        if (teachplans.size() <= 1) {
            return;
        }


        //根据移动类型进行排序
        if (moveType.equals("moveup")) {//上移，找到比当前计划小的，交换位置
            //降序，先找到当前计划，下一个就是要和他交换位置的计划
            Collections.sort(teachplans, new Comparator<Teachplan>() {
                @Override
                public int compare(Teachplan o1, Teachplan o2) {
                    return o2.getOrderby() - o1.getOrderby();
                }
            });
        } else {
            //升序
            Collections.sort(teachplans, new Comparator<Teachplan>() {
                @Override
                public int compare(Teachplan o1, Teachplan o2) {
                    return o1.getOrderby() - o2.getOrderby();
                }
            });
        }

        //找到当前计划
        Teachplan one = null;
        Teachplan two = null;
        Iterator<Teachplan> iterator = teachplans.iterator();
        while (iterator.hasNext()) {
            Teachplan next = iterator.next();
            if (next.getId().equals(teachplan.getId())) {
                one = next;
                try {
                    Teachplan next1 = iterator.next();
                    two = next1;
                } catch (Exception e) {

                }
            }
        }

        swapTeachplan(one, two);

    }

    @Transactional
    @Override
    public TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        //教学计划id
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan==null){
            XueChengPlusException.cast("教学计划不存在");
        }

        //先刪除原有記錄,根據課程計劃id刪除它所綁定的媒資,delete方法要newLambda查詢,泛型指定要查的表,eq是參數1要和參數2相同
        int delete = teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getTeachplanId,
                bindTeachplanMediaDto.getTeachplanId()));
        //再添加新記錄
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachplanMediaDto,teachplanMedia);
        teachplanMedia.setCourseId(teachplan.getCourseId());
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMediaMapper.insert(teachplanMedia);
        return null;
    }

    //交换位置
    private void swapTeachplan(Teachplan left, Teachplan right) {
        if (left == null || right == null) {
            return;
        }
        Integer orderby_left = left.getOrderby();
        Integer orderby_right = right.getOrderby();
        left.setOrderby(orderby_right);
        right.setOrderby(orderby_left);
        teachplanMapper.updateById(left);
        teachplanMapper.updateById(right);
        log.debug("课程计划交换位置，left:{},right:{}", left.getId(), right.getId());
    }
}