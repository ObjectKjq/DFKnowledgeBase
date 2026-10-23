package com.df.knowledge.controller.model;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.controller.model.vo.ModelConfigurationRespVO;
import com.df.knowledge.controller.model.vo.ModelConfigurationSaveReqVO;
import com.df.knowledge.controller.model.vo.ModelConfigurationSimpleRespVO;
import com.df.knowledge.dal.dataobject.model.ModelConfigurationDO;
import com.df.knowledge.service.model.ModelConfigurationService;
import com.df.knowledge.service.model.ModelConfigurationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.df.knowledge.common.CommonResult.success;

/** 管理员维护接入配置，普通用户只读取编号、名称及模拟标记。 */
@RestController
@RequestMapping("/system/model")
@Validated
@Tag(name = "modelConfigurationController")
@RequiredArgsConstructor
public class ModelConfigurationController {

    private final ModelConfigurationService service;

    @PostMapping("/create")
    @Operation(summary = "新增命名模型")
    @SaCheckRole("instance_admin")
    public CommonResult<Long> create(@Valid @RequestBody ModelConfigurationSaveReqVO request) {
        return success(service.create(request));
    }

    @PutMapping("/update")
    @Operation(summary = "更新命名模型")
    @SaCheckRole("instance_admin")
    public CommonResult<Boolean> update(@Valid @RequestBody ModelConfigurationSaveReqVO request) {
        service.update(request);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除模型")
    @SaCheckRole("instance_admin")
    @Parameter(name = "id", description = "配置编号", required = true)
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        service.delete(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "查询模型，不返回密钥")
    @SaCheckRole("instance_admin")
    @Parameter(name = "id", description = "配置编号", required = true)
    public CommonResult<ModelConfigurationRespVO> get(@RequestParam("id") Long id) {
        return success(toResponse(service.get(id)));
    }

    @GetMapping("/list")
    @Operation(summary = "列出模型配置")
    @SaCheckRole("instance_admin")
    public CommonResult<List<ModelConfigurationRespVO>> list() {
        return success(service.list().stream().map(this::toResponse).toList());
    }

    @GetMapping("/simple-list")
    @Operation(summary = "用户可选择的命名对话模型")
    public CommonResult<List<ModelConfigurationSimpleRespVO>> simpleList() {
        return success(service.listChatModels().stream()
                .map(model -> new ModelConfigurationSimpleRespVO(model.getId(), model.getName(),
                        ModelConfigurationServiceImpl.isSimulated(model))).toList());
    }

    @PostMapping("/test")
    @Operation(summary = "真实测试连接并保存状态和测试时间；模拟项返回 SIMULATED")
    @SaCheckRole("instance_admin")
    @Parameter(name = "id", description = "配置编号", required = true)
    public CommonResult<ModelConfigurationRespVO> test(@RequestParam("id") Long id) {
        return success(toResponse(service.test(id)));
    }

    private ModelConfigurationRespVO toResponse(ModelConfigurationDO model) {
        return BeanUtil.copyProperties(model, ModelConfigurationRespVO.class)
                .setHasApiKey(model.getApiKey() != null && !model.getApiKey().isBlank())
                .setSimulated(ModelConfigurationServiceImpl.isSimulated(model));
    }
}
