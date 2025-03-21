package com.publiccms.common.base;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.jdbc.ScriptRunner;

import com.publiccms.common.constants.CommonConstants;
import com.publiccms.common.constants.Constants;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.common.tools.ExtendUtils;
import com.publiccms.common.tools.UserPasswordUtils;
import com.publiccms.entities.sys.SysExtendField;
import com.publiccms.logic.component.config.SafeConfigComponent;
import com.publiccms.logic.component.site.SiteComponent;
import com.publiccms.logic.component.template.MetadataComponent;
import com.publiccms.views.pojo.entities.CmsCategoryType;
import com.publiccms.views.pojo.entities.CmsPageData;

/**
 *
 * AbstractCmsUpgrader
 *
 */
public abstract class AbstractCmsUpgrader {
    /**
     * 表名_ID_SEQ SEQUENCE主键策略
     */
    public static final String IDENTIFIER_GENERATOR_SEQUENCE = "com.publiccms.common.datasource.IDSequenceStyleGenerator";
    /**
     * ID自增主键策略
     */
    public static final String IDENTIFIER_GENERATOR_IDENTITY = "org.hibernate.id.IdentityGenerator";
    /**
     * 主键策略
     */
    public static final String IDENTIFIER_GENERATOR = IDENTIFIER_GENERATOR_IDENTITY;
    protected String version;

    /**
     * @param stringWriter
     * @param connection
     * @param fromVersion
     * @throws SQLException
     * @throws IOException
     */
    public abstract void update(StringWriter stringWriter, Connection connection, String fromVersion)
            throws SQLException, IOException;
    /**
     * @return old database config version list
     */
    public abstract List<String> getOldDatabaseConfigVersionList();
    
    /**
     * @return version list
     */
    public abstract List<String> getVersionList();

    /**
     * @return default port
     */
    public abstract int getDefaultPort();

    /**
     * @param dbconfig
     * @param host
     * @param port
     * @param database
     * @param timeZone
     * @throws IOException
     * @throws URISyntaxException
     */
    public abstract void setDataBaseUrl(Properties dbconfig, String host, String port, String database, String timeZone)
            throws IOException, URISyntaxException;

