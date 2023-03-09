package com.publiccms.controller.admin.sys;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.multipart.MultipartFile;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.publiccms.common.constants.CommonConstants;
import com.publiccms.common.handler.PageHandler;
import com.publiccms.common.tools.CmsFileUtils;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.common.tools.ImageUtils;
import com.publiccms.common.tools.LanguagesUtils;
import com.publiccms.common.tools.RequestUtils;
import com.publiccms.common.tools.VerificationUtils;
import com.publiccms.entities.log.LogUpload;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.entities.sys.SysUser;
import com.publiccms.logic.component.config.SafeConfigComponent;
import com.publiccms.logic.component.site.SiteComponent;
import com.publiccms.logic.service.log.LogLoginService;
import com.publiccms.logic.service.log.LogUploadService;
import com.publiccms.views.pojo.entities.FileSize;
import com.publiccms.views.pojo.entities.UeditorConfig;

/**
 *
 * UeditorAdminController
 * 
 */
@Controller
@RequestMapping("ueditor")
public class UeditorAdminController {
    protected final Log log = LogFactory.getLog(getClass());
    @Resource
    protected LogUploadService logUploadService;
    @Resource
    protected SiteComponent siteComponent;
    @Resource
    protected SafeConfigComponent safeConfigComponent;

    protected static final String ACTION_CONFIG = "config";
    protected static final String ACTION_UPLOAD = "upload";
    protected static final String ACTION_UPLOAD_SCRAW = "uploadScraw";
    protected static final String ACTION_CATCHIMAGE = "catchimage";
    protected static final String ACTION_LISTIMAGE = "listimage";
    protected static final String ACTION_LISTFILE = "listfile";
    protected static final String ACTION_LISTVIDEO = "listvideo";

    protected static final String FIELD_NAME = "file";
    protected static final String SCRAW_TYPE = ".jpg";

    /**
     * @param site
     * @return view name
     */
    @RequestMapping(params = "action=" + ACTION_CONFIG)
    @ResponseBody
    public UeditorConfig config(@RequestAttribute SysSite site) {
        String urlPrefix = site.getSitePath();
        UeditorConfig config = new UeditorConfig();
        config.setImageActionName(ACTION_UPLOAD);
        config.setSnapscreenActionName(ACTION_UPLOAD);
        config.setScrawlActionName(ACTION_UPLOAD_SCRAW);
        config.setVideoActionName(ACTION_UPLOAD);
        config.setFileActionName(ACTION_UPLOAD);
        config.setCatcherActionName(ACTION_CATCHIMAGE);
        config.setImageManagerActionName(ACTION_LISTIMAGE);
        config.setFileManagerActionName(ACTION_LISTFILE);
        config.setVideoManagerActionName(ACTION_LISTVIDEO);
        config.setImageFieldName(FIELD_NAME);
        config.setScrawlFieldName(FIELD_NAME);
        config.setCatcherFieldName(FIELD_NAME);
        config.setVideoFieldName(FIELD_NAME);
        config.setFileFieldName(FIELD_NAME);
        config.setImageUrlPrefix(urlPrefix);
        config.setScrawlUrlPrefix(urlPrefix);
        config.setSnapscreenUrlPrefix(urlPrefix);
        config.setCatcherUrlPrefix(urlPrefix);
        config.setVideoUrlPrefix(urlPrefix);
        config.setFileUrlPrefix(urlPrefix);
        config.setImageManagerUrlPrefix(urlPrefix);
        config.setVideoManagerUrlPrefix(urlPrefix);
        config.setFileManagerUrlPrefix(urlPrefix);
        config.setImageAllowFiles(CmsFileUtils.IMAGE_FILE_SUFFIXS);
        config.setCatcherAllowFiles(CmsFileUtils.IMAGE_FILE_SUFFIXS);
        config.setVideoAllowFiles(CmsFileUtils.VIDEO_FILE_SUFFIXS);
        config.setFileAllowFiles(safeConfigComponent.getSafeSuffix(site));
        config.setImageManagerAllowFiles(CmsFileUtils.IMAGE_FILE_SUFFIXS);
        config.setFileManagerAllowFiles(safeConfigComponent.getSafeSuffix(site));
        return config;
    }

