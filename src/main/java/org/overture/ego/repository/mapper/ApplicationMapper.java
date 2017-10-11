package org.overture.ego.repository.mapper;

import org.overture.ego.model.entity.Application;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ApplicationMapper implements ResultSetMapper<Application> {
    @Override
    public Application map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return Application.builder().id(resultSet.getString("id"))
                .applicationName(resultSet.getString("applicationName"))
                .clientId(resultSet.getString("clientId"))
                .clientSecret(resultSet.getString("clientSecret"))
                .description(resultSet.getString("description"))
                .redirectUri(resultSet.getString("redirectUri"))
                .status(resultSet.getString("status")).build();
    }
}
