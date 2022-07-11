package com.publiccms.controller.admin;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

import com.publiccms.common.annotation.Csrf;
import com.publiccms.common.api.Config;
import com.publiccms.common.constants.CommonConstants;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.common.tools.ControllerUtils;
import com.publiccms.common.tools.RequestUtils;
import com.publiccms.common.tools.UserPasswordUtils;
import com.publiccms.entities.log.LogLogin;
import com.publiccms.entities.log.LogOperate;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.entities.sys.SysUser;
import com.publiccms.entities.sys.SysUserToken;
import com.publiccms.logic.component.cache.CacheComponent;
import com.publiccms.logic.component.config.ConfigComponent;
import com.publiccms.logic.component.config.SiteConfigComponent;
import com.publiccms.logic.component.site.LockComponent;
import com.publiccms.logic.component.site.SiteComponent;
import com.publiccms.logic.service.log.LogLoginService;
import com.publiccms.logic.service.log.LogOperateService;
import com.publiccms.logic.service.sys.SysLockService;
import com.publiccms.logic.service.sys.SysUserService;
import com.publiccms.logic.service.sys.SysUserTokenService;

/**
 *
 * LoginAdminController
 *
 */
@Controller
public class LoginAdminController {
    @Resource
    protected LogOperateService logOperateService;
    @Resource
    private SysUserService service;
    @Resource
    private SysUserTokenService sysUserTokenService;
    @Resource
    private LogLoginService logLoginService;
    @Resource
    private CacheComponent cacheComponent;
    @Resource
    private ConfigComponent configComponent;
    @Resource
    private LockComponent lockComponent;
    @Resource
    protected SiteComponent siteComponent;

    /**
     * @param site
     * @param username
     * @param password
     * @param returnUrl
     * @param encoding
     * @param request
     * @param session
     * @param response
     * @param model
     * @return view name
     */
    @RequestMapping(value = "login", method = RequestMethod.POST)
    public String login(@RequestAttribute SysSite site, String username, String password, String returnUrl, String encoding,
            HttpServletRequest request, HttpSession session, HttpServletResponse response, ModelMap model) {
        username = StringUtils.trim(username);
        password = StringUtils.trim(password);
        if (ControllerUtils.errorNotEmpty("username", username, model)
                || ControllerUtils.errorNotEmpty("password", password, model)) {
            model.addAttribute("username", username);
            model.addAttribute("returnUrl", returnUrl);
            return "login";
        }
        String ip = RequestUtils.getIpAddress(request);
        SysUser user = service.findByName(site.getId(), username);
        boolean locked = lockComponent.isLocked(site.getId(), SysLockService.ITEM_TYPE_IP_LOGIN, ip, null);
        if (ControllerUtils.errorNotEquals("password", user, model)
                || ControllerUtils.errorCustom("locked.ip", locked && ControllerUtils.ipNotEquals(ip, user), model)) {
            model.addAttribute("username", username);
            model.addAttribute("returnUrl", returnUrl);
            lockComponent.lock(site.getId(), SysLockService.ITEM_TYPE_IP_LOGIN, ip, null, true);
            logLoginService.save(new LogLogin(site.getId(), username, null, ip, LogLoginService.CHANNEL_WEB_MANAGER, false,
                    CommonUtils.getDate(), password));
            return "login";
        } else {
            locked = lockComponent.isLocked(site.getId(), SysLockService.ITEM_TYPE_LOGIN, String.valueOf(user.getId()), null);
            if (ControllerUtils.errorCustom("locked.user", locked, model)
                    || ControllerUtils.errorNotEquals("password",
                            UserPasswordUtils.passwordEncode(password, user.getSalt(), encoding), user.getPassword(), model)
                    || verifyNotAdmin(user, model) || verifyNotEnablie(user, model)) {
                model.addAttribute("username", username);
                model.addAttribute("returnUrl", returnUrl);
                Long userId = user.getId();
                lockComponent.lock(site.getId(), SysLockService.ITEM_TYPE_LOGIN, String.valueOf(user.getId()), null, true);
                lockComponent.lock(site.getId(), SysLockService.ITEM_TYPE_IP_LOGIN, ip, null, true);
                logLoginService.save(new LogLogin(site.getId(), username, userId, ip, LogLoginService.CHANNEL_WEB_MANAGER, false,
                        CommonUtils.getDate(), password));
                return "login";
            }
        }
        lockComponent.unLock(site.getId(), SysLockService.ITEM_TYPE_IP_LOGIN, String.valueOf(user.getId()), null);
        lockComponent.unLock(site.getId(), SysLockService.ITEM_TYPE_LOGIN, String.valueOf(user.getId()), null);

        ControllerUtils.setAdminToSession(session, user);
        if (UserPasswordUtils.needUpdate(user.getSalt())) {
            String salt = UserPasswordUtils.getSalt();
            service.updatePassword(user.getId(), UserPasswordUtils.passwordEncode(password, salt, encoding), salt);
        }
        service.updateLoginStatus(user.getId(), ip);
        String authToken = UUID.randomUUID().toString();
        Date now = CommonUtils.getDate();
        Map<String, String> config = configComponent.getConfigData(site.getId(), Config.CONFIG_CODE_SITE);
        int expiryMinutes = ConfigComponent.getInt(config.get(SiteConfigComponent.CONFIG_EXPIRY_MINUTES_MANAGER),
                SiteConfigComponent.DEFAULT_EXPIRY_MINUTES);
        sysUserTokenService.save(new SysUserToken(authToken, site.getId(), user.getId(), LogLoginService.CHANNEL_WEB_MANAGER, now,
                DateUtils.addMinutes(now, expiryMinutes), ip));
        StringBuilder sb = new StringBuilder();
        sb.append(user.getId()).append(CommonConstants.getCookiesUserSplit()).append(authToken);
        RequestUtils.addCookie(request.getContextPath(), request.getScheme(), response, CommonConstants.getCookiesAdmin(),
                sb.toString(), expiryMinutes * 60, null);
        logLoginService.save(new LogLogin(site.getId(), username, user.getId(), ip, LogLoginService.CHANNEL_WEB_MANAGER, true,
                CommonUtils.getDate(), null));
        String safeReturnUrl = config.get(SiteConfigComponent.CONFIG_RETURN_URL);
        if (SiteConfigComponent.isUnSafeUrl(returnUrl, site, safeReturnUrl, request.getContextPath())) {
            returnUrl = CommonConstants.getDefaultPage();
        }
        return UrlBasedViewResolver.REDIRECT_URL_PREFIX + returnUrl;
    }

