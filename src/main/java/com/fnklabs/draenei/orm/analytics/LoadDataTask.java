package com.fnklabs.draenei.orm.analytics;

import com.fnklabs.draenei.orm.DataProvider;
import com.hazelcast.config.*;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IMap;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Load data from DataProvider
 */
class LoadDataTask<T> implements Callable<Integer>, Serializable, AnalyticsInstanceAware, HazelcastInstanceAware {
    private final long startToken;
    private final long endToken;
    private final String mapName;
    private final Class<T> entityClass;

    private transient Analytics analytics;
    private transient HazelcastInstance hazelcastInstance;

    public LoadDataTask(long startToken, long endToken, String mapName, Class<T> entityClass) {
        this.startToken = startToken;
        this.endToken = endToken;
        this.mapName = mapName;
        this.entityClass = entityClass;
    }

    @Override
    public Integer call() throws Exception {
        DataProvider<T> dataProvider = analytics.getDataProvider(entityClass);

        IMap<Long, T> map = getMap();

        LoadIntoHazelcastConsumer<T> consumer = new LoadIntoHazelcastConsumer<>(map, dataProvider);

        return dataProvider.load(startToken, endToken, consumer);
    }

    @Override
    public void setAnalyticsInstance(@NotNull Analytics analytics) {
        this.analytics = analytics;
    }

    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    private IMap<Long, T> getMap() {
        Config config = hazelcastInstance.getConfig();

        MapConfig mapConfig = config.getMapConfig(mapName);
        mapConfig.setEvictionPolicy(EvictionPolicy.NONE);
        mapConfig.setInMemoryFormat(InMemoryFormat.OBJECT);
        mapConfig.setMaxIdleSeconds(0);
        mapConfig.setMaxSizeConfig(new MaxSizeConfig());
        mapConfig.setMaxIdleSeconds(0);
        mapConfig.setTimeToLiveSeconds(0);

        config.addMapConfig(mapConfig);

        return hazelcastInstance.<Long, T>getMap(mapName);
    }

}