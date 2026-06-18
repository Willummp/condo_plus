package	com.condoplus.iam.dto;

import com.condoplus.iam.domain.TipoRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record NovaCredencialRequest(
        @NotBlank @Email @Size(max = 320)
        String email,
        @NotBlank @Size(min	= 8, max = 128)
        String senha,
        @NotEmpty
        Set<TipoRole> roles
)	{}