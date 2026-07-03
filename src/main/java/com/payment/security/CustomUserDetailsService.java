package com.payment.security;

import com.payment.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads {@link com.payment.models.User} principals for Spring Security's authentication
 * providers, backed by {@link UserRepository}.
 *
 * @author orvigas@gmail.com
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  /**
   * Loads a user by username for authentication.
   *
   * @param username the login username
   * @return the matching user details
   * @throws UsernameNotFoundException if no user has that username
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }
}
