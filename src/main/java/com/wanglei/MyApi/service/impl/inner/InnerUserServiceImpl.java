package com.wanglei.MyApi.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wanglei.MyApi.commmon.ErrorCode;
import com.wanglei.MyApi.exception.BusinessException;
import com.wanglei.MyApi.mapper.UserMapper;
import com.wanglei.MyApi.service.UserService;
import com.wanglei.MyApicommon.model.User;
import com.wanglei.MyApicommon.service.InnerUserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@DubboService
public class InnerUserServiceImpl implements InnerUserService {
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public static final String GATEWAY_USER_KEY = "gatewayUser:";

    @Override
    public User getInvokeUser(String accessKey) {
        if (StringUtils.isAnyBlank(accessKey)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User safeuser = null;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(GATEWAY_USER_KEY + accessKey))) {
            safeuser = (User) redisTemplate.opsForValue().get(GATEWAY_USER_KEY + accessKey);
        } else {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("accessKey", accessKey);
            User user = userService.getOne(queryWrapper);
            safeuser = userService.getSafetUser(user);
            redisTemplate.opsForValue().set(GATEWAY_USER_KEY + accessKey, safeuser, 1, TimeUnit.DAYS);
        }
        return safeuser;
    }
}