    /**
     * @param site
     * @param admin
     * @param file
     * @param request
     * @return view name
     */
    @RequestMapping(params = "action=" + ACTION_UPLOAD)
    @ResponseBody
    public Map<String, Object> upload(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, MultipartFile file,
            HttpServletRequest request) {
        if (null != file) {
            String originalName = file.getOriginalFilename();
            String suffix = CmsFileUtils.getSuffix(originalName);
            if (ArrayUtils.contains(safeConfigComponent.getSafeSuffix(site), suffix)) {
                String fileName = CmsFileUtils.getUploadFileName(suffix);
                String filepath = siteComponent.getWebFilePath(site.getId(), fileName);
                String metadataPath = siteComponent.getPrivateFilePath(site.getId(), CmsFileUtils.getMetadataFileName(fileName));
                try {
                    CmsFileUtils.upload(file, filepath, originalName, metadataPath);
                    if (CmsFileUtils.isSafe(filepath, suffix)) {
                        FileSize fileSize = CmsFileUtils.getFileSize(filepath, suffix);
                        logUploadService.save(new LogUpload(site.getId(), admin.getId(), LogLoginService.CHANNEL_WEB_MANAGER,
                                originalName, false, CmsFileUtils.getFileType(suffix), file.getSize(), fileSize.getWidth(),
                                fileSize.getHeight(), RequestUtils.getIpAddress(request), CommonUtils.getDate(), fileName));
                        Map<String, Object> map = getResultMap();
                        map.put("size", file.getSize());
                        map.put("title", originalName);
                        map.put("url", fileName);
                        map.put("type", suffix);
                        map.put("original", originalName);
                        return map;
                    } else {
                        CmsFileUtils.delete(filepath);
                        return getResultMap(false, LanguagesUtils.getMessage(CommonConstants.applicationContext,
                                request.getLocale(), "verify.custom.file.unsafe"));
                    }
                } catch (IllegalStateException | IOException e) {
                    log.error(e.getMessage(), e);
                    return getResultMap(false, e.getMessage());
                }
            } else {
                return getResultMap(false, LanguagesUtils.getMessage(CommonConstants.applicationContext, request.getLocale(),
                        "verify.custom.fileType"));
            }
        } else {
            return getResultMap(false,
                    LanguagesUtils.getMessage(CommonConstants.applicationContext, request.getLocale(), "verify.notEmpty.file"));
        }
    }

    /**
     * @param site
     * @param admin
     * @param file
     * @param request
     * @return view name
     */
    @RequestMapping(params = "action=" + ACTION_UPLOAD_SCRAW)
    @ResponseBody
    public Map<String, Object> uploadScraw(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, String file,
            HttpServletRequest request) {
        if (CommonUtils.notEmpty(file)) {
            byte[] data = VerificationUtils.base64Decode(file);
            String fileName = CmsFileUtils.getUploadFileName(SCRAW_TYPE);
            String filepath = siteComponent.getWebFilePath(site.getId(), fileName);
            try {
                CmsFileUtils.writeByteArrayToFile(filepath, data);
                FileSize fileSize = CmsFileUtils.getFileSize(filepath, SCRAW_TYPE);
                logUploadService.save(new LogUpload(site.getId(), admin.getId(), LogLoginService.CHANNEL_WEB_MANAGER,
                        CommonConstants.BLANK, false, CmsFileUtils.FILE_TYPE_IMAGE, data.length, fileSize.getWidth(),
                        fileSize.getHeight(), RequestUtils.getIpAddress(request), CommonUtils.getDate(), fileName));
                Map<String, Object> map = getResultMap();
                map.put("size", data.length);
                map.put("title", fileName);
                map.put("url", fileName);
                map.put("type", SCRAW_TYPE);
                map.put("original", "scraw" + SCRAW_TYPE);
                return map;
            } catch (IllegalStateException | IOException e) {
                log.error(e.getMessage(), e);
                return getResultMap(false, e.getMessage());
            }
        } else {
            return getResultMap(false,
                    LanguagesUtils.getMessage(CommonConstants.applicationContext, request.getLocale(), "verify.notEmpty.file"));
        }

    }

