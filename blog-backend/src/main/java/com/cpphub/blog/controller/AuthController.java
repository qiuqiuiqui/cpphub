package com.cpphub.blog.controller;

import com.cpphub.blog.common.Result;
import com.cpphub.blog.dto.LoginDTO;
import com.cpphub.blog.dto.LoginResponse;
import com.cpphub.blog.entity.User;
import com.cpphub.blog.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuthController {
  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  @PostMapping("/login")
  public Result<LoginResponse> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request,
      jakarta.servlet.http.HttpServletResponse response) {
    User user = userService.findByUsername(dto.getUsername());
    if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
      return Result.fail(401, "Invalid username or password");
    }
    // Create authentication and persist to session
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        user.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
    ctx.setAuthentication(auth);
    SecurityContextHolder.setContext(ctx);
    securityContextRepository.saveContext(ctx, request, response);
    // Also create a fresh HTTP session for cookie issuance
    HttpSession session = request.getSession(true);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
    return Result.ok(new LoginResponse(user.getUsername(), user.getRole()));
  }

  @PostMapping("/logout")
  public Result<Void> logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null)
      session.invalidate();
    SecurityContextHolder.clearContext();
    return Result.ok();
  }

  @GetMapping("/me")
  public Result<LoginResponse> me() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      return Result.fail(401, "Not logged in");
    }
    // principal name is username; look up role from DB to ensure freshness
    User user = userService.findByUsername(auth.getName());
    if (user == null)
      return Result.fail(401, "User not found");
    return Result.ok(new LoginResponse(user.getUsername(), user.getRole()));
  }
}