package com.publiccms.controller.admin.cms;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.publiccms.common.annotation.Csrf;
import com.publiccms.common.constants.CommonConstants;
import com.publiccms.common.handler.PageHandler;
import com.publiccms.common.tools.CmsFileUtils;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.common.tools.ControllerUtils;
import com.publiccms.common.tools.DateFormatUtils;
import com.publiccms.common.tools.ExtendUtils;
import com.publiccms.common.tools.JsonUtils;
import com.publiccms.common.tools.LanguagesUtils;
import com.publiccms.common.tools.RequestUtils;
import com.publiccms.common.view.ExcelView;
import com.publiccms.entities.cms.CmsPlace;
import com.publiccms.entities.cms.CmsPlaceAttribute;
import com.publiccms.entities.log.LogOperate;
import com.publiccms.entities.sys.SysDept;
import com.publiccms.entities.sys.SysDeptPageId;
import com.publiccms.entities.sys.SysExtendField;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.entities.sys.SysUser;
import com.publiccms.logic.component.site.SiteComponent;
import com.publiccms.logic.component.template.MetadataComponent;
import com.publiccms.logic.component.template.TemplateComponent;
import com.publiccms.logic.service.cms.CmsPlaceAttributeService;
import com.publiccms.logic.service.cms.CmsPlaceService;
import com.publiccms.logic.service.log.LogLoginService;
import com.publiccms.logic.service.log.LogOperateService;
import com.publiccms.logic.service.sys.SysDeptPageService;
import com.publiccms.logic.service.sys.SysDeptService;
import com.publiccms.logic.service.sys.SysUserService;
import com.publiccms.views.pojo.entities.CmsPageData;
import com.publiccms.views.pojo.entities.CmsPlaceMetadata;
import com.publiccms.views.pojo.model.ExtendDataParameters;

import freemarker.template.TemplateException;

/**
 * 
 * cmsPlaceController
 *
 */
@Controller
@RequestMapping("cmsPlace")
public class CmsPlaceAdminController {
    protected final Log log = LogFactory.getLog(getClass());
    @Resource
    private CmsPlaceService service;
    @Resource
    private CmsPlaceAttributeService attributeService;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private MetadataComponent metadataComponent;
    @Resource
    private SysDeptPageService sysDeptPageService;
    @Resource
    private SysDeptService sysDeptService;
    @Resource
    protected LogOperateService logOperateService;
    @Resource
    protected SiteComponent siteComponent;
    @Resource
    private TemplateComponent templateComponent;

    private String[] ignoreProperties = new String[] { "id", "siteId", "status", "userId", "type", "clicks", "path", "createDate",
            "disabled" };

