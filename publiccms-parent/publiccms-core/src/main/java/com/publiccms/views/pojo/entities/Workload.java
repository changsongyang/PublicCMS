package com.publiccms.views.pojo.entities;

public class Workload implements java.io.Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Integer categoryId;
    private Long userId;
    private Integer deptId;
    private long count;

    public Workload(Integer categoryId, long count) {
        this.categoryId = categoryId;
        this.count = count;
    }

    public Workload(Integer categoryId, Long userId, long count) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.count = count;
    }

    public Workload(Integer categoryId, Integer deptId, long count) {
        this.deptId = deptId;
        this.categoryId = categoryId;
        this.count = count;
    }

    /**
     * @return the categoryId
     */
    public Integer getCategoryId() {
        return categoryId;
    }

    /**
     * @param categoryId
     *            the categoryId to set
     */
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * @return the userId
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * @param userId
     *            the userId to set
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * @return the deptId
     */
    public Integer getDeptId() {
        return deptId;
    }

    /**
     * @param deptId
     *            the deptId to set
     */
    public void setDeptId(Integer deptId) {
        this.deptId = deptId;
    }

    /**
     * @return the count
     */
    public long getCount() {
        return count;
    }

    /**
     * @param count
     *            the count to set
     */
    public void setCount(long count) {
        this.count = count;
    }
}
