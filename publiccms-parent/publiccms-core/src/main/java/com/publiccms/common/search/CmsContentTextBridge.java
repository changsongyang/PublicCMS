package com.publiccms.common.search;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

import com.publiccms.common.constants.CommonConstants;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.common.tools.ExtendUtils;
import com.publiccms.entities.cms.CmsContent;
import com.publiccms.entities.cms.CmsContentAttribute;
import com.publiccms.logic.component.BeanComponent;

public class CmsContentTextBridge implements TypeBridge<CmsContent> {
    private final IndexFieldReference<String> textField;
    private final IndexFieldReference<String> filesField;
    private final IndexFieldReference<String> extendsField;
    private final IndexFieldReference<BigDecimal> minPriceField;
    private final IndexFieldReference<BigDecimal> maxPriceField;
    private final IndexObjectFieldReference extendField;

    public CmsContentTextBridge(IndexFieldReference<String> textField, IndexFieldReference<String> filesField,
            IndexFieldReference<String> extendsField, IndexFieldReference<BigDecimal> minPriceField,
            IndexFieldReference<BigDecimal> maxPriceField, IndexObjectFieldReference extendField) {
        this.textField = textField;
        this.filesField = filesField;
        this.extendsField = extendsField;
        this.minPriceField = minPriceField;
        this.maxPriceField = maxPriceField;
        this.extendField = extendField;
    }

    @Override
    public void write(DocumentElement target, CmsContent bridgedElement, TypeBridgeWriteContext context) {
        CmsContentAttribute attribute = BeanComponent.getContentAttributeService().getEntity(bridgedElement.getId());
        if (null != attribute) {
            if (CommonUtils.notEmpty(attribute.getExtendsFields()) && CommonUtils.notEmpty(attribute.getData())) {
                String[] fields = StringUtils.split(attribute.getExtendsFields(), CommonConstants.COMMA);
                Map<String, String> map = ExtendUtils.getExtendMap(attribute.getData());
                if (CommonUtils.notEmpty(fields) && CommonUtils.notEmpty(map)) {
                    DocumentElement extend = target.addObject(extendField);
                    for (String field : fields) {
                        extend.addValue(field, map.get(field));
                    }
                }
                target.addValue(this.textField, attribute.getSearchText());
                target.addValue(this.filesField, attribute.getFilesText());
                target.addValue(this.extendsField, attribute.getExtendsText());
                target.addValue(this.minPriceField, attribute.getMinPrice());
                target.addValue(this.maxPriceField, attribute.getMaxPrice());
            }
        }
    }
}