    /**
     * @param site
     * @param admin
     * @param entity
     * @param extendDataParameters
     * @param request
     * @param model
     * @return view name
     */
    @RequestMapping(value = "save", method = RequestMethod.POST)
    @Csrf
    public String save(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, CmsPlace entity,
            @ModelAttribute ExtendDataParameters extendDataParameters, HttpServletRequest request, ModelMap model) {
        if (null != entity && CommonUtils.notEmpty(entity.getPath())) {
            if (!entity.getPath().startsWith(CommonConstants.SEPARATOR)) {
                entity.setPath(CommonConstants.SEPARATOR + entity.getPath());
            }
            entity.setPath(entity.getPath().replace("//", CommonConstants.SEPARATOR));

            SysDept dept = sysDeptService.getEntity(admin.getDeptId());
            if (ControllerUtils.errorNotEmpty("deptId", admin.getDeptId(), model)
                    || ControllerUtils.errorNotEmpty("deptId", dept, model)
                    || ControllerUtils.errorCustom("noright",
                            !(dept.isOwnsAllPage() || null != sysDeptPageService.getEntity(new SysDeptPageId(admin.getDeptId(),
                                    CommonConstants.SEPARATOR + TemplateComponent.INCLUDE_DIRECTORY + entity.getPath()))),
                            model)) {
                return CommonConstants.TEMPLATE_ERROR;
            }
            if (CommonUtils.empty(entity.getItemType()) || CommonUtils.empty(entity.getItemId())) {
                entity.setItemType(CmsPlaceService.ITEM_TYPE_CUSTOM);
                entity.setItemId(null);
            }
            if (null != entity.getId()) {
                CmsPlace oldEntity = service.getEntity(entity.getId());
                if (null == oldEntity || ControllerUtils.errorNotEquals("siteId", site.getId(), oldEntity.getSiteId(), model)) {
                    return CommonConstants.TEMPLATE_ERROR;
                }
                entity = service.update(entity.getId(), entity, ignoreProperties);
                if (null != entity) {
                    logOperateService.save(new LogOperate(site.getId(), admin.getId(), admin.getDeptId(),
                            LogLoginService.CHANNEL_WEB_MANAGER, "update.place", RequestUtils.getIpAddress(request),
                            CommonUtils.getDate(), JsonUtils.getString(entity)));
                }
            } else {
                entity.setUserId(admin.getId());
                entity.setSiteId(site.getId());
                entity.setStatus(CmsPlaceService.STATUS_NORMAL);
                entity.setCheckUserId(admin.getId());
                service.save(entity);
                logOperateService.save(new LogOperate(site.getId(), admin.getId(), admin.getDeptId(),
                        LogLoginService.CHANNEL_WEB_MANAGER, "save.place", RequestUtils.getIpAddress(request),
                        CommonUtils.getDate(), JsonUtils.getString(entity)));
            }
            String filepath = siteComponent.getWebTemplateFilePath(site, TemplateComponent.INCLUDE_DIRECTORY + entity.getPath());
            CmsPlaceMetadata metadata = metadataComponent.getPlaceMetadata(filepath);
            Map<String, String> map = ExtendUtils.getExtentDataMap(extendDataParameters.getExtendDataList(),
                    metadata.getExtendList());
            String extentString = ExtendUtils.getExtendString(map);
            attributeService.updateAttribute(entity.getId(), extentString);
            staticPlace(site, entity.getPath());
        }
        return CommonConstants.TEMPLATE_DONE;
    }

    /**
     * @param site
     * @param admin
     * @param path
     * @param ids
     * @param request
     * @param model
     * @return view name
     */
    @RequestMapping("refresh")
    @Csrf
    public String refresh(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, String path, Long[] ids,
            HttpServletRequest request, ModelMap model) {
        SysDept dept = sysDeptService.getEntity(admin.getDeptId());
        if (ControllerUtils.errorNotEmpty("deptId", admin.getDeptId(), model)
                || ControllerUtils.errorNotEmpty("deptId", dept, model)
                || ControllerUtils.errorCustom("noright",
                        !(dept.isOwnsAllPage() || null != sysDeptPageService.getEntity(new SysDeptPageId(admin.getDeptId(),
                                CommonConstants.SEPARATOR + TemplateComponent.INCLUDE_DIRECTORY + path))),
                        model)) {
            return CommonConstants.TEMPLATE_ERROR;
        }
        if (CommonUtils.notEmpty(ids)) {
            service.refresh(site.getId(), ids, path);
            logOperateService.save(new LogOperate(site.getId(), admin.getId(), admin.getDeptId(),
                    LogLoginService.CHANNEL_WEB_MANAGER, "refresh.place", RequestUtils.getIpAddress(request),
                    CommonUtils.getDate(), StringUtils.join(ids, CommonConstants.COMMA)));
            staticPlace(site, path);
        }
        return CommonConstants.TEMPLATE_DONE;
    }

