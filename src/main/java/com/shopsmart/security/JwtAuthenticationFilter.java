package com.shopsmart.security;
import com.shopsmart.user.UserRepository;
import jakarta.servlet.*; import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component; import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
@Component public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final JwtService jwtService; private final UserRepository users;
 public JwtAuthenticationFilter(JwtService jwtService,UserRepository users){this.jwtService=jwtService;this.users=users;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  String header=request.getHeader("Authorization");
  if(header!=null&&header.startsWith("Bearer ")&&SecurityContextHolder.getContext().getAuthentication()==null){try{String token=header.substring(7);String email=jwtService.getEmail(token);int tokenVersion=jwtService.getTokenVersion(token);users.findByEmail(email).filter(user->user.getTokenVersion()==tokenVersion).ifPresent(user->{var auth=new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));SecurityContextHolder.getContext().setAuthentication(auth);});}catch(RuntimeException ignored){}}
  chain.doFilter(request,response);
 }
}
