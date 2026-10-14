package chronos.codereviewapi.services;

import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.exceptions.UserNotFoundException;
import chronos.codereviewapi.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {
    private final UserRepository userRepository;

    public UserEntity getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        Long userId;

        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new UserNotFoundException("Invalid user identity in token");
        }

        return userRepository.findById(userId)
                .filter(UserEntity::isEnabled)
                .orElseThrow(() ->
                        new UserNotFoundException("Authenticated user not found!")
                );
    }

}
