package com.greathiit.bwc.module.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访客预约请求（后台/小程序/公众号/H5 共用）。
 */
@Data
public class VisitorCreateRequest {

    @NotBlank(message = "访客姓名不能为空")
    private String visitorName;

    private String idCard;

    private String phone;

    @NotBlank(message = "来访事由不能为空")
    private String reason;

    @NotNull(message = "来访时间不能为空")
    private LocalDateTime visitTime;

    private LocalDateTime leaveTime;

    private Integer accompanyCount = 0;

    private String plateNo;

    private Long visitUserId;

    private String visitUserName;

    private String source = "PC";

    private String remark;
}
