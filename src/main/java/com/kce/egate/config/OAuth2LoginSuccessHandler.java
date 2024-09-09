package com.kce.egate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kce.egate.entity.Admins;
import com.kce.egate.entity.User;
import com.kce.egate.repository.AdminsRepository;
import com.kce.egate.repository.UserRepository;
import com.kce.egate.response.CommonResponse;
import com.kce.egate.service.serviceImpl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AdminsRepository adminsRepository;
    @Autowired
    private UserServiceImpl userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        var admins = adminsRepository.findAll()
                .parallelStream()
                .map(Admins::getAdminEmail)
                .toList();
        String _id = null;
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
        }
        CommonResponse commonResponse = userService.oauth2Callback(email,name,picture,_id);
        Map<String,Object> responseData = new HashMap<>();
        responseData.put("code",commonResponse.getCode());
        responseData.put("status",commonResponse.getStatus());
        responseData.put("successMessage",commonResponse.getSuccessMessage());
        responseData.put("errorMessage",commonResponse.getErrorMessage());
        responseData.put("data",commonResponse.getData());

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(responseData);

        response.setContentType("text/html");
        String script = String.format("""
                                    <script>
                                    window.opener.postMessage(%s, '*');
                                    window.close();
                                    </script>
                                    """, jsonResponse
        );
        response.getWriter().write(script);
    }
}
