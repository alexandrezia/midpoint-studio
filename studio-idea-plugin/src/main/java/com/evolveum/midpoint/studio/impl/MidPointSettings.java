package com.evolveum.midpoint.studio.impl;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointSettings implements Serializable {

    private String projectId;

    private String midPointVersion;

    private String dowloadFilePattern;

    private String generatedFilePattern;

    private boolean printRestCommunicationToConsole;

    private DocGeneratorOptions docGeneratorOptions;

    public MidPointSettings() {
    }

    public String getMidPointVersion() {
        return midPointVersion;
    }

    public void setMidPointVersion(String midPointVersion) {
        this.midPointVersion = midPointVersion;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getDowloadFilePattern() {
        return dowloadFilePattern;
    }

    public void setDowloadFilePattern(String dowloadFilePattern) {
        this.dowloadFilePattern = dowloadFilePattern;
    }

    public String getGeneratedFilePattern() {
        return generatedFilePattern;
    }

    public boolean isPrintRestCommunicationToConsole() {
        return printRestCommunicationToConsole;
    }

    public void setPrintRestCommunicationToConsole(boolean printRestCommunicationToConsole) {
        this.printRestCommunicationToConsole = printRestCommunicationToConsole;
    }

    public void setGeneratedFilePattern(String generatedFilePattern) {
        this.generatedFilePattern = generatedFilePattern;
    }

    public DocGeneratorOptions getDocGeneratorOptions() {
        return docGeneratorOptions;
    }

    public void setDocGeneratorOptions(DocGeneratorOptions docGeneratorOptions) {
        this.docGeneratorOptions = docGeneratorOptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MidPointSettings that = (MidPointSettings) o;

        if (printRestCommunicationToConsole != that.printRestCommunicationToConsole) return false;
        if (projectId != null ? !projectId.equals(that.projectId) : that.projectId != null) return false;
        if (midPointVersion != null ? !midPointVersion.equals(that.midPointVersion) : that.midPointVersion != null)
            return false;
        if (dowloadFilePattern != null ? !dowloadFilePattern.equals(that.dowloadFilePattern) : that.dowloadFilePattern != null)
            return false;
        if (generatedFilePattern != null ? !generatedFilePattern.equals(that.generatedFilePattern) : that.generatedFilePattern != null)
            return false;
        return docGeneratorOptions != null ? docGeneratorOptions.equals(that.docGeneratorOptions) : that.docGeneratorOptions == null;
    }

    @Override
    public int hashCode() {
        int result = projectId != null ? projectId.hashCode() : 0;
        result = 31 * result + (midPointVersion != null ? midPointVersion.hashCode() : 0);
        result = 31 * result + (dowloadFilePattern != null ? dowloadFilePattern.hashCode() : 0);
        result = 31 * result + (generatedFilePattern != null ? generatedFilePattern.hashCode() : 0);
        result = 31 * result + (printRestCommunicationToConsole ? 1 : 0);
        result = 31 * result + (docGeneratorOptions != null ? docGeneratorOptions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MidPointSettings{" +
                "projectId='" + projectId + '\'' +
                ", midPointVersion='" + midPointVersion + '\'' +
                '}';
    }

    public static MidPointSettings createDefaultSettings() {
        MidPointSettings settings = new MidPointSettings();
        settings.setProjectId(UUID.randomUUID().toString());
        settings.setMidPointVersion("4.1-SNAPSHOT");

        settings.setDowloadFilePattern("objects/$T/$n.xml");
        settings.setGeneratedFilePattern("scratches/gen/$n.xml");

        return settings;
    }
}
