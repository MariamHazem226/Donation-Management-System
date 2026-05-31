package com.givinghands.givinghands.service.impl;

import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.repository.UserRepository;
import com.givinghands.givinghands.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    public User updateUser(Long id, String name, String email) {
        User user = getUserById(id);
        if (name != null && !name.isEmpty()) user.setName(name);
        if (email != null && !email.isEmpty()) user.setEmail(email);
        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id))
            throw new RuntimeException("User not found with id: " + id);
        userRepository.deleteById(id);
    }

    @Override
    public void updateAvatar(Long id, String avatarUrl) {
        User user = getUserById(id);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
    }

    @Override
    public void updateCover(Long id, String coverUrl) {
        User user = getUserById(id);
        user.setCoverUrl(coverUrl);
        userRepository.save(user);
    }
}
