package com.evolveum.midpoint.studio.impl;

import com.evolveum.midpoint.client.api.AddOptions;
import com.evolveum.midpoint.client.api.AuthenticationException;
import com.evolveum.midpoint.client.api.MessageListener;
import com.evolveum.midpoint.client.api.Service;
import com.evolveum.midpoint.client.impl.ServiceFactory;
import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.studio.util.MidPointUtils;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointClient {

    private static final Logger LOG = Logger.getInstance(MidPointClient.class);

    private static final String NOTIFICATION_KEY = "MidPoint Rest Client";

    private Project project;

    private Environment environment;

    private MidPointManager midPointManager;

    private Service client;

    public MidPointClient(@NotNull Project project, @NotNull Environment environment) {
        this.project = project;
        this.environment = environment;

        this.midPointManager = MidPointManager.getInstance(project);

        init();
    }

    private void init() {
        LOG.debug("Initialization of rest client for environment " + environment.getName());
        long time = System.currentTimeMillis();

        try {
            ServiceFactory factory = new ServiceFactory();
            factory
                    .url(environment.getUrl())
                    .username(environment.getUsername())
                    .password(environment.getPassword())
                    .proxyServer(environment.getProxyServerHost())
                    .proxyServerPort(environment.getProxyServerPort())
                    .proxyServerType(environment.getProxyServerType())
                    .proxyUsername(environment.getProxyUsername())
                    .proxyPassword(environment.getProxyPassword())
                    .ignoreSSLErrors(environment.isIgnoreSslErrors());

            factory.messageListener((messageId, type, message) -> {

                ConsoleViewContentType contentType = ConsoleViewContentType.LOG_INFO_OUTPUT;
                if (MessageListener.MessageType.FAULT == type) {
                    contentType = ConsoleViewContentType.LOG_ERROR_OUTPUT;
                }

                midPointManager.printToConsole(MidPointClient.class, message, null, contentType);
            });

            client = factory.create();

            midPointManager.printToConsole(MidPointClient.class, "Client created", null, ConsoleViewContentType.LOG_INFO_OUTPUT);
        } catch (Exception ex) {
            handleGenericException("Couldn't create rest client", ex);
        }

        LOG.debug("Rest client initialized in {}ms", (System.currentTimeMillis() - time));
    }

    public Project getProject() {
        return project;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public PrismContext getPrismContext() {
        return client.prismContext();
    }

    public <O extends ObjectType> SearchResultList search(Class<O> type, ObjectQuery query, boolean raw) {
        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
        if (raw) {
            options.add(SelectorOptions.create(GetOperationOptions.createRaw()));
        }

        PrismContext ctx = client.prismContext();
        options.add(SelectorOptions.create(ctx.toUniformPath(ObjectType.F_NAME), GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        options.add(SelectorOptions.create(ctx.toUniformPath(ObjectType.F_SUBTYPE), GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        options.add(SelectorOptions.create(ctx.toUniformPath(OrgType.F_DISPLAY_NAME), GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));

        return search(type, query, options);
    }

    private <O extends ObjectType> SearchResultList<O> search(Class<O> type, ObjectQuery query,
                                                              Collection<SelectorOptions<GetOperationOptions>> options) {
        midPointManager.printToConsole(MidPointClient.class, "Starting objects search for "
                + type.getSimpleName() + ", " + options);

        SearchResultList<O> result = null;
        try {
            result = (SearchResultList) client.search(ObjectTypes.getObjectType(type).getClassDefinition())
                    .list(query, options);

            midPointManager.printToConsole(MidPointClient.class, "Search done");
        } catch (Exception ex) {
            handleGenericException("Error occurred while searching objects", ex);
        }

        return result;
    }

    private void handleGenericException(String message, Exception ex) {
        MidPointUtils.handleGenericException(project, MidPointClient.class, NOTIFICATION_KEY, message, ex);
    }

    public <O extends ObjectType> PrismObject<O> get(Class<O> type, String oid, SearchOptions opts) {
        midPointManager.printToConsole(MidPointClient.class, "Getting object "
                + type.getSimpleName() + " oid= " + oid + ", " + opts);

        PrismObject<O> result = null;
        try {
            Collection<SelectorOptions<GetOperationOptions>> options =
                    SelectorOptions.createCollection(GetOperationOptions.createRaw());
            ObjectType o = client.oid(ObjectTypes.getObjectType(type).getClassDefinition(), oid).get(options);
            result = (PrismObject) o.asPrismObject();

            midPointManager.printToConsole(MidPointClient.class, "Get done");
        } catch (Exception ex) {
            handleGenericException("Error occurred while searching objects", ex);
        }

        return result;
    }

    public OperationResult testResource(String oid) {
        midPointManager.printToConsole(MidPointClient.class, "Starting test resource for " + oid);

        try {
            return client.oid(ResourceType.class, oid).testConnection();
        } catch (Exception ex) {
            handleGenericException("Error occurred while testing resource", ex);
        }

        return null;
    }

    public <O extends ObjectType> UploadResponse upload(PrismObject<O> obj, UploadOptions options) throws AuthenticationException {
        AddOptions opts = options.buildAddOptions();

        UploadResponse response = new UploadResponse();

        String oid = client.add((ObjectType) obj.asObjectable()).add(opts);
        response.setOid(oid);

        if (options.testConnection() && ResourceType.class.equals(obj.getCompileTimeClass())) {
            OperationResult result = testResource(obj.getOid());

            response.setResult(result);

            if (!result.isSuccess()) {
                MidPointUtils.publishNotification(NOTIFICATION_KEY, "Test connection error",
                        "Test connection error for '" + obj.getName() + "'", NotificationType.ERROR,
                        new ShowResultNotificationAction(result));
            }
        }

        return response;
    }

    public PrismObject<?> parseObject(String xml) throws IOException, SchemaException {
        CredentialsManager cm = CredentialsManager.getInstance(project);
        Expander expander = new Expander(cm, new EnvironmentProperties(environment));

        String expanded = expander.expand(xml);

        PrismParser parser = createParser(new ByteArrayInputStream(expanded.getBytes()), getPrismContext());
        return parser.parse();
    }

    public List<PrismObject<?>> parseObjects(String xml) throws IOException, SchemaException {
        CredentialsManager cm = CredentialsManager.getInstance(project);
        Expander expander = new Expander(cm, new EnvironmentProperties(environment));

        String expanded = expander.expand(xml);

        PrismParser parser = createParser(new ByteArrayInputStream(expanded.getBytes()), getPrismContext());
        return parser.parseObjects();
    }

    public List<PrismObject<?>> parseObjects(VirtualFile file) throws IOException, SchemaException {
        CredentialsManager cm = CredentialsManager.getInstance(project);
        Expander expander = new Expander(cm, new EnvironmentProperties(environment));

        try (InputStream is = file.getInputStream()) {
            Charset charset = file.getCharset();
            InputStream expanded = expander.expand(is, charset != null ? charset : StandardCharsets.UTF_8);

            PrismParser parser = createParser(expanded, getPrismContext());
            return parser.parseObjects();
        }
    }

    public <O extends ObjectType> PrismObject<O> parseObject(VirtualFile file) throws IOException, SchemaException {
        CredentialsManager cm = CredentialsManager.getInstance(project);
        Expander expander = new Expander(cm, new EnvironmentProperties(environment));

        try (InputStream is = file.getInputStream()) {
            Charset charset = file.getCharset();
            InputStream expanded = expander.expand(is, charset != null ? charset : StandardCharsets.UTF_8);

            PrismParser parser = createParser(expanded, getPrismContext());
            return parser.parse();
        }
    }

    private PrismParser createParser(InputStream data, PrismContext ctx) {
        ParsingContext parsingContext = ctx.createParsingContextForCompatibilityMode();
        return ctx.parserFor(data).language(PrismContext.LANG_XML).context(parsingContext);
    }
}