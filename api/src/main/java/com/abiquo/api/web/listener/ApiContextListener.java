/**
 * 
 */
package com.abiquo.api.web.listener;

import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.api.eventing.SQLTracerListener;
import com.abiquo.commons.amqp.impl.tracer.TracerCallback;
import com.abiquo.commons.amqp.impl.tracer.TracerConsumer;

/**
 * Initializes configuration and loads the scheduled tasks.
 */
public class ApiContextListener implements ServletContextListener
{

    /** The logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiContextListener.class);

    /** The RabbitMQ consumer for Tracer **/
    protected TracerConsumer tracerConsumer;

    @Override
    public void contextDestroyed(final ServletContextEvent sce)
    {
        String contextName = sce.getServletContext().getServletContextName();
        LOGGER.info("Destroying the context  [" + contextName + "] ... ");

        try
        {
            shutdownTracerListener();
        }
        catch (IOException e)
        {
            LOGGER.error("An exception occurred while shutting down the VSMConsumer. " + e);
        }

        LOGGER.info("The context [" + contextName + "] has been destroyed");
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce)
    {
        try
        {
            String contextName = sce.getServletContext().getServletContextName();
            LOGGER.info("Initializing the context [" + contextName + "] ...");

            initializeTracerListener();

            LOGGER.info("The context [" + contextName + "] has been initialized");
        }
        catch (Exception ex)
        {
            LOGGER.error("An error occurred while initializing the context", ex);
        }
    }

    /**
     * Creates an instance of {@link TracerConsumer}, add all the needed listeners
     * {@link TracerCallback} and starts the consuming.
     * 
     * @throws IOException When there is some network error.
     */
    protected void initializeTracerListener() throws IOException
    {
        LOGGER.info("Initializing the tracer listener...");
        tracerConsumer = new TracerConsumer();
        tracerConsumer.addCallback(new SQLTracerListener());
        tracerConsumer.start();
    }

    /**
     * Stops the {@link TracerConsumer}.
     * 
     * @throws IOException When there is some network error.
     */
    protected void shutdownTracerListener() throws IOException
    {
        tracerConsumer.stop();
    }
}
