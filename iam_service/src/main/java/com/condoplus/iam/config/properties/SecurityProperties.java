package	com.condoplus.iam.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix	= "condoplus.security")
@Validated

public record SecurityProperties(
        @Min(1)
        int	maxFailedAttempts,
        @Min(1)
        int	lockoutDurationMinutes
)	{}