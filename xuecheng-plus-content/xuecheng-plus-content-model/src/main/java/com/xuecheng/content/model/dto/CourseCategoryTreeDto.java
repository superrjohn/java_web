
package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseCategory;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description 课程分类树型结点dto
 * @date 2022/9/7 15:16
 */
@Data
public class CourseCategoryTreeDto extends CourseCategory implements Serializable {
    //擴展子節點
    List<CourseCategoryTreeDto> childrenTreeNodes;
} 
 