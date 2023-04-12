package com.publiccms.logic.component.parameter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.publiccms.common.api.Config;
import com.publiccms.common.base.AbstractIntegerParameterHandler;
import com.publiccms.common.tools.CmsUrlUtils;
import com.publiccms.entities.cms.CmsCategory;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.logic.service.cms.CmsCategoryService;

import jakarta.annotation.Priority;
import jakarta.annotation.Resource;

/**
 * CategoryParameterComponent 分类参数处理组件
 */
@Component
@Priority(4)
public class CategoryParameterComponent extends AbstractIntegerParameterHandler<CmsCategory> {
	@Resource
	private CmsCategoryService service;

	@Override
	public String getType() {
		return Config.INPUTTYPE_CATEGORY;
	}

	@Override
	public List<CmsCategory> getParameterValueList(SysSite site, Integer[] ids) {
		List<CmsCategory> entityList = service.getEntitys(ids);
		entityList = entityList.stream().filter(entity -> site.getId() == entity.getSiteId() && !entity.isDisabled())
				.toList();
		entityList.forEach(e -> CmsUrlUtils.initCategoryUrl(site, e));
		return entityList;
	}

	@Override
	public CmsCategory getParameterValue(SysSite site, Integer id) {
		CmsCategory entity = service.getEntity(id);
        if (null != entity && !entity.isDisabled() && entity.getSiteId() == site.getId()) {
            CmsUrlUtils.initCategoryUrl(site, entity);
            return entity;
        }
        return null;
	}
}
