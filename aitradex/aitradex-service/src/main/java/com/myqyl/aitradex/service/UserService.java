package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.CreateUserRequest;
import com.myqyl.aitradex.api.dto.UserDto;
import com.myqyl.aitradex.domain.User;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public UserDto create(CreateUserRequest request) {
    User user =
        User.builder()
            .email(request.email().toLowerCase())
            .displayName(request.displayName())
            .role(request.role())
            .build();
    return toDto(userRepository.save(user));
  }

  @Transactional(readOnly = true)
  public List<UserDto> list() {
    return userRepository.findAll().stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public UserDto get(UUID id) {
    return userRepository.findById(id).map(this::toDto).orElseThrow(() -> notFound(id));
  }

  private UserDto toDto(User user) {
    return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(),
        user.getCreatedAt());
  }

  private NotFoundException notFound(UUID id) {
    return new NotFoundException("User %s not found".formatted(id));
  }
}
