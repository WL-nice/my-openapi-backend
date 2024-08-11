package com.wanglei.MyApi.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wanglei.MyApi.commmon.ErrorCode;
import com.wanglei.MyApi.exception.BusinessException;
import com.wanglei.MyApi.model.domain.request.userInterfaceInfo.UserInterfaceInfoAddRequest;
import com.wanglei.MyApi.service.UserInterfaceInfoService;
import com.wanglei.MyApi.mapper.UserInterfaceInfoMapper;
import com.wanglei.MyApicommon.model.UserInterfaceInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author admin
 * @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service实现
 * @createDate 2024-03-16 10:55:46
 */
@Service
public class UserInterfaceInfoServiceImpl extends ServiceImpl<UserInterfaceInfoMapper, UserInterfaceInfo>
        implements UserInterfaceInfoService {
    @Resource
    UserInterfaceInfoMapper userInterfaceInfoMapper;

    @Override
    public void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add) {
        if (userInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Integer leftNum = userInterfaceInfo.getLeftNum();
        if (add && (userInterfaceInfo.getInterfaceInfoId() <= 0 || userInterfaceInfo.getUserId() <= 0)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口或用户不存在");

        }
        if (leftNum <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "调用次数不能小于0");
        }
    }

    @Override
    @Transactional
    public boolean invokeCount(long userId, long interfaceInfoId) {
        QueryWrapper<UserInterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("interfaceInfoId", interfaceInfoId);
        UserInterfaceInfo userInterfaceInfo = userInterfaceInfoMapper.selectOne(queryWrapper);
        if (userInterfaceInfo == null) {
            UserInterfaceInfo userInterfaceInfoAdd = new UserInterfaceInfo();
            userInterfaceInfoAdd.setUserId(userId);
            userInterfaceInfoAdd.setInterfaceInfoId(interfaceInfoId);
            userInterfaceInfoAdd.setTotalNum(0);
            userInterfaceInfoAdd.setLeftNum(100);
            this.save(userInterfaceInfoAdd);
        } else if(userInterfaceInfo.getLeftNum() <= 0) {
            log.error("调用次数不足");
           return false;
        }

        UpdateWrapper<UserInterfaceInfo> updateQueryWrapper = new UpdateWrapper<>();
        updateQueryWrapper.eq("userId", userId);
        updateQueryWrapper.eq("interfaceInfoId", interfaceInfoId);
        updateQueryWrapper.gt("leftNum", 0);
        updateQueryWrapper.setSql("leftNum = leftNum - 1, totalNum = totalNum + 1");
        return this.update(updateQueryWrapper);
    }

    @Override
    @Transactional
    public boolean recoverInvokeCount(Long userId, Long interfaceInfoId) {
        if (userId<0 || interfaceInfoId<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户或接口不存在");
        }
        UpdateWrapper<UserInterfaceInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("userId",userId);
        updateWrapper.eq("interfaceInfoId",interfaceInfoId);
        updateWrapper.gt("leftNum",0);
        updateWrapper.setSql("totalNum = totalNum -1,leftNum = leftNum+1");
        return this.update(updateWrapper);
    }
}




