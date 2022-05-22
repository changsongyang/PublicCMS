package com.publiccms.controller.admin.sys;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.multipart.MultipartFile;

import com.publiccms.common.constants.CommonConstants;
import com.publiccms.common.handler.PageHandler;
import com.publiccms.common.tools.CmsFileUtils;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.common.tools.RequestUtils;
import com.publiccms.entities.log.LogUpload;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.entities.sys.SysUser;
import com.publiccms.logic.component.config.SiteConfigComponent;
import com.publiccms.logic.component.site.SiteComponent;
import com.publiccms.logic.service.log.LogLoginService;
import com.publiccms.logic.service.log.LogUploadService;
import com.publiccms.views.pojo.entities.FileSize;

/**
 *
 * CkeditorAdminController
 * 
 */
@Controller
@RequestMapping("tinymce")
public class TinymceAdminController {
    @Autowired
    protected LogUploadService logUploadService;
    @Autowired
    protected SiteComponent siteComponent;
    @Autowired
    protected SiteConfigComponent siteConfigComponent;

    private static final String RESULT_URL = "location";

    /**
     * @param site
     * @param admin
     * @param file
     * @param request
     * @return view name
     */
    @RequestMapping("upload")
    @ResponseBody
    public Map<String, Object> upload(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, MultipartFile file,
            HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put(CommonConstants.ERROR, 0);
        if (null != file && !file.isEmpty()) {
            String originalName = file.getOriginalFilename();
            String suffix = CmsFileUtils.getSuffix(originalName);
            if (ArrayUtils.contains(siteConfigComponent.getSafeSuffix(site), suffix)) {
                String fileName = CmsFileUtils.getUploadFileName(suffix);
                String filepath = siteComponent.getWebFilePath(site, fileName);
                try {
                    CmsFileUtils.upload(file, filepath);
                    FileSize fileSize = CmsFileUtils.getFileSize(filepath, suffix);
                    logUploadService.save(new LogUpload(site.getId(), admin.getId(), LogLoginService.CHANNEL_WEB_MANAGER,
                            originalName, CmsFileUtils.getFileType(suffix), file.getSize(), fileSize.getWidth(),
                            fileSize.getHeight(), RequestUtils.getIpAddress(request), CommonUtils.getDate(), fileName));
                    map.put(RESULT_URL, fileName);
                    return map;
                } catch (IllegalStateException | IOException e) {
                    map.put(CommonConstants.MESSAGE, e.getMessage());
                }
            } else {
                map.put(CommonConstants.MESSAGE, "unsafe file");
            }
        } else {
            map.put(CommonConstants.MESSAGE, "no file");
        }
        map.put(CommonConstants.ERROR, 1);
        return map;
    }

    /**
     * @param site
     * @param admin
     * @return view name
     */
    @SuppressWarnings("unchecked")
    @RequestMapping("imageList")
    @ResponseBody
    public List<Map<String, String>> imageList(SysSite site, SysUser admin) {
        PageHandler page = logUploadService.getPage(site.getId(), admin.getId(), null, CmsFileUtils.IMAGE_FILETYPES, null, null,
                null, null, 1, 20);
        List<Map<String, String>> result = new ArrayList<>();
        for (LogUpload logUpload : ((List<LogUpload>) page.getList())) {
            Map<String, String> map = new HashMap<>();
            map.put("value", logUpload.getFilePath());
            map.put("title", logUpload.getOriginalName());
            result.add(map);
        }
        return result;
    }
}