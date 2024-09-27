package com.wanglei.MyApi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wanglei.MyApi.annotation.AuthCheck;
import com.wanglei.MyApi.commmon.*;
import com.wanglei.MyApi.constant.CrossSite;
import com.wanglei.MyApi.exception.BusinessException;
import com.wanglei.MyApi.model.domain.enums.InterfaceStatus;
import com.wanglei.MyApi.model.domain.request.interfaceInfo.InterfaceInfoAddRequest;
import com.wanglei.MyApi.model.domain.request.interfaceInfo.InterfaceInfoInvokeRequest;
import com.wanglei.MyApi.model.domain.request.interfaceInfo.InterfaceInfoQueryRequest;
import com.wanglei.MyApi.model.domain.request.interfaceInfo.InterfaceInfoUpdateRequest;
import com.wanglei.MyApi.model.domain.vo.InterfaceInfoVO;
import com.wanglei.MyApi.service.InterfaceInfoService;
import com.wanglei.MyApi.service.UserService;
import com.wanglei.MyApicommon.model.InterfaceInfo;
import com.wanglei.MyApicommon.model.User;
import com.wanglei.myapiclientsdk.client.MyApiClient;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.wanglei.MyApi.constant.UserConstant.ADMIN_ROLE;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;

@RestController
@RequestMapping("/InterfaceInfo")
@Slf4j
@CrossOrigin(origins = {"http://localhost:8000", "http://192.168.237.129", CrossSite.ALLOW_ORIGIN}, allowCredentials = "true")
public class InterfaceInfoController {

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // region 增删改查

    /**
     * 创建
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Long> addInterfaceInfo(@RequestBody InterfaceInfoAddRequest interfaceInfoAddRequest, HttpServletRequest request) {
        if (interfaceInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoAddRequest, interfaceInfo);
        interfaceInfoService.validInterfaceInfo(interfaceInfo, true);
        User loginUser = userService.getLoginUser(request);
        interfaceInfo.setUserId(loginUser.getId());
        boolean result = interfaceInfoService.save(interfaceInfo);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long newInterfaceInfoId = interfaceInfo.getId();
        return ResultUtils.success(newInterfaceInfoId);
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteInterfaceInfo(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "未发现接口");
        }
        // 仅本人或管理员可删除
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        boolean b = interfaceInfoService.removeById(id);
        // 判断缓存是否存在,存在则删除缓存
        if(Boolean.TRUE.equals(redisTemplate.hasKey(INTERFACE_KEY + id))){
            redisTemplate.delete(INTERFACE_KEY + id);
        }
        if(Boolean.TRUE.equals(redisTemplate.hasKey(INTERFACE_KEY + oldInterfaceInfo.getUrl()))){
            redisTemplate.delete(INTERFACE_KEY + oldInterfaceInfo.getUrl());
        }

        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> updateInterfaceInfo(@RequestBody InterfaceInfoUpdateRequest interfaceInfoUpdateRequest,
                                                     HttpServletRequest request) {
        if (interfaceInfoUpdateRequest == null || interfaceInfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = interfaceInfoService.updateInterfaceInfo(interfaceInfoUpdateRequest);

        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     */
    @GetMapping("/get/vo")
    @AuthCheck
    public BaseResponse<InterfaceInfoVO> getInterfaceInfoVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfoVO interfaceInfoVO = interfaceInfoService.getInterfaceInfoVOById(id);
        return ResultUtils.success(interfaceInfoVO);
    }

    /**
     * 分页获取列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<InterfaceInfo>> listInterfaceInfoByPage(
            @RequestBody InterfaceInfoQueryRequest interfaceInfoQueryRequest, HttpServletRequest request) {
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = interfaceInfoQueryRequest.getCurrent();
        long size = interfaceInfoQueryRequest.getPageSize();
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = interfaceInfoService.getQueryWrapper(interfaceInfoQueryRequest);
        Page<InterfaceInfo> interfaceInfoPage = interfaceInfoService.page(new Page<>(current, size), queryWrapper);
        User user = userService.getLoginUser(request);
        // 仅管理员可查看所有接口
        if (user == null || !user.getUserRole().equals(ADMIN_ROLE)) {
            List<InterfaceInfo> interfaceInfoList = interfaceInfoPage.getRecords().stream()
                    .filter(interfaceInfo1 -> interfaceInfo1.getStatus().equals(InterfaceStatus.online.getCode())).collect(Collectors.toList());
            interfaceInfoPage.setRecords(interfaceInfoList);
        }
        return ResultUtils.success(interfaceInfoPage);
    }

    /**
     * 发布
     */

    @PostMapping("/online")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> onlineInterfaceInfo(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = idRequest.getId();

        boolean result = interfaceInfoService.updateStatus(id, InterfaceStatus.online.getCode());

        return ResultUtils.success(result);
    }

    /**
     * 下线接口
     */
    @PostMapping("/offline")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> offlineInterfaceInfo(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //判断是否存在
        Long id = idRequest.getId();

        boolean result = interfaceInfoService.updateStatus(id, InterfaceStatus.offline.getCode());

        return ResultUtils.success(result);
    }

    /**
     * 测试调用
     */
    @PostMapping("/invoke")
    @AuthCheck
    public BaseResponse<Object> invokeInterfaceInfo(@RequestBody InterfaceInfoInvokeRequest interfaceInfoInvokeRequest,
                                                    HttpServletRequest request) {
        // 校验请求参数
        if (interfaceInfoInvokeRequest == null || interfaceInfoInvokeRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String result = interfaceInfoService.invokeInterfaceInfo(interfaceInfoInvokeRequest, request);
        return ResultUtils.success(Objects.requireNonNullElse(result, "调用失败"));
    }

    @GetMapping("/sdk")
    public void getSdk(HttpServletResponse response) throws IOException {
        interfaceInfoService.getSdk(response);
    }


}
