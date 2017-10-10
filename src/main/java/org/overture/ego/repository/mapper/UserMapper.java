package org.overture.ego.repository.mapper;

import lombok.SneakyThrows;
import org.overture.ego.model.entity.User;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;


public class UserMapper implements ResultSetMapper<User> {
    @Override
    @SneakyThrows
    public User map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        SimpleDateFormat formatter =
                new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        return User.builder().id(resultSet.getString("id"))
                .userName(resultSet.getString("userName"))
                .email(resultSet.getString("email"))
                .firstName(resultSet.getString("firstName"))
                .lastName(resultSet.getString("lastName"))
                .createdAt(resultSet.getString("createdAt"))
                .lastLogin(resultSet.getString("lastLogin"))
                .role(resultSet.getString("role"))
                .status(resultSet.getString("status")).build();

    }

}
