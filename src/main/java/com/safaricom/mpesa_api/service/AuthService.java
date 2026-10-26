package com.safaricom.mpesa_api.service;

import com.safaricom.mpesa_api.dto.LoginRequest;
import com.safaricom.mpesa_api.dto.RegisterRequest;
import com.safaricom.mpesa_api.exception.AppException;
import com.safaricom.mpesa_api.model.User;
import com.safaricom.mpesa_api.repository.UserRepository;
import com.safaricom.mpesa_api.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public void register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new AppException(HttpStatus.CONFLICT, "Username already taken");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        userRepository.save(user);
    }

    public String login(LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return jwtUtil.generateToken(req.getUsername());
    }
}
