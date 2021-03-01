package com.quorum.tessera.data;

import com.quorum.tessera.config.Config;
import com.quorum.tessera.config.ConfigFactory;
import com.quorum.tessera.config.JdbcConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrivacyGroupDAOProviderTest {
    private boolean autocreateTables;

    public PrivacyGroupDAOProviderTest(boolean autocreateTables) {
        this.autocreateTables = autocreateTables;
    }

    @Test
    public void defaultConstructorForCoverage() {
        assertThat(new PrivacyGroupDAOProvider()).isNotNull();
    }

    @Test
    public void provides() {
        try(
            var mockedConfigFactory = mockStatic(ConfigFactory.class);
            var mockedDataSourceFactory = mockStatic(DataSourceFactory.class);
            var mockedPersistence = mockStatic(Persistence.class)
        ) {

            mockedPersistence.when(() -> Persistence.createEntityManagerFactory(anyString(), anyMap())).thenReturn(mock(EntityManagerFactory.class));

            Config config = mock(Config.class);
            JdbcConfig jdbcConfig = mock(JdbcConfig.class);
            when(jdbcConfig.isAutoCreateTables()).thenReturn(autocreateTables);
            when(config.getJdbcConfig()).thenReturn(jdbcConfig);

            ConfigFactory configFactory = mock(ConfigFactory.class);
            when(configFactory.getConfig()).thenReturn(config);

            mockedConfigFactory.when(ConfigFactory::create).thenReturn(configFactory);

            mockedDataSourceFactory
                .when(DataSourceFactory::create).thenReturn(mock(DataSourceFactory.class));


            PrivacyGroupDAO result = PrivacyGroupDAOProvider.provider();
            assertThat(result).isNotNull().isExactlyInstanceOf(PrivacyGroupDAOImpl.class);

            mockedPersistence.verify(() -> Persistence.createEntityManagerFactory(anyString(), anyMap()));
            mockedPersistence.verifyNoMoreInteractions();
            PrivacyGroupDAOProvider.provider();
        }
    }

    @Parameterized.Parameters
    public static Collection<Boolean> autoCreateTables() {
        return List.of(true,false);
    }
}