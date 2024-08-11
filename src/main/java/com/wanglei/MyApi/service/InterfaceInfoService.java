package com.wanglei.MyApi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wanglei.MyApi.model.domain.request.interfaceInfo.InterfaceInfoInvokeRequest;
import com.wanglei.MyApi.model.domain.request.interfaceInfo.InterfaceInfoQueryRequest;
import com.wanglei.MyApi.model.domain.request.interfaceInfo.InterfaceInfoUpdateRequest;
import com.wanglei.MyApi.model.domain.vo.InterfaceInfoVO;
import com.wanglei.MyApicommon.model.InterfaceInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.http.HttpResponse;

/**
 * @author admin
 * @description 针对表【interface_info(接口信息)】的数据库操作Service
 * @createDate 2024-03-16 10:55:25
 */
public interface InterfaceInfoService extends IService<InterfaceInfo> {

    /**
     * 校验
     *
     * @param interfaceInfo
     * @param b
     */
    void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean b);

    /**
     * 获取查询包装类
     *
     * @param interfaceInfoQueryRequest
     * @return
     */
    QueryWrapper<InterfaceInfo> getQueryWrapper(InterfaceInfoQueryRequest interfaceInfoQueryRequest);

    /**
     * 调用接口
     *
     * @param interfaceInfoInvokeRequest
     * @param request
     * @return
     */
    String invokeInterfaceInfo(InterfaceInfoInvokeRequest interfaceInfoInvokeRequest, HttpServletRequest request);

    /**
     * 更新接口状态
     *
     * @param id
     * @param status
     * @return
     */
    boolean updateStatus(Long id, int status);

    /**
     * 更新接口信息
     *
     * @param interfaceInfoUpdateRequest
     * @return
     */
    boolean updateInterfaceInfo(InterfaceInfoUpdateRequest interfaceInfoUpdateRequest);

    InterfaceInfoVO getInterfaceInfoVOById(long id);

    void getSdk(HttpServletResponse response) throws IOException;
}
