package com.shopsmart.service;
import com.shopsmart.user.UserRepository; import org.springframework.security.core.userdetails.*; import org.springframework.stereotype.Service;
@Service public class ShopUserDetailsService implements UserDetailsService {private final UserRepository users;public ShopUserDetailsService(UserRepository u){users=u;}@Override public UserDetails loadUserByUsername(String email){return users.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("User not found"));}}
