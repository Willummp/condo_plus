package	com.condoplus.iam.dto;

import com.condoplus.iam.domain.StatusCredencial;
import jakarta.validation.constraints.NotNull;

public record AlteracaoStatusRequest(
        @NotNull
        StatusCredencial novoStatus,
        String	motivo
)	{}