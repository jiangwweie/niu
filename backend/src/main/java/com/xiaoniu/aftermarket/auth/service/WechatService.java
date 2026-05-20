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
    public void unbindWechat(Long storeId, Long userId) {
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
                .set(SysUserEntity::getWechatBoundAt, null));
    }

    private String code2Session(String code) {
        try {
            WxMaJscode2SessionResult result = wxMaService.getUserService().getSessionInfo(code);
            return result.getOpenid();
        } catch (WxErrorException e) {
            log.error("WeChat code2Session failed, error code: {}, error msg: {}",
                    e.getError().getErrorCode(), e.getError().getErrorMsg());
            throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED, "微信登录失败");
        }
    }
}
