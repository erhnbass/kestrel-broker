package com.erhan.kestrel_broker.service.security;

import com.erhan.kestrel_broker.domain.entity.Customer;
import com.erhan.kestrel_broker.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerDetailsService implements UserDetailsService {

    private final CustomerRepository repo;

    private static final Logger log = LoggerFactory.getLogger(CustomerDetailsService.class);



    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Loading user: {}", username);
        var c = repo.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
        log.info("User found: {} with role {}", username, c.getRole());
        return User.withUsername(c.getUsername())
                .password(c.getPassword())
                .roles(c.getRole())
                .build();
    }
}
