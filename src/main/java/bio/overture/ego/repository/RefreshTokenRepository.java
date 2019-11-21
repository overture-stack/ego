package bio.overture.ego.repository;

import org.springframework.data.repository.CrudRepository;

import bio.overture.ego.model.entity.RefreshToken;

import java.util.UUID;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, UUID> {

}