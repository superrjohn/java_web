package com.xuecheng.ucenter.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author Mr.M
 * @version 1.0
 * @description 账号密码认证
 * @date 2022/9/29 12:12
 */
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CheckCodeClient checkCodeClient;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //帳號
        String username = authParamsDto.getUsername();
        //前端輸入的驗證碼
        String checkcode = authParamsDto.getCheckcode();
        //驗證碼的key
        String checkcodekey = authParamsDto.getCheckcodekey();
        //校驗驗證碼
        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if(verify ==null || !verify){
            throw new RuntimeException("驗證碼輸入錯誤!");
        }
        //帳號是否存在
        //根據username查詢數據庫
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        //查詢到用戶不存在,返回null,spring security框架拋出異常
        if (xcUser == null) {
            throw new RuntimeException("帳號不存在");
        }
        //驗證密碼是否正確
        // 如果查詢到用戶拿到正常密碼,最終封裝成一個UserDetails對象給spring security框架返回,由框架進行比對密碼
        String passwordDb = xcUser.getPassword();
        String passwordFrom = authParamsDto.getPassword();
        //校驗密碼
        boolean matches = passwordEncoder.matches(passwordFrom, passwordDb);
        if (!matches) {
            throw new RuntimeException("帳號密碼有錯!");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);
        return xcUserExt;
    }
}