package com.wanglei.MyApi.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wanglei.MyApi.utils.RedissonLockUtil;
import com.wanglei.MyApicommon.model.InterfaceInfo;
import com.wanglei.MyApicommon.service.InnerInterfaceInfoService;
import com.wanglei.MyApi.commmon.ErrorCode;
import com.wanglei.MyApi.exception.BusinessException;
import com.wanglei.MyApi.mapper.InterfaceInfoMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@DubboService
public class InnerInterfaceInfoServiceImpl implements InnerInterfaceInfoService {

    @Resource
    private InterfaceInfoMapper interfaceInfoMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonLockUtil redissonLockUtil;

    public static final String INTERFACE_INFO_KEY = "interfaceInfo:";

    @Override
    public InterfaceInfo getInterfaceInfo(String url, String method) {
        if (StringUtils.isAnyBlank(url, method)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(INTERFACE_INFO_KEY + url + ":" + method))) {
            return (InterfaceInfo) redisTemplate.opsForValue().get(INTERFACE_INFO_KEY + url + ":" + method);
        }
        return redissonLockUtil.redissonDistributedLocks("gateway:interfaceKey:"+url,()->{
            QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("url", url);
            queryWrapper.eq("method", method);
            InterfaceInfo interfaceInfo = interfaceInfoMapper.selectOne(queryWrapper);
            redisTemplate.opsForValue().set(INTERFACE_INFO_KEY + url + ":" + method, interfaceInfo, 1, TimeUnit.DAYS);
            return interfaceInfo;
        });
    }
}