    /**
     * @param site
     * @param admin
     * @param path
     * @param ids
     * @param request
     * @param model
     * @return view name
     */
    @RequestMapping("check")
    @Csrf
    public String check(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, String path, Long[] ids,
            HttpServletRequest request, ModelMap model) {
        SysDept dept = sysDeptService.getEntity(admin.getDeptId());
        if (ControllerUtils.errorNotEmpty("deptId", admin.getDeptId(), model)
                || ControllerUtils.errorNotEmpty("deptId", dept, model)
                || ControllerUtils.errorCustom("noright",
                        !(dept.isOwnsAllPage() || null != sysDeptPageService.getEntity(new SysDeptPageId(admin.getDeptId(),
                                CommonConstants.SEPARATOR + TemplateComponent.INCLUDE_DIRECTORY + path))),
                        model)) {
            return CommonConstants.TEMPLATE_ERROR;
        }
        if (CommonUtils.notEmpty(ids)) {
            service.check(site.getId(), admin.getId(), ids, path);
            logOperateService.save(new LogOperate(site.getId(), admin.getId(), admin.getDeptId(),
                    LogLoginService.CHANNEL_WEB_MANAGER, "check.place", RequestUtils.getIpAddress(request), CommonUtils.getDate(),
                    StringUtils.join(ids, CommonConstants.COMMA)));
            staticPlace(site, path);
        }
        return CommonConstants.TEMPLATE_DONE;
    }

    /**
     * @param site
     * @param admin
     * @param path
     * @param ids
     * @param request
     * @param model
     * @return view name
     */
    @RequestMapping("uncheck")
    @Csrf
    public String uncheck(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, String path, Long[] ids,
            HttpServletRequest request, ModelMap model) {
        SysDept dept = sysDeptService.getEntity(admin.getDeptId());
        if (ControllerUtils.errorNotEmpty("deptId", admin.getDeptId(), model)
                || ControllerUtils.errorNotEmpty("deptId", dept, model)
                || ControllerUtils.errorCustom("noright",
                        !(dept.isOwnsAllPage() || null != sysDeptPageService.getEntity(new SysDeptPageId(admin.getDeptId(),
                                CommonConstants.SEPARATOR + TemplateComponent.INCLUDE_DIRECTORY + path))),
                        model)) {
            return CommonConstants.TEMPLATE_ERROR;
        }
        if (CommonUtils.notEmpty(ids)) {
            service.uncheck(site.getId(), ids, path);
            logOperateService.save(new LogOperate(site.getId(), admin.getId(), admin.getDeptId(),
                    LogLoginService.CHANNEL_WEB_MANAGER, "check.place", RequestUtils.getIpAddress(request), CommonUtils.getDate(),
                    StringUtils.join(ids, CommonConstants.COMMA)));
            staticPlace(site, path);
        }
        return CommonConstants.TEMPLATE_DONE;
    }

