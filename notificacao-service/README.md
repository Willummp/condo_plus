# Documentação - TP1

Integrantes do Grupo:
- Rodolpho da Fonseca Rodrigues
- Lucas Ferreira
- Nicholas
- Fernando
- Caio

Turma: GRPENGR2C1-M1-P1
Entrega: Grupo (5 integrantes)

- Link do repositório:
  https://github.com/Willummp/condo_plus

1. Tema do trabalho:
   Sistema de gerenciamento de condomínio (Condo+).

2. Problema que o sistema resolve:
   Controlar acessos de portaria, registrar eventos do condomínio, enviar notificações, gerenciar funcionários, moradores e entregas de encomendas.

3. Usuários principais:
   Moradores, síndicos e funcionários do condomínio.

4. Principais funcionalidades:
   Cadastro de moradores, controle de entrada e saída na portaria, logs de auditoria e envio de avisos por e-mail, whatsapp, push e/ou app.

5. Justificativa para uso de microsserviço:
   Separar notificações do resto do sistema é importante porque assim como outros microsserviços do sistema, recebe muitos acessos ao mesmo tempo. Se o envio de avisos ficar lento ou cair por falha na rede externa, a portaria não trava e os moradores continuam conseguindo entrar e sair do condomínio por exemplo. Também permite usar bancos de dados diferentes conforme a necessidade de cada serviço.


Detalhamento do microsserviço: notificacao-service

- Responsabilidade principal:
  Receber pedidos de aviso, verificar como o morador quer receber notificacoes (preferencias de canal) e fazer o disparo de mensagens. Os pontos de entrada REST ja contam com anotacoes de seguranca do Spring Security para exigir autenticacao obrigatoria baseada no Token do usuario.

- Por que e um servico separado:
  O envio de mensagens depende de internet e servicos de terceiros que podem falhar ou demorar. Deixando isolado, o resto do sistema fica protegido de lentidao.

- Dados manipulados:
  Notificacao (ID, ID do destinatario, tipo de aviso, titulo, corpo da mensagem, status do envio, numero de tentativas e logs de erros).
  PreferenciaNotificacao (ID do morador, tipo de evento, canal escolhido e data da ultima atualizacao).

- Endpoints implementados (TP1):
  GET /notificacoes/minhas (buscar histórico de notificações do morador logado)
  POST /notificacoes/{id}/retry (solicitar "retentativa" manual de envio - restrito a Admin/Sindico)
  GET /notificacoes/preferencias (listar canais preferidos do usuário logado)
  PUT /notificacoes/preferencias (atualizar ou cadastrar os canais escolhidos)

- Banco de dados:
  PostgreSQL com Spring Data R2DBC. Escolhemos o R2DBC para trabalhar de forma reativa e não-bloqueante, o que ajuda o sistema a processar muitos envios ao mesmo tempo,
  gastando menos memoria do servidor. No modelo do grupo, o banco nao-relacional ficou sob responsabilidade do serviço de auditoria.