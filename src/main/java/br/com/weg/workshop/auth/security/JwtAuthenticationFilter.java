package br.com.weg.workshop.auth.security;
import br.com.weg.workshop.auth.service.JwtService;
import br.com.weg.workshop.shared.error.ApiErrorFactory;
import br.com.weg.workshop.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final JwtService jwt; private final ApiErrorFactory errors; private final ObjectMapper mapper;
 public JwtAuthenticationFilter(JwtService jwt, ApiErrorFactory errors, ObjectMapper mapper) { this.jwt=jwt;this.errors=errors;this.mapper=mapper; }
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  String header=request.getHeader("Authorization");
  if (header==null || !header.startsWith("Bearer ")) { chain.doFilter(request,response); return; }
  try { Claims claims=jwt.parse(header.substring(7)); String role=claims.get("role",String.class);
   Boolean mustChange=claims.get("mustChangePassword", Boolean.class);
   if (Boolean.TRUE.equals(mustChange) && !request.getRequestURI().equals("/api/v1/auth/change-password")) {
    response.setStatus(403); response.setContentType(MediaType.APPLICATION_JSON_VALUE); mapper.writeValue(response.getOutputStream(),errors.create(request,HttpStatus.FORBIDDEN,ErrorCode.FORBIDDEN,"Password change is required.",List.of())); return;
   }
   var auth=new UsernamePasswordAuthenticationToken(claims.getSubject(),null,List.of(new SimpleGrantedAuthority("ROLE_"+role)));
   SecurityContextHolder.getContext().setAuthentication(auth); chain.doFilter(request,response);
  } catch (Exception exception) { SecurityContextHolder.clearContext(); response.setStatus(401);response.setContentType(MediaType.APPLICATION_JSON_VALUE);mapper.writeValue(response.getOutputStream(),errors.create(request,HttpStatus.UNAUTHORIZED,ErrorCode.UNAUTHORIZED,"Invalid or expired access token.",List.of())); }
 }
}
