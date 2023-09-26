package com.xuecheng.content.service.impl;


import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //調用mapper递歸查詢出分類信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);

        //找到每個節點的子節點,最終封裝成List<CourseCategoryTreeDto>
        //先將list轉成map,key就是節點的id,value就是CourseCategoryTreeDto對象,目的就是為了方便從map獲得節點,filter把根節點排除
        Map<String,CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos.stream()
                .filter(item->!id.equals(item.getId()))
                .collect(Collectors.toMap(key->key.getId(),value->value,(key1,key2)->key2));
        //定義一個list作為最終返回的list
        List<CourseCategoryTreeDto> courseCategoryList = new ArrayList<>();
        //從頭遍歷List<CourseCategoryTreeDto>,一邊遍歷一邊找子節點放在父節點的childrenTreeNodes
        courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).forEach(item ->{
            if(item.getParentid().equals(id)){
                courseCategoryList.add(item);
            }
            //找到節點的父節點
            CourseCategoryTreeDto courseCategoryParent = mapTemp.get(item.getParentid());
            if(courseCategoryParent!=null){
                if(courseCategoryParent.getChildrenTreeNodes()==null){
                    //如果該父節點的ChildrenTreeNodes屬性為空,要new一個集合,因為要向該集合中放它的子節點
                    courseCategoryParent.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                //到每個節點的子節點放在父節點的ChildrenTreeNodes屬性中
                courseCategoryParent.getChildrenTreeNodes().add(item);
            }
        });
        return courseCategoryList;
    }
}
