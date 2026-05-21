package com.xiaoniu.aftermarket.auth.service;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xiaoniu.aftermarket.auth.dto.LoginResponse;
import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.auth.security.JwtProvider;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.user.entity.SysUserEntity;
import com.xiaoniu.aftermarket.user.mapper.SysUserMapper;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class WechatService {

    private final WxMaService wxMaService;
    private final SysUserMapper userMapper;
    private final AuthService authService;
    private final JwtProvider jwtProvider;

    public WechatService(WxMaService wxMaService,
                         SysUserMapper userMapper,
                         AuthService authService,
                         JwtProvider jwtProvider) {
        this.wxMaService = wxMaService;
        this.userMapper = userMapper;
        this.authService = authService;
        this.jwtProvider = jwtProvider;
    }

    // 微信登录：小程序扫码获取 openid，通过 openid 反查已绑定的 STORE 用户
    // PLATFORM 账户不允许微信登录（运营角色走密码登录，且微信登录仅面向门店店员场景）
    @Transactional
    public LoginResponse loginWithWechat(String code) {
        String openid = code2Session(code);

        SysUserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUserEntity>()
                        .eq(SysUserEntity::getWechatOpenid, openid)
                        .eq(SysUserEntity::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (user == null) {
            throw new BusinessException(ErrorCode.WECHAT_NOT_BOUND);
        }
        // 只允许 STORE 类型账户微信登录，PLATFORM 账户强制走密码登录
        if ("PLATFORM".equals(user.getAccountType())) {
            throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED, "该微信账号不支持此登录方式");
        }
        if (!CommonStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED, "账号已停用或不存在");
        }

        AuthenticatedUser authenticatedUser = authService.buildAuthenticatedUser(user);
        JwtProvider.JwtToken token = jwtProvider.generateAccessToken(authenticatedUser);

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        return new LoginResponse(
                token.token(),
                "Bearer",
                token.expiresAt(),
                authService.toResponse(authenticatedUser)
        );
    }

    // 绑定微信：openid 由服务端调用微信 API 获取，不接受前端传入（防伪造）
    // 一个 openid 只能绑定一个用户，一个用户也只能绑定一个 openid
    @Transactional
    public void bindWechat(Long userId, String code) {
        SysUserEntity user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() != null && user.getDeleted() != 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if ("PLATFORM".equals(user.getAccountType())) {
            throw new BusinessException(ErrorCode.PLATFORM_CANNOT_BIND_WECHAT);
        }
        if (user.getWechatOpenid() != null) {
            throw new BusinessException(ErrorCode.WECHAT_ALREADY_BOUND);
        }

        String openid = code2Session(code);

        Long existingCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUserEntity>()
                        .eq(SysUserEntity::getWechatOpenid, openid)
                        .eq(SysUserEntity::getDeleted, 0)
        );

        if (existingCount > 0) {
            throw new BusinessException(ErrorCode.WECHAT_OPENID_ALREADY_BOUND);
        }

        user.setWechatOpenid(openid);
        user.setWechatBoundAt(LocalDateTime.now());
        user.setUpdatedBy(userId);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public void unbindWechat(Long storeId, Long userId, Long operatorId) {
        SysUserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUserEntity>()
                        .eq(SysUserEntity::getStoreId, storeId)
                        .eq(SysUserEntity::getId, userId)
                        .eq(SysUserEntity::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userMapper.update(null, new LambdaUpdateWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, userId)
                .set(SysUserEntity::getWechatOpenid, null)
                .set(SysUserEntity::getWechatUnionid, null)
                .set(SysUserEntity::getWechatBoundAt, null)
                .set(SysUserEntity::getUpdatedBy, operatorId)
                .set(SysUserEntity::getUpdatedAt, LocalDateTime.now()));
    }

    // openid 来自微信服务端 code2session 接口，是绑定凭据而非前端可信输入
    // 日志中禁止打印 appSecret、session_key、openid，防止泄露导致会话劫持
    private String code2Session(String code) {
        WxMaJscode2SessionResult result;
        try {
            result = wxMaService.getUserService().getSessionInfo(code);
        } catch (WxErrorException e) {
            // 只记录错误码，不记录完整响应（响应中含 session_key）
            log.error("WeChat code2Session failed, error code: {}, error msg: {}",
                    e.getError().getErrorCode(), e.getError().getErrorMsg());
            throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED, "微信登录失败");
        } catch (Exception e) {
            log.error("WeChat code2Session unexpected error", e);
            throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED, "微信登录失败");
        }
        if (result == null || result.getOpenid() == null || result.getOpenid().isBlank()) {
            throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED, "微信登录失败");
        }
        return result.getOpenid();
    }
}
