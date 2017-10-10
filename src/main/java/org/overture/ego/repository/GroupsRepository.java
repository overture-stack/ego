package org.overture.ego.repository;

import org.overture.ego.repository.mapper.GroupsMapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(GroupsMapper.class)
public interface GroupsRepository {
}
