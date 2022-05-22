package com.publiccms.controller.api;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import jakarta.annotation.Resource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.publiccms.common.base.AbstractTaskDirective;
import com.publiccms.common.base.AbstractTemplateDirective;
import com.publiccms.common.constants.CommonConstants;
import com.publiccms.common.directive.BaseTemplateDirective;
import com.publiccms.common.directive.HttpDirective;
import com.publiccms.common.handler.HttpParameterHandler;
import com.publiccms.logic.component.site.DirectiveComponent;
import com.publiccms.logic.component.site.SiteComponent;

/**
 * 
 * DirectiveController 接口指令统一分发
 *
 */
@Controller
public class DirectiveController {
    protected final Log log = LogFactory.getLog(getClass());
    private Map<String, BaseTemplateDirective> actionMap = new HashMap<>();
    private Map<String, List<Map<String, Object>>> namespaceMap = new TreeMap<>(new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }

    });
    private List<Map<String, String>> actionList = new ArrayList<>();
    @Resource
    protected MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter;
    @Resource
    protected SiteComponent siteComponent;
    protected DirectiveComponent directiveComponent;

    /**
     * 接口指令统一分发
     * 
     * @param action
     * @param request
     * @param response
     */
    @RequestMapping("directive/{action}")
    public void directive(@PathVariable String action, HttpServletRequest request, HttpServletResponse response) {
        try {
            HttpDirective directive = actionMap.get(action);
            if (null != directive) {
                directive.execute(mappingJackson2HttpMessageConverter, CommonConstants.jsonMediaType, request, response);
            } else {
                HttpParameterHandler handler = new HttpParameterHandler(mappingJackson2HttpMessageConverter,
                        CommonConstants.jsonMediaType, request, response);
                handler.put(CommonConstants.ERROR, ApiController.INTERFACE_NOT_FOUND).render();
            }
        } catch (Exception e) {
            HttpParameterHandler handler = new HttpParameterHandler(mappingJackson2HttpMessageConverter,
                    CommonConstants.jsonMediaType, request, response);
            try {
                handler.put(CommonConstants.ERROR, ApiController.EXCEPTION).render();
            } catch (Exception renderException) {
                log.error(renderException.getMessage());
            }
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 接口指令统一分发
     * 
     * @param namespace
     * @param directive
     * @param request
     * @param response
     */
    @RequestMapping("directive/{namespace}/{directive}")
    public void directive(@PathVariable String namespace, @PathVariable String directive, HttpServletRequest request,
            HttpServletResponse response) {
        try {
            Map<String, BaseTemplateDirective> directiveMap = directiveComponent.getNamespaceMap().get(namespace);
            BaseTemplateDirective d;
            if (null != directiveMap && null != (d = directiveMap.get(directive)) && d.httpEnabled()) {
                d.execute(mappingJackson2HttpMessageConverter, CommonConstants.jsonMediaType, request, response);
            } else {
                HttpParameterHandler handler = new HttpParameterHandler(mappingJackson2HttpMessageConverter,
                        CommonConstants.jsonMediaType, request, response);
                handler.put(CommonConstants.ERROR, ApiController.INTERFACE_NOT_FOUND).render();
            }
        } catch (Exception e) {
            HttpParameterHandler handler = new HttpParameterHandler(mappingJackson2HttpMessageConverter,
                    CommonConstants.jsonMediaType, request, response);
            try {
                log.error(e.getMessage(), e);
                handler.put(CommonConstants.ERROR, e.getMessage()).render();
            } catch (Exception renderException) {
                log.error(renderException.getMessage());
            }
        }
    }

    /**
     * 接口列表
     * 
     * @return result
     */
    @RequestMapping("directives")
    @ResponseBody
    public List<Map<String, String>> directives() {
        return actionList;
    }

    /**
     * 接口列表
     * 
     * @return result
     */
    @RequestMapping("namespaces")
    @ResponseBody
    public Map<String, List<Map<String, Object>>> namespaces() {
        if (namespaceMap.isEmpty()) {
            for (Entry<String, Map<String, BaseTemplateDirective>> entry : directiveComponent.getNamespaceMap().entrySet()) {
                List<Map<String, Object>> namespace = namespaceMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
                for (Entry<String, BaseTemplateDirective> entry2 : entry.getValue().entrySet()) {
                    if (entry2.getValue().httpEnabled()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", entry2.getValue().getShortName());
                        map.put("namespace", entry2.getValue().getNamespace());
                        if (entry2.getValue() instanceof AbstractTemplateDirective) {
                            AbstractTemplateDirective directive = (AbstractTemplateDirective) entry2.getValue();
                            map.put("needAppToken", directive.needAppToken());
                            map.put("needUserToken", directive.needUserToken());
                        } else {
                            map.put("needAppToken", true);
                            map.put("needUserToken", false);
                        }
                        namespace.add(map);
                    }
                }
                Collections.sort(namespace, new Comparator<Map<String, Object>>() {
                    @Override
                    public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                        return Collator.getInstance().compare(o1.get("name"), o2.get("name"));
                    }
                });
            }
        }
        return namespaceMap;

    }

    /**
     * 接口初始化
     * 
     * @param directiveComponent
     * 
     */
    @Resource
    public void init(DirectiveComponent directiveComponent) {
        this.directiveComponent = directiveComponent;
        for (Entry<String, AbstractTemplateDirective> entry : directiveComponent.getTemplateDirectiveMap().entrySet()) {
            if (entry.getValue().httpEnabled()) {
                Map<String, String> map = new HashMap<>();
                map.put("name", entry.getKey());
                map.put("shortName", entry.getValue().getShortName());
                map.put("namespace", entry.getValue().getNamespace());
                map.put("needAppToken", String.valueOf(entry.getValue().needAppToken()));
                map.put("needUserToken", String.valueOf(entry.getValue().needUserToken()));
                actionList.add(map);
                actionMap.put(entry.getKey(), entry.getValue());
            }
        }
        for (Entry<String, AbstractTaskDirective> entry : directiveComponent.getTaskDirectiveMap().entrySet()) {
            if (entry.getValue().httpEnabled()) {
                Map<String, String> map = new HashMap<>();
                map.put("name", entry.getKey());
                map.put("shortName", entry.getValue().getShortName());
                map.put("namespace", entry.getValue().getNamespace());
                map.put("needAppToken", String.valueOf(true));
                map.put("needUserToken", String.valueOf(false));
                actionList.add(map);
                actionMap.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
