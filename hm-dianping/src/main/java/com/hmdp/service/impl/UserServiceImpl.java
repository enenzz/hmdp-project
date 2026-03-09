package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * 发送手机验证码
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号是否合格
        if (RegexUtils.isPhoneInvalid(phone)) {
            //不合格，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        //合格,生成6位验证码
        String code = RandomUtil.randomNumbers(6);
        //保存验证码到session
        session.setAttribute("code", code);
        // TODO 发送验证码
        log.debug("发送验证码成功，验证码： {}", code);
        //返回ok
        return Result.ok();
    }

    /**
     * 登录功能
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号输入错误！");
        }
        //校验验证码
        Object cacheCode = session.getAttribute("code");
        String code = loginForm.getCode();
        if (cacheCode == null || !code.equals(cacheCode)) {
            return Result.fail("验证码输入错误!");
        }
        //一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();
        //用户不存在，创建新用户并保存到数据库中
        if (user == null) {
            user = createNewUser(phone);
        }
        //将用户保存到session中
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, UserDTO.class);
        session.setAttribute("user", userDTO);
        //返回ok
        return Result.ok();
    }

    /**
     * 创建新用户并保存到数据库中
     * @param phone
     *
     * @return
     */
    private User createNewUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        //生成随机用户名
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(5));
        //保存用户
        save(user);
        return user;
    }
}
