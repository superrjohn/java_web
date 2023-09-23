package com.xuecheng.base.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @description分頁查詢參數
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PageParams {
    //當前頁碼
    @ApiModelProperty("頁碼")
    private Long pageNo = 1L;
    //每頁顯示記錄
    @ApiModelProperty("每頁顯示記錄")
    private Long pageSize = 30L;
}
