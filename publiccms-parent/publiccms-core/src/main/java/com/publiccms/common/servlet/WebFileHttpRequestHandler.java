package com.publiccms.common.servlet;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.UrlPathHelper;

import com.publiccms.common.constants.CmsVersion;
import com.publiccms.common.constants.CommonConstants;
import com.publiccms.common.constants.Constants;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.logic.component.site.SiteComponent;

/**
 *
 * MultiSiteWebHttpRequestHandler 多站点静态资源处理器
 * 
 */
public class WebFileHttpRequestHandler extends ResourceHttpRequestHandler {
    private SiteComponent siteComponent;
    private static Resource favicon = new ClassPathResource("favicon.ico");

    /**
     * @param siteComponent
     */
    public WebFileHttpRequestHandler(SiteComponent siteComponent) {
        this.siteComponent = siteComponent;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader(CommonConstants.getXPowered(), CmsVersion.getVersion());
        super.handleRequest(request, response);

    }

    @Override
    protected Resource getResource(HttpServletRequest request) throws IOException {
        if (CmsVersion.isInitialized()) {
            String path = getUrlPathHelper().getLookupPathForRequest(request);
            if (path.endsWith(Constants.SEPARATOR)) {
                path = CommonUtils.joinString(path, CommonConstants.getDefaultPage());
            }
            SysSite site = siteComponent.getSite(request.getServerName(), path);
            path = siteComponent.getPath(site, path);
            Resource resource = new FileSystemResource(siteComponent.getWebFilePath(site.getId(), path));
            if (resource.exists() && (resource.isReadable())) {
                return resource;
            } else if ("/favicon.ico".equals(path) && favicon.exists()) {
                return favicon;
            }
        }
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setUrlPathHelper(UrlPathHelper.defaultInstance);
        if (null == getResourceHttpMessageConverter()) {
            setResourceHttpMessageConverter(new ResourceHttpMessageConverter());
        }
        if (null == getResourceRegionHttpMessageConverter()) {
            setResourceRegionHttpMessageConverter(new ResourceRegionHttpMessageConverter());
        }

    }
}
