package com.catfish;

import com.catfish.controller.TestController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by A on 2017/3/27.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-mvc.xml","classpath:spring-applicationContext.xml"})
@WebAppConfiguration
@EnableWebMvc
public class ControllerTest {

    private MockMvc mockMvc;

    @Autowired
    TestController testController;

    @Before
    public void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(testController).build();
    }

    private String getForm(String url, Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.get(url);
        return getResult(requestForm(mockHttpServletRequestBuilder, params));
    }

    private ResultActions requestForm(MockHttpServletRequestBuilder mockHttpServletRequestBuilder, Map<String, String> params) throws Exception {
        mockHttpServletRequestBuilder
                .accept(MediaType.MULTIPART_FORM_DATA)
                .characterEncoding("UTF-8");
        Set<String> set = params.keySet();
        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            String value = params.get(key);
            mockHttpServletRequestBuilder.param(key, value);
        }
        ResultActions ra = this.mockMvc
                .perform(mockHttpServletRequestBuilder)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());
        return ra;
    }

    private ResultActions requestJson(MockHttpServletRequestBuilder mockHttpServletRequestBuilder, String content) throws Exception {
        mockHttpServletRequestBuilder
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .characterEncoding("UTF-8").content(content);
        ResultActions ra = this.mockMvc
                .perform(mockHttpServletRequestBuilder)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());
        return ra;
    }

    private ResultActions requestFile(MockHttpServletRequestBuilder mockHttpServletRequestBuilder, byte[] bytes) throws Exception {
        mockHttpServletRequestBuilder
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .characterEncoding("UTF-8");
        ResultActions ra = this.mockMvc
                .perform(mockHttpServletRequestBuilder)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());
        return ra;
    }

    private String getResult(ResultActions ra) throws Exception {
        MvcResult mr = ra.andReturn();
        String result = mr.getResponse().getContentAsString();
        return result;
    }

    private String postFile(String url, byte[] bytes) throws Exception {
        MockHttpServletRequestBuilder fileRequestBuilder =
                MockMvcRequestBuilders.fileUpload(url).file("image", bytes);
        return getResult(requestFile(fileRequestBuilder, bytes));
    }

    private String postForm(String url) throws Exception {
        return postForm(url, new HashMap<String, String>());
    }

    private String postForm(String url, Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.post(url);
        return getResult(requestForm(mockHttpServletRequestBuilder, params));
    }

    private String postJson(String url, String content) throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.post(url);
        return getResult(requestJson(mockHttpServletRequestBuilder, content));
    }

    @Test
    public void getParameterByPathTest() throws Exception {
        String json = "{\n" +
                "    \"id\": \"1\",\n" +
                "    \"name\": \"qqq\"\n" +
                "}";
        String result = postJson("/getParameterByPath.do", json);
        Assert.assertEquals("1,qqq",result);
    }


}
