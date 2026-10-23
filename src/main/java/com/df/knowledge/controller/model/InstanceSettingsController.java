package com.df.knowledge.controller.model;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.controller.model.vo.InstanceSettingsSaveReqVO;
import com.df.knowledge.controller.model.vo.InstanceSettingsRespVO;
import com.df.knowledge.service.model.InstanceSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.df.knowledge.common.CommonResult.success;

/** 实例运行设置，只有实例管理员可读写。 */
@RestController
@RequestMapping("/system/settings")
@Validated
@SaCheckRole("instance_admin")
@Tag(name = "instanceSettingsController")
@RequiredArgsConstructor
public class InstanceSettingsController {

    private final InstanceSettingsService service;

    @GetMapping("/get")
    @Operation(summary = "获取实例设置")
    public CommonResult<InstanceSettingsRespVO> get() {
        return success(BeanUtil.copyProperties(service.get(), InstanceSettingsRespVO.class));
    }

    @PutMapping("/update")
    @Operation(summary = "保存实例设置")
    public CommonResult<Boolean> update(@Valid @RequestBody InstanceSettingsSaveReqVO request) {
        service.update(request);
        return success(true);
    }
}
