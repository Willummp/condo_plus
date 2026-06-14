package	com.condoplus.iam.exception;

import	java.util.UUID;
public	class	CredencialNaoEncontradaException	extends	RuntimeException	{
    public	CredencialNaoEncontradaException(UUID	id)	{
        super("Credencial	não	encontrada:	"	+	id);
    }
}