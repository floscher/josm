package org.apache.commons.jcs.utils.threadpool;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jcs.utils.config.PropertySetter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This manages threadpools for an application
 * <p>
 * It is a singleton since threads need to be managed vm wide.
 * <p>
 * This manager forces you to use a bounded queue. By default it uses the current thread for
 * execution when the buffer is full and no free threads can be created.
 * <p>
 * You can specify the props file to use or pass in a properties object prior to configuration. By
 * default it looks for configuration information in thread_pool.properties.
 * <p>
 * If set, the Properties object will take precedence.
 * <p>
 * If a value is not set for a particular pool, the hard coded defaults will be used.
 *
 * <pre>
 * int boundarySize_DEFAULT = 2000;
 *
 * int maximumPoolSize_DEFAULT = 150;
 *
 * int minimumPoolSize_DEFAULT = number of processors as reported by the JVM;
 *
 * int keepAliveTime_DEFAULT = 1000 * 60 * 5;
 *
 * boolean abortWhenBlocked = false;
 *
 * String whenBlockedPolicy_DEFAULT = IPoolConfiguration.POLICY_RUN;
 *
 * int startUpSize_DEFAULT = 4;
 * </pre>
 *
 * You can configure default settings by specifying a default pool in the properties, ie "cache.ccf"
 * <p>
 * @author Aaron Smuts
 */
public class ThreadPoolManager
{
    /** The logger */
    private static final Log log = LogFactory.getLog( ThreadPoolManager.class );

    /** The default config, created using property defaults if present, else those above. */
    private PoolConfiguration defaultConfig;

    /** the root property name */
    private static final String PROP_NAME_ROOT = "thread_pool";

    /** default property file name */
    private static final String DEFAULT_PROP_NAME_ROOT = "thread_pool.default";

    /**
     * You can specify the properties to be used to configure the thread pool. Setting this post
     * initialization will have no effect.
     */
    private static volatile Properties props = null;

    /** singleton instance */
    private static ThreadPoolManager INSTANCE = null;

    /** Map of names to pools. */
    private ConcurrentHashMap<String, ThreadPoolExecutor> pools;

    /**
     * No instances please. This is a singleton.
     */
    private ThreadPoolManager()
    {
        this.pools = new ConcurrentHashMap<String, ThreadPoolExecutor>();
        configure();
    }

    /**
     * Creates a pool based on the configuration info.
     * <p>
     * @param config
     * @return A ThreadPoll wrapper
     */
    private ThreadPoolExecutor createPool( PoolConfiguration config )
    {
        BlockingQueue<Runnable> queue = null;
        if ( config.isUseBoundary() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Creating a Bounded Buffer to use for the pool" );
            }

            queue = new LinkedBlockingQueue<Runnable>(config.getBoundarySize());
        }
        else
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Creating a non bounded Linked Queue to use for the pool" );
            }
            queue = new LinkedBlockingQueue<Runnable>();
        }

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
            config.getStartUpSize(),
            config.getMaximumPoolSize(),
            config.getKeepAliveTime(),
            TimeUnit.MILLISECONDS,
            queue,
            new DaemonThreadFactory("JCS-ThreadPoolManager-"));

        // when blocked policy
        switch (config.getWhenBlockedPolicy())
        {
            case ABORT:
                pool.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
                break;

            case RUN:
                pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
                break;

            case WAIT:
                throw new RuntimeException("POLICY_WAIT no longer supported");

            case DISCARDOLDEST:
                pool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
                break;

            default:
                break;
        }

        pool.prestartAllCoreThreads();

        return pool;
    }

    /**
     * Returns a configured instance of the ThreadPoolManger To specify a configuration file or
     * Properties object to use call the appropriate setter prior to calling getInstance.
     * <p>
     * @return The single instance of the ThreadPoolManager
     */
    public static synchronized ThreadPoolManager getInstance()
    {
        if ( INSTANCE == null )
        {
            INSTANCE = new ThreadPoolManager();
        }
        return INSTANCE;
    }

    /**
     * Dispose of the instance of the ThreadPoolManger and shut down all thread pools
     */
    public static synchronized void dispose()
    {
        if ( INSTANCE != null )
        {
            for ( String poolName : INSTANCE.getPoolNames())
            {
                try
                {
                    INSTANCE.getPool(poolName).shutdownNow();
                }
                catch (Throwable t)
                {
                    log.warn("Failed to close pool " + poolName, t);
                }
            }

            INSTANCE = null;
        }
    }

    /**
     * Returns a pool by name. If a pool by this name does not exist in the configuration file or
     * properties, one will be created using the default values.
     * <p>
     * Pools are lazily created.
     * <p>
     * @param name
     * @return The thread pool configured for the name.
     */
    public ThreadPoolExecutor getPool( String name )
    {
        ThreadPoolExecutor pool = pools.get( name );

        if ( pool == null )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Creating pool for name [" + name + "]" );
            }

            PoolConfiguration config = loadConfig( PROP_NAME_ROOT + "." + name );
            pool = createPool( config );
            ThreadPoolExecutor _pool = pools.putIfAbsent( name, pool );
            if (_pool != null)
            {
                pool = _pool;
            }

            if ( log.isDebugEnabled() )
            {
                log.debug( "PoolName = " + getPoolNames() );
            }
        }

        return pool;
    }

    /**
     * Returns the names of all configured pools.
     * <p>
     * @return ArrayList of string names
     */
    public ArrayList<String> getPoolNames()
    {
        return new ArrayList<String>(pools.keySet());
    }

    /**
     * This will be used if it is not null on initialization. Setting this post initialization will
     * have no effect.
     * <p>
     * @param props The props to set.
     */
    public static void setProps( Properties props )
    {
        ThreadPoolManager.props = props;
    }

    /**
     * Initialize the ThreadPoolManager and create all the pools defined in the configuration.
     */
    private void configure()
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Initializing ThreadPoolManager" );
        }

        if ( props == null )
        {
            log.warn( "No configuration settings found.  Using hardcoded default values for all pools." );
            props = new Properties();
        }

        // set intial default and then override if new
        // settings are available
        defaultConfig = new PoolConfiguration();
        defaultConfig = loadConfig( DEFAULT_PROP_NAME_ROOT );
    }

    /**
     * Configures the default PoolConfiguration settings.
     * <p>
     * @param root
     * @return PoolConfiguration
     */
    private PoolConfiguration loadConfig( String root )
    {
        PoolConfiguration config = defaultConfig.clone();
        PropertySetter.setProperties( config, props, root + "." );

        if ( log.isDebugEnabled() )
        {
            log.debug( root + " PoolConfiguration = " + config );
        }

        return config;
    }
}
