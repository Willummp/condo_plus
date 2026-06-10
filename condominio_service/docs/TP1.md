# 📚 Documentação de Estudos Profunda — TP1 (Fundação, Integração e Resiliência)

Este guia foi elaborado para ser o seu **material de estudo principal**. Ele não apenas diz *o que* foi feito, mas explica **o que é a tecnologia, por que ela existe e qual problema resolve** antes de mostrar onde ela está no código.

---

## 1. Arquitetura: Service Discovery (Eureka) e API Gateway

### O que são essas tecnologias?
Em um sistema monolítico (tudo no mesmo projeto), as partes do código conversam entre si chamando funções. Em **Microsserviços**, os pedaços do sistema estão rodando em máquinas diferentes e precisam se comunicar pela rede (internet).

- **Service Registry (Eureka):** Imagine que os microsserviços mudam de endereço IP o tempo todo (quando escalam ou reiniciam). O Eureka funciona como uma **Lista Telefônica** dinâmica. Quando o `condominio-service` liga, ele avisa o Eureka: *"Oi, eu sou o condominio-service e estou no IP 192.168.0.5"*. Se outro serviço precisar falar com ele, pergunta ao Eureka, sem precisar decorar IPs.
- **API Gateway:** É o **"Porteiro do Prédio"**. O cliente (usuário, app mobile) nunca conversa diretamente com os microsserviços. Ele sempre bate no Gateway. O Gateway olha a requisição, valida a segurança e diz: *"Ah, você quer ver condomínios? O Eureka me disse que isso fica no IP 192.168.0.5, vou encaminhar você"*.

📍 **Onde encontrar no código:**
- **Eureka Server:** [application.yml](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/infra/eureka-server/src/main/resources/application.yml)
- **API Gateway:** Roteamentos em [application.yml](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/infra/api-gateway/src/main/resources/application.yml)

---

## 2. Spring Data JDBC vs JPA (Domain-Driven Design)

### O que é o JPA (Hibernate)? E qual o problema dele?
O JPA é o padrão de mercado para ligar objetos Java ao Banco de Dados Relacional. Ele é famoso por fazer "mágica".
- **A Mágica do Lazy Loading:** Se você tem um objeto `Condominio` e pede a lista de `Moradores`, o JPA pausa seu código invisivelmente, vai até o banco, faz um `SELECT` e te devolve os moradores. Isso é confortável, mas gera o famoso erro `LazyInitializationException` se a conexão com o banco já tiver sido fechada.
- **A Mágica do Dirty Checking:** No JPA, se você mudar o nome de um Morador, você não precisa avisar o banco de dados. O JPA "percebe" a mudança na memória e salva automaticamente no fim da transação. Isso deixa o código imprevisível.

### Por que escolhemos o Spring Data JDBC?
O **Spring Data JDBC** é propositalmente "burro". Ele não tem mágica. Ele foi criado para forçar desenvolvedores a seguirem o **DDD (Domain-Driven Design)**. No DDD, temos a regra do "Agregado": uma Unidade e suas Vinculações de moradores são tratadas como um único bloco inseparável.
Se você quiser salvar algo no JDBC, você **tem que chamar** o comando `save()` explicitamente. Se você puxar uma Unidade, ela já vem com todos os dados de uma vez. O código fica óbvio, sem surpresas e extremamente rápido.

📍 **Onde encontrar no código:**
- **Mapeamento:** Em [Unidade.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/estrutura/domain/Unidade.java) (`@MappedCollection`).
- **Salvamento Manual:** Nos Controllers ou Services, é obrigatório chamar `unidadeRepository.save()`.

---

## 3. Resilience4j (Tolerância a Falhas)

