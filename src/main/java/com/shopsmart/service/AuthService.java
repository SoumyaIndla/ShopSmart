package com.shopsmart.service;
import com.shopsmart.api.ApiDtos.*; import com.shopsmart.security.JwtService; import com.shopsmart.user.*;
import org.springframework.http.HttpStatus; import org.springframework.security.authentication.*; import org.springframework.security.core.AuthenticationException; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service; import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
@Service public class AuthService {
 private final UserRepository users;private final PasswordEncoder encoder;private final AuthenticationManager manager;private final JwtService jwt;
 public AuthService(UserRepository u,PasswordEncoder e,AuthenticationManager m,JwtService j){users=u;encoder=e;manager=m;jwt=j;}
 public AuthResponse register(RegisterRequest r){String email=r.email().trim().toLowerCase();if(users.existsByEmail(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists");return response(users.save(new ShopUser(r.fullName().trim(),email,encoder.encode(r.password()),Role.CUSTOMER)));}
 public AuthResponse login(LoginRequest r){String email=r.email().trim().toLowerCase();try{manager.authenticate(new UsernamePasswordAuthenticationToken(email,r.password()));}catch(AuthenticationException e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect");}return response(users.findByEmail(email).orElseThrow());}
 @Transactional public void logout(String email){var user=users.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required"));user.invalidateTokens();}
 private AuthResponse response(ShopUser u){return new AuthResponse(jwt.create(u),"Bearer",u.getEmail(),u.getFullName(),u.getRole().name());}
}
