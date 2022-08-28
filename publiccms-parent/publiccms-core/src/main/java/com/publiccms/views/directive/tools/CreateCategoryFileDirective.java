package com.publiccms.views.directive.tools;

import java.io.IOException;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import com.publiccms.common.base.AbstractTemplateDirective;
import com.publiccms.common.handler.RenderHandler;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.entities.cms.CmsCategory;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.logic.component.template.TemplateComponent;
import com.publiccms.logic.service.cms.CmsCategoryService;

import freemarker.template.TemplateException;

/**
 *
 * createCategoryFile 创建分类静态文件指令
 * 
 */
@Component
public class CreateCategoryFileDirective extends AbstractTemplateDirective {

    @Override
    public void execute(RenderHandler handler) throws IOException, Exception {
        Integer id = handler.getInteger("id");
        String templatePath = handler.getString("templatePath");
        String filepath = handler.getString("filePath");
        Integer pageIndex = handler.getInteger("pageIndex");
        if (CommonUtils.notEmpty(id) && CommonUtils.notEmpty(templatePath) && CommonUtils.notEmpty(filepath)) {
            SysSite site = getSite(handler);
            try {
                CmsCategory category = categoryService.getEntity(id);
                if (null != category && site.getId() == category.getSiteId()) {
                    handler.put("url",
                            templateComponent.createCategoryFile(site, category, templatePath, filepath, pageIndex, null))
                            .render();
                }
            } catch (IOException | TemplateException e) {
                handler.print(e.getMessage());
            }
        }
    }

    @Override
    public boolean needAppToken() {
        return true;
    }

    @Resource
    private TemplateComponent templateComponent;
    @Resource
    private CmsCategoryService categoryService;

}
