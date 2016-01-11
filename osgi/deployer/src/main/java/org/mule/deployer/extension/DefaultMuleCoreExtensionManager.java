/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.deployer.extension;

import org.mule.MuleCoreExtension;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.deployer.api.BundleContextAware;
import org.mule.deployer.api.DeploymentService;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

public class DefaultMuleCoreExtensionManager implements MuleCoreExtensionManager
{

    protected static final Log logger = LogFactory.getLog(DefaultMuleCoreExtensionManager.class);

    private final BundleContext bundleContext;
    private final MuleCoreExtensionDiscoverer coreExtensionDiscoverer;
    private final MuleCoreExtensionDependencyResolver coreExtensionDependencyResolver;
    private List<MuleCoreExtension> coreExtensions = new LinkedList<>();
    private DeploymentService deploymentService;
    private List<MuleCoreExtension> orderedCoreExtensions;
    //private PluginClassLoaderManager pluginClassLoaderManager;


    public DefaultMuleCoreExtensionManager(BundleContext bundleContext)
    {
        this(bundleContext, new OsgiCoreExtensionDiscoverer(bundleContext), new ReflectionMuleCoreExtensionDependencyResolver());
    }

    public DefaultMuleCoreExtensionManager(BundleContext bundleContext, MuleCoreExtensionDiscoverer coreExtensionDiscoverer, MuleCoreExtensionDependencyResolver coreExtensionDependencyResolver)
    {
        this.bundleContext = bundleContext;
        this.coreExtensionDiscoverer = coreExtensionDiscoverer;
        this.coreExtensionDependencyResolver = coreExtensionDependencyResolver;
    }

    @Override
    public void dispose()
    {
        for (MuleCoreExtension extension : coreExtensions)
        {
            try
            {
                extension.dispose();
            }
            catch (Exception ex)
            {
                logger.fatal("Error disposing core extension " + extension.getName(), ex);
            }
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            coreExtensions = coreExtensionDiscoverer.discover();

            orderedCoreExtensions = coreExtensionDependencyResolver.resolveDependencies(coreExtensions);

            initializeCoreExtensions();

        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    public void start() throws MuleException
    {
        logger.info("Starting core extensions");
        for (MuleCoreExtension extension : orderedCoreExtensions)
        {
            extension.start();
        }
    }

    @Override
    public void stop() throws MuleException
    {
        if (orderedCoreExtensions == null)
        {
            return;
        }

        for (int i = orderedCoreExtensions.size() - 1; i >= 0; i--)
        {
            MuleCoreExtension extension = orderedCoreExtensions.get(i);

            try
            {
                extension.stop();
            }
            catch (MuleException e)
            {
                logger.warn("Error stopping core extension: " + extension.getName(), e);
            }
        }
    }

    private void initializeCoreExtensions() throws InitialisationException, DefaultMuleException
    {
        logger.info("Initializing core extensions");

        for (MuleCoreExtension extension : orderedCoreExtensions)
        {
            if (extension instanceof BundleContextAware)
            {
                ((BundleContextAware) extension).setBundleContext(bundleContext);
            }
            //TODO(pablo.kraan): OSGi - add core extensions dependency injection
            //if (extension instanceof DeploymentServiceAware)
            //{
            //    ((DeploymentServiceAware) extension).setDeploymentService(deploymentService);
            //}
            //
            //if (extension instanceof DeploymentListener)
            //{
            //    deploymentService.addDeploymentListener((DeploymentListener) extension);
            //}
            //
            //if (extension instanceof PluginClassLoaderManagerAware)
            //{
            //    ((PluginClassLoaderManagerAware) extension).setPluginClassLoaderManager(pluginClassLoaderManager);
            //}
            //
            //if (extension instanceof CoreExtensionsAware)
            //{
            //    ((CoreExtensionsAware) extension).setCoreExtensions(orderedCoreExtensions);
            //}

            extension.initialise();
        }
    }

    @Override
    public void setDeploymentService(DeploymentService deploymentService)
    {
        this.deploymentService = deploymentService;
    }

    //@Override
    //public void setPluginClassLoaderManager(PluginClassLoaderManager pluginClassLoaderManager)
    //{
    //    this.pluginClassLoaderManager = pluginClassLoaderManager;
    //}
}
