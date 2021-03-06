package org.apache.solr.handler.admin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.EnumSet;
import org.apache.solr.core.SolrCore;
import org.apache.sentry.core.model.search.SearchModelAction;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.SecureRequestHandlerUtil;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.core.CoreContainer;

/**
 * Secure (sentry-aware) version of CoreAdminHandler
 */
public class SecureCoreAdminHandler extends CoreAdminHandler {

  public SecureCoreAdminHandler() {
    super();
  }

  public SecureCoreAdminHandler(final CoreContainer coreContainer) {
     super(coreContainer);
  }

  private String getCollectionFromCoreName(String coreName) {
    SolrCore solrCore = null;
    try {
      if (coreName != null && !coreName.equals("")) {
        solrCore = coreContainer.getCore(coreName);
        if (solrCore != null) {
          return solrCore.getCoreDescriptor().getCloudDescriptor().getCollectionName();
        }
      }
    } finally {
      if (solrCore != null) {
        solrCore.close();
      }
    }
    return null;
  }

  @Override
  public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
    SolrParams params = req.getParams();
    CoreAdminAction action = CoreAdminAction.STATUS;
    String a = params.get(CoreAdminParams.ACTION);
    if (a != null) {
      action = CoreAdminAction.get(a);
      if (action == null) {
        // some custom action -- let's reqiure QUERY and UPDATE
        SecureRequestHandlerUtil.checkSentryAdmin(req, SecureRequestHandlerUtil.QUERY_AND_UPDATE, true, null);
      }
    }
    String collection = null;
    boolean checkCollection = true;
    if (action != null) {
      switch (action) {
        case RENAME:
        case UNLOAD:
        case RELOAD:
        case SWAP:
        case MERGEINDEXES:
        case SPLIT:
        case PREPRECOVERY:
        case REQUESTRECOVERY:
        case REQUESTSYNCSHARD: {
          String cname = params.get(CoreAdminParams.CORE,"");
          collection = getCollectionFromCoreName(cname);
          break;
        }
        case REQUESTAPPLYUPDATES: {
          String cname = params.get(CoreAdminParams.NAME, "");
          collection = getCollectionFromCoreName(cname);
          break;
        }
        case CREATE: {
          collection = params.get(CoreAdminParams.COLLECTION);
          break;
        }
        case STATUS:
          // CORE is an optional param for STATUS, but since the
          // non-parameterized version returns all the core info, it doesn't
          // make sense to check for core permissions.
        case PERSIST:
        case CREATEALIAS:
        case DELETEALIAS:
        case LOAD_ON_STARTUP:
        case TRANSIENT:
        default: {
          // these are actions that are not core related or not actually
          // handled by the CoreAdminHandler
          checkCollection = false;
          break;
        }
      }

      switch (action) {
        case STATUS: {
          SecureRequestHandlerUtil.checkSentryAdmin(req, SecureRequestHandlerUtil.QUERY_ONLY, checkCollection, collection);
          break;
        }
        case LOAD:
        case UNLOAD:
        case RELOAD:
        case CREATE:
        case PERSIST:
        case SWAP:
        case RENAME:
        case MERGEINDEXES:
        case SPLIT:
        case PREPRECOVERY:
        case REQUESTRECOVERY:
        case REQUESTSYNCSHARD:
        case REQUESTAPPLYUPDATES:
        // these next few aren't handled by the CoreAdminHandler currently,
        // but let's check them just in case something changes
        case CREATEALIAS:
        case DELETEALIAS:
        case LOAD_ON_STARTUP:
        case REQUESTBUFFERUPDATES:
        case OVERSEEROP:
        case REQUESTSTATUS:
        case TRANSIENT: {
          SecureRequestHandlerUtil.checkSentryAdmin(req, SecureRequestHandlerUtil.UPDATE_ONLY, checkCollection, collection);
          break;
        }
        default: {
          // some custom action -- let's reqiure QUERY and UPDATE
          SecureRequestHandlerUtil.checkSentryAdmin(req, SecureRequestHandlerUtil.QUERY_AND_UPDATE, checkCollection, collection);
          break;
        }
      }
    }
    super.handleRequestBody(req, rsp);
  }
}