    /**
     * @param site
     * @param path
     * @param userId
     * @param status
     * @param itemType
     * @param itemId
     * @param startPublishDate
     * @param endPublishDate
     * @param orderField
     * @param orderType
     * @param request
     * @return view name
     */
    @RequestMapping("export")
    @Csrf
    public ExcelView export(@RequestAttribute SysSite site, String path, Long userId, Integer[] status, String itemType,
            Long itemId, @DateTimeFormat(pattern = "yyyy-MM-dd") Date startPublishDate,
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date endPublishDate, String orderField, String orderType,
            HttpServletRequest request) {
        if (CommonUtils.notEmpty(path)) {
            path = path.replace("//", CommonConstants.SEPARATOR);
        }
        Locale locale = RequestContextUtils.getLocale(request);

        String filepath = siteComponent.getWebTemplateFilePath(site, TemplateComponent.INCLUDE_DIRECTORY + path);
        CmsPlaceMetadata metadata = metadataComponent.getPlaceMetadata(filepath);

        PageHandler page = service.getPage(site.getId(), userId, path, itemType, itemId, startPublishDate, endPublishDate,
                CommonUtils.getMinuteDate(), status, false, orderField, orderType, 1, PageHandler.MAX_PAGE_SIZE);
        @SuppressWarnings("unchecked")
        List<CmsPlace> entityList = (List<CmsPlace>) page.getList();
        Map<String, List<Serializable>> pksMap = new HashMap<>();
        for (CmsPlace entity : entityList) {
            List<Serializable> userIds = pksMap.computeIfAbsent("userIds", k -> new ArrayList<>());
            List<Serializable> ids = pksMap.computeIfAbsent("ids", k -> new ArrayList<>());
            userIds.add(entity.getUserId());
            userIds.add(entity.getCheckUserId());
            ids.add(entity.getId());
        }
        Map<Serializable, SysUser> userMap = new HashMap<>();
        Map<Serializable, CmsPlaceAttribute> attributeMap = new HashMap<>();
        if (null != pksMap.get("userIds")) {
            List<Serializable> userIds = pksMap.get("userIds");
            List<SysUser> entitys = sysUserService.getEntitys(userIds.toArray(new Serializable[userIds.size()]));
            for (SysUser entity : entitys) {
                userMap.put(entity.getId(), entity);
            }
            List<Serializable> ids = pksMap.get("ids");
            List<CmsPlaceAttribute> attributes = attributeService.getEntitys(ids.toArray(new Serializable[ids.size()]));
            for (CmsPlaceAttribute attribute : attributes) {
                attributeMap.put(attribute.getPlaceId(), attribute);
            }
        }

        ExcelView view = new ExcelView(workbook -> {
            Sheet sheet = workbook
                    .createSheet(LanguagesUtils.getMessage(CommonConstants.applicationContext, locale, "page.content"));
            int i = 0, j = 0;
            Row row = sheet.createRow(i++);

            row.createCell(j++).setCellValue(LanguagesUtils.getMessage(CommonConstants.applicationContext, locale, "page.id"));
            row.createCell(j++).setCellValue(LanguagesUtils.getMessage(CommonConstants.applicationContext, locale, "page.title"));
            row.createCell(j++).setCellValue(LanguagesUtils.getMessage(CommonConstants.applicationContext, locale, "page.url"));
            row.createCell(j++).setCellValue(
                    LanguagesUtils.getMessage(CommonConstants.applicationContext, locale, "page.content.promulgator"));
            row.createCell(j++)
                    .setCellValue(LanguagesUtils.getMessage(CommonConstants.applicationContext, locale, "page.clicks"));
            row.createCell(j++)
                    .setCellValue(LanguagesUtils.getMessage(CommonConstants.applicationContext, locale, "page.publish_date"));
            row.createCell(j++)
                    .setCellValue(LanguagesUtils.getMessage(CommonConstants.applicationContext, locale, "page.create_date"));
            row.createCell(j++)
                    .setCellValue(LanguagesUtils.getMessage(CommonConstants.applicationContext, locale, "page.status"));
            row.createCell(j++)
                    .setCellValue(LanguagesUtils.getMessage(CommonConstants.applicationContext, locale, "page.inspector"));

            if (CommonUtils.notEmpty(metadata.getExtendList())) {
                for (SysExtendField extend : metadata.getExtendList()) {
                    row.createCell(j++).setCellValue(extend.getName());
                }
            }

            SysUser user;
            CmsPlaceAttribute attribute;
            DateFormat dateFormat = DateFormatUtils.getDateFormat(DateFormatUtils.FULL_DATE_FORMAT_STRING);
            for (CmsPlace entity : entityList) {
                row = sheet.createRow(i++);
                j = 0;
                row.createCell(j++).setCellValue(entity.getId().toString());
                row.createCell(j++).setCellValue(entity.getTitle());
                row.createCell(j++).setCellValue(entity.getUrl());
                user = userMap.get(entity.getUserId());
                row.createCell(j++).setCellValue(null == user ? null : user.getNickname());
                row.createCell(j++).setCellValue(String.valueOf(entity.getClicks()));
                row.createCell(j++).setCellValue(dateFormat.format(entity.getPublishDate()));
                row.createCell(j++).setCellValue(dateFormat.format(entity.getCreateDate()));

                row.createCell(j++).setCellValue(LanguagesUtils.getMessage(CommonConstants.applicationContext, locale,
                        "page.status.place.data." + entity.getStatus()));

                user = userMap.get(entity.getCheckUserId());
                row.createCell(j++).setCellValue(null == user ? null : user.getNickname());

                if (CommonUtils.notEmpty(metadata.getExtendList())) {
                    attribute = attributeMap.get(entity.getId());
                    Map<String, String> map = ExtendUtils.getExtendMap(null == attribute ? null : attribute.getData());
                    for (SysExtendField extend : metadata.getExtendList()) {
                        row.createCell(j++).setCellValue(map.get(extend.getId().getCode()));
                    }
                }
            }
        });
        DateFormat dateFormat = DateFormatUtils.getDateFormat(DateFormatUtils.SHORT_DATE_FORMAT_STRING);
        view.setFilename(metadata.getAlias() + "_" + dateFormat.format(new Date()));
        return view;
    }

    /**
     * @param site
     * @param admin
     * @param path
     * @param request
     * @param model
     * @return view name
     */
    @RequestMapping("clear")
    @Csrf
    public String clear(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, String path, HttpServletRequest request,
            ModelMap model) {
        SysDept dept = sysDeptService.getEntity(admin.getDeptId());
        if (ControllerUtils.errorNotEmpty("deptId", admin.getDeptId(), model)
                || ControllerUtils.errorNotEmpty("deptId", dept, model)
                || ControllerUtils.errorCustom("noright",
                        !(dept.isOwnsAllPage() || null != sysDeptPageService.getEntity(new SysDeptPageId(admin.getDeptId(),
                                CommonConstants.SEPARATOR + TemplateComponent.INCLUDE_DIRECTORY + path))),
                        model)) {
            return CommonConstants.TEMPLATE_ERROR;
        }
        if (CommonUtils.notEmpty(path)) {
            service.delete(site.getId(), path);
            logOperateService
                    .save(new LogOperate(site.getId(), admin.getId(), admin.getDeptId(), LogLoginService.CHANNEL_WEB_MANAGER,
                            "clear.place", RequestUtils.getIpAddress(request), CommonUtils.getDate(), path));
            staticPlace(site, path);
        }
        return CommonConstants.TEMPLATE_DONE;
    }

    /**
     * @param site
     * @param admin
     * @param path
     * @param ids
     * @param request
     * @param model
     * @return view name
     */
    @RequestMapping("delete")
    @Csrf
    public String delete(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, String path, Long[] ids,
            HttpServletRequest request, ModelMap model) {
        SysDept dept = sysDeptService.getEntity(admin.getDeptId());
        if (ControllerUtils.errorNotEmpty("deptId", admin.getDeptId(), model)
                || ControllerUtils.errorNotEmpty("deptId", dept, model)
                || ControllerUtils.errorCustom("noright",
                        !(dept.isOwnsAllPage() || null != sysDeptPageService.getEntity(new SysDeptPageId(admin.getDeptId(),
                                CommonConstants.SEPARATOR + TemplateComponent.INCLUDE_DIRECTORY + path))),
                        model)) {
            return CommonConstants.TEMPLATE_ERROR;
        }
        if (CommonUtils.notEmpty(ids)) {
            service.delete(site.getId(), ids, path);
            logOperateService.save(new LogOperate(site.getId(), admin.getId(), admin.getDeptId(),
                    LogLoginService.CHANNEL_WEB_MANAGER, "delete.place", RequestUtils.getIpAddress(request),
                    CommonUtils.getDate(), StringUtils.join(ids, CommonConstants.COMMA)));
            staticPlace(site, path);
        }
        return CommonConstants.TEMPLATE_DONE;
    }

    private void staticPlace(SysSite site, String path) {
        if (CmsFileUtils.exists(siteComponent.getWebFilePath(site, TemplateComponent.INCLUDE_DIRECTORY + path))) {
            try {
                String filepath = siteComponent.getWebTemplateFilePath(site, TemplateComponent.INCLUDE_DIRECTORY + path);
                CmsPlaceMetadata metadata = metadataComponent.getPlaceMetadata(filepath);
                CmsPageData data = metadataComponent.getTemplateData(filepath);
                templateComponent.staticPlace(site, path, metadata, data);
            } catch (IOException | TemplateException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
