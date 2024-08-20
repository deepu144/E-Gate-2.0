package com.kce.egate.config;

import com.kce.egate.entity.Admins;
import com.kce.egate.entity.User;
import com.kce.egate.repository.AdminsRepository;
import com.kce.egate.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AdminsRepository adminsRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        var admins = adminsRepository.findAll()
                .parallelStream()
                .map(Admins::getAdminEmail)
                .toList();
        String _id;
        if(admins.contains(email)){
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                User user = new User();
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode("karpagam"));
                user.setRole("ADMIN");
                userRepository.save(user);
                _id = user.get_id();
            }else{
                _id = userOptional.get().get_id();
            }
            String name = oAuth2User.getAttribute("name");
            String picture = oAuth2User.getAttribute("picture");
            String url = String.format("/auth/oauth2/callback?email=%s&name=%s&picture=%s&id=%s",email,name,picture,_id);
            getRedirectStrategy().sendRedirect(request, response, url);
        }else{
            String url = "/auth/oauth2/callback?email=&name=&picture=&id=";
            getRedirectStrategy().sendRedirect(request, response, url);
        }
    }
}

