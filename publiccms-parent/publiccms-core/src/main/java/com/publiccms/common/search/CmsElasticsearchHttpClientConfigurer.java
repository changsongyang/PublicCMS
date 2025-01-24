package com.publiccms.common.search;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.hibernate.search.backend.elasticsearch.client.ElasticsearchHttpClientConfigurationContext;
import org.hibernate.search.backend.elasticsearch.client.ElasticsearchHttpClientConfigurer;

public class CmsElasticsearchHttpClientConfigurer implements ElasticsearchHttpClientConfigurer {

    @Override
    public void configure(ElasticsearchHttpClientConfigurationContext context) {
        context.clientBuilder().setSSLHostnameVerifier(new NoopHostnameVerifier());
    }

}
