package com.publiccms.controller.admin.sys;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.publiccms.common.annotation.Csrf;
import com.publiccms.common.constants.CommonConstants;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.common.tools.ControllerUtils;
import com.publiccms.common.tools.JsonUtils;
import com.publiccms.common.tools.RequestUtils;
import com.publiccms.entities.log.LogOperate;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.entities.sys.SysUser;
import com.publiccms.entities.sys.SysUserToken;
import com.publiccms.logic.component.site.SiteComponent;
import com.publiccms.logic.service.log.LogLoginService;
import com.publiccms.logic.service.log.LogOperateService;
import com.publiccms.logic.service.sys.SysUserTokenService;

/**
 *
 * SysUserTokenAdminController
 * 
 */
@Controller
@RequestMapping("sysUserToken")
public class SysUserTokenAdminController {
    @Resource
    protected LogOperateService logOperateService;
    @Resource
    protected SiteComponent siteComponent;

    /**
     * @param site
     * @param admin
     * @param authToken
     * @param request
     * @param model
     * @return view name
     */
    @RequestMapping("delete")
    @Csrf
    public String delete(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, String authToken,
            HttpServletRequest request, ModelMap model) {
        SysUserToken entity = service.getEntity(authToken);
        if (null != entity) {
            if (ControllerUtils.errorNotEquals("userId", admin.getId(), entity.getUserId(), model)) {
                return CommonConstants.TEMPLATE_ERROR;
            }
            service.delete(authToken);
            logOperateService.save(new LogOperate(site.getId(), admin.getId(), admin.getDeptId(), LogLoginService.CHANNEL_WEB_MANAGER, "delete.usertoken",
                    RequestUtils.getIpAddress(request), CommonUtils.getDate(), JsonUtils.getString(entity)));
        }
        return CommonConstants.TEMPLATE_DONE;
    }

    @Resource
    private SysUserTokenService service;

}