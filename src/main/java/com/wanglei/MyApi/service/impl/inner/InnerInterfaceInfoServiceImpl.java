package com.wanglei.MyApi.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wanglei.MyApi.commmon.ErrorCode;
import com.wanglei.MyApi.exception.BusinessException;
import com.wanglei.MyApi.mapper.InterfaceInfoMapper;
import com.wanglei.MyApi.utils.RedissonLockUtil;
import com.wanglei.MyApicommon.model.InterfaceInfo;
import com.wanglei.MyApicommon.service.InnerInterfaceInfoService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static com.wanglei.MyApi.constant.RedisKey.GATEWAY_INTERFACE_LOCK;
import static com.wanglei.MyApi.constant.RedisKey.INTERFACE_KEY;

@DubboService
public class InnerInterfaceInfoServiceImpl implements InnerInterfaceInfoService {

    @Resource
    private InterfaceInfoMapper interfaceInfoMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonLockUtil redissonLockUtil;


    @Override
    public InterfaceInfo getInterfaceInfo(String url, String method) {
        if (StringUtils.isAnyBlank(url, method)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(INTERFACE_KEY + url))) {
            return (InterfaceInfo) redisTemplate.opsForValue().get(INTERFACE_KEY + url);
        }
        return redissonLockUtil.redissonDistributedLocks(GATEWAY_INTERFACE_LOCK + url, () -> {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(INTERFACE_KEY + url))) {
                return (InterfaceInfo) redisTemplate.opsForValue().get(INTERFACE_KEY + url);
            }
            QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("url", url);
            queryWrapper.eq("method", method);
            InterfaceInfo interfaceInfo = interfaceInfoMapper.selectOne(queryWrapper);
            if(interfaceInfo == null){
                redisTemplate.opsForValue().set(INTERFACE_KEY + url, new InterfaceInfo(), 1, TimeUnit.MINUTES);
            }else{
                redisTemplate.opsForValue().set(INTERFACE_KEY + url, interfaceInfo, 1, TimeUnit.DAYS);
            }

            return interfaceInfo;
        });
    }
}
