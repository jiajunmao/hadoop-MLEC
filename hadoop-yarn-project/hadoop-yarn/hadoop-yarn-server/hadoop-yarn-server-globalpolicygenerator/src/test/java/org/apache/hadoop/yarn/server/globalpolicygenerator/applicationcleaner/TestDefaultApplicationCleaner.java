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

package org.apache.hadoop.yarn.server.globalpolicygenerator.applicationcleaner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.exceptions.YarnRuntimeException;
import org.apache.hadoop.yarn.server.federation.store.impl.MemoryFederationStateStore;
import org.apache.hadoop.yarn.server.federation.store.records.AddApplicationHomeSubClusterRequest;
import org.apache.hadoop.yarn.server.federation.store.records.ApplicationHomeSubCluster;
import org.apache.hadoop.yarn.server.federation.store.records.GetApplicationsHomeSubClusterRequest;
import org.apache.hadoop.yarn.server.federation.store.records.SubClusterId;
import org.apache.hadoop.yarn.server.federation.utils.FederationStateStoreFacade;
import org.apache.hadoop.yarn.server.globalpolicygenerator.GPGContext;
import org.apache.hadoop.yarn.server.globalpolicygenerator.GPGContextImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for DefaultApplicationCleaner in GPG.
 */
public class TestDefaultApplicationCleaner {
  private Configuration conf;
  private MemoryFederationStateStore stateStore;
  private FederationStateStoreFacade facade;
  private ApplicationCleaner appCleaner;
  private GPGContext gpgContext;

  private List<ApplicationId> appIds;
  // The list of applications returned by mocked router
  private Set<ApplicationId> routerAppIds;

  @Before
  public void setup() throws Exception {
    conf = new YarnConfiguration();

    // No Router query retry
    conf.set(YarnConfiguration.GPG_APPCLEANER_CONTACT_ROUTER_SPEC, "1,1,0");

    stateStore = new MemoryFederationStateStore();
    stateStore.init(conf);

    facade = FederationStateStoreFacade.getInstance();
    facade.reinitialize(stateStore, conf);

    gpgContext = new GPGContextImpl();
    gpgContext.setStateStoreFacade(facade);

    appCleaner = new TestableDefaultApplicationCleaner();
    appCleaner.init(conf, gpgContext);

    routerAppIds = new HashSet<>();

    appIds = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      ApplicationId appId = ApplicationId.newInstance(0, i);
      appIds.add(appId);

      SubClusterId subClusterId =
          SubClusterId.newInstance("SUBCLUSTER-" + i);

      stateStore.addApplicationHomeSubCluster(
          AddApplicationHomeSubClusterRequest.newInstance(
              ApplicationHomeSubCluster.newInstance(appId, subClusterId)));
    }
  }

  @After
  public void breakDown() {
    if (stateStore != null) {
      stateStore.close();
      stateStore = null;
    }
  }

  @Test
  public void testFederationStateStoreAppsCleanUp() throws YarnException {
    // Set first app to be still known by Router
    ApplicationId appId = appIds.get(0);
    routerAppIds.add(appId);

    // Another random app not in stateStore known by Router
    appId = ApplicationId.newInstance(100, 200);
    routerAppIds.add(appId);

    appCleaner.run();

    // Only one app should be left
    Assert.assertEquals(1,
        stateStore
            .getApplicationsHomeSubCluster(
                GetApplicationsHomeSubClusterRequest.newInstance())
            .getAppsHomeSubClusters().size());
  }

  /**
   * Testable version of DefaultApplicationCleaner.
   */
  public class TestableDefaultApplicationCleaner
      extends DefaultApplicationCleaner {
    @Override
    public Set<ApplicationId> getAppsFromRouter() throws YarnRuntimeException {
      return routerAppIds;
    }
  }
}
