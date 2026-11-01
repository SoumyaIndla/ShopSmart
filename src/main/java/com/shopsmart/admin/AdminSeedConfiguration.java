package com.shopsmart.admin;
import com.shopsmart.user.ShopUser;import com.shopsmart.user.UserRepository;import com.shopsmart.user.Role;import org.springframework.beans.factory.annotation.Value;import org.springframework.boot.CommandLineRunner;import org.springframework.context.annotation.Bean;import org.springframework.context.annotation.Configuration;import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration public class AdminSeedConfiguration {
 @Bean CommandLineRunner seedAdmin(UserRepository users,PasswordEncoder encoder,@Value("${shopsmart.admin.email:}") String email,@Value("${shopsmart.admin.password:}") String password){return args->{if(!email.isBlank()&&!password.isBlank()&&!users.existsByEmail(email.toLowerCase()))users.save(new ShopUser("ShopSmart Admin",email.trim().toLowerCase(),encoder.encode(password),Role.ADMIN));};}
}
