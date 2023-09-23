package com.xuecheng.content.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @description課程查詢條件模型
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class QueryCourseParamsDto {
    //審核狀態
    private String auditStatus;
    //課程名稱
    private String courseName;
    //發布狀況
    private String publishStatus;
}
