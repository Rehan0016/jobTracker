package com.rehan.jobtracker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DataSourceBeanPostProcessorIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StartupDiagnostics startupDiagnostics;

    @Test
    @DisplayName("Verify StartupDiagnostics is registered with BeanFactory as BeanPostProcessor")
    void shouldBeRegisteredAsBeanPostProcessor() {
        ConfigurableListableBeanFactory beanFactory =
                ((ConfigurableApplicationContext) applicationContext).getBeanFactory();

        assertTrue(beanFactory instanceof AbstractBeanFactory, "BeanFactory should be AbstractBeanFactory");
        boolean isRegistered = ((AbstractBeanFactory) beanFactory).getBeanPostProcessors().stream()
                .anyMatch(bpp -> bpp instanceof StartupDiagnostics);

        assertTrue(isRegistered, "StartupDiagnostics must be registered in BeanFactory's BeanPostProcessors list");
    }

    @Test
    @DisplayName("Verify postProcessBeforeInitialization() executed on the actual DataSource bean")
    void shouldExecuteOnActualDataSourceBean() {
        assertNotNull(dataSource, "DataSource bean must be present in ApplicationContext");
        assertTrue(startupDiagnostics.isDataSourceProcessed(),
                "postProcessBeforeInitialization() must have executed for the actual DataSource bean during startup");
    }

    @Test
    @DisplayName("Verify exactly one instance of StartupDiagnostics exists in ApplicationContext")
    void shouldHaveSingleInstance() {
        String[] beanNames = applicationContext.getBeanNamesForType(StartupDiagnostics.class);
        assertEquals(1, beanNames.length, "Exactly one StartupDiagnostics bean should exist in ApplicationContext");
    }
}
