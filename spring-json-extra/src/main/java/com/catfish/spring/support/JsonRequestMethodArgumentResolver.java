package com.catfish.spring.support;

import com.catfish.bean.JSONArrayWrapper;
import com.catfish.bean.JSONObjectWrapper;
import com.catfish.bean.JSONWrapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StreamUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodArgumentResolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by A on 2017/3/21.
 */
public class JsonRequestMethodArgumentResolver extends AbstractMessageConverterMethodArgumentResolver {


    private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache = new ConcurrentHashMap<MethodParameter, NamedValueInfo>(256);
    private static ThreadLocal<HttpInputMessage> threadLocal = new ThreadLocal<HttpInputMessage>();
    private static ThreadLocal<byte[]> threadLocal2 = new ThreadLocal<byte[]>();

    private Logger logger = LoggerFactory.getLogger(getClass());


    private static final Object NO_VALUE = new Object();
/*
    private static class ThreadCache {
        private static ThreadLocal<HttpInputMessage> threadLocal = new ThreadLocal<HttpInputMessage>();

        public static HttpInputMessage getPostRequestParams() {
            return threadLocal.get();
        }

        public static void setPostRequestParams(HttpInputMessage postRequestParams) {
            threadLocal.set(postRequestParams);
        }

        public static void removePostRequestParams() {
            threadLocal.remove();
        }
    }*/

    public JsonRequestMethodArgumentResolver(List<HttpMessageConverter<?>> converters) {
        super(converters);
        logger.info("JsonRequestMethodArgumentResolver");
    }

    public JsonRequestMethodArgumentResolver(List<HttpMessageConverter<?>> converters, List<Object> requestResponseBodyAdvice) {
        super(converters, requestResponseBodyAdvice);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(JsonRequest.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Object arg = readWithMessageConverters(webRequest, parameter, parameter.getGenericParameterType());
        JsonRequest jsonRequest = parameter.getParameterAnnotation(JsonRequest.class);
        if (arg == null) {
            if (jsonRequest != null) {
                if (!jsonRequest.defaultValue().equals(ValueConstants.DEFAULT_NONE)) {
                    arg = jsonRequest.defaultValue();
                }
            }
        }
        //校验是否必填
        if (arg == null) {
            if (checkRequired(parameter)) {
                throw new PathNotFoundException("Required request body is missing: " +
                        parameter.getParameterName());
            }
        }
        NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
        if (binderFactory != null) {
            WebDataBinder binder = binderFactory.createBinder(webRequest, null, namedValueInfo.name);
            try {
                arg = binder.convertIfNecessary(arg, parameter.getParameterType(), parameter);
            } catch (ConversionNotSupportedException ex) {
                throw new MethodArgumentConversionNotSupportedException(arg, ex.getRequiredType(),
                        "", parameter, ex.getCause());
            } catch (TypeMismatchException ex) {
                throw new MethodArgumentTypeMismatchException(arg, ex.getRequiredType(),
                        "", parameter, ex.getCause());
            }
        }
        return arg;
    }

    private NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        JsonRequest jsonRequest = parameter.getParameterAnnotation(JsonRequest.class);
        return new NamedValueInfo(jsonRequest.value(), jsonRequest.required(), jsonRequest.defaultValue());
    }

