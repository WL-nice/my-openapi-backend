package com.wanglei.MyApi.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wanglei.MyApi.commmon.ErrorCode;
import com.wanglei.MyApi.constant.CommonConstant;
import com.wanglei.MyApi.constant.RedisKey;
import com.wanglei.MyApi.exception.BusinessException;
import com.wanglei.MyApi.mapper.InterfaceInfoMapper;
import com.wanglei.MyApi.model.domain.enums.InterfaceStatus;
import com.wanglei.MyApi.model.domain.request.interfaceInfo.InterfaceInfoInvokeRequest;
import com.wanglei.MyApi.model.domain.request.interfaceInfo.InterfaceInfoQueryRequest;
import com.wanglei.MyApi.model.domain.request.interfaceInfo.InterfaceInfoUpdateRequest;
import com.wanglei.MyApi.model.domain.vo.InterfaceInfoVO;
import com.wanglei.MyApi.service.InterfaceInfoService;
import com.wanglei.MyApi.service.UserService;
import com.wanglei.MyApi.utils.RedissonLockUtil;
import com.wanglei.MyApicommon.model.InterfaceInfo;
import com.wanglei.MyApicommon.model.User;
import com.wanglei.myapiclientsdk.utils.SignUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;


/**
 * @author admin
 * @description 针对表【interface_info(接口信息)】的数据库操作Service实现
 * @createDate 2024-03-16 10:55:25
 */
