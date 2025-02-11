package com.publiccms.logic.component.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.publiccms.common.api.Config;
import com.publiccms.common.constants.CommonConstants;
import com.publiccms.common.constants.Constants;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.common.tools.LanguagesUtils;
import com.publiccms.entities.sys.SysExtendField;
import com.publiccms.entities.sys.SysSite;

/**
 *
 * ImageConfigComponent 图片配置组件
 *
 */
@Component
public class ImageConfigComponent implements Config {

    /**
     *
     */
    public static final String CONFIG_CODE = "image";

    /**
     *
     */
    public static final String CONFIG_CODE_DESCRIPTION = CommonUtils.joinString(CONFIGPREFIX, CONFIG_CODE);

    /**
     * max image width
     */
    public static final String CONFIG_MAX_IMAGE_WIDTH = "max_image_width";
    /**
     * watermark position
     */
    public static final String CONFIG_WATERMARK_POSITION = "watermark_position";
    /**
     * watermark alpha
     */
    public static final String CONFIG_WATERMARK_ALPHA = "watermark_alpha";
    /**
     * watermark image
     */
    public static final String CONFIG_WATERMARK_IMAGE = "watermark_image";
    /**
     * watermark text
     */
    public static final String CONFIG_WATERMARK_TEXT = "watermark_text";
    /**
     * watermark use nickname
     */
    public static final String CONFIG_WATERMARK_USE_NICKNAME = "watermark_use_nickname";
    /**
     * watermark text font
     */
    public static final String CONFIG_WATERMARK_TEXT_FONT = "watermark_text_font";
    /**
     * watermark text font size
     */
    public static final String CONFIG_WATERMARK_TEXT_FONTSIZE = "watermark_text_fontsize";
    /**
     * watermark text color
     */
    public static final String CONFIG_WATERMARK_TEXT_COLOR = "watermark_text_color";
    /**
     * watermark alpha
     */
    public static final String DEFAULT_POSITION = "rightBottom";
    /**
     * default watermark alpha
     */
    public static final int DEFAULT_FONTSIZE = 14;
    /**
     * default watermark alpha
     */
    public static final float DEFAULT_ALPHA = 0.5f;

    @Override
    public String getCode(short siteId, boolean showAll) {
        return CONFIG_CODE;
    }

    /**
     * @param locale
     * @return
     */
    @Override
    public String getCodeDescription(Locale locale) {
        return LanguagesUtils.getMessage(CommonConstants.applicationContext, locale, CONFIG_CODE_DESCRIPTION);
    }

    @Override
    public List<SysExtendField> getExtendFieldList(SysSite site, Locale locale) {
        List<SysExtendField> extendFieldList = new ArrayList<>();

        extendFieldList
                .add(new SysExtendField(CONFIG_MAX_IMAGE_WIDTH, INPUTTYPE_NUMBER,
                        getMessage(locale,
                                CommonUtils.joinString(CONFIG_CODE_DESCRIPTION, Constants.DOT, CONFIG_MAX_IMAGE_WIDTH)),
                        null));

        extendFieldList.add(new SysExtendField(CONFIG_WATERMARK_POSITION, INPUTTYPE_POSITION, true,
                getMessage(locale,
                        CommonUtils.joinString(CONFIG_CODE_DESCRIPTION, Constants.DOT, CONFIG_WATERMARK_POSITION)),
                null, DEFAULT_POSITION));
        extendFieldList.add(new SysExtendField(CONFIG_WATERMARK_ALPHA, INPUTTYPE_NUMBER, true,
                getMessage(locale,
                        CommonUtils.joinString(CONFIG_CODE_DESCRIPTION, Constants.DOT, CONFIG_WATERMARK_ALPHA)),
                null, String.valueOf(DEFAULT_ALPHA)));

        extendFieldList.add(new SysExtendField(CONFIG_WATERMARK_IMAGE, INPUTTYPE_PRIVATEIMAGE,
                getMessage(locale,
                        CommonUtils.joinString(CONFIG_CODE_DESCRIPTION, Constants.DOT, CONFIG_WATERMARK_IMAGE)),
                null));
        extendFieldList
                .add(new SysExtendField(CONFIG_WATERMARK_TEXT, INPUTTYPE_TEXT,
                        getMessage(locale,
                                CommonUtils.joinString(CONFIG_CODE_DESCRIPTION, Constants.DOT, CONFIG_WATERMARK_TEXT)),
                        null));
        extendFieldList.add(new SysExtendField(CONFIG_WATERMARK_USE_NICKNAME, INPUTTYPE_BOOLEAN,
                getMessage(locale,
                        CommonUtils.joinString(CONFIG_CODE_DESCRIPTION, Constants.DOT, CONFIG_WATERMARK_USE_NICKNAME)),
                null));
        extendFieldList.add(new SysExtendField(CONFIG_WATERMARK_TEXT_FONT, INPUTTYPE_FONT,
                getMessage(locale,
                        CommonUtils.joinString(CONFIG_CODE_DESCRIPTION, Constants.DOT, CONFIG_WATERMARK_TEXT_FONT)),
                null));
        extendFieldList.add(new SysExtendField(
                CONFIG_WATERMARK_TEXT_FONTSIZE, INPUTTYPE_NUMBER, true, getMessage(locale, CommonUtils
                        .joinString(CONFIG_CODE_DESCRIPTION, Constants.DOT, CONFIG_WATERMARK_TEXT_FONTSIZE)),
                null, String.valueOf(DEFAULT_FONTSIZE)));
        extendFieldList.add(new SysExtendField(CONFIG_WATERMARK_TEXT_COLOR, INPUTTYPE_COLOR,
                getMessage(locale,
                        CommonUtils.joinString(CONFIG_CODE_DESCRIPTION, Constants.DOT, CONFIG_WATERMARK_TEXT_COLOR)),
                null));

        return extendFieldList;
    }

    @Override
    public boolean exportable() {
        return true;
    }
}
