/*
# Copyright (c) 2006-2016 Apple Inc. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package org.bedework.davtester;

// XML Elements/Attributes

import javax.xml.namespace.QName;

@SuppressWarnings("unused")
public interface XmlDefs {
  QName ELEMENT_ADDRESSDATAFILTER = new QName("addressdatafilter");
  QName ELEMENT_ARG = new QName("arg");
  QName ELEMENT_AUTHTYPE = new QName("authtype");
  QName ELEMENT_BODY = new QName("body");
  QName ELEMENT_CALDAVTEST = new QName("caldavtest");
  QName ELEMENT_CALENDARDATAFILTER = new QName("calendardatafilter");
  QName ELEMENT_CALLBACK = new QName("callback");
  QName ELEMENT_CERTDIR = new QName("certdir");
  QName ELEMENT_CLIENTS = new QName("clients");
  QName ELEMENT_CONTENTTYPE = new QName("content-type");
  QName ELEMENT_DATA = new QName("data");
  QName ELEMENT_DEFAULTFILTERS = new QName("default-filters");
  QName ELEMENT_DEFAULTFILTERSAPPLIED = new QName("default-filters-applied");
  QName ELEMENT_DESCRIPTION = new QName("description");
  QName ELEMENT_END = new QName("end");
  QName ELEMENT_EXCLUDE_FEATURE = new QName("exclude-feature");
  QName ELEMENT_FEATURES = new QName("features");
  QName ELEMENT_FEATURE = new QName("feature");
  QName ELEMENT_FILEPATH = new QName("filepath");
  QName ELEMENT_GENERATOR = new QName("generator");
  QName ELEMENT_GRABCALPROP = new QName("grabcalproperty");
  QName ELEMENT_GRABCALPARAM = new QName("grabcalparameter");
  QName ELEMENT_GRABCOUNT = new QName("grabcount");
  QName ELEMENT_GRABELEMENT = new QName("grabelement");
  QName ELEMENT_GRABHEADER = new QName("grabheader");
  QName ELEMENT_GRABJSON = new QName("grabjson");
  QName ELEMENT_GRABPROPERTY = new QName("grabproperty");
  QName ELEMENT_GRABURI = new QName("graburi");
  QName ELEMENT_HEADER = new QName("header");
  QName ELEMENT_HOST = new QName("host");
  //QName ELEMENT_HOST2 = new QName("host2");
  QName ELEMENT_KEY = new QName("key");
  QName ELEMENT_LOGGING = new QName("logging");
  QName ELEMENT_MAILFROM = new QName("mailfrom");
  QName ELEMENT_MAILTO = new QName("mailto");
  QName ELEMENT_METHOD = new QName("method");
  QName ELEMENT_NAME = new QName("name");
  QName ELEMENT_NONSSLPORT = new QName("nonsslport");
  QName ELEMENT_NONSSLPORT2 = new QName("nonsslport2");
  QName ELEMENT_NOTIFY = new QName("notify");
  QName ELEMENT_PARENT = new QName("parent");
  QName ELEMENT_PAUSE = new QName("pause");
  QName ELEMENT_PERIOD = new QName("period");
  QName ELEMENT_POINTER = new QName("pointer");
  QName ELEMENT_PROPERTY = new QName("property");
  QName ELEMENT_REPEAT = new QName("repeat");
  QName ELEMENT_REQUEST = new QName("request");
  QName ELEMENT_REQUIRE_FEATURE = new QName("require-feature");
  QName ELEMENT_RUNS = new QName("runs");
  QName ELEMENT_RURI = new QName("ruri");
  QName ELEMENT_SERVERINFO = new QName("serverinfo");
  QName ELEMENT_SPREAD = new QName("spread");
  QName ELEMENT_SSLPORT = new QName("sslport");
  QName ELEMENT_SSLPORT2 = new QName("sslport2");
  QName ELEMENT_START = new QName("start");
  QName ELEMENT_SUBJECT = new QName("subject");
  QName ELEMENT_SUBSTITUTE = new QName("substitute");
  QName ELEMENT_SUBSTITUTIONS = new QName("substitutions");
  QName ELEMENT_SUBSTITUTION = new QName("substitution");
  QName ELEMENT_TEST = new QName("test");
  QName ELEMENT_TESTINFO = new QName("testinfo");
  QName ELEMENT_TESTS = new QName("tests");
  QName ELEMENT_TESTSUITE = new QName("test-suite");
  QName ELEMENT_TIMEOUT = new QName("timeout");
  QName ELEMENT_THREADS = new QName("threads");
  QName ELEMENT_UNIX = new QName("unix");
  QName ELEMENT_UNIX2 = new QName("unix2");
  QName ELEMENT_VALUE = new QName("value");
  QName ELEMENT_VARIABLE = new QName("variable");
  QName ELEMENT_VERIFY = new QName("verify");
  QName ELEMENT_WAITCOUNT = new QName("waitcount");
  QName ELEMENT_WAITDELAY = new QName("waitdelay");
  QName ELEMENT_WAITSUCCESS = new QName("waitsuccess");
  QName ELEMENT_WARNINGTIME = new QName("warningtime");

  //String ATTR_HOST2 = "host2";
  String ATTR_AUTH = "auth";
  String ATTR_CERT = "cert";
  String ATTR_CHANGE_UID = "change-uid";
  String ATTR_COUNT = "count";
  String ATTR_DETAILS = "details";
  String ATTR_ENABLE = "enable";
  String ATTR_END_DELETE = "end-delete";
  String ATTR_GENERATE = "generate";
  String ATTR_HTTP_TRACE = "http-trace";
  String ATTR_IGNORE = "ignore";
  String ATTR_INTERVAL = "interval";
  String ATTR_ITERATE_DATA = "iterate-data";
  String ATTR_NAME = "name";
  String ATTR_ONLY = "only";
  String ATTR_PRINT_REQUEST = "print-request";
  String ATTR_PRINT_RESPONSE = "print-response";
  String ATTR_PSWD = "pswd";
  String ATTR_QUOTE = "quote";
  String ATTR_REQUEST_FAILED = "request-failed";
  String ATTR_SKIP_SUITE_ON_FAIL = "skip-suite-on-fail";
  String ATTR_STATS = "stats";
  String ATTR_SUBSTITUTIONS = "substitutions";
  String ATTR_TIME_EXCEEDED = "time-exceeded";
  String ATTR_USER = "user";
  String ATTR_WAIT_FOR_SUCCESS = "wait-for-success";

  String ATTR_VALUE_NO = "no";
  String ATTR_VALUE_RANDOM = "random";
  String ATTR_VALUE_YES = "yes";
}