    /**
     * @param site
     * @param username
     * @param password
     * @param encoding
     * @param request
     * @param session
     * @param response
     * @param model
     * @return view name
     */
    @RequestMapping(value = "loginDialog", method = RequestMethod.POST)
    public String loginDialog(@RequestAttribute SysSite site, String username, String password, String encoding,
            HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap model) {
        if ("login".equals(login(site, username, password, null, encoding, request, session, response, model))) {
            return CommonConstants.TEMPLATE_ERROR;
        }
        return CommonConstants.TEMPLATE_DONE;
    }

    /**
     * @param site
     * @param admin
     * @param oldpassword
     * @param password
     * @param repassword
     * @param encoding
     * @param request
     * @param response
     * @param model
     * @return view name
     */
    @RequestMapping(value = "changePassword", method = RequestMethod.POST)
    @Csrf
    public String changeMyselfPassword(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, String oldpassword,
            String password, String repassword, String encoding, HttpServletRequest request, HttpServletResponse response,
            ModelMap model) {
        SysUser user = service.getEntity(admin.getId());
        String encodedOldPassword = UserPasswordUtils.passwordEncode(oldpassword, user.getSalt(), encoding);
        if (null != user.getPassword()
                && ControllerUtils.errorNotEquals("password", user.getPassword(), encodedOldPassword, model)) {
            return CommonConstants.TEMPLATE_ERROR;
        } else if (ControllerUtils.errorNotEmpty("password", password, model)
                || ControllerUtils.errorNotEquals("repassword", password, repassword, model)) {
            return CommonConstants.TEMPLATE_ERROR;
        } else {
            ControllerUtils.clearAdminToSession(request.getContextPath(), request.getScheme(), request.getSession(), response);
            model.addAttribute(CommonConstants.MESSAGE, "message.needReLogin");
        }
        String salt = UserPasswordUtils.getSalt();
        service.updatePassword(user.getId(), UserPasswordUtils.passwordEncode(password, salt, encoding), salt);
        if (user.isWeakPassword()) {
            service.updateWeekPassword(user.getId(), false);
        }
        sysUserTokenService.delete(user.getId());
        logOperateService.save(new LogOperate(site.getId(), user.getId(), user.getDeptId(), LogLoginService.CHANNEL_WEB_MANAGER,
                "changepassword", RequestUtils.getIpAddress(request), CommonUtils.getDate(), encodedOldPassword));
        return "common/ajaxTimeout";
    }

    /**
     * @param userId
     * @param request
     * @param response
     * @return view name
     */
    @RequestMapping(value = "logout", method = RequestMethod.GET)
    public String logout(Long userId, HttpServletRequest request, HttpServletResponse response) {
        SysUser admin = ControllerUtils.getAdminFromSession(request.getSession());
        if (null != userId && null != admin && userId.equals(admin.getId())) {
            Cookie userCookie = RequestUtils.getCookie(request.getCookies(), CommonConstants.getCookiesAdmin());
            if (null != userCookie && CommonUtils.notEmpty(userCookie.getValue())) {
                String value = userCookie.getValue();
                if (null != value) {
                    String[] userData = value.split(CommonConstants.getCookiesUserSplit());
                    if (userData.length > 1) {
                        sysUserTokenService.delete(userData[1]);
                    }
                }
            }
            ControllerUtils.clearAdminToSession(request.getContextPath(), request.getScheme(), request.getSession(), response);
        }
        return UrlBasedViewResolver.REDIRECT_URL_PREFIX + CommonConstants.getDefaultPage();
    }

    /**
     * @return view name
     */
    @RequestMapping(value = "clearCache")
    public String clearCache() {
        cacheComponent.clear();
        return CommonConstants.TEMPLATE_DONE;
    }

    protected boolean verifyNotAdmin(SysUser user, ModelMap model) {
        if (!user.isDisabled() && !user.isSuperuser()) {
            model.addAttribute(CommonConstants.ERROR, "verify.user.notAdmin");
            return true;
        }
        return false;
    }

    protected boolean verifyNotEnablie(SysUser user, ModelMap model) {
        if (user.isDisabled()) {
            model.addAttribute(CommonConstants.ERROR, "verify.user.notEnablie");
            return true;
        }
        return false;
    }
}
