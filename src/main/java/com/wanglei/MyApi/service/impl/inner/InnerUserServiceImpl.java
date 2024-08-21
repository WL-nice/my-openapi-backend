package com.wanglei.MyApi.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wanglei.MyApi.commmon.ErrorCode;
import com.wanglei.MyApi.exception.BusinessException;
import com.wanglei.MyApi.service.UserService;
import com.wanglei.MyApi.utils.RedissonLockUtil;
import com.wanglei.MyApicommon.model.User;
import com.wanglei.MyApicommon.service.InnerUserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static com.wanglei.MyApi.constant.RedisKey.GATEWAY_USER_KEY;
import static com.wanglei.MyApi.constant.RedisKey.GATEWAY_USER_LOCK;

@DubboService
public class InnerUserServiceImpl implements InnerUserService {
    @Resource
    private UserService userService;

    @Resource
    private RedissonLockUtil redissonLockUtil;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public User getInvokeUser(String accessKey) {
        if (StringUtils.isAnyBlank(accessKey)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(GATEWAY_USER_KEY + accessKey))) {
            return (User) redisTemplate.opsForValue().get(GATEWAY_USER_KEY + accessKey);
        }
        return redissonLockUtil.redissonDistributedLocks(GATEWAY_USER_LOCK + accessKey, () -> {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("accessKey", accessKey);
            User user = userService.getOne(queryWrapper);
            if(user == null){
                redisTemplate.opsForValue().set(GATEWAY_USER_KEY + accessKey, new User(), 1, TimeUnit.MINUTES);
            }else{
                redisTemplate.opsForValue().set(GATEWAY_USER_KEY + accessKey, user, 1, TimeUnit.DAYS);
            }

            return user;
        });
    }
}
