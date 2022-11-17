package com.publiccms.views.directive.cms;

// Generated 2022-5-10 by com.publiccms.common.generator.SourceGenerator

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.publiccms.entities.cms.CmsContentTextHistory;
import com.publiccms.logic.service.cms.CmsContentTextHistoryService;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.common.base.AbstractTemplateDirective;
import com.publiccms.common.handler.RenderHandler;

/**
*
* contentTextHistory 正文历史查询指令
* <p>
* 参数列表
* <ul>
* <li><code>id</code> 分类id,结果返回<code>object</code>
* {@link com.publiccms.entities.cms.CmsContentTextHistory}
* <li><code>ids</code>
* 多个评论id,逗号或空格间隔,当id为空时生效,结果返回<code>map</code>(id,<code>object</code>)
* </ul>
* 使用示例
* <p>
* &lt;@cms.contentTextHistory id=1&gt;${object.text}&lt;/@cms.contentTextHistory&gt;
* <p>
* &lt;@cms.contentTextHistory ids=1,2,3&gt;&lt;#list map as
* k,v&gt;${k}:${v.text}&lt;#sep&gt;,&lt;/#list&gt;&lt;/@cms.contentTextHistory&gt;
* 
* <pre>
  &lt;script&gt;
   $.getJSON('${site.dynamicPath}api/directive/cms/contentTextHistory?id=1&amp;appToken=接口访问授权Token', function(data){    
     console.log(data.text);
   });
   &lt;/script&gt;
* </pre>
*/
@Component
public class CmsContentTextHistoryDirective extends AbstractTemplateDirective {

    @Override
    public void execute(RenderHandler handler) throws IOException, Exception {
        Long id = handler.getLong("id");
        if (CommonUtils.notEmpty(id)) {
            CmsContentTextHistory entity = service.getEntity(id);
            if (null != entity) {
                handler.put("object", entity).render();
            }
        } else {
            Long[] ids = handler.getLongArray("ids");
            if (CommonUtils.notEmpty(ids)) {
                List<CmsContentTextHistory> entityList = service.getEntitys(ids);
                Map<String, CmsContentTextHistory> map = CommonUtils.listToMap(entityList, k -> k.getId().toString(), null, null);
                handler.put("map", map).render();
            }
        }
    }

    @Override
    public boolean needAppToken() {
        return true;
    }

    @Autowired
    private CmsContentTextHistoryService service;

}
