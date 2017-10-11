package org.overture.ego.service;

import lombok.val;
import org.overture.ego.model.entity.Application;
import org.overture.ego.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class ApplicationService {

    @Autowired
    ApplicationRepository applicationRepository;

    private final String APP_PREFIX = "";

    public Application create(Application applicationInfo) {
        String appId = applicationInfo.getId();
        if(appId == null || appId.isEmpty())
            appId = APP_PREFIX + UUID.randomUUID().toString().substring(0,4);

        applicationInfo.setId(appId);
        val output = applicationRepository.create(applicationInfo);
        return applicationRepository.read(appId);
    }

    public Application get(String applicationId) {
        return applicationRepository.read(applicationId);
    }

    public Application update(Application updatedApplicationInfo) {
        applicationRepository.update(updatedApplicationInfo.getId(), updatedApplicationInfo);
        return updatedApplicationInfo;
    }

    public void delete(String applicationId) {
        applicationRepository.delete(applicationId);
    }
}
