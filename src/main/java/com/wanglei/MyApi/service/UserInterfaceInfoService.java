package com.wanglei.MyApi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wanglei.MyApicommon.model.UserInterfaceInfo;

/**
* @author admin
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service
* @createDate 2024-03-16 10:55:46
*/
public interface UserInterfaceInfoService extends IService<UserInterfaceInfo> {

    void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add);

    /**
     * 接口调用统计
     * @param userId
     * @param interfaceInfoId
     * @return
     */
    boolean invokeCount(long userId,long interfaceInfoId);

    /**
     * 恢复调用次数
     * @param userId
     * @param interfaceInfoId
     * @return
     */
    boolean recoverInvokeCount(Long userId, Long interfaceInfoId);
}