    /**
     * @param site
     * @param admin
     * @param request
     * @return view name
     */
    @RequestMapping(params = "action=" + ACTION_CATCHIMAGE)
    @ResponseBody
    public Map<String, Object> catchimage(@RequestAttribute SysSite site, @SessionAttribute SysUser admin,
            HttpServletRequest request) {
        try (CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(CommonConstants.defaultRequestConfig)
                .build()) {
            String[] files = request.getParameterValues(FIELD_NAME + "[]");
            if (CommonUtils.notEmpty(files)) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (String image : files) {
                    HttpGet httpget = new HttpGet(image);
                    CloseableHttpResponse response = httpclient.execute(httpget);
                    HttpEntity entity = response.getEntity();
                    if (null != entity) {
                        BufferedInputStream inputStream = new BufferedInputStream(entity.getContent());
                        FileType fileType = FileTypeDetector.detectFileType(inputStream);
                        String suffix = fileType.getCommonExtension();
                        if (null != fileType.getMimeType() && fileType.getMimeType().startsWith("image/")
                                && CommonUtils.notEmpty(suffix)) {
                            String fileName;
                            FileSize fileSize;
                            if (fileType.equals(FileType.WebP)) {
                                fileName = CmsFileUtils.getUploadFileName("jpg");
                                String filepath = siteComponent.getWebFilePath(site.getId(), fileName);
                                ImageUtils.webp2Image(inputStream, false, new File(filepath));
                                fileSize = CmsFileUtils.getFileSize(filepath, suffix);
                            } else {
                                fileName = CmsFileUtils.getUploadFileName(suffix);
                                String filepath = siteComponent.getWebFilePath(site.getId(), fileName);
                                CmsFileUtils.copyInputStreamToFile(inputStream, filepath);
                                fileSize = CmsFileUtils.getFileSize(filepath, suffix);
                            }
                            logUploadService.save(new LogUpload(site.getId(), admin.getId(), LogLoginService.CHANNEL_WEB_MANAGER,
                                    CommonConstants.BLANK, false, CmsFileUtils.getFileType(suffix), fileSize.getFileSize(),
                                    fileSize.getWidth(), fileSize.getHeight(), RequestUtils.getIpAddress(request),
                                    CommonUtils.getDate(), fileName));
                            Map<String, Object> map = getResultMap();
                            map.put("size", fileSize.getFileSize());
                            map.put("title", fileName);
                            map.put("url", fileName);
                            map.put("source", image);
                            list.add(map);
                        }

                    }
                    EntityUtils.consume(entity);
                }
                if (list.isEmpty()) {
                    return getResultMap(false, LanguagesUtils.getMessage(CommonConstants.applicationContext, request.getLocale(),
                            "verify.notEmpty.file"));
                } else {
                    Map<String, Object> map = getResultMap();
                    map.put("list", list);
                    return map;
                }
            } else {
                return getResultMap(false, LanguagesUtils.getMessage(CommonConstants.applicationContext, request.getLocale(),
                        "verify.notEmpty.file"));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return getResultMap(false, e.getMessage());
        }
    }

    /**
     * @param site
     * @param admin
     * @param start
     * @return view name
     */
    @RequestMapping(params = { "action=" + ACTION_LISTIMAGE })
    @ResponseBody
    public Map<String, Object> listimage(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, Integer start) {
        return listfile(site, admin, CmsFileUtils.IMAGE_FILETYPES, start);
    }

    /**
     * @param site
     * @param admin
     * @param start
     * @return view name
     */
    @RequestMapping(params = { "action=" + ACTION_LISTFILE })
    @ResponseBody
    public Map<String, Object> listfile(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, Integer start) {
        return listfile(site, admin, null, start);
    }

    /**
     * @param site
     * @param admin
     * @param start
     * @return view name
     */
    @RequestMapping(params = { "action=" + ACTION_LISTVIDEO })
    @ResponseBody
    public Map<String, Object> listvideo(@RequestAttribute SysSite site, @SessionAttribute SysUser admin, Integer start) {
        return listfile(site, admin, CmsFileUtils.VIDEO_FILETYPES, start);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> listfile(SysSite site, SysUser admin, String[] fileTyps, Integer start) {
        if (CommonUtils.empty(start)) {
            start = 0;
        }
        PageHandler page = logUploadService.getPage(site.getId(), admin.getId(), null, false, fileTyps, null, null, null, null,
                start / 20 + 1, 20);

        Map<String, Object> map = getResultMap();
        List<Map<String, Object>> list = new ArrayList<>();
        for (LogUpload logUpload : ((List<LogUpload>) page.getList())) {
            Map<String, Object> tempMap = getResultMap();
            tempMap.put("url", logUpload.getFilePath());
            tempMap.put("original", logUpload.getOriginalName());
            list.add(tempMap);
        }
        map.put("list", list);
        map.put("start", start);
        map.put("total", page.getTotalCount());
        return map;
    }

    protected static Map<String, Object> getResultMap() {
        return getResultMap(true, null);
    }

    protected static Map<String, Object> getResultMap(boolean success, String message) {
        Map<String, Object> map = new HashMap<>();
        if (success) {
            map.put("state", "SUCCESS");
        } else {
            if (CommonUtils.notEmpty(message)) {
                map.put("state", message);
            } else {
                map.put("state", "error");
            }
        }
        return map;
    }
}