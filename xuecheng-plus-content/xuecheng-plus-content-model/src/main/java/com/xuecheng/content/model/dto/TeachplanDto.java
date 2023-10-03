package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description 课程计划树型结构dto
 * @date 2022/9/9 10:27
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TeachplanDto extends Teachplan {

    //课程计划关联的媒资信息
    private TeachplanMedia teachplanMedia;

    //子结点
    private List<TeachplanDto> teachPlanTreeNodes;

}