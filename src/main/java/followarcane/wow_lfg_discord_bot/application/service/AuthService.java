package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AuthService {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private final DiscordService discordService;

    public AuthService(DiscordService discordService) {
        this.discordService = discordService;
    }

    public String generateToken(String discordId) {
        User user = discordService.getUserByDiscordId(discordId);
        return Jwts.builder()
                .setSubject(user.getDiscordId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(SignatureAlgorithm.HS512, jwtSecret.getBytes())
                .compact();
    }

    public String getUserDiscordIdFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(jwtSecret.getBytes())
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