@Service
public class InterfaceInfoServiceImpl extends ServiceImpl<InterfaceInfoMapper, InterfaceInfo>
        implements InterfaceInfoService {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonLockUtil redissonLockUtil;


    @Override
    public void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean add) {

        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = interfaceInfo.getName();
        String description = interfaceInfo.getDescription();
        String url = interfaceInfo.getUrl();
        String requestParams = interfaceInfo.getRequestParams();
        String method = interfaceInfo.getMethod();


        // 创建时，所有参数必须非空
        if (add && (StringUtils.isAnyBlank(name))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);

        }
        if (StringUtils.isBlank(requestParams)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (StringUtils.isBlank(description)
                || StringUtils.isBlank(url)
                || StringUtils.isBlank(method)
        ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isNotBlank(name) && name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长");
        }
    }

    @Override
    public QueryWrapper<InterfaceInfo> getQueryWrapper(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        Long id = interfaceInfoQueryRequest.getId();
        String name = interfaceInfoQueryRequest.getName();
        String description = interfaceInfoQueryRequest.getDescription();
        String url = interfaceInfoQueryRequest.getUrl();
        Integer status = interfaceInfoQueryRequest.getStatus();
        String method = interfaceInfoQueryRequest.getMethod();
        Long userId = interfaceInfoQueryRequest.getUserId();
        String sortOrder = interfaceInfoQueryRequest.getSortOrder();
        String sortField = interfaceInfoQueryRequest.getSortField();

        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(userId != null && userId > 0, "userId", userId);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        queryWrapper.like(StringUtils.isNotBlank(url), "url", url);
        queryWrapper.eq(StringUtils.isNotBlank(method), "method", method);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        return queryWrapper;
    }

    @Override
    public String invokeInterfaceInfo(InterfaceInfoInvokeRequest interfaceInfoInvokeRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 根据请求信息获取接口信息
        Long id = interfaceInfoInvokeRequest.getId();
        String userRequestParams = interfaceInfoInvokeRequest.getUserRequestParams();
        InterfaceInfo oldInterfaceInfo = null;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(RedisKey.INTERFACE_KEY + id))) {
            oldInterfaceInfo = (InterfaceInfo) redisTemplate.opsForValue().get(RedisKey.INTERFACE_KEY + id);
        } else {
            oldInterfaceInfo = this.getById(id);
        }
        // 接口信息存在性校验
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "未发现接口");
        }
        if (oldInterfaceInfo.getStatus().equals(InterfaceStatus.offline.getCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口已下线");
        }
        User usert = userService.getById(loginUser.getId());
        String accessKey = usert.getAccessKey();
        String secretKey = usert.getSecretKey();
        String method = oldInterfaceInfo.getMethod();
        String url = oldInterfaceInfo.getUrl();
        if (method.equals("POST")) {
            try (cn.hutool.http.HttpResponse httpResponse = HttpRequest.post(url)
                    .addHeaders(getHeaderMap(userRequestParams, accessKey, secretKey))
                    .body(userRequestParams)
                    .execute()) {
                return httpResponse.body();
            }
        } else if (method.equals("GET")) {
            try (cn.hutool.http.HttpResponse httpResponse = HttpRequest.get(url)
                    .addHeaders(getHeaderMap(userRequestParams, accessKey, secretKey))
                    .execute()) {
                return httpResponse.body();
            }
        }
        return null;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, int status) {
        //判断是否存在
        InterfaceInfo oldInterfaceInfo = this.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "未发现接口");
        }

        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(status);
        return this.updateById(interfaceInfo);
    }

    @Override
    @Transactional
    public boolean updateInterfaceInfo(InterfaceInfoUpdateRequest interfaceInfoUpdateRequest) {
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoUpdateRequest, interfaceInfo);
        long id = interfaceInfoUpdateRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = this.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "未发现接口");
        }
        boolean result = this.updateById(interfaceInfo);
        // 判断缓存是否存在，存在则删除缓存
        if (Boolean.TRUE.equals(redisTemplate.hasKey(RedisKey.INTERFACE_KEY + id))) {
            redisTemplate.delete(RedisKey.INTERFACE_KEY + id);
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(INTERFACE_KEY + oldInterfaceInfo.getUrl()))) {
            redisTemplate.delete(INTERFACE_KEY + oldInterfaceInfo.getUrl());
        }
        return result;
    }

    @Override
    public InterfaceInfoVO getInterfaceInfoVOById(long id) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(RedisKey.INTERFACE_KEY + id))) {
            return (InterfaceInfoVO) redisTemplate.opsForValue().get(RedisKey.INTERFACE_KEY + id);
        }
        // 使用分布式锁，防止缓存击穿
        return redissonLockUtil.redissonDistributedLocks("interfaceVO:id", () -> {
            //双重判定锁
            if (Boolean.TRUE.equals(redisTemplate.hasKey(RedisKey.INTERFACE_KEY + id))) {
                return (InterfaceInfoVO) redisTemplate.opsForValue().get(RedisKey.INTERFACE_KEY + id);
            }
            InterfaceInfoVO interfaceInfoVO = new InterfaceInfoVO();
            InterfaceInfo interfaceInfo = this.getById(id);
            // 防止缓存穿透
            if (interfaceInfo == null) {
                redisTemplate.opsForValue().set(RedisKey.INTERFACE_KEY + id, interfaceInfoVO, 60, TimeUnit.SECONDS);
                throw new BusinessException(ErrorCode.NULL_ERROR, "未发现接口");
            }
            BeanUtils.copyProperties(interfaceInfo, interfaceInfoVO);
            redisTemplate.opsForValue().set(RedisKey.INTERFACE_KEY + id, interfaceInfoVO, 1, TimeUnit.DAYS);
            return interfaceInfoVO;
        }, "获取失败");
    }

    @Override
    public void getSdk(HttpServletResponse response) throws IOException {
        // 获取要下载的文件
        org.springframework.core.io.Resource resource = new ClassPathResource("MyApi-client-sdk-0.0.1.jar");
        InputStream inputStream = resource.getInputStream();

        // 设置响应头
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=MyApi-client-sdk-0.0.1.jar");

        // 将文件内容写入响应
        try (OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
        } catch (IOException e) {
            // 处理异常
            e.printStackTrace();
        } finally {
            inputStream.close();
        }
    }

    private Map<String, String> getHeaderMap(String body, String accessKey, String secretKet) {
        Map<String, String> hashMap = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        hashMap.put("nonce", RandomUtil.randomNumbers(5));
        hashMap.put("body", body);
        hashMap.put("timestamp", timestamp);
        hashMap.put("accessKey", accessKey);
        hashMap.put("sign", SignUtils.getSign(timestamp,accessKey, secretKet));
        return hashMap;
    }

}





