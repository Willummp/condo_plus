package	com.condoplus.iam.exception;
public	class	EmailJaExisteException	extends	RuntimeException	{
    public	EmailJaExisteException(String	email)	{
        super("E-mail	já	cadastrado:	"	+	email);
    }
}
