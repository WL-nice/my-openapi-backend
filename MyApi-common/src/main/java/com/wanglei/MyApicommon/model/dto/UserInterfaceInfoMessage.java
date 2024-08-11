package com.wanglei.MyApicommon.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInterfaceInfoMessage {
    private Long userId;
    private Long interfaceInfoId;
}
