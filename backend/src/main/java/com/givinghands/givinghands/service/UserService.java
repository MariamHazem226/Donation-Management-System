package com.givinghands.givinghands.service;

import com.givinghands.givinghands.entity.User;
import java.util.List;

public interface UserService {

    User getUserById(Long id);

    User updateUser(Long id, String name, String email);

    List<User> getAllUsers();

    void deleteUser(Long id);

    void updateAvatar(Long id, String avatarUrl);

    void updateCover(Long id, String coverUrl);
}
