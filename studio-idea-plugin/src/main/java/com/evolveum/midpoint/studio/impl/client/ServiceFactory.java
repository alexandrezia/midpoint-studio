package com.evolveum.midpoint.studio.impl.client;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismContextFactory;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.util.DOMUtilSettings;
import com.evolveum.midpoint.util.MiscUtil;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ServiceFactory {

    public static final int TIMEOUT = 60;

    public static final PrismContext DEFAULT_PRISM_CONTEXT;

    static {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(ServiceFactory.class.getClassLoader());

            // just to initialize MiscUtil class with correct classloader
            MiscUtil.emptyIfNull("");

            DOMUtilSettings.setAddTransformerFactorySystemProperty(false);
            // todo create web client just to obtain extension schemas!

            PrismContextFactory factory = new MidPointPrismContextFactory();
            PrismContext prismContext = factory.createPrismContext();
            prismContext.initialize();

            DEFAULT_PRISM_CONTEXT = prismContext;
        } catch (Exception ex) {
            throw new IllegalStateException("Couldn't initialize prism context", ex);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    private String url;

    private String username;

    private String password;

    private boolean ignoreSSLErrors;

    private String proxyServer;

    private Integer proxyServerPort;

    private ProxyType proxyServerType = ProxyType.HTTP;

    private String proxyUsername;

    private String proxyPassword;

    private MessageListener messageListener;

    public ServiceFactory url(final String url) {
        this.url = url;
        return this;
    }

    public ServiceFactory username(final String username) {
        this.username = username;
        return this;
    }

    public ServiceFactory password(final String password) {
        this.password = password;
        return this;
    }

    public ServiceFactory proxyServer(final String proxyServer) {
        this.proxyServer = proxyServer;
        return this;
    }

    public ServiceFactory proxyServerPort(final Integer proxyServerPort) {
        this.proxyServerPort = proxyServerPort;
        return this;
    }

    public ServiceFactory proxyServerType(final ProxyType proxyServerType) {
        this.proxyServerType = proxyServerType;
        return this;
    }

    public ServiceFactory proxyUsername(final String proxyUsername) {
        this.proxyUsername = proxyUsername;
        return this;
    }

    public ServiceFactory proxyPassword(final String proxyPassword) {
        this.proxyPassword = proxyPassword;
        return this;
    }

    public ServiceFactory messageListener(final MessageListener messageListener) {
        this.messageListener = messageListener;
        return this;
    }

    public ServiceFactory ignoreSSLErrors(final boolean ignoreSSLErrors) {
        this.ignoreSSLErrors = ignoreSSLErrors;
        return this;
    }

    public Service create() throws Exception {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.followSslRedirects(false);
        builder.followRedirects(false);

        builder.writeTimeout(TIMEOUT, TimeUnit.SECONDS);
        builder.readTimeout(TIMEOUT, TimeUnit.SECONDS);
        builder.connectTimeout(TIMEOUT, TimeUnit.SECONDS);

        if (username != null || password != null) {
            builder.authenticator((route, response) -> {

                if (response.request().header("Authorization") != null) {
                    return null; // Give up, we've already failed to authenticate.
                }

                String credential = Credentials.basic(username, password);
                return response.request().newBuilder()
                        .header("Authorization", credential)
                        .build();
            });
        }

        if (ignoreSSLErrors) {
            X509TrustManager tm = new EmptyTrustManager();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{tm}, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, tm);
            builder.hostnameVerifier((hostname, session) -> true);
        }

        if (StringUtils.isNotEmpty(proxyServer)) {
            Proxy proxy = new Proxy(proxyServerType.getType(), new InetSocketAddress(proxyServer, proxyServerPort));
            builder.proxy(proxy);

            if (proxyUsername != null || proxyPassword != null) {
                builder.proxyAuthenticator((route, response) -> {

                    String credential = Credentials.basic(proxyUsername, proxyPassword);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                });
            }
        }

        builder.addInterceptor(chain -> {

            Request request = chain.request();
            Request newRequest;

            newRequest = request.newBuilder()
                    .addHeader("Accept", MediaType.APPLICATION_XML)
                    .addHeader("Content-Type", MediaType.APPLICATION_XML)
                    .build();

            return chain.proceed(newRequest);
        });

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                message -> {
                    if (messageListener == null) {
                        return;
                    }

                    messageListener.handleMessage(message);
                });
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        builder.addInterceptor(logging);

        ServiceContext context = new ServiceContext(url, DEFAULT_PRISM_CONTEXT, builder.build());

        return new ServiceImpl(context);
    }
}
