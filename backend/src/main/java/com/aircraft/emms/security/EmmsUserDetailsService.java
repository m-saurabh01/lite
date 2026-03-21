package com.aircraft.emms.security;

import com.aircraft.emms.entity.User;
import com.aircraft.emms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmmsUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String serviceId) throws UsernameNotFoundException {
        User user = userRepository.findByServiceId(serviceId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + serviceId));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is deactivated: " + serviceId);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getServiceId(),
                user.getPassword(),
                user.isActive(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
