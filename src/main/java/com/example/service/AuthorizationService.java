package com.example.service;

import java.util.Date;
import java.util.HashMap;

import org.springframework.stereotype.Service;
import java.text.ParseException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class AuthorizationService {

	static RSAKey rsaJWKPublic = null;
	static RSAKey rsaJWK = null;
	
	
	public String generateToken() throws JOSEException
	{
		try {
            rsaJWK = new RSAKeyGenerator(2048).keyID("456").generate();
            rsaJWKPublic = rsaJWK.toPublicJWK();
        } catch (JOSEException e) {
            e.printStackTrace();
        }
		JWSSigner signer = new RSASSASigner(rsaJWK);
		JWTClaimsSet claimSet = new JWTClaimsSet.Builder().expirationTime(new Date(System.currentTimeMillis()*1000*60*2)).build();
		
		SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(), claimSet);
		signedJWT.sign(signer);
		return signedJWT.serialize();
	}
	
	public HashMap<String, Boolean> authorize(String authorization)
	{
		HashMap<String, Boolean> authorizationStatus = new HashMap<String, Boolean>();
		if(authorization == null || authorization.isEmpty())
		{
			authorizationStatus.put("NO_TOKEN_FOUND", false);
			return authorizationStatus;
		}
		if(!authorization.contains("Bearer"))
		{
			authorizationStatus.put("INVALID_FORMAT", false);
			return authorizationStatus;
		}
        String token = authorization.split(" ")[1];

		try {
			SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new RSASSAVerifier(rsaJWKPublic);

            if (!signedJWT.verify(verifier))
            {
            	authorizationStatus.put("INVALID_TOKEN", false);
            	return authorizationStatus;
            }

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (new Date().after(expirationTime)) 
            {
            	authorizationStatus.put("TOKEN_EXPIRED", false);
                return authorizationStatus;
            }
		}
		catch(JOSEException | ParseException e) {
			authorizationStatus.put("INVALID_TOKEN", false);
			return authorizationStatus;
		}
		catch(NullPointerException n)
		{
			authorizationStatus.put("INVALID_TOKEN", false);
			return authorizationStatus;
		}
		authorizationStatus.put("VALID_TOKEN", true);
		return authorizationStatus;
	}
}