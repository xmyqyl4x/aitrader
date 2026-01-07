package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.CreateUserRequest;
import com.myqyl.aitradex.api.dto.UserDto;
import com.myqyl.aitradex.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserDto createUser(@Valid @RequestBody CreateUserRequest request) {
    return userService.create(request);
  }

  @GetMapping
  public List<UserDto> listUsers() {
    return userService.list();
  }

  @GetMapping("/{id}")
  public UserDto getUser(@PathVariable UUID id) {
    return userService.get(id);
  }
}
