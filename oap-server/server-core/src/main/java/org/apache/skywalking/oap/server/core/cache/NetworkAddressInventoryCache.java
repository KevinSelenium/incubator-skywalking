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
 *
 */

package org.apache.skywalking.oap.server.core.cache;

import com.google.common.cache.*;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.register.NetworkAddressInventory;
import org.apache.skywalking.oap.server.core.storage.StorageModule;
import org.apache.skywalking.oap.server.core.storage.cache.INetworkAddressInventoryCacheDAO;
import org.apache.skywalking.oap.server.library.module.*;
import org.apache.skywalking.oap.server.library.util.StringUtils;
import org.slf4j.*;

import static java.util.Objects.isNull;

/**
 * @author peng-yongsheng
 */
public class NetworkAddressInventoryCache implements Service {

    private static final Logger logger = LoggerFactory.getLogger(NetworkAddressInventoryCache.class);

    private final Cache<String, Integer> networkAddressCache = CacheBuilder.newBuilder().initialCapacity(1000).maximumSize(5000).build();
    private final Cache<Integer, NetworkAddressInventory> addressIdCache = CacheBuilder.newBuilder().initialCapacity(1000).maximumSize(5000).build();

    private final ModuleManager moduleManager;
    private INetworkAddressInventoryCacheDAO cacheDAO;

    public NetworkAddressInventoryCache(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private INetworkAddressInventoryCacheDAO getCacheDAO() {
        if (isNull(cacheDAO)) {
            this.cacheDAO = moduleManager.find(StorageModule.NAME).getService(INetworkAddressInventoryCacheDAO.class);
        }
        return this.cacheDAO;
    }

    public int getAddressId(String networkAddress) {
        int addressId = Const.NONE;
        try {
            addressId = networkAddressCache.get(NetworkAddressInventory.buildId(networkAddress), () -> getCacheDAO().getAddressId(networkAddress));

            if (addressId == Const.NONE) {
                addressId = getCacheDAO().getAddressId(networkAddress);
                if (addressId != Const.NONE) {
                    networkAddressCache.put(NetworkAddressInventory.buildId(networkAddress), addressId);
                }
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }

        return addressId;
    }

    public NetworkAddressInventory get(int addressId) {
        NetworkAddressInventory networkAddress = null;
        try {
            networkAddress = addressIdCache.get(addressId, () -> getCacheDAO().get(addressId));
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }

        if (isNull(networkAddress)) {
            networkAddress = getCacheDAO().get(addressId);
            if (StringUtils.isNotEmpty(networkAddress)) {
                addressIdCache.put(addressId, networkAddress);
            }
        }
        return networkAddress;
    }
}
