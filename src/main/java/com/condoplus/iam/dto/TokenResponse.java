package	com.condoplus.iam.dto;

public	record	TokenResponse(
        String	token,
        long	expiresInSeconds
)	{}