package com.evolveum.midpoint.studio.impl;

import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created by Viliam Repan (lazyman).
 */
@State(
        name = "EnvironmentManager", storages = @Storage(value = "midpoint.xml")
)
public class EnvironmentManagerImpl extends ManagerBase<EnvironmentSettings> implements EnvironmentManager {

    public static final String EVT_SELECTION_CHANGED = "selection_changed";

    private static final String KEY_PROXY_SUFFIX = "/proxy";

    private static final String DESCRIPTION_PROXY_SUFFIX = " (proxy)";

    private static final Logger LOG = Logger.getInstance(EnvironmentManagerImpl.class);

    private CredentialsManager credentialsManager;

    public EnvironmentManagerImpl(@NotNull Project project, @NotNull CredentialsManager credentialsManager) {
        super(project, EnvironmentSettings.class);

        this.credentialsManager = credentialsManager;
    }

    @Override
    protected EnvironmentSettings createDefaultSettings() {
        return EnvironmentSettings.createDefaultSettings();
    }

    @Override
    public void setSettings(EnvironmentSettings settings) {
        LOG.debug("Setting new settings " + settings);

        Set<String> newIds = new HashSet<>();
        for (Environment env : settings.getEnvironments()) {
            add(env);
            newIds.add(env.getId());
        }

        Iterator<Environment> iterator = getEnvironments().iterator();
        while (iterator.hasNext()) {
            Environment env = iterator.next();
            if (!newIds.contains(env.getId())) {
                iterator.remove();
            }
        }

        select(settings.getSelectedId());

        super.setSettings(settings);
    }

    @Override
    public EnvironmentSettings getFullSettings() {
        EnvironmentSettings settings = new EnvironmentSettings();
        settings.setEnvironments(getEnvironments());
        settings.setSelectedId(getSettings().getSelectedId());

        return settings;
    }

    @Override
    public List<Environment> getEnvironments() {
        List<Environment> result = new ArrayList<>();

        List<Environment> environments = getSettings().getEnvironments();
        for (Environment env : environments) {
            Environment copy = buildFullEnvironment(env);
            result.add(copy);
        }

        return result;
    }

    private Environment buildFullEnvironment(Environment env) {
        Environment copy = new Environment();
        try {
            BeanUtils.copyProperties(copy, env);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            //todo handle exception
            throw new RuntimeException(ex);
        }


        Credentials credentials = credentialsManager.get(copy.getId());
        if (credentials != null) {
            copy.setUsername(credentials.getUsername());
            copy.setPassword(credentials.getPassword());
        }

        credentials = credentialsManager.get(copy.getId() + KEY_PROXY_SUFFIX);
        if (credentials != null) {
            copy.setProxyUsername(credentials.getUsername());
            copy.setProxyPassword(credentials.getPassword());
        }

        return copy;
    }

    @Override
    public Environment getSelected() {
        Environment env = getSettings().getSelected();
        return env != null ? buildFullEnvironment(env) : null;
    }

    @Override
    public void select(String id) {
        Environment selected = getSelected();
        Environment newSelected = get(id);

        LOG.debug("Selecting new environment " + newSelected + ", old one " + selected);

        if (Objects.equals(selected, newSelected)) {
            return;
        }

        getSettings().setSelectedId(id);

        fireOnEvent(new Event(EVT_SELECTION_CHANGED, newSelected));
    }

    @Override
    public String add(Environment env) {
        LOG.debug("Adding environment " + env);

        if (!StringUtils.isAllEmpty(env.getUsername(), env.getPassword())) {
            Credentials credentials = new Credentials(env.getId(), env.getUsername(), env.getPassword(), env.getName());
            credentialsManager.add(credentials);
        }

        if (!StringUtils.isAllEmpty(env.getProxyUsername(), env.getProxyPassword())) {
            Credentials credentials = new Credentials(env.getId() + KEY_PROXY_SUFFIX, env.getProxyUsername(),
                    env.getProxyPassword(), env.getName() + DESCRIPTION_PROXY_SUFFIX);
            credentialsManager.add(credentials);
        }

        getSettings().getEnvironments().add(env);
        settingsUpdated();

        return env.getId();
    }

    @Override
    public boolean delete(String id) {
        LOG.debug("Deleting environment " + id);
        Environment env = get(id);
        if (env == null) {
            return false;
        }

        if (!StringUtils.isAllEmpty(env.getUsername(), env.getPassword())) {
            credentialsManager.delete(env.getId());
        }

        if (!StringUtils.isAllEmpty(env.getProxyUsername(), env.getProxyPassword())) {
            credentialsManager.delete(env.getId() + KEY_PROXY_SUFFIX);
        }

        getSettings().getEnvironments().remove(env);
        settingsUpdated();

        return true;
    }

    @Override
    public Environment get(String id) {
        if (id == null) {
            return null;
        }

        List<Environment> envs = getSettings().getEnvironments();
        for (Environment env : envs) {
            if (Objects.equals(id, env.getId())) {
                return buildFullEnvironment(env);
            }
        }

        return null;
    }
}
