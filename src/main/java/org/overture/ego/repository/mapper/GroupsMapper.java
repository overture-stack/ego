package org.overture.ego.repository.mapper;


import org.overture.ego.model.entity.Group;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GroupsMapper implements ResultSetMapper<Group> {
    @Override
    public Group map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return null;
    }
}
