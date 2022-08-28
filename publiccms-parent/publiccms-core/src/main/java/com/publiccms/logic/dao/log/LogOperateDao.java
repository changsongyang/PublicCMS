package com.publiccms.logic.dao.log;

import java.util.Date;

import org.springframework.stereotype.Repository;

import com.publiccms.common.base.BaseDao;
import com.publiccms.common.handler.PageHandler;
import com.publiccms.common.handler.QueryHandler;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.entities.log.LogOperate;

/**
 *
 * LogOperateDao
 * 
 */
@Repository
public class LogOperateDao extends BaseDao<LogOperate> {

    /**
     * @param siteId
     * @param channel
     * @param operate
     * @param userId
     * @param startCreateDate
     * @param endCreateDate
     * @param content
     * @param ip
     * @param orderType
     * @param pageIndex
     * @param pageSize
     * @return results page
     */
    public PageHandler getPage(Short siteId, String channel, String operate, Long userId, Date startCreateDate,
            Date endCreateDate, String content, String ip, String orderType, Integer pageIndex, Integer pageSize) {
        QueryHandler queryHandler = getQueryHandler("from LogOperate bean");
        if (CommonUtils.notEmpty(siteId)) {
            queryHandler.condition("bean.siteId = :siteId").setParameter("siteId", siteId);
        }
        if (CommonUtils.notEmpty(channel)) {
            queryHandler.condition("bean.channel = :channel").setParameter("channel", channel);
        }
        if (CommonUtils.notEmpty(operate)) {
            queryHandler.condition("bean.operate = :operate").setParameter("operate", operate);
        }
        if (CommonUtils.notEmpty(userId)) {
            queryHandler.condition("bean.userId = :userId").setParameter("userId", userId);
        }
        if (null != startCreateDate) {
            queryHandler.condition("bean.createDate > :startCreateDate").setParameter("startCreateDate", startCreateDate);
        }
        if (null != endCreateDate) {
            queryHandler.condition("bean.createDate <= :endCreateDate").setParameter("endCreateDate", endCreateDate);
        }
        if (CommonUtils.notEmpty(content)) {
            queryHandler.condition("bean.content like :content").setParameter("content", like(content));
        }
        if (CommonUtils.notEmpty(ip)) {
            queryHandler.condition("bean.ip like :ip").setParameter("ip", like(ip));
        }
        if (!ORDERTYPE_ASC.equalsIgnoreCase(orderType)) {
            orderType = ORDERTYPE_DESC;
        }
        queryHandler.order("bean.id").append(orderType);
        return getPage(queryHandler, pageIndex, pageSize);
    }

    /**
     * @param siteId
     * @param channel
     * @param operate
     * @param startCreateDate
     * @param endCreateDate
     * @param workloadType
     * @param pageIndex
     * @param pageSize
     * @return results page
     */
    public PageHandler getWorkLoadPage(Short siteId, String channel, String operate, Date startCreateDate, Date endCreateDate,
            String workloadType, Integer pageIndex, Integer pageSize) {
        String queryString = "bean.deptId";
        if ("user".equalsIgnoreCase(workloadType)) {
            queryString = "bean.userId";
        }
        QueryHandler queryHandler = getQueryHandler("select new com.publiccms.views.pojo.entities.Workload(");
        queryHandler.append(queryString);
        queryHandler.append(",count(*)) from LogOperate bean");
        if (null != siteId) {
            queryHandler.condition("bean.siteId = :siteId").setParameter("siteId", siteId);
        }
        if (CommonUtils.notEmpty(channel)) {
            queryHandler.condition("bean.channel = :channel").setParameter("channel", channel);
        }
        if (CommonUtils.notEmpty(operate)) {
            queryHandler.condition("bean.operate = :operate").setParameter("operate", operate);
        }
        if (null != startCreateDate) {
            queryHandler.condition("bean.createDate > :startCreateDate").setParameter("startCreateDate", startCreateDate);
        }
        if (null != endCreateDate) {
            queryHandler.condition("bean.createDate <= :endCreateDate").setParameter("endCreateDate", endCreateDate);
        }
        queryHandler.group(queryString);
        queryHandler.order("count(*) desc");
        return getPage(queryHandler, pageIndex, pageSize);
    }

    /**
     * @param siteId
     * @param createDate
     * @return number of data deleted
     */
    public int delete(Short siteId, Date createDate) {
        if (null != createDate) {
            QueryHandler queryHandler = getQueryHandler("delete from LogOperate bean");
            if (CommonUtils.notEmpty(siteId)) {
                queryHandler.condition("bean.siteId = :siteId").setParameter("siteId", siteId);
            }
            queryHandler.condition("bean.createDate <= :createDate").setParameter("createDate", createDate);
            return delete(queryHandler);
        }
        return 0;
    }

    @Override
    protected LogOperate init(LogOperate entity) {
        if (null == entity.getCreateDate()) {
            entity.setCreateDate(CommonUtils.getDate());
        }
        if (CommonUtils.notEmpty(entity.getContent())) {
            entity.setContent(CommonUtils.keep(entity.getContent(), 65535));
        }
        return entity;
    }

}