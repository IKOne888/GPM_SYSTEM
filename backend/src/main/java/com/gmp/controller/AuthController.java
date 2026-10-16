package com.gmp.controller;

import com.gmp.common.Result;
import com.gmp.dto.LoginDTO;
import com.gmp.dto.LoginVO;
import com.gmp.dto.auth.AdminPasswordUpdateDTO;
import com.gmp.dto.auth.AdminProfileUpdateDTO;
import com.gmp.dto.auth.AdminProfileVO;
import com.gmp.service.AuthService;
import com.gmp.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

/**
 * 认证Controller
 * <p>
 * 统一登录/登出入口，与前端契约对齐（POST /auth/login）。
 * 登录前需先调用 GET /auth/captcha 获取图形验证码。
 */
@Api(tags = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysUserService userService;

    @ApiOperation("获取图形验证码")
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        return Result.success(authService.generateCaptcha());
    }

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO loginVO = authService.login(dto);
        return Result.success(loginVO);
    }

    @ApiOperation("用户登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }

    // ==================== 个人中心（登录后自助维护） ====================
    // 注意：以下接口依赖全局 checkLogin 兜底，严禁加入 Sa-Token 白名单，否则将导致未登录可改密

    @ApiOperation("查询当前登录用户资料")
    @GetMapping("/profile")
    public Result<AdminProfileVO> profile() {
        return Result.success(userService.currentProfile());
    }

    @ApiOperation("修改当前登录用户资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody AdminProfileUpdateDTO dto) {
        userService.updateCurrentProfile(dto);
        return Result.success();
    }

    @ApiOperation("修改当前登录用户密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody AdminPasswordUpdateDTO dto) {
        userService.updateCurrentPassword(dto);
        return Result.success();
    }
}
