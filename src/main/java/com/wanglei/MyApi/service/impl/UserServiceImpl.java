package com.wanglei.MyApi.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wanglei.MyApi.commmon.ErrorCode;
import com.wanglei.MyApi.constant.CommonConstant;
import com.wanglei.MyApi.exception.BusinessException;
import com.wanglei.MyApi.mapper.UserMapper;
import com.wanglei.MyApi.model.domain.dto.UserAkSk;
import com.wanglei.MyApi.model.domain.request.user.UserAddRequest;
import com.wanglei.MyApi.model.domain.request.user.UserQueryRequest;
import com.wanglei.MyApi.model.domain.request.user.UserRegisterRequest;
import com.wanglei.MyApi.model.domain.request.user.UserUpdatePasswordRequest;
import com.wanglei.MyApi.service.UserService;
import com.wanglei.MyApicommon.model.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.wanglei.MyApi.constant.RedisKey.GATEWAY_USER_KEY;
import static com.wanglei.MyApi.constant.UserConstant.ADMIN_ROLE;
import static com.wanglei.MyApi.constant.UserConstant.USER_LOGIN_STATE;


/**
 * @author master
 * 用户服务实现类
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Resource
    private UserMapper userMapper;
    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "muqiu";

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public long UserRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String captcha = userRegisterRequest.getCaptcha();
        String captchaKey = userRegisterRequest.getCaptchaKey();

        //1、校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, captcha, captchaKey)) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 确认验证码
        String validCode = (String) redisTemplate.opsForValue().get(captchaKey);
        if (validCode == null || !validCode.equals(captcha)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }

        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度小于4位");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度小于8位");
        }

        //账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能包含特殊字符");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不同");
        }
        //账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已注册");
        }

//        //校验编号不能重复
//        queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("acptCode", acptCode);
//        count = userMapper.selectCount(queryWrapper);
//        if (count > 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR,"编号重复");
//        }


        //2、加密
        String entryptfPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        String accessKey = DigestUtil.md5Hex((SALT + userAccount + RandomUtil.randomNumbers(5)).getBytes());
        String secretKey = DigestUtil.md5Hex((SALT + userAccount + RandomUtil.randomNumbers(8)).getBytes());

        //3、插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(entryptfPassword);
        user.setAccessKey(accessKey);
        user.setSecretKey(secretKey);
        user.setUsername(userAccount);

        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        return user.getId();
    }

    @Override
    public User doLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1、校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度小于4位");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度小于8位");
        }

        //账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }

        //2、加密
        String entryptfPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        //查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", entryptfPassword);
        User user = userMapper.selectOne(queryWrapper);
        //
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }


        //3、脱敏
        User safetUser = getSafetUser(user);
        //4、记录用户登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetUser);
        return safetUser;
    }

    /**
     * 用户脱敏
     *
     * @param user 用户
     * @return 脱敏后的信息
     */
    @Override
    public User getSafetUser(User user) {
        User safetUser = new User();
        safetUser.setId(user.getId());
        safetUser.setUsername(user.getUsername());
        safetUser.setUserAccount(user.getUserAccount());
        safetUser.setAvatarUrl(user.getAvatarUrl());
        safetUser.setGender(user.getGender());
        safetUser.setPhone(user.getPhone());
        safetUser.setEmail(user.getEmail());
        safetUser.setAcptCode(user.getAcptCode());
        safetUser.setUserRole(user.getUserRole());
        safetUser.setUserStatus(user.getUserStatus());
        safetUser.setCreateTime(user.getCreateTime());
        safetUser.setAccessKey(user.getAccessKey());
        return safetUser;

    }

    @Override
    public int userLogout(HttpServletRequest request) {
        //移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;

    }

    /**
     * 判断是否为管理员
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getUserRole() == ADMIN_ROLE;
    }

    @Override
    public UserAkSk getKey(User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", loginUser.getId());
        User user = userMapper.selectOne(queryWrapper);
        UserAkSk userAkSk = new UserAkSk();
        userAkSk.setAccessKey(user.getAccessKey());
        userAkSk.setSecretKey(user.getSecretKey());
        return userAkSk;

    }

    @Override
    public Boolean addUser(UserAddRequest userAddRequest, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        String userAccount = user.getUserAccount();
        if (StringUtils.isAnyBlank(userAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度小于4位");
        }
        user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + "12345678").getBytes()));
        String accessKey = DigestUtil.md5Hex((SALT + userAccount + RandomUtil.randomNumbers(5)).getBytes());
        String secretKey = DigestUtil.md5Hex((SALT + userAccount + RandomUtil.randomNumbers(8)).getBytes());
        user.setAccessKey(accessKey);
        user.setSecretKey(secretKey);
        boolean result = this.save(user);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加失败");
        }
        return true;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        Long id = userQueryRequest.getId();
        String username = userQueryRequest.getUsername();
        String userAccount = userQueryRequest.getUserAccount();
        String sortOrder = userQueryRequest.getSortOrder();
        String sortField = userQueryRequest.getSortField();


        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(username), "username", username);
        queryWrapper.like(StringUtils.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        return queryWrapper;
    }

    @Override
    public UserAkSk rebuildKey(User user) {
        String userAccount = user.getUserAccount();
        String newAccessKey = DigestUtil.md5Hex((SALT + userAccount + RandomUtil.randomNumbers(5)).getBytes());
        String newSecretKey = DigestUtil.md5Hex((SALT + userAccount + RandomUtil.randomNumbers(8)).getBytes());
        user.setAccessKey(newAccessKey);
        user.setSecretKey(newSecretKey);
        boolean result = this.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成失败");
        }
        //删除缓存
        if(Boolean.TRUE.equals(redisTemplate.hasKey(GATEWAY_USER_KEY+user.getAccessKey()))){
            redisTemplate.delete(GATEWAY_USER_KEY+user.getAccessKey());
        }
        return new UserAkSk(newAccessKey, newSecretKey);
    }

    @Override
    public boolean updatePassword(User loginUser, UserUpdatePasswordRequest userUpdatePasswordRequest) {
        Long id = userUpdatePasswordRequest.getId();
        String password = userUpdatePasswordRequest.getPassword();
        String checkPassword = userUpdatePasswordRequest.getCheckPassword();

        if (StringUtils.isNotBlank(password) && StringUtils.isNotBlank(checkPassword)) {
            if (password.length() < 8) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度小于8位");
            }
            if(!password.equals(checkPassword)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
            }
            String updatePassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
            User oldUser = this.getById(id);
            if (oldUser.getUserPassword().equals(updatePassword)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能修改相同密码");
            }
            User user = new User();
            user.setId(id);
            user.setUserPassword(updatePassword);
            boolean result = this.updateById(user);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
            }
            return true;
        }else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码或校验密码不能为空");
        }
    }


    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        return (User) userObj;
    }

}




