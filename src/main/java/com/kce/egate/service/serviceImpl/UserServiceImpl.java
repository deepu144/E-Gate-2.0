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
import com.kce.egate.util.exceptions.InvalidPassword;
import com.kce.egate.util.exceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.management.InvalidAttributeValueException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final JWTUtils jwtUtils;
    private final TokenRepository tokenRepository;
    private final AdminsRepository adminsRepository;
    private final UserRepository userRepository;
    private final EmailUtils emailUtils;
    private final OtpInfoRepository otpInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Long EXPIRE_TIME = 300000L;

    @Override
    public CommonResponse signInUser(AuthenticationRequest authenticationRequest) throws IllegalAccessException, InvalidPassword {
        List<String> admins = adminsRepository.findAll()
                .parallelStream()
                .map(Admins::getAdminEmail)
                .toList();
        if(!admins.contains(authenticationRequest.getEmail())){
            throw new IllegalAccessException(Constant.ILLEGAL_ACCESS);
        }
        Optional<User> userOptional = userRepository.findByEmail(authenticationRequest.getEmail());
        String email;
        String role;
        if(userOptional.isEmpty()){
            User user = new User();
            user.setEmail(authenticationRequest.getEmail());
            user.setPassword(passwordEncoder.encode("karpagam"));
            user.setRole("ADMIN");
            userRepository.save(user);
            email = user.getEmail();
            role = user.getRole();
        }else{
            if(!passwordEncoder.matches(authenticationRequest.getPassword(), userOptional.get().getPassword())){
                throw new InvalidPassword(Constant.PASSWORD_INCORRECT);
            }
            User user = userOptional.get();
            email = user.getEmail();
            role = user.getRole();
        }
        HashMap<String,Object> claims = new HashMap<>();
        claims.put("roles",List.of(role));
        String token = jwtUtils.generateToken(claims,email);
        expireAndDeleteAllExistingToken(email);
        saveToken(token,email);
        return CommonResponse.builder()
                .code(200)
                .status(ResponseStatus.SUCCESS)
                .data(token)
                .successMessage(Constant.SIGN_IN_SUCCESS)
                .build();
    }

    private void expireAndDeleteAllExistingToken(String email) {
        var oldTokens = tokenRepository.findByUserEmail(email);
        if(oldTokens.isEmpty()) return;
        oldTokens.forEach(token -> tokenRepository.deleteById(token.get_id()));
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
        expireAndDeleteAllExistingToken(email);
        saveToken(token, email);
        return CommonResponse.builder()
                .code(200)
                .status(ResponseStatus.SUCCESS)
                .data(Arrays.asList(token,email))
                .successMessage(Constant.SIGN_IN_SUCCESS)
                .build();
    }

    @Override
    public CommonResponse logout(HttpServletRequest request, HttpServletResponse response) {
        final String authHeader = request.getHeader("Authorization");
        var jwtToken = authHeader.substring(7);
        var userEmail = jwtUtils.extractUsername(jwtToken);
        tokenRepository.deleteTokensByUserEmail(userEmail);
        SecurityContextHolder.clearContext();
        try {
            response.sendRedirect("http://localhost:3000/admin");
            return null;
        } catch (IOException e) {
            return CommonResponse.builder()
                    .code(400)
                    .status(ResponseStatus.FAILED)
                    .data(null)
                    .errorMessage(Constant.LOGOUT_ERROR)
                    .build();
        }
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
        EmailDetailRequest emailRequest = new EmailDetailRequest();
        String body = String.format(
                """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Password Change Confirmation</title>
                            <style>
                                body {
                                    font-family: Arial, sans-serif;
                                    background-color: #f4f4f4;
                                    margin: 0;
                                    padding: 0;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 20px auto;
                                    background-color: #ffffff;
                                    padding: 20px;
                                    border-radius: 8px;
                                    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
                                }
                                .header {
                                    background-color: #2c3e50;
                                    padding: 20px;
                                    border-radius: 8px 8px 0 0;
                                    text-align: center;
                                    color: #ffffff;
                                }
                                .header h1 {
                                    margin: 0;
                                    font-size: 24px;
                                }
                                .content {
                                    padding: 20px;
                                    font-size: 16px;
                                    line-height: 1.6;
                                    color: #333333;
                                }
                                .content h2 {
                                    color: #2c3e50;
                                    font-size: 20px;
                                }
                                .content p {
                                    margin: 10px 0;
                                }
                                .content ul {
                                    list-style-type: none;
                                    padding: 0;
                                }
                                .content ul li {
                                    background-color: #ecf0f1;
                                    margin: 5px 0;
                                    padding: 10px;
                                    border-radius: 4px;
                                }
                                .footer {
                                    text-align: center;
                                    padding: 20px;
                                    font-size: 14px;
                                    color: #777777;
                                    background-color: #ecf0f1;
                                    border-radius: 0 0 8px 8px;
                                }
                                .footer a {
                                    color: #2c3e50;
                                    text-decoration: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>Password Change Notification</h1>
                                </div>
                                <div class="content">
                                    <h2>Dear Admin,</h2>
                                    <p>We are pleased to inform you that your password for the E-gate 2.0 system has been successfully updated.</p>
                                    <p><strong>Summary of Changes:</strong></p>
                                    <ul>
                                        <li><strong>Account:</strong> %s</li>
                                        <li><strong>Date and Time of Change:</strong> %s</li>
                                    </ul>
                                    <p>If you did not request this change, please contact our support team immediately to ensure the security of your account.</p>
                                    <p>For your protection, please avoid sharing your password with anyone and ensure it is stored securely.</p>
                                </div>
                                <div class="footer">
                                    <p>Thank you for using E-gate 2.0.</p>
                                    <p>If you have any questions or need assistance, feel free to <a href="%s">contact us</a>.</p>
                                    <p>Best regards,<br>The E-gate 2.0 Team</p>
                                </div>
                            </div>
                        </body>
                        </html>
               """
                ,user.getEmail(), LocalDate.now()+" "+ LocalTime.now(),"mailto:kce.egate@gmail.com");
        emailRequest.setRecipient(user.getEmail());
        emailRequest.setMsgBody(body);
        emailRequest.setSubject("E-gate 2.0: Your Password Has Been Successfully Updated");
        emailUtils.sendMimeMessage(emailRequest);
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
