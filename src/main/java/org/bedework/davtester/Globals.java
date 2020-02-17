/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.davtester;

import org.bedework.util.misc.ToString;

import java.util.List;
import java.util.Map;

/**
 * User: mike Date: 2/13/20 Time: 12:10
 */
public class Globals {
  private boolean all;

  private String basedir = "src/main/rsrc/";

  private String dtds = "$basedir:dtds";

  private List<String> excludes;

  private boolean httptrace;

  private List<String> observers;

  private String outputName;

  private String posttest;

  private String pretest;

  private boolean printDetailsOnFail = true;

  private boolean printRequest = true;

  private boolean printResponse = true;

  private boolean random = true;

  private String resDir = "$basedir:";

  private String serverInfo = "$basedir:server/serverinfo.xml";

  private boolean ssl;

  private boolean stopOnFail;

  private String subdir;

  private List<String> tests;

  private Map<String, List<String>> testsets;

  private String testsDir = "$basedir:tests";

  private boolean waitCountDump;

  public void setAll(final boolean val) {
    all = val;
  }

  public boolean getAll() {
    return all;
  }

  public void setBasedir(final String val) {
    basedir = val;
  }

  public String getBasedir() {
    return basedir;
  }

  public void setDtds(final String val) {
    dtds = val;
  }

  public String getDtds() {
    return dtds;
  }

  public void setExcludes(final List<String> val) {
    excludes = val;
  }

  public List<String> getExcludes() {
    return excludes;
  }

  public void setHttptrace(final boolean val) {
    httptrace = val;
  }

  public boolean getHttptrace() {
    return httptrace;
  }

  public void setObservers(final List<String> val) {
    observers = val;
  }

  public List<String> getObservers() {
    return observers;
  }

  public void setOutputName(final String val) {
    outputName = val;
  }

  public String getOutputName() {
    return outputName;
  }

  public void setPosttest(final String val) {
    posttest = val;
  }

  public String getPosttest() {
    return posttest;
  }

  public void setPretest(final String val) {
    pretest = val;
  }

  public String getPretest() {
    return pretest;
  }

  public void setPrintDetailsOnFail(final boolean val) {
    printDetailsOnFail = val;
  }

  public boolean getPrintDetailsOnFail() {
    return printDetailsOnFail;
  }

  public void setPrintRequest(final boolean val) {
    printRequest = val;
  }

  public boolean getPrintRequest() {
    return printRequest;
  }

  public void setPrintResponse(final boolean val) {
    printResponse = val;
  }

  public boolean getPrintResponse() {
    return printResponse;
  }

  public void setRandom(final boolean val) {
    random = val;
  }

  public boolean getRandom() {
    return random;
  }

  public void setResDir(final String val) {
    resDir = val;
  }

  public String getResDir() {
    return resDir;
  }

  public void setServerInfo(final String val) {
    serverInfo = val;
  }

  public String getServerInfo() {
    return serverInfo;
  }

  public void setSsl(final boolean val) {
    ssl = val;
  }

  public boolean getSsl() {
    return ssl;
  }

  public void setStopOnFail(final boolean val) {
    stopOnFail = val;
  }

  public boolean getStopOnFail() {
    return stopOnFail;
  }

  public void setSubdir(final String val) {
    subdir = val;
  }

  public String getSubdir() {
    return subdir;
  }

  public void setTests(final List<String> val) {
    tests = val;
  }

  public List<String> getTests() {
    return tests;
  }

  public void setTestsets(final Map<String, List<String>> val) {
    testsets = val;
  }

  public Map<String, List<String>> getTestsets() {
    return testsets;
  }

  public void setTestsDir(final String val) {
    testsDir = val;
  }

  public String getTestsDir() {
    return testsDir;
  }

  public void setWaitCountDump(final boolean val) {
    waitCountDump = val;
  }

  public boolean getWaitCountDump() {
    return waitCountDump;
  }

  public String toString() {
    final ToString ts = new ToString(this);

    return ts.append("all", getAll())
             .append("baseDir", getBasedir())
             .append("dtds", getDtds())
             .append("excludes", getExcludes())
             .append("httptrace", getHttptrace())
             .append("outputName", getOutputName())
             .append("posttest", getPosttest())
             .append("pretest", getPretest())
             .append("printDetailsOnFail", getPrintDetailsOnFail())
             .append("printRequest", getPrintRequest())
             .append("printResponse", getPrintResponse())
             .append("printResponse", getPrintResponse())
             .append("random", getRandom())
             .append("resDir", getResDir())
             .append("serverInfo", getServerInfo())
             .append("ssl", getSsl())
             .append("stopOnFail", getStopOnFail())
             .append("subdir", getSubdir())
             .append("tests", getTests())
             .append("testsets", getTestsets())
             .append("testsDir", getTestsDir())
             .append("waitCountDump", getWaitCountDump())
             .toString();
  }
}
