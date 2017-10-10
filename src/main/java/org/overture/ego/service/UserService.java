package org.overture.ego.service;

import org.overture.ego.model.entity.User;
import org.overture.ego.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    public User create(User userInfo) {
        if(userInfo.getId() == null || userInfo.getId().isEmpty())
            userInfo.setId(userInfo.getUserName());
        if(userInfo.getEmail() == null || userInfo.getEmail().isEmpty())
            userInfo.setEmail(userInfo.getUserName());
        userRepository.create(userInfo);
        return userInfo;
    }

    public User get(String userId) {
        if(userRepository.read(userId) == null || userRepository.read(userId).size() == 0)
            return null;
        else
        return userRepository.read(userId).get(0);
    }

    public User update(User updatedUserInfo) {
        userRepository.update(updatedUserInfo);
        return updatedUserInfo;
    }

    public void delete(String userId) {
        userRepository.delete(userId);
    }

    public List<User> listUsers() {
        return userRepository.getAllUsers();
    }
}
