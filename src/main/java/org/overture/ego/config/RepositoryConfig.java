package org.overture.ego.config;

import org.h2.jdbcx.JdbcConnectionPool;
import org.overture.ego.repository.ApplicationRepository;
import org.overture.ego.repository.GroupsRepository;
import org.overture.ego.repository.UserRepository;
import org.skife.jdbi.v2.DBI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.sql.DataSource;

@Lazy
@Configuration
public class RepositoryConfig {

    DataSource dataSource = JdbcConnectionPool.create("jdbc:h2:/Users/rverma/test",
            "test",
            "");

    @Bean
    public DBI dbi() {
        return new DBI(dataSource);
    }

    @Bean
    public UserRepository userRepository(DBI dbi) {
        return dbi.open(UserRepository.class);
    }

    @Bean
    public ApplicationRepository applicationRepository(DBI dbi) {
        return dbi.open(ApplicationRepository.class);
    }

    @Bean
    public GroupsRepository groupRepository(DBI dbi) {
        return dbi.open(GroupsRepository.class);
    }


}
