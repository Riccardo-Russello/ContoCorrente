package it.russello.contocorrente.controller;

import it.russello.contocorrente.dto.LoginRequest;
import it.russello.contocorrente.dto.LoginResponse;
import it.russello.contocorrente.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final String issuer;

    public AuthController(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder, @Value("${security.jwt.issuer}")String issuer){
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request){
        if(request.username() == null || request.username().isBlank()
            || request.password() == null || request.password().isBlank()){
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LOGIN_DATA", "Username e password obbligatori");
        }
        String username;
        try{
            var authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username(),
                            request.password()));
            username = authentication.getName();
        }catch(AuthenticationException e){
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Username o password non corretti");
        }
        Instant now = Instant.now();
        long expiresIn = 3600;
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(username)
                .issuedAt(now).expiresAt(now.plusSeconds(expiresIn)).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", expiresIn);
    }

}
