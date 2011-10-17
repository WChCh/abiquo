/**
 * Abiquo community edition
 * cloud management application for hybrid clouds
 * Copyright (C) 2008-2010 - Abiquo Holdings S.L.
 *
 * This application is free software; you can redistribute it and/or
 * modify it under the terms of the GNU LESSER GENERAL PUBLIC
 * LICENSE as published by the Free Software Foundation under
 * version 3 of the License
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * LESSER GENERAL PUBLIC LICENSE v.3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package com.abiquo.abiserver.listener;

import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.abicloud.taskservice.TaskService;
import com.abiquo.abicloud.taskservice.exception.TaskServiceException;
import com.abiquo.abicloud.taskservice.factory.TaskServiceFactory;
import com.abiquo.abiserver.eventing.AMSink;
import com.abiquo.abiserver.eventing.VSMListener;
import com.abiquo.commons.amqp.impl.am.AMCallback;
import com.abiquo.commons.amqp.impl.am.AMConsumer;
import com.abiquo.commons.amqp.impl.vsm.VSMCallback;
import com.abiquo.commons.amqp.impl.vsm.VSMConfiguration;
import com.abiquo.commons.amqp.impl.vsm.VSMConsumer;
import com.abiquo.tracer.client.TracerFactory;
import com.abiquo.tracer.server.LoggingTracerProcessor;
import com.abiquo.tracer.server.TracerCollector;
import com.abiquo.tracer.server.TracerCollectorFactory;
import com.abiquo.tracer.server.TracerProcessor;

/**
 * Initializes configuration and loads the scheduled tasks.
 */
public class ContextListener implements ServletContextListener
{
    /** The logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextListener.class);

    /** The task service to run scheduled tasks. */
    private TaskService taskService;

    /** The RabbitMQ consumer for VSM **/
    protected VSMConsumer consumer;

    /** The RabbitMQ consumer for AM **/
    protected AMConsumer amconsumer;

    @Override
    public void contextInitialized(final ServletContextEvent sce)
    {
        try
        {
            String contextName = sce.getServletContext().getServletContextName();
            LOGGER.info("Initializing the context [" + contextName + "] ...");

            initializeTracer();
            initializeTaskService();
            initializeVSMListener();
            initializeAMListener();

            LOGGER.info("The context [" + contextName + "] has been initialized");
        }
        catch (Exception ex)
        {
            LOGGER.error("An error occurred while initializing the context", ex);
        }
    }

    /**
     * Creates an instance of {@link VSMConsumer}, add all the needed listeners {@link VSMCallback}
     * and starts the consuming.
     * 
     * @throws IOException When there is some network error.
     */
    protected void initializeVSMListener() throws IOException
    {
        consumer = new VSMConsumer(VSMConfiguration.EVENT_SYNK_QUEUE);
        consumer.addCallback(new VSMListener());
        consumer.start();
    }

    /**
     * Creates an instance of {@link AMConsumer}, add all the needed listeners {@link AMCallback}
     * and starts the consuming.
     * 
     * @throws IOException When there is some network error.
     */
    protected void initializeAMListener() throws IOException
    {
        amconsumer = new AMConsumer();
        amconsumer.addCallback(new AMSink());
        amconsumer.start();
    }

    /**
     * Stops the {@link VSMConsumer}.
     * 
     * @throws IOException When there is some network error.
     */
    private void shutdownVSMListener() throws IOException
    {
        consumer.stop();
    }

    /**
     * Stops the {@link AMConsumer}.
     * 
     * @throws IOException When there is some network error.
     */
    private void shutdownAMListener() throws IOException
    {
        amconsumer.stop();
    }

    /**
     * Registers the {@link TracerCollector} and all the default {@link TracerProcessor}.
     */
    private void initializeTracer()
    {
        try
        {
            TracerCollector t = TracerCollectorFactory.getTracerCollector();
            t.addListener(new LoggingTracerProcessor());
            // t.addListener(new MailingTracerProcessor());
            t.init();
        }
        catch (Exception ex)
        {
            LOGGER.error("Could not start TracerCollector", ex);
        }
    }

    /**
     * Initializes the {@link #taskService} and schedules all defined tasks.
     */
    private void initializeTaskService()
    {
        try
        {
            taskService = TaskServiceFactory.getService();
            taskService.scheduleAll();
        }
        catch (TaskServiceException ex)
        {
            LOGGER.error("Could not initialize TaskService", ex);
        }
    }

    /**
     * Destroys the TracerCollector.
     */
    private void destroyTracer()
    {
        try
        {
            TracerFactory.getTracer().destroy();
        }
        catch (Exception ex)
        {
            LOGGER.error("Could not destroy TracerClient", ex);
        }

        try
        {
            TracerCollector t = TracerCollectorFactory.getTracerCollector();
            t.destroy();
        }
        catch (Exception ex)
        {
            LOGGER.error("Could not destroy TracerCollector", ex);
        }
    }

    /**
     * Shutdown the task service and unschedules all tasks.
     */
    private void shutdownTaskService()
    {
        try
        {
            taskService.shutdown();
        }
        catch (TaskServiceException ex)
        {
            LOGGER.error("Could not shutdown TaskService", ex);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce)
    {
        String contextName = sce.getServletContext().getServletContextName();
        LOGGER.info("Destroying the context  [" + contextName + "] ... ");

        try
        {
            shutdownVSMListener();
            shutdownAMListener();
        }
        catch (IOException e)
        {
            LOGGER.error("An exception occurred while shutting down the VSMConsumer. " + e);
        }

        destroyTracer();
        shutdownTaskService();

        LOGGER.info("The context [" + contextName + "] has been destroyed");
    }

}