    public void setPassword(Connection connection, String username, String password) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("update sys_user set name=?,password=? where id = 1")) {
            statement.setString(1, username);
            statement.setString(2, UserPasswordUtils.passwordEncode(password, UserPasswordUtils.getSalt(), null, null));
            statement.executeUpdate();
        }
    }

    public void setSiteUrl(Connection connection, String siteurl) throws SQLException {
        if (null != siteurl) {
            String dynamicPath = siteurl.endsWith("/") ? siteurl : CommonUtils.joinString(siteurl, "/");
            String sitePath = CommonUtils.joinString(dynamicPath, "webfile/");
            try (PreparedStatement statement = connection
                    .prepareStatement("update sys_site set dynamic_path=?,site_path=? where id = 1")) {
                statement.setString(1, dynamicPath);
                statement.setString(2, sitePath);
                statement.executeUpdate();
            }
        }
    }

    protected void updateMetadata(StringWriter stringWriter, Connection connection) {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("select * from sys_site")) {
            while (rs.next()) {
                String filepath = CommonUtils.joinString(CommonConstants.CMS_FILEPATH, Constants.SEPARATOR,
                        SiteComponent.TEMPLATE_PATH, Constants.SEPARATOR, SiteComponent.SITE_PATH_PREFIX, rs.getString("id"),
                        Constants.SEPARATOR, MetadataComponent.METADATA_FILE);
                File file = new File(filepath);
                try {
                    Map<String, CmsPageData> dataMap = Constants.objectMapper.readValue(file, Constants.objectMapper
                            .getTypeFactory().constructMapType(HashMap.class, String.class, CmsPageData.class));
                    Constants.objectMapper.writeValue(new File(CommonUtils.joinString(CommonConstants.CMS_FILEPATH,
                            Constants.SEPARATOR, SiteComponent.TEMPLATE_PATH, Constants.SEPARATOR, SiteComponent.SITE_PATH_PREFIX,
                            rs.getString("id"), Constants.SEPARATOR, MetadataComponent.DATA_FILE)), dataMap);
                } catch (IOException | ClassCastException e) {
                    stringWriter.write(e.getMessage());
                    stringWriter.write(System.lineSeparator());
                }
            }
        } catch (SQLException e) {
            stringWriter.write(e.getMessage());
            stringWriter.write(System.lineSeparator());
            e.printStackTrace();
        }
    }

    protected void updateSiteConfig(StringWriter stringWriter, Connection connection) {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("select * from sys_config_data where code = 'code'");
                PreparedStatement insertStatement = connection
                        .prepareStatement("insert into sys_config_data (site_id, code, data) VALUES (?, ?, ?)")) {
            while (rs.next()) {
                if (null != rs.getString("data")) {
                    String data = rs.getString("data");
                    try {
                        Map<String, String> safeConfig = new HashMap<>();
                        Map<String, String> siteConfig = ExtendUtils.getExtendMap(data);
                        {
                            String value = siteConfig.remove(SafeConfigComponent.CONFIG_EXPIRY_MINUTES_WEB);
                            if (null == value) {
                                safeConfig.put(SafeConfigComponent.CONFIG_EXPIRY_MINUTES_WEB, value);
                            }
                        }
                        {
                            String value = siteConfig.remove(SafeConfigComponent.CONFIG_EXPIRY_MINUTES_MANAGER);
                            if (null == value) {
                                safeConfig.put(SafeConfigComponent.CONFIG_EXPIRY_MINUTES_MANAGER, value);
                            }
                        }
                        {
                            String value = siteConfig.remove(SafeConfigComponent.CONFIG_ALLOW_FILES);
                            if (null == value) {
                                safeConfig.put(SafeConfigComponent.CONFIG_ALLOW_FILES, value);
                            }
                        }
                        {
                            String value = siteConfig.remove(SafeConfigComponent.CONFIG_RETURN_URL);
                            if (null == value) {
                                safeConfig.put(SafeConfigComponent.CONFIG_RETURN_URL, value);
                            }
                        }
                        if (!safeConfig.isEmpty()) {
                            insertStatement.setShort(1, rs.getShort("site_id"));
                            insertStatement.setString(2, "safe");
                            insertStatement.setString(3, ExtendUtils.getExtendString(safeConfig, ""));
                            insertStatement.executeUpdate();
                        }
                    } catch (ClassCastException e) {
                        stringWriter.write(e.getMessage());
                        stringWriter.write(System.lineSeparator());
                        e.printStackTrace();
                    }
                }
            }
        } catch (SQLException e2) {
            stringWriter.write(e2.getMessage());
            stringWriter.write(System.lineSeparator());
            e2.printStackTrace();
        }
    }

    protected void updateCategoryType(StringWriter stringWriter, Connection connection) {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("select * from cms_category_type")) {
            while (rs.next()) {
                try {
                    CmsCategoryType entity = new CmsCategoryType();
                    String filepath = CommonUtils.joinString(CommonConstants.CMS_FILEPATH, Constants.SEPARATOR,
                            SiteComponent.TEMPLATE_PATH, Constants.SEPARATOR, SiteComponent.SITE_PATH_PREFIX,
                            rs.getString("site_id"), Constants.SEPARATOR, SiteComponent.CATEGORY_TYPE_FILE);
                    File file = new File(filepath);
                    file.getParentFile().mkdirs();
                    Map<String, CmsCategoryType> categoryTypeMap;
                    try {
                        categoryTypeMap = Constants.objectMapper.readValue(file, Constants.objectMapper.getTypeFactory()
                                .constructMapType(HashMap.class, String.class, CmsCategoryType.class));
                    } catch (IOException | ClassCastException e) {
                        categoryTypeMap = new HashMap<>();
                    }
                    entity.setId(rs.getString("id"));
                    entity.setName(rs.getString("name"));
                    entity.setSort(rs.getInt("sort"));
                    entity.setOnlyUrl(false);
                    if (null != rs.getString("extend_id")) {
                        List<SysExtendField> extendList = new ArrayList<>();
                        try (Statement extendFieldStatement = connection.createStatement();
                                ResultSet extendFieldRs = extendFieldStatement.executeQuery(CommonUtils.joinString(
                                        "select * from sys_extend_field where extend_id = ", rs.getString("extend_id")))) {
                            while (extendFieldRs.next()) {
                                SysExtendField e = new SysExtendField(extendFieldRs.getString("code"),
                                        extendFieldRs.getString("input_type"), extendFieldRs.getBoolean("required"),
                                        extendFieldRs.getString("name"), extendFieldRs.getString("description"),
                                        extendFieldRs.getString("default_value"));
                                e.setSort(extendFieldRs.getInt("sort"));
                                e.setMaxlength(extendFieldRs.getInt("maxlength"));
                                if (e.getMaxlength() != null && 0 == e.getMaxlength()) {
                                    e.setMaxlength(null);
                                }
                                e.setDictionaryId(extendFieldRs.getString("dictionary_id"));
                                extendList.add(e);
                            }
                        } catch (SQLException e1) {
                            stringWriter.write(e1.getMessage());
                            stringWriter.write(System.lineSeparator());
                            e1.printStackTrace();
                        }
                        entity.setExtendList(extendList);
                    }
                    categoryTypeMap.put(entity.getId(), entity);
                    Constants.objectMapper.writeValue(file, categoryTypeMap);
                } catch (IOException | SQLException e) {
                    stringWriter.write(e.getMessage());
                    stringWriter.write(System.lineSeparator());
                    e.printStackTrace();
                }
            }
        } catch (SQLException e2) {
            stringWriter.write(e2.getMessage());
            stringWriter.write(System.lineSeparator());
            e2.printStackTrace();
        }
    }

    protected void runScript(StringWriter stringWriter, Connection connection, String fromVersion, String toVersion)
            throws IOException {
        ScriptRunner runner = new ScriptRunner(connection);
        runner.setLogWriter(null);
        runner.setErrorLogWriter(new PrintWriter(stringWriter));
        runner.setAutoCommit(true);
        try (InputStream inputStream = getClass()
                .getResourceAsStream(CommonUtils.joinString("/initialization/sql/", fromVersion, "-", toVersion, ".sql"))) {
            if (null != inputStream) {
                runner.runScript(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            }
        }
        version = toVersion;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }
}
