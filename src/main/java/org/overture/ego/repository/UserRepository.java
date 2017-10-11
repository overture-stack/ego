package org.overture.ego.repository;

import lombok.NonNull;
import lombok.Singular;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.User;
import org.overture.ego.repository.mapper.UserMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.Date;
import java.util.List;

@RegisterMapper(UserMapper.class)
public interface UserRepository {

    @SqlQuery("SELECT * FROM USERS")
    List<User> getAllUsers();

    @SqlUpdate("INSERT INTO USERS (userName, email, role, status, firstName, lastName, createdAt,lastLogin,preferredLanguage) " +
            "VALUES (:userName, :email, :role, :status, :firstName, :lastName, :createdAt, :lastLogin, :preferredLanguage)")
    int create(@BindBean User user);

    @SqlQuery("SELECT * FROM USERS WHERE userName=:userName")
    List<User> read(@Bind("userName") String username);

    @SqlUpdate("UPDATE USERS SET role=:role, status=:status," +
            "firstName=:firstName, lastName=:lastName, createdAt=:createdAt , lastLogin=:lastLogin, " +
            "preferredLanguage=:preferredLanguage WHERE id=:id")
    int update(@BindBean User user);

    @SqlUpdate("DELETE from USERS where id=:id")
    int delete(@Bind("id") String id);

}
