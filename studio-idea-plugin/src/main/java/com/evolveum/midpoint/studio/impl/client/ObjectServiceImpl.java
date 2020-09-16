package com.evolveum.midpoint.studio.impl.client;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang.Validate;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ObjectServiceImpl<O extends ObjectType> extends CommonService<O> implements ObjectService<O> {

    private String oid;

    public ObjectServiceImpl(ServiceContext context, Class<O> type, String oid) {
        super(context, type);

        Validate.notNull(oid, "Oid must not be null");
        this.oid = oid;
    }

    @Override
    public O get() throws ObjectNotFoundException, AuthenticationException {
        return get(null);

    }

    @Override
    public O get(Collection<SelectorOptions<GetOperationOptions>> options) throws ObjectNotFoundException, AuthenticationException {
        String obj = getRaw(options);
        try {
            ParsingContext parsingContext = prismContext().createParsingContextForCompatibilityMode();
            PrismParser parser = prismContext().parserFor(obj).language(PrismContext.LANG_XML).context(parsingContext);

            return (O) parser.parse().asObjectable();
        } catch (SchemaException | IOException ex) {
            throw new RuntimeException("Couldn't parse object, reason: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getRaw() throws ObjectNotFoundException, AuthenticationException {
        return getRaw(null);
    }

    @Override
    public String getRaw(Collection<SelectorOptions<GetOperationOptions>> options) throws ObjectNotFoundException, AuthenticationException {
        return executeGet(options);
    }

    private String executeGet(Collection<SelectorOptions<GetOperationOptions>> options)
            throws ObjectNotFoundException, AuthenticationException {

        if (options == null) {
            options = new ArrayList<>();
        }

        // todo use options
        // todo
        return null;
//        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
//
//        String query = null;
//        if (GetOperationOptions.isRaw(rootOptions)) {
//            query = "options=raw";
//        }
//
//        OkHttpClient client = client();
//
//        String path = ObjectTypes.getRestTypeFromClass(type());
//        client.replacePath(REST_PREFIX + "/" + path + "/" + oid);
//
//        Response response = client.get();
//
//        validateResponseCode(response);
//
//        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
//            throw new SystemException("Unknown response status: " + response.getStatus());
//        }
//
//        return response.readEntity(String.class);
    }

    @Override
    public void modify(ObjectDelta<O> delta) throws CommonException {
        // todo implement
    }

    @Override
    public void delete() throws ObjectNotFoundException, AuthenticationException {
        delete(new DeleteOptions());
    }

    @Override
    public void delete(DeleteOptions opts) throws ObjectNotFoundException, AuthenticationException {
        if (opts == null) {
            opts = new DeleteOptions();
        }

        Map<String, String> options = new HashMap<>();
        if (opts.raw()) {
            options.put("options", "raw");
        }

        String path = ObjectTypes.getRestTypeFromClass(type());

        Request.Builder builder = context().build("/" + path + "/" + oid, options)
                .delete();

        Request req = builder.build();

        OkHttpClient client = context().getClient();
        try (okhttp3.Response response = client.newCall(req).execute()) {
            validateResponseCode(response);

        } catch (IOException ex) {
            // todo handle exception
            ex.printStackTrace();
        }
    }

    @Override
    public OperationResult testConnection() throws ObjectNotFoundException, AuthenticationException {
        if (!ResourceType.class.equals(type())) {
            throw new IllegalStateException("Can't call testConnection operation on non ResourceType object");
        }

        return null;
        // todo
//        OkHttpClient client = client();
//
//        String path = ObjectTypes.getRestTypeFromClass(type());
//        client.replacePath(REST_PREFIX + "/" + path + "/" + oid + "/test");
//
//        Response response = client.post(null);
//
//        validateResponseCode(response);
//
//        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
//            throw new SystemException("Unknown response status: " + response.getStatus());
//        }
//
//        OperationResultType res = response.readEntity(OperationResultType.class);
//        return res != null ? OperationResult.createOperationResult(res) : null;
    }

    private void validateResponseCode(okhttp3.Response response) throws ObjectNotFoundException, AuthenticationException {
        if (Response.Status.NOT_FOUND.getStatusCode() == response.code()) {
            throw new ObjectNotFoundException("Cannot get object with oid '" + oid + "'. Object doesn't exist");
        }

        if (Response.Status.UNAUTHORIZED.getStatusCode() == response.code()) {
            throw new AuthenticationException(Response.Status.fromStatusCode(response.code()).getReasonPhrase());
        }
    }
}
