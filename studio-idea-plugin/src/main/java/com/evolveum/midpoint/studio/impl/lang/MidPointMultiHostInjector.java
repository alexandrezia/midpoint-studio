package com.evolveum.midpoint.studio.impl.lang;

import com.evolveum.midpoint.studio.util.MidPointUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.GroovyLanguage;

import java.util.Collections;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointMultiHostInjector implements MultiHostInjector {

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        if (!(context instanceof XmlText)) {
            return;
        }

        XmlText text = (XmlText) context;
        XmlTag code = text.getParentTag();
        if (code == null || !"code".equalsIgnoreCase(code.getName())) {
            return;
        }

        XmlTag script = code.getParentTag();
        if (script == null || !"script".equalsIgnoreCase(script.getName())) {
            return;
        }

        XmlTag language = MidPointUtils.findSubTag(script, ScriptExpressionEvaluatorType.F_LANGUAGE);
        if (language != null) {
            if (!"groovy".equals(language.getValue().getText())) {
                return;
            }
        }

        registrar
                .startInjecting(GroovyLanguage.INSTANCE)
                .addPlace(null, null, (PsiLanguageInjectionHost) context, new TextRange(0, context.getTextLength()))
                .doneInjecting();
    }

    @NotNull
    @Override
    public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return Collections.singletonList(XmlText.class);
    }
}
