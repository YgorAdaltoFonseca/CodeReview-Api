package chronos.codereviewapi.services.settingsServices;

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

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new UserNotFoundException("No authenticated user found");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .filter(UserEntity::isEnabled)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found!"
                        )
                );
    }


}
