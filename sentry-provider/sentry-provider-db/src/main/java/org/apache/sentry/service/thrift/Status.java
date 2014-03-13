/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sentry.service.thrift;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.annotation.Nullable;

import org.apache.sentry.service.thrift.ServiceConstants.ThriftConstants;

/**
 * Simple factory to make returning TSentryStatus objects easy
 */
public enum Status {
  OK(ThriftConstants.TSENTRY_STATUS_OK),
  ALREADY_EXISTS(ThriftConstants.TSENTRY_STATUS_ALREADY_EXISTS),
  NO_SUCH_OBJECT(ThriftConstants.TSENTRY_STATUS_NO_SUCH_OBJECT),
  RUNTIME_ERROR(ThriftConstants.TSENTRY_STATUS_RUNTIME_ERROR),
  INVALID_INPUT(ThriftConstants.TSENTRY_STATUS_INVALID_INPUT),
  UNKNOWN(-1)
  ;
  private int code;
  private Status(int code) {
    this.code = code;
  }
  public int getCode() {
    return code;
  }
  public static Status fromCode(int code) {
    for (Status status : Status.values()) {
      if (status.getCode() == code) {
        return status;
      }
    }
    return Status.UNKNOWN;
  }
  public static TSentryResponseStatus OK() {
    return Create(Status.OK, "");
  }
  public static TSentryResponseStatus AlreadyExists(String message, Throwable t) {
    return Create(Status.ALREADY_EXISTS, message, t);
  }
  public static TSentryResponseStatus NoSuchObject(String message, Throwable t) {
    return Create(Status.NO_SUCH_OBJECT, message, t);
  }
  public static TSentryResponseStatus RuntimeError(String message, Throwable t) {
    return Create(Status.RUNTIME_ERROR, message, t);
  }
  public static TSentryResponseStatus Create(Status value, String message) {
    return Create(value, message, null);
  }
  public static TSentryResponseStatus InvalidInput(String message, Throwable t) {
    return Create(Status.INVALID_INPUT, message, t);
  }
  public static TSentryResponseStatus Create(Status value, String message, @Nullable Throwable t) {
    TSentryResponseStatus status = new TSentryResponseStatus();
    status.setValue(value.getCode());
    status.setMessage(message);
    if (t != null) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      t.printStackTrace(printWriter);
      printWriter.close();
      status.setStack(stringWriter.toString());
    }
    return status;
  }
}