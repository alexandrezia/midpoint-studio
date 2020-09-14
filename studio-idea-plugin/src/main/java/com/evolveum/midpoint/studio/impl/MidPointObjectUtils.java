package com.evolveum.midpoint.studio.impl;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.studio.impl.browse.Constants;
import com.evolveum.midpoint.studio.util.MidPointUtils;
import com.evolveum.midpoint.util.DOMUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointObjectUtils {

    public static final String OBJECTS_XML_PREFIX = "<objects xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\">\n";

    public static final String OBJECTS_XML_SUFFIX = "</objects>\n";

    public static List<MidPointObject> parseProjectFile(VirtualFile file, String notificationKey) {
        try {
            Document doc = parseProjectFileToDOM(file, notificationKey);
            List<MidPointObject> objects = parseDocument(doc, file, file.getPath());

            if (objects.size() == 1) {
                objects.get(0).setWholeFile(true);
            }

            return objects;
        } catch (RuntimeException ex) {
            MidPointUtils.publishExceptionNotification(notificationKey,
                    "Couldn't parse file " + (file != null ? file.getName() : null), ex);

            return new ArrayList<>();
        }
    }

    public static Document parseProjectFileToDOM(VirtualFile file, String notificationKey) {
        try (InputStream is = file.getInputStream()) {
            return DOMUtil.parse(is);
        } catch (IOException ex) {
            MidPointUtils.publishExceptionNotification(notificationKey,
                    "Couldn't parse file " + (file != null ? file.getName() : null) + " to DOM", ex);
            return null;
        }
    }

    private static List<MidPointObject> parseDocument(Document doc, VirtualFile file, String displayName) {
        List<MidPointObject> rv = new ArrayList<>();
        if (doc == null) {
            return rv;
        }

        Element root = doc.getDocumentElement();
        String localName = root.getLocalName();
        if ("actions".equals(localName) || "objects".equals(localName)) {
            for (Element child : DOMUtil.listChildElements(root)) {
                DOMUtil.fixNamespaceDeclarations(child);
                MidPointObject o = parseElement(child);
                if (o != null) {
                    o.setRoot(false);
                    rv.add(o);
                }
            }
        } else {
            MidPointObject o = parseElement(root);
            if (o != null) {
                o.setRoot(true);
                rv.add(o);
            }
        }

        for (int i = 0; i < rv.size(); i++) {
            MidPointObject o = rv.get(i);
            o.setObjectIndex(i);
            o.setFile(file);
            String name;
            if (displayName != null) {
                name = displayName;
            } else if (file != null) {
                name = file.getPath();
            } else {
                name = "(unknown source)";
            }
            if (rv.size() > 1) {
                name += " (object " + (i + 1) + " of " + rv.size() + ")";
            }
            o.setDisplayName(name);
        }

        if (rv.size() > 0) {
            rv.get(rv.size() - 1).setLast(true);
        }

        return rv;
    }

    private static MidPointObject parseElement(Element element) {
        String localName = element.getLocalName();
        boolean executable = Constants.SCRIPTING_ACTIONS.contains(localName);
        ObjectTypes type = getObjectType(localName);

        MidPointObject o = new MidPointObject(DOMUtil.serializeDOMToString(element), type, executable);
        String oid = element.getAttribute("oid");
        if (StringUtils.isNotBlank(oid)) {
            o.setOid(oid);
        }
        Element nameElement = DOMUtil.getChildElement(element, "name");
        if (nameElement != null) {
            o.setName(nameElement.getTextContent());
        }
        return o;
    }

    private static ObjectTypes getObjectType(String localName) {
        if (localName == null) {
            return null;
        }

        for (ObjectTypes t : ObjectTypes.values()) {
            if (localName.equals(t.getElementName().getLocalPart())) {
                return t;
            }
        }

        return null;
    }
}
