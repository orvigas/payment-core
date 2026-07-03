package com.payment.repositories;

import com.payment.models.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * @author orvigas@gmail.com
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Finds a user by login username.
   *
   * @param username the login username
   * @return the matching user, if any
   */
  Optional<User> findByUsername(String username);
}
