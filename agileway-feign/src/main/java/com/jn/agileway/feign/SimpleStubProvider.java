package com.jn.agileway.feign;

import com.jn.agileway.feign.supports.rpc.rest.EasyjsonDecoder;
import com.jn.agileway.feign.supports.rpc.rest.EasyjsonEncoder;
import com.jn.agileway.feign.supports.rpc.rest.EasyjsonErrorDecoder;
import com.jn.agileway.feign.loadbalancer.DynamicLBClientFactory;
import com.jn.agileway.feign.supports.adaptable.AdaptableDecoder;
import com.jn.agileway.feign.supports.rpc.RpcInvocationHandlerFactory;
import com.jn.agileway.feign.supports.adaptable.ResponseBodyAdapter;
import com.jn.easyjson.core.JSONFactory;
import com.jn.easyjson.core.factory.JsonFactorys;
import com.jn.easyjson.core.factory.JsonScope;
import com.jn.langx.Nameable;
import com.jn.langx.annotation.Prototype;
import com.jn.langx.http.rest.RestRespBody;
import com.jn.langx.lifecycle.Initializable;
import com.jn.langx.lifecycle.InitializationException;
import com.jn.langx.text.StringTemplates;
import com.jn.langx.util.Preconditions;
import com.jn.langx.util.collection.Maps;
import com.jn.langx.util.function.Supplier;
import com.jn.langx.util.net.NetworkAddress;
import com.jn.langx.util.reflect.Reflects;
import feign.Client;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.form.FormEncoder;
import feign.httpclient.ApacheHttpClient;
import feign.ribbon.LBClientFactory;
import feign.ribbon.RibbonClient;
import feign.slf4j.Slf4jLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleStubProvider implements Initializable, StubProvider, Nameable {
    private static final Logger logger = LoggerFactory.getLogger(SimpleStubProvider.class);
    private Feign.Builder builder;
    private HttpConnectionContext context;
    private volatile boolean inited = false;
    private ConcurrentHashMap<Class, Object> serviceMap = new ConcurrentHashMap<Class, Object>();
    private JSONFactory jsonFactory;
    private Encoder encoder;
    private Decoder decoder;
    private ResponseBodyAdapter responseBodyAdapter;
    private ErrorDecoder errorDecoder;
    private InvocationHandlerFactory invocationHandlerFactory;
    private String name;


    /**
     * 如果项目中，没有对返回值进行统一处理，则可以设置为 Object.class
     */
    private Class unifiedRestResponseClass = RestRespBody.class;

    public JSONFactory getJsonFactory() {
        return jsonFactory;
    }

    public void setJsonFactory(JSONFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public void setContext(HttpConnectionContext context) {
        this.context = context;
    }

    public void setResponseBodyAdapter(ResponseBodyAdapter responseBodyAdapter) {
        this.responseBodyAdapter = responseBodyAdapter;
    }

    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    public void setErrorDecoder(ErrorDecoder errorDecoder) {
        this.errorDecoder = errorDecoder;
    }

    public void setInvocationHandlerFactory(InvocationHandlerFactory invocationHandlerFactory) {
        this.invocationHandlerFactory = invocationHandlerFactory;
    }

    public void setUnifiedRestResponseClass(Class unifiedRestResponseClass) {
        if (unifiedRestResponseClass != null) {
            this.unifiedRestResponseClass = unifiedRestResponseClass;
        }
    }

    @Override
    public void init() throws InitializationException {
        if (!inited) {
            inited = true;
            builder = createFeignBuilder();
        }
    }

    private Feign.Builder createFeignBuilder() {
        Preconditions.checkNotNull(context, "the connection context is null");

        // server addresses
        final List<NetworkAddress> licenseServerAddresses = context.getNodes();
        Preconditions.checkNotEmpty(licenseServerAddresses, new Supplier<Object[], String>() {
            @Override
            public String get(Object[] input) {
                return StringTemplates.formatWithPlaceholder("server addresses is invalid : {}", licenseServerAddresses);
            }
        });

        String loggerName = context.getConfiguration().getAccessLoggerName();
        Logger accessLogger = LoggerFactory.getLogger(loggerName);

        // access log level
        feign.Logger.Level accessLogLevel = context.getAccessLogLevel();
        if (accessLogLevel == null) {
            if (accessLogger.isDebugEnabled()) {
                accessLogLevel = feign.Logger.Level.FULL;
            } else {
                accessLogLevel = feign.Logger.Level.NONE;
            }
        }
        if (accessLogLevel != feign.Logger.Level.NONE && !logger.isDebugEnabled()) {
            logger.warn("the access log is enabled, the logger level of {} should be DEBUG at least", logger.getName());
        }

        // http client
        Client client = new ApacheHttpClient(context.getHttpClient());
        if (context.isLoadbalancerEnabled()) {
            LBClientFactory clientFactory = new DynamicLBClientFactory(context);
            client = RibbonClient.builder().delegate(client).lbClientFactory(clientFactory).build();
        }

        if (jsonFactory == null) {
            jsonFactory = JsonFactorys.getJSONFactory(JsonScope.SINGLETON);
        }
        if (encoder == null) {
            encoder = new FormEncoder(new EasyjsonEncoder(jsonFactory));
        }
        if (decoder == null) {
            decoder = new EasyjsonDecoder(jsonFactory);
        }
        if (responseBodyAdapter != null) {
            if (decoder instanceof AdaptableDecoder) {
                ((AdaptableDecoder) decoder).setAdapter(responseBodyAdapter);
            } else {
                decoder = new AdaptableDecoder(decoder, responseBodyAdapter);
            }
        }
        if (errorDecoder == null) {
            errorDecoder = new EasyjsonErrorDecoder();
        }
        Feign.Builder apiBuilder = Feign.builder()
                .logger(new Slf4jLogger(loggerName))
                .logLevel(accessLogLevel)
                .client(client)
                .encoder(encoder)
                .decoder(decoder)
                .errorDecoder(errorDecoder);

        if (this.invocationHandlerFactory == null) {
            RpcInvocationHandlerFactory factory = new RpcInvocationHandlerFactory();
            factory.setJsonFactory(jsonFactory);
            factory.setUnifiedResponseClass(unifiedRestResponseClass);
            this.invocationHandlerFactory = factory;
        }
        apiBuilder.invocationHandlerFactory(invocationHandlerFactory);
        return apiBuilder;

    }

    @Override
    public <Stub> Stub getStub(Class<Stub> stubInterface) {
        if (!inited) {
            init();
        }
        Preconditions.checkTrue(inited, "service provider is not inited");
        Preconditions.checkArgument(stubInterface.isInterface(), new Supplier<Object[], String>() {
            @Override
            public String get(Object[] objects) {
                return StringTemplates.formatWithPlaceholder("the service class {} is not interface");
            }
        }, Reflects.getFQNClassName(stubInterface));

        boolean isNotSingleton = Reflects.isAnnotationPresent(stubInterface, Prototype.class);
        if (isNotSingleton) {
            return createStub(stubInterface);
        }
        return (Stub) Maps.putIfAbsent(serviceMap, stubInterface, (Supplier<Class<Stub>, Stub>) new Supplier<Class<Stub>, Stub>() {
            @Override
            public Stub get(Class<Stub> clazz) {
                return createStub(clazz);
            }
        });

    }

    private <Service> Service createStub(Class<Service> serviceClass) {
        String url = context.getUrl();
        logger.info("create a service [{}] at the [{}] url: {}", Reflects.getFQNClassName(serviceClass), context.getConfiguration().getLoadbalancerHost(), url);
        return builder.target(serviceClass, url);
    }
}
