package com.kce.egate.service.serviceImpl;

import com.kce.egate.constant.Constant;
import com.kce.egate.entity.Admins;
import com.kce.egate.entity.OtpInfo;
import com.kce.egate.entity.Token;
import com.kce.egate.entity.User;
import com.kce.egate.enumeration.TokenType;
import com.kce.egate.enumeration.ResponseStatus;
import com.kce.egate.repository.AdminsRepository;
import com.kce.egate.repository.OtpInfoRepository;
import com.kce.egate.repository.TokenRepository;
import com.kce.egate.repository.UserRepository;
import com.kce.egate.request.AuthenticationRequest;
import com.kce.egate.request.EmailDetailRequest;
import com.kce.egate.request.PasswordChangeOTPRequest;
import com.kce.egate.request.VerifyOTPRequest;
import com.kce.egate.response.CommonResponse;
import com.kce.egate.service.UserService;
import com.kce.egate.util.EmailUtils;
import com.kce.egate.util.JWTUtils;
import com.kce.egate.util.exceptions.InvalidEmailException;
import com.kce.egate.util.exceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.bcel.Const;
import org.springdoc.core.service.RequestBodyService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.management.InvalidAttributeValueException;
import javax.swing.text.html.Option;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final JWTUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final AdminsRepository adminsRepository;
    private final UserRepository userRepository;
    private final EmailUtils emailUtils;
    private final OtpInfoRepository otpInfoRepository;
    private static final Long EXPIRE_TIME = 300000L;
    private final PasswordEncoder passwordEncoder;

    @Override
    public CommonResponse signInUser(AuthenticationRequest authenticationRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(), authenticationRequest.getPassword())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        HashMap<String,Object> claims = new HashMap<>();
        claims.put("roles",roles);
        String token = jwtUtils.generateToken(claims,userDetails);
        expireAllExistingToken(userDetails.getUsername());
        saveToken(token, userDetails.getUsername());
        return CommonResponse.builder()
                .code(200)
                .status(ResponseStatus.SUCCESS)
                .data(token)
                .successMessage(Constant.SIGN_IN_SUCCESS)
                .build();
    }

    private void expireAllExistingToken(String email) {
        var oldTokens = tokenRepository.findByUserEmail(email);
        if(oldTokens.isEmpty()) return;
        oldTokens.forEach(t -> t.setExpired(true));
        tokenRepository.saveAll(oldTokens);
    }

    private void saveToken(String token, String email) {
        Token token1 = new Token();
        token1.setToken(token);
        token1.setUserEmail(email);
        token1.setTokenType(TokenType.BEARER);
        token1.setExpired(false);
        tokenRepository.save(token1);
    }

    @Override
    public CommonResponse oauth2Callback(String email,String name,String picture,String id) throws IllegalAccessException {
        if(email.isBlank()){
            throw new IllegalAccessException(Constant.UNAUTHORIZED_ADMIN);
        }
        var admins = adminsRepository.findAll()
                .parallelStream()
                .map(Admins::getAdminEmail)
                .toList();
        if(!admins.contains(email)){
            throw new IllegalAccessException(Constant.UNAUTHORIZED_ADMIN);
        }
        if(!userRepository.existsById(id)){
            throw new IllegalAccessException(Constant.UNAUTHORIZED_ADMIN);
        }
        List<String> roles = List.of("ADMIN");
        HashMap<String,Object> claims = new HashMap<>();
        claims.put("roles",roles);
        String token = jwtUtils.generateToken(claims,email);
        expireAllExistingToken(email);
        saveToken(token, email);
        return CommonResponse.builder()
                .code(200)
                .status(ResponseStatus.SUCCESS)
                .data(token)
                .successMessage(Constant.SIGN_IN_SUCCESS)
                .build();
    }

    @Override
    public CommonResponse logout(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        var jwtToken = authHeader.substring(7);
        var userEmail = jwtUtils.extractUsername(jwtToken);
        tokenRepository.deleteTokensByUserEmail(userEmail);
        SecurityContextHolder.clearContext();
        return CommonResponse.builder()
                .code(200)
                .status(ResponseStatus.SUCCESS)
                .data(null)
                .successMessage(Constant.LOGOUT_SUCCESS)
                .build();
    }

    @Override
    public CommonResponse addAdmin(String email) throws InvalidEmailException {
        EmailDetailRequest request = new EmailDetailRequest();
        String subject = "Welcome to E-Gate 2.0 - Your Admin Access Details";
        String body = """
        <html>
        <head>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    background-color: #f4f4f4;
                    color: #333;
                    margin: 0;
                    padding: 20px;
                }
                .container {
                    background-color: #ffffff;
                    border-radius: 8px;
                    padding: 20px;
                    max-width: 600px;
                    margin: auto;
                    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
                }
                .header {
                    background-color: #007BFF;
                    color: #ffffff;
                    padding: 10px 20px;
                    border-radius: 8px 8px 0 0;
                    text-align: center;
                }
                .content {
                    margin: 20px 0;
                }
                .button {
                    display: inline-block;
                    font-size: 16px;
                    color: #ffffff;
                    background-color: #007BFF;
                    padding: 10px 20px;
                    text-decoration: none;
                    border-radius: 5px;
                }
                .footer {
                    font-size: 12px;
                    color: #777;
                    text-align: center;
                    margin-top: 20px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    Welcome to E-Gate v2.0
                </div>
                <div class="content">
                    <p>Dear Administrator,</p>
                    <p>We are pleased to inform you that you have been granted administrative access to E-Gate v2.0. Your default password is <strong>"karpagam"</strong>. For security reasons, we encourage you to change your password immediately after logging in.</p>
                    <p>To access the E-Gate v2.0 system, please use the following link:</p>
                    <p><a href="#" class="button">Log in to E-Gate v2.0</a></p>
                    <p>If you encounter any issues or have any questions regarding your new role or the system, please do not hesitate to reach out to our support team.</p>
                    <p>Thank you for your attention to this matter. We look forward to your effective management within E-Gate v2.0.</p>
                </div>
                <div class="footer">
                    <p>This is an automated message. Please do not reply to this email.</p>
                    <p>&copy; 2024 E-gate v2.0. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """;
        request.setSubject(subject);
        request.setRecipient(email);
        request.setMsgBody(body);
        boolean isSent = emailUtils.sendMimeMessage(request);
        if(!isSent){
            throw new InvalidEmailException(Constant.INVALID_EMAIL);
        }
        Admins admins = new Admins();
        admins.setAdminEmail(email);
        adminsRepository.save(admins);
        return CommonResponse.builder()
                .code(201)
                .successMessage(Constant.ADMIN_ADDED_SUCCESS)
                .data(email)
                .status(ResponseStatus.CREATED)
                .build();
    }

    @Override
    public CommonResponse forgotPassword(String email) throws UserNotFoundException {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if(userOptional.isEmpty()){
            throw new UserNotFoundException(Constant.USER_NOT_FOUND);
        }
        EmailDetailRequest emailDetailRequest = new EmailDetailRequest();
        Integer otp = generateOtp();
        String subject = "E-gate v2.0: Secure Password Change Request";
        String body = String.format("""
                <!DOCTYPE html>
                       <html lang="en">
                       <head>
                           <style>
                               body {
                                   font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                   line-height: 1.6;
                                   color: #333;
                                   background-color: #f4f4f4;
                                   margin: 0;
                                   padding: 0;
                               }
                               .container {
                                   max-width: 600px;
                                   margin: 20px auto;
                                   background-color: #ffffff;
                                   border-radius: 8px;
                                   overflow: hidden;
                                   box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
                               }
                               .header {
                                   background-color: #0056b3;
                                   color: white;
                                   padding: 30px;
                                   text-align: center;
                               }
                               .header h1 {
                                   margin: 0;
                                   font-size: 28px;
                                   font-weight: 300;
                               }
                               .content {
                                   padding: 40px;
                               }
                               .footer {
                                   background-color: #f9f9f9;
                                   text-align: center;
                                   padding: 20px;
                                   font-size: 0.8em;
                                   color: #666;
                               }
                               .otp {
                                   font-size: 32px;
                                   font-weight: bold;
                                   color: #0056b3;
                                   text-align: center;
                                   margin: 30px 0;
                                   padding: 15px;
                                   background-color: #f0f7ff;
                                   border-radius: 6px;
                               }
                               .btn {
                                   display: inline-block;
                                   background-color: #0056b3;
                                   color: white;
                                   text-decoration: none;
                                   padding: 12px 25px;
                                   border-radius: 5px;
                                   margin-top: 20px;
                               }
                               .warning {
                                   background-color: #fff5f5;
                                   border-left: 4px solid #ff4d4d;
                                   padding: 15px;
                                   margin-top: 30px;
                               }
                           </style>
                       </head>
                       <body>
                           <div class="container">
                               <div class="header">
                                   <h1>E-gate 2.0 Password Change Request</h1>
                               </div>
                               <div class="content">
                                   <p>Dear Valued User,</p>
                                   <p>We have received a request to change the password for your E-gate 2.0 account. To ensure the security of your account, we require verification of this request.</p>
                                   <p>Please use the following One-Time Password (OTP) to complete the password change process:</p>
                                   <div class="otp">%d</div>
                                   <p><strong>Important:</strong> This OTP will expire in 5 minutes for security reasons.</p>
                                   <div class="warning">
                                       <p><strong>Security Notice:</strong> If you did not initiate this password change request, please contact our support team immediately.</p>
                                   </div>
                                   <p>Thank you for helping us keep your account secure.</p>
                                   <p>Best regards,<br>The E-gate v2.0 Security Team</p>
                               </div>
                               <div class="footer">
                                   <p>This is an automated message. Please do not reply to this email.</p>
                                   <p>&copy; 2024 E-gate v2.0. All rights reserved.</p>
                               </div>
                           </div>
                       </body>
                       </html>
                """,otp);
        emailDetailRequest.setRecipient(email);
        emailDetailRequest.setSubject(subject);
        emailDetailRequest.setMsgBody(body);
        emailUtils.sendMimeMessage(emailDetailRequest);
        long currentTimeMillis = System.currentTimeMillis();
        long expireTimeMillis = currentTimeMillis + EXPIRE_TIME;
        Optional<OtpInfo> otpInfoOptional = otpInfoRepository.findByEmail(email);
        OtpInfo otpInfo;
        if(otpInfoOptional.isPresent()){
            otpInfo = otpInfoOptional.get();
            otpInfo.setCreatedAt(currentTimeMillis);
            otpInfo.setExpireAt(expireTimeMillis);
            otpInfo.setOtp(otp);
            otpInfoRepository.save(otpInfo);
        }else{
            otpInfo = new OtpInfo();
            otpInfo.setOtp(otp);
            otpInfo.setEmail(email);
            otpInfo.setCreatedAt(currentTimeMillis);
            otpInfo.setExpireAt(expireTimeMillis);
            otpInfoRepository.save(otpInfo);
        }
        return CommonResponse.builder()
                .code(200)
                .status(ResponseStatus.SUCCESS)
                .successMessage(Constant.OTP_SENT_SUCCESS)
                .data(email)
                .build();
    }

    @Override
    public CommonResponse verifyOtp(VerifyOTPRequest request) throws UserNotFoundException, IllegalAccessException, InvalidAttributeValueException {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if(userOptional.isEmpty()){
            throw new UserNotFoundException(Constant.USER_NOT_FOUND);
        }
        Optional<OtpInfo> otpInfoOptional = otpInfoRepository.findByEmail(request.getEmail());
        if(otpInfoOptional.isEmpty()){
            throw new IllegalAccessException(Constant.ILLEGAL_ACCESS);
        }
        OtpInfo otpInfo = otpInfoOptional.get();
        if(!Objects.equals(otpInfo.getOtp(), request.getOtp())){
            throw new InvalidAttributeValueException(Constant.INVALID_OTP);
        }
        if(otpInfo.getExpireAt()<System.currentTimeMillis()){
            otpInfoRepository.deleteById(otpInfo.get_id());
            throw new InvalidAttributeValueException(Constant.INVALID_OTP);
        }
        String uniqueId = UUID.randomUUID().toString();
        otpInfo.setUniqueId(uniqueId);
        otpInfoRepository.save(otpInfo);
        return CommonResponse.builder()
                .code(200)
                .successMessage(Constant.OTP_VERIFIED_SUCCESS)
                .status(ResponseStatus.SUCCESS)
                .data(uniqueId)
                .build();
    }

    @Override
    public CommonResponse changePassword(String uniqueId, PasswordChangeOTPRequest request) throws IllegalAccessException, UserNotFoundException {
        Optional<OtpInfo> otpInfoOptional = otpInfoRepository.findByEmail(request.getEmail());
        if(otpInfoOptional.isEmpty()){
            throw new IllegalAccessException(Constant.ILLEGAL_ACCESS);
        }
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if(userOptional.isEmpty()){
            throw new UserNotFoundException(Constant.USER_NOT_FOUND);
        }
        if(!uniqueId.equals(otpInfoOptional.get().getUniqueId())){
            throw new IllegalAccessException(Constant.ILLEGAL_ACCESS);
        }
        User user = userOptional.get();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        otpInfoRepository.deleteById(otpInfoOptional.get().get_id());
        return CommonResponse.builder()
                .code(200)
                .data(request.getEmail())
                .successMessage(Constant.PASSWORD_CHANGED_SUCCESS)
                .status(ResponseStatus.UPDATED)
                .build();
    }

    public Integer generateOtp() {
        Random random = new Random();
        return 100000 + random.nextInt(900000);
    }

}