    private NamedValueInfo updateNamedValueInfo(MethodParameter parameter, NamedValueInfo info) {
        String name = info.name;
        if (info.name.length() == 0) {
            name = parameter.getParameterName();
            if (name == null) {
                throw new IllegalArgumentException(
                        "Name for argument type [" + parameter.getNestedParameterType().getName() +
                                "] not available, and parameter name information not found in class file either.");
            }
        }
        String defaultValue = (ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
        return new NamedValueInfo(name, info.required, defaultValue);
    }

    private NamedValueInfo getNamedValueInfo(MethodParameter parameter) {
        NamedValueInfo namedValueInfo = this.namedValueInfoCache.get(parameter);
        if (namedValueInfo == null) {
            namedValueInfo = createNamedValueInfo(parameter);
            namedValueInfo = updateNamedValueInfo(parameter, namedValueInfo);
            this.namedValueInfoCache.put(parameter, namedValueInfo);
        }
        return namedValueInfo;
    }


    protected static class NamedValueInfo {

        private final String name;

        private final boolean required;

        private final String defaultValue;

        public NamedValueInfo(String name, boolean required, String defaultValue) {
            this.name = name;
            this.required = required;
            this.defaultValue = defaultValue;
        }
    }

    protected boolean checkRequired(MethodParameter methodParam) {
        return methodParam.getParameterAnnotation(JsonRequest.class).required();
    }

    @Override
    protected <T> Object readWithMessageConverters(HttpInputMessage inputMessage, MethodParameter param, Type targetType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
        String currentThreadName = Thread.currentThread().getName();
        long id = Thread.currentThread().getId();
        if (logger.isDebugEnabled()) {
            logger.debug(currentThreadName + id + " is running!");
        }

        MediaType contentType;
        boolean noContentType = false;
        try {
            contentType = inputMessage.getHeaders().getContentType();
        } catch (InvalidMediaTypeException ex) {
            throw new HttpMediaTypeNotSupportedException(ex.getMessage());
        }
        if (contentType == null) {
            noContentType = true;
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        Class<?> contextClass = (param != null ? param.getContainingClass() : null);
        Class<T> targetClass = (targetType instanceof Class<?> ? (Class<T>) targetType : null);
        if (targetClass == null) {
            ResolvableType resolvableType = (param != null ?
                    ResolvableType.forMethodParameter(param) : ResolvableType.forType(targetType));
            targetClass = (Class<T>) resolvableType.resolve();
        }
        //第一次获取流
        if (threadLocal.get() == null) {
            threadLocal2.set(StreamUtils.copyToByteArray(inputMessage.getBody()));
            threadLocal.set(new CloneBodyHttpInputMessage(inputMessage, threadLocal2.get()));
            inputMessage = threadLocal.get();
        } else {
            inputMessage = new CloneBodyHttpInputMessage(threadLocal.get(), threadLocal2.get());
        }
        Object body = null;
        Charset charset = contentType.getCharset();
        if (charset == null) {
            charset = Charset.defaultCharset();
        }
        if (canJsonPathRead(targetClass)) {
            body = jsonPathRead(inputMessage, param,charset);
        } else if (canJSONWrapper(targetClass)) {
            String json = StreamUtils.copyToString(inputMessage.getBody(), charset);
            try {
                body = new JSONWrapper(JSONObject.fromObject(json));
            }catch (JSONException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("", e);
                }
            }
        } else if (canJsonObjectWrapper(targetClass)) {
            String json = StreamUtils.copyToString(inputMessage.getBody(), charset);
            String value = param.getParameterAnnotation(JsonRequest.class).value();
            if (value == null || value.isEmpty()) {
                value = "$." + param.getParameterName();
            }
            try {
                body = new JSONObjectWrapper(JSONObject.fromObject(JsonPath.read(json, value)));
            } catch (JSONException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("", e);
                }
            }
        } else if (canJsonArrayWrapper(targetClass)) {
            String json = StreamUtils.copyToString(inputMessage.getBody(), charset);
            if (logger.isDebugEnabled()){
                logger.debug(json);
            }
            String value = param.getParameterAnnotation(JsonRequest.class).value();
            if (value == null || value.isEmpty()) {
                value = "$." + param.getParameterName();
            }
            try {
                body = new JSONArrayWrapper(JSONArray.fromObject(JsonPath.read(json, value)));
            } catch (JSONException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("", e);
                }
            }
        } else {
            body = jsonRead(inputMessage, targetType, contextClass, contentType, targetClass, param);
        }
        return body;
    }

    private Boolean canJsonObjectWrapper(Class targetClass) {
        return targetClass.equals(JSONObjectWrapper.class);
    }

    private Boolean canJsonArrayWrapper(Class targetClass) {
        return targetClass.equals(JSONArrayWrapper.class);
    }

    private Boolean canJSONWrapper(Class targetClass) {
        return targetClass.equals(JSONWrapper.class);
    }

    private Boolean canJsonPathRead(Class targetClass) {
        return
                (
                        targetClass.equals(String.class) ||
                                targetClass.equals(Integer.class) ||
                                targetClass.equals(Byte.class) ||
                                targetClass.equals(Long.class) ||
                                targetClass.equals(Double.class) ||
                                targetClass.equals(Float.class) ||
                                targetClass.equals(Character.class) ||
                                targetClass.equals(Short.class) ||
                                targetClass.equals(BigDecimal.class) ||
                                targetClass.equals(BigInteger.class) ||
                                targetClass.equals(Boolean.class) ||
                                targetClass.equals(Date.class) ||
                                targetClass.isPrimitive()
                );
    }

    private Object jsonRead(HttpInputMessage inputMessage, Type targetType, Class<?> contextClass, MediaType contentType, Class targetClass, MethodParameter param) throws IOException {
        Object body = NO_VALUE;
        Charset charset = contentType.getCharset();
        if (charset == null) {
            charset = Charset.defaultCharset();
        }
        String json = StreamUtils.copyToString(inputMessage.getBody(), charset);
        String value = param.getParameterAnnotation(JsonRequest.class).value();
        if (value == null || value.isEmpty()) {
            value = "$." + param.getParameterName();
        }
        JSONObject jsonObject = JSONObject.fromObject(JsonPath.read(json, value));
        try {
            body = JSONObject.toBean(jsonObject, targetClass);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
        }
       /* for (HttpMessageConverter<?> converter : this.messageConverters) {
            Class<HttpMessageConverter<?>> converterType = (Class<HttpMessageConverter<?>>) converter.getClass();
            if (converter instanceof GenericHttpMessageConverter) {
                GenericHttpMessageConverter<?> genericConverter = (GenericHttpMessageConverter<?>) converter;
                if (genericConverter.canRead(targetType, contextClass, contentType)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Read [" + targetType + "] as \"" + contentType + "\" with [" + converter + "]");
                    }
                    if (inputMessage.getBody() != null) {
                        body = genericConverter.read(targetType, contextClass, inputMessage);
                    } else {
                        body = null;
                    }
                    break;
                }
            } else if (targetClass != null) {
                if (converter.canRead(targetClass, contentType)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Read [" + targetType + "] as \"" + contentType + "\" with [" + converter + "]");
                    }
                    if (inputMessage.getBody() != null) {
                        body = ((HttpMessageConverter) converter).read(targetClass, inputMessage);
                    } else {
                        body = null;
                    }
                    break;
                }
            }
        }*/
        return body;
    }

    private Object jsonPathRead(HttpInputMessage inputMessage, MethodParameter param,Charset charset) throws IOException {
        try {
            InputStream inputStream = inputMessage.getBody();

            String json = StreamUtils.copyToString(inputStream, charset);
            JsonRequest jsonRequest = param.getParameterAnnotation(JsonRequest.class);
            String value = jsonRequest.value();
            if (value == null || value.isEmpty()) {
                value = "$." + param.getParameterName();
            }
            return JsonPath.read(json, value);
        } catch (PathNotFoundException ex) {
            return null;
        }
    }

    private static class CloneBodyHttpInputMessage implements HttpInputMessage{
        private final InputStream body;

        private final HttpMethod method;

        private final HttpHeaders headers;

        public CloneBodyHttpInputMessage(HttpInputMessage inputMessage, byte[] bytes) throws IOException {
            this.headers = inputMessage.getHeaders();
            InputStream inputStream = new ByteArrayInputStream(bytes);

            if (inputStream == null) {
                this.body = null;
            } else if (inputStream.markSupported()) {
                inputStream.mark(1);
                this.body = (inputStream.read() != -1 ? inputStream : null);
                inputStream.reset();
            } else {
                PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
                int b = pushbackInputStream.read();
                if (b == -1) {
                    this.body = null;
                } else {
                    this.body = pushbackInputStream;
                    pushbackInputStream.unread(b);
                }
            }
            if (inputMessage instanceof CloneBodyHttpInputMessage) {
                this.method = ((CloneBodyHttpInputMessage) inputMessage).getMethod();
            } else {
                this.method = ((HttpRequest) inputMessage).getMethod();
            }

        }

        @Override
        public InputStream getBody() throws IOException {
            return body;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        public HttpMethod getMethod() {
            return this.method;
        }
    }
}
