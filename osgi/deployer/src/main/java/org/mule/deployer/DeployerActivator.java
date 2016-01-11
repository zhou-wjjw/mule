/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.deployer;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.deployer.api.DeploymentService;
import org.mule.deployer.api.MuleFoldersUtil;
import org.mule.deployer.extension.DefaultMuleCoreExtensionManager;
import org.mule.deployer.extension.MuleCoreExtensionManager;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.subsystem.Subsystem;

public class DeployerActivator implements BundleActivator
{

    private ServiceRegistration<DeploymentService> registeredDeploymentService;
    private DeploymentService deploymentService;

    @Override
    public void start(BundleContext bundleContext) throws Exception
    {
        //TODO(pablo.kraan): OSGi - need a feature for the deployer in order to include it when runnging the container and not when runnignt he functinal tests
        bundleContext.addFrameworkListener(new FrameworkListener()
        {

            private DeploymentDirectoryWatcher deploymentDirectoryWatcher;

            @Override
            public void frameworkEvent(FrameworkEvent event)
            {
                if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED)
                {
                    //TODO(pablo.kraan): OSGi - need a system property for this value
                    //int defStartLevel = Integer.parseInt(System.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
                    int defStartLevel = 0;
                    int startLevel = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class).getStartLevel();
                    if (startLevel >= defStartLevel)
                    {

                        try
                        {

                            list(bundleContext);


                            final Subsystem rootSubsystem = getSubsystem(0, bundleContext);

                            createExecutionMuleFolder();

                            //ServiceReference<RegionDigraph> serviceReference = bundleContext.getServiceReference(RegionDigraph.class);
                            //
                            //if (serviceReference == null)
                            //{
                            //    throw new IllegalStateException("Unable to obtain a RegionDigraph service");
                            //}
                            //final RegionDigraph regions = bundleContext.getService(serviceReference);
                            //
                            //for (Region region : regions.getRegions())
                            //{   StringBuilder builder = new StringBuilder("Region: " + region.getName() + " contains { ");
                            //    for (Long id : region.getBundleIds())
                            //    {
                            //        builder.append(bundleContext.getBundle(id).getSymbolicName());
                            //        builder.append("\n");
                            //    }
                            //    builder.append("}");
                            //
                            //    System.out.println(builder.toString());
                            //}

                            //ArtifactDeployer<Application> applicationMuleDeployer = new DefaultArtifactDeployer<>();
                            //
                            //DefaultApplicationFactory applicationFactory = new DefaultApplicationFactory();
                            //
                            //DefaultArchiveDeployer<Application> applicationDeployer = new DefaultArchiveDeployer<>(applicationMuleDeployer, applicationFactory, applications, deploymentLock, NOP_ARTIFACT_DEPLOYMENT_TEMPLATE);
                            //
                            //deploymentDirectoryWatcher = new DeploymentDirectoryWatcher(applicationDeployer, new ObservableList<Application>(), new DebuggableReentrantLock());
                            //deploymentDirectoryWatcher.start();
                            final CompositeDeploymentListener deploymentListener = new CompositeDeploymentListener();
                            OsgiDeploymentListenerManager.create(bundleContext, deploymentListener);
                            deploymentService = new MuleDeploymentService(bundleContext, deploymentListener, rootSubsystem);

                            Dictionary<String, String> serviceProperties = new Hashtable<>();
                            registeredDeploymentService = bundleContext.registerService(DeploymentService.class, deploymentService, serviceProperties);

                            MuleCoreExtensionManager coreExtensionManager = new DefaultMuleCoreExtensionManager(bundleContext);

                            coreExtensionManager.initialise();
                            coreExtensionManager.start();
                        }
                        catch (Exception e)
                        {
                            throw new IllegalStateException("Unable to start core extension manager", e);
                        }

                        deploymentService.start();
                    }
                }
            }
        });
    }

    public void list(BundleContext bundleContext) throws InvalidSyntaxException
    {
        Map<Long, String> subsystems = new TreeMap<Long, String>();

        for (ServiceReference<Subsystem> ref : bundleContext.getServiceReferences(Subsystem.class, null)) {
            Subsystem s = bundleContext.getService(ref);
            if (s != null) {
                subsystems.put(s.getSubsystemId(),
                               String.format("%d\t%s\t%s %s", s.getSubsystemId(), s.getState(), s.getSymbolicName(), s.getVersion()));
            }
        }

        StringBuilder builder = new StringBuilder("Subsystems:");
        for (String entry : subsystems.values()) {
            builder.append("\n").append(entry);
        }
        System.out.println(builder.toString());
    }

    private Subsystem getSubsystem(long id, BundleContext bundleContext) {
        try {
            for (ServiceReference<Subsystem> ref :
                    bundleContext.getServiceReferences(Subsystem.class, "(subsystem.id=" + id + ")")) {
                Subsystem svc = bundleContext.getService(ref);
                if (svc != null)
                    return svc;
            }
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Unable to find subsystem " + id);
    }

    private void createExecutionMuleFolder()
    {
        File executionFolder = MuleFoldersUtil.getExecutionFolder();
        if (!executionFolder.exists())
        {
            if (!executionFolder.mkdirs())
            {
                throw new MuleRuntimeException(MessageFactory.createStaticMessage(
                        String.format("Could not create folder '%s', validate that the process has permissions over that directory", executionFolder.getAbsolutePath())));
            }
        }
    }

    //protected Region createMuleRegion(BundleContext bundleContext, RegionDigraph regions) throws BundleException
    //{
    //    Region root = regions.createRegion("mule");
    //    for (Bundle bundle : bundleContext.getBundles())
    //    {
    //        root.addBundle(bundle);
    //    }
    //    return root;
    //}

    @Override
    public void stop(BundleContext context) throws Exception
    {
        if (registeredDeploymentService != null)
        {
            registeredDeploymentService.unregister();
        }

        if (deploymentService != null)
        {
            deploymentService.stop();
        }
    }
}
