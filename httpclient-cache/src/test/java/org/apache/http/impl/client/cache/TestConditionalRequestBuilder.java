/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.http.impl.client.cache;

import java.util.Date;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestConditionalRequestBuilder {

    private ConditionalRequestBuilder impl;

    @Before
    public void setUp() throws Exception {
        impl = new ConditionalRequestBuilder();
    }

    @Test
    public void testBuildConditionalRequestWithLastModified() throws ProtocolException {
        String theMethod = "GET";
        String theUri = "/theuri";
        String lastModified = "this is my last modified date";

        HttpRequest request = new BasicHttpRequest(theMethod, theUri);
        request.addHeader("Accept-Encoding", "gzip");

        Header[] headers = new Header[] {
                new BasicHeader("Date", DateUtils.formatDate(new Date())),
                new BasicHeader("Last-Modified", lastModified) };

        CacheEntry cacheEntry = new CacheEntry(new Date(), new Date(),
                new OKStatus(), headers,
                new ByteArrayEntity(new byte[] {}));
        HttpRequest newRequest = impl.buildConditionalRequest(request, cacheEntry);

        Assert.assertNotSame(request, newRequest);

        Assert.assertEquals(theMethod, newRequest.getRequestLine().getMethod());
        Assert.assertEquals(theUri, newRequest.getRequestLine().getUri());
        Assert.assertEquals(request.getRequestLine().getProtocolVersion(), newRequest
                .getRequestLine().getProtocolVersion());
        Assert.assertEquals(2, newRequest.getAllHeaders().length);

        Assert.assertEquals("Accept-Encoding", newRequest.getAllHeaders()[0].getName());
        Assert.assertEquals("gzip", newRequest.getAllHeaders()[0].getValue());

        Assert.assertEquals("If-Modified-Since", newRequest.getAllHeaders()[1].getName());
        Assert.assertEquals(lastModified, newRequest.getAllHeaders()[1].getValue());
    }

    @Test
    public void testBuildConditionalRequestWithETag() throws ProtocolException {
        String theMethod = "GET";
        String theUri = "/theuri";
        String theETag = "this is my eTag";

        HttpRequest request = new BasicHttpRequest(theMethod, theUri);
        request.addHeader("Accept-Encoding", "gzip");

        Header[] headers = new Header[] {
                new BasicHeader("Date", DateUtils.formatDate(new Date())),
                new BasicHeader("Last-Modified", DateUtils.formatDate(new Date())),
                new BasicHeader("ETag", theETag) };

        CacheEntry cacheEntry = new CacheEntry(new Date(), new Date(),
                new OKStatus(), headers, new ByteArrayEntity(new byte[] {}));

        HttpRequest newRequest = impl.buildConditionalRequest(request, cacheEntry);

        Assert.assertNotSame(request, newRequest);

        Assert.assertEquals(theMethod, newRequest.getRequestLine().getMethod());
        Assert.assertEquals(theUri, newRequest.getRequestLine().getUri());
        Assert.assertEquals(request.getRequestLine().getProtocolVersion(), newRequest
                .getRequestLine().getProtocolVersion());

        Assert.assertEquals(2, newRequest.getAllHeaders().length);

        Assert.assertEquals("Accept-Encoding", newRequest.getAllHeaders()[0].getName());
        Assert.assertEquals("gzip", newRequest.getAllHeaders()[0].getValue());

        Assert.assertEquals("If-None-Match", newRequest.getAllHeaders()[1].getName());
        Assert.assertEquals(theETag, newRequest.getAllHeaders()[1].getValue());
    }

    @Test
    public void testCacheEntryWithMustRevalidateDoesEndToEndRevalidation() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET","/",HttpVersion.HTTP_1_1);
        Date now = new Date();
        Date elevenSecondsAgo = new Date(now.getTime() - 11 * 1000L);
        Date tenSecondsAgo = new Date(now.getTime() - 10 * 1000L);
        Date nineSecondsAgo = new Date(now.getTime() - 9 * 1000L);

        Header[] cacheEntryHeaders = new Header[] {
                new BasicHeader("Date", DateUtils.formatDate(tenSecondsAgo)),
                new BasicHeader("ETag", "\"etag\""),
                new BasicHeader("Cache-Control","max-age=5, must-revalidate") };
        CacheEntry cacheEntry = new CacheEntry(elevenSecondsAgo, nineSecondsAgo,
                new OKStatus(), cacheEntryHeaders, new ByteArrayEntity(new byte[0]));

        HttpRequest result = impl.buildConditionalRequest(request, cacheEntry);

        boolean foundMaxAge0 = false;
        for(Header h : result.getHeaders("Cache-Control")) {
            for(HeaderElement elt : h.getElements()) {
                if ("max-age".equalsIgnoreCase(elt.getName())
                    && "0".equals(elt.getValue())) {
                    foundMaxAge0 = true;
                }
            }
        }
        Assert.assertTrue(foundMaxAge0);
    }

    @Test
    public void testCacheEntryWithProxyRevalidateDoesEndToEndRevalidation() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET","/",HttpVersion.HTTP_1_1);
        Date now = new Date();
        Date elevenSecondsAgo = new Date(now.getTime() - 11 * 1000L);
        Date tenSecondsAgo = new Date(now.getTime() - 10 * 1000L);
        Date nineSecondsAgo = new Date(now.getTime() - 9 * 1000L);

        Header[] cacheEntryHeaders = new Header[] {
                new BasicHeader("Date", DateUtils.formatDate(tenSecondsAgo)),
                new BasicHeader("ETag", "\"etag\""),
                new BasicHeader("Cache-Control","max-age=5, proxy-revalidate") };
        CacheEntry cacheEntry = new CacheEntry(elevenSecondsAgo, nineSecondsAgo,
                new OKStatus(), cacheEntryHeaders, new ByteArrayEntity(new byte[0]));

        HttpRequest result = impl.buildConditionalRequest(request, cacheEntry);

        boolean foundMaxAge0 = false;
        for(Header h : result.getHeaders("Cache-Control")) {
            for(HeaderElement elt : h.getElements()) {
                if ("max-age".equalsIgnoreCase(elt.getName())
                    && "0".equals(elt.getValue())) {
                    foundMaxAge0 = true;
                }
            }
        }
        Assert.assertTrue(foundMaxAge0);
    }
}