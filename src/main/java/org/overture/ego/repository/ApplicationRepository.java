package org.overture.ego.repository;

import org.overture.ego.model.entity.Application;
import org.overture.ego.repository.mapper.ApplicationMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(ApplicationMapper.class)
public interface ApplicationRepository {


    @SqlUpdate("INSERT INTO APPLICATION (id, applicationName, clientId, clientSecret, redirectUri, description, status) " +
            "VALUES (:id, :applicationName, :clientId, :clientSecret, :redirectUri, :description, :status)")
    int create(@BindBean Application application);

    @SqlQuery("SELECT id,applicationName,clientId,clientSecret,redirectUri,description,status FROM APPLICATION WHERE id=:id")
    Application read(@Bind("id") String appId);

    @SqlUpdate("UPDATE APPLICATION " +
            "SET applicationName=:applicationName, clientId=:clientId" +
            ", clientSecret=:clientSecret, redirectUri=:redirectUri, description=:description, status=:status"+
            " WHERE id=:id")
    int update(@Bind("id") String id, @BindBean Application application);

    @SqlUpdate("DELETE from APPLICATION where id=:id")
    int delete(@Bind("id") String id);



}
