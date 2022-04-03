package com.publiccms.views.directive.cms;

// Generated 2020-3-26 11:46:48 by com.publiccms.common.generator.SourceGenerator

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import com.publiccms.common.base.AbstractTemplateDirective;
import com.publiccms.common.handler.RenderHandler;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.entities.cms.CmsUserScore;
import com.publiccms.entities.cms.CmsUserScoreId;
import com.publiccms.logic.service.cms.CmsUserScoreService;

/**
 *
 * CmsUserScoreDirective
 * 
 */
@Component
public class CmsUserScoreDirective extends AbstractTemplateDirective {

    @Override
    public void execute(RenderHandler handler) throws IOException, Exception {
        Long userId = handler.getLong("userId");
        String itemType = handler.getString("itemType");
        Long itemId = handler.getLong("itemId");
        if (null != userId && CommonUtils.notEmpty(itemType)) {
            if (null != itemId) {
                CmsUserScore entity = service.getEntity(new CmsUserScoreId(userId, itemType, itemId));
                if (null != entity) {
                    handler.put("object", entity).render();
                }
            } else {
                Long[] itemIds = handler.getLongArray("itemIds");
                if (CommonUtils.notEmpty(itemIds)) {
                    CmsUserScoreId[] entityIds = new CmsUserScoreId[itemIds.length];
                    for (int i = 0; i < itemIds.length; i++) {
                        entityIds[i] = new CmsUserScoreId(userId, itemType, itemIds[i]);
                    }
                    List<CmsUserScore> entityList = service.getEntitys(entityIds);
                    Map<String, CmsUserScore> map = CommonUtils.listToMap(entityList, k -> String.valueOf(k.getId().getItemId()),
                            null, null);
                    handler.put("map", map).render();
                }
            }
        }
    }

    @Resource
    private CmsUserScoreService service;

}
