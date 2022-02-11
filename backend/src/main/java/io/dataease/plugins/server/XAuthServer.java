package io.dataease.plugins.server;


import io.dataease.auth.api.dto.CurrentUserDto;
import io.dataease.commons.constants.AuthConstants;
import io.dataease.commons.utils.AuthUtils;
import io.dataease.controller.handler.annotation.I18n;
import io.dataease.listener.util.CacheUtils;
import io.dataease.plugins.config.SpringContextUtil;
import io.dataease.plugins.xpack.auth.dto.request.XpackBaseTreeRequest;
import io.dataease.plugins.xpack.auth.dto.request.XpackSysAuthRequest;
import io.dataease.plugins.xpack.auth.dto.response.XpackSysAuthDetail;
import io.dataease.plugins.xpack.auth.dto.response.XpackSysAuthDetailDTO;
import io.dataease.plugins.xpack.auth.dto.response.XpackVAuthModelDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import io.dataease.plugins.xpack.auth.service.AuthXpackService;

import java.util.*;

@RequestMapping("/plugin/auth")
@RestController
public class XAuthServer {

    private static final Set<String> cacheTypes = new HashSet<>();

    @PostMapping("/authModels")
    @I18n
    public List<XpackVAuthModelDTO> authModels(@RequestBody XpackBaseTreeRequest request) {
        AuthXpackService sysAuthService = SpringContextUtil.getBean(AuthXpackService.class);
        CurrentUserDto user = AuthUtils.getUser();
        return sysAuthService.searchAuthModelTree(request, user.getUserId(), user.getIsAdmin());
    }

    @PostMapping("/authDetails")
    public Map<String, List<XpackSysAuthDetailDTO>> authDetails(@RequestBody XpackSysAuthRequest request) {
        AuthXpackService sysAuthService = SpringContextUtil.getBean(AuthXpackService.class);
        return sysAuthService.searchAuthDetails(request);
    }

    @GetMapping("/authDetailsModel/{authType}/{direction}")
    @I18n
    public List<XpackSysAuthDetail> authDetailsModel(@PathVariable String authType, @PathVariable String direction) {
        AuthXpackService sysAuthService = SpringContextUtil.getBean(AuthXpackService.class);
        List<XpackSysAuthDetail> authDetails = sysAuthService.searchAuthDetailsModel(authType);
        if (authType.equalsIgnoreCase("dataset")) {
            XpackSysAuthDetail xpackSysAuthDetail = new XpackSysAuthDetail();
            xpackSysAuthDetail.setPrivilegeName("i18n_auth_row_permission");
            xpackSysAuthDetail.setPrivilegeType(20);
            xpackSysAuthDetail.setPrivilegeValue(1);
            authDetails.add(0, xpackSysAuthDetail);
        }
        return authDetails;
    }

    @PostMapping("/authChange")
    public void authChange(@RequestBody XpackSysAuthRequest request) {
        AuthXpackService sysAuthService = SpringContextUtil.getBean(AuthXpackService.class);
        CurrentUserDto user = AuthUtils.getUser();
        sysAuthService.authChange(request, user.getUserId(), user.getUsername(), user.getIsAdmin());
        // 当权限发生变化 前端实时刷新对应菜单
        Optional.ofNullable(request.getAuthSourceType()).ifPresent(type -> {
            if (StringUtils.equals("menu", type)) {
                CacheUtils.removeAll(AuthConstants.USER_CACHE_NAME);
                CacheUtils.removeAll(AuthConstants.USER_ROLE_CACHE_NAME);
                CacheUtils.removeAll(AuthConstants.USER_PERMISSION_CACHE_NAME);
            }
            String authCacheKey = getAuthCacheKey(request);
            if (StringUtils.isNotBlank(authCacheKey)) {
                CacheUtils.remove(authCacheKey, request.getAuthTargetType() + request.getAuthTarget());
            }
        });
    }


    private String getAuthCacheKey(XpackSysAuthRequest request) {
        if (CollectionUtils.isEmpty(cacheTypes)) {
            cacheTypes.add("link");
            cacheTypes.add("dataset");
            cacheTypes.add("panel");
        }
        String authTargetType = request.getAuthTargetType();
        String authSourceType = request.getAuthSourceType();
        if (!cacheTypes.contains(authSourceType)) {
            return null;
        }
        return authTargetType + "_" + authSourceType;

    }
}