### O que é Tolerância a Falhas e Circuit Breaker?
Em microsserviços, se o Serviço A chama o Serviço B, e o Serviço B cai, o Serviço A vai ficar esperando eternamente. Isso gera um engarrafamento que derruba a rede inteira (efeito cascata).
A biblioteca **Resilience4j** implementa o padrão de **Circuit Breaker (Disjuntor)**.
- **Circuito Fechado (Normal):** A energia passa. As requisições funcionam.
- **Circuito Aberto (Falha):** Se o Serviço B der erro 5 vezes seguidas, o disjuntor "desarma" (Abre). A partir daí, o Serviço A nem tenta mais chamar o Serviço B, ele imediatamente retorna um erro (ou um comportamento padrão chamado de **Fallback**). Isso dá tempo para o Serviço B se recuperar sem ser bombardeado de requisições.
- **Time Limiter:** Limita quanto tempo o Serviço A aceita esperar (ex: máximo de 2 segundos). Passou disso, é cancelado.

📍 **Onde encontrar no código:**
- **As regras:** No [application.yml](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/resources/application.yml) (configure tempo, % de erros).
- **A execução:** Na classe [IamClient.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/estrutura/client/IamClient.java) (`@CircuitBreaker` e método `fallback`).

---

## 4. Segurança Stateless (Sem Estado) com JWT

### O que é Stateful e Stateless?
- **Stateful (Como era antes):** Você faz login e o servidor anota no próprio caderno (memória) dele: *"O João está logado"*. Se você for redirecionado para um Servidor B que não tem esse caderno, ele te expulsa.
- **Stateless (Aplicações Modernas):** O servidor **sofre de amnésia**. Ele não guarda quem está logado. Ao invés disso, no momento do login ele te dá um **Token JWT** (uma string enorme com seus dados assinados criptograficamente). Em **toda** requisição, você envia esse Token. O Gateway confere a criptografia, vê que é válido, extrai que você é "Síndico" e injeta isso no Cabeçalho (`Header`) da requisição interna. O `condominio-service` recebe a requisição, lê o header e confia. A aplicação torna-se infinitamente escalável, pois não depende de memória.

📍 **Onde encontrar no código:**
- Em [SecurityConfig.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/config/SecurityConfig.java), um Filtro lê `request.getHeader("X-User-Id")` e autoriza o usuário temporariamente só enquanto aquela requisição durar.

---

## 5. Nível de Isolamento do Banco: SERIALIZABLE

### Qual é o problema de concorrência?
Imagine duas pessoas tentando reservar a churrasqueira no mesmo dia, apertando o botão no milissegundo exato.
Ambas as requisições perguntam ao banco: "Está livre?". O banco, por padrão (nível *Read Committed*), é rápido mas não vigia o futuro. Ele diz "Sim" para as duas. Ambas gravam. O resultado são duas pessoas brigando pela churrasqueira. É o que chamamos de **Condição de Corrida (Race Condition)**.

### O que o SERIALIZABLE faz?
É o nível de segurança máxima de um banco relacional (PostgreSQL). Ele instrui o banco a agir como se houvesse uma "fila indiana" (serialização). Se o banco detectar que duas transações concorrentes tentaram escrever dados que gerariam sobreposição, ele **aborta intencionalmente** uma delas e joga um erro. O nosso código captura o erro e avisa o segundo usuário: "Opa, o horário acabou de ser pego!".

📍 **Onde encontrar no código:**
- No [ReservaService.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/convivencia/service/ReservaService.java), com a anotação `@Transactional(isolation = Isolation.SERIALIZABLE)` no método `criar()`.

---

## 6. Testcontainers

### O que é?
Para testes de integração, muitos projetos usam um "banco falso" na memória RAM (H2 Database). Mas um banco falso não entende o `SERIALIZABLE` do PostgreSQL perfeitamente. O código passa no teste local e falha no servidor de produção.
A biblioteca **Testcontainers** fala diretamente com o Docker do seu computador. Quando você aperta play nos testes, o Java manda o Docker "subir" um servidor PostgreSQL real. Os testes executam gravando os dados nesse banco real. Terminou os testes? O Java manda o Docker destruir e apagar tudo. Teste 100% fiel à realidade.

📍 **Onde encontrar no código:**
- Em [UnidadeRepositoryIT.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/test/java/com/condoplus/condominio/estrutura/repository/UnidadeRepositoryIT.java) (`@Testcontainers` e `@Container static PostgreSQLContainer`).
