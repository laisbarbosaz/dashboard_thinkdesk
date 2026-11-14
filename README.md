# Thindesk — Dashboard Administrativo

Sistema de chamados técnicos (Spring Boot + MongoDB + Thymeleaf) com um dashboard administrativo alimentado por uma API REST própria.

A documentação completa (requisitos, endpoints, testes e decisões de projeto) está em **[docs/DOCUMENTACAO.md](docs/DOCUMENTACAO.md)** e, no formato ABNT, em **[docs/Documentacao_Thindesk_ABNT.pdf](docs/Documentacao_Thindesk_ABNT.pdf)**.

![Dashboard](docs/evidencias/01-dashboard-geral.png)

## O que tem no dashboard

- Indicadores gerais: total, abertos, em andamento, concluídos, cancelados, taxa de resolução, tempo médio e pendentes fora do prazo
- Gráfico de chamados abertos x concluídos por dia, semana ou mês
- Situação dos chamados e ranking dos tipos mais atendidos
- Atendimento por técnico e lista de chamados com as considerações técnicas
- Filtros por período, status, tipo e técnico, com atualização automática a cada minuto

## Como rodar

Requisitos: JDK 23 ou mais novo (funciona no JDK 25) e um MongoDB. Crie o arquivo local de configuração a partir do exemplo:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Defina uma senha de administrador antes de iniciar. No PowerShell:

```powershell
$env:THINDESK_ADMIN_PASSWORD = "defina-uma-senha-forte"
.\mvnw.cmd spring-boot:run
```

A aplicação conecta por padrão em `mongodb://localhost:27017`. Para usar o Atlas, defina `MONGODB_URI`. Acesse http://localhost:8080, entre com o usuário `admin` (ou o valor de `THINDESK_ADMIN_USERNAME`) e a senha definida em `THINDESK_ADMIN_PASSWORD`, depois clique em **Dashboard** no menu.

Variáveis de ambiente:

| Variável | Para quê |
|---|---|
| `THINDESK_ADMIN_PASSWORD` | Senha obrigatória para autenticação do administrador |
| `THINDESK_ADMIN_USERNAME` | Usuário administrador (padrão: `admin`) |
| `MONGODB_URI` | URI de conexão do MongoDB (padrão: `mongodb://localhost:27017`) |
| `THINDESK_SEED=true` | Inserir ~400 chamados fictícios dos últimos 120 dias (só quando a coleção tem menos de 20 registros) |

Exemplo com dados de demonstração (PowerShell):

```powershell
$env:THINDESK_ADMIN_PASSWORD = "defina-uma-senha-forte"
$env:THINDESK_SEED="true"
.\mvnw.cmd spring-boot:run
```

## API

| Endpoint | Descrição |
|---|---|
| `GET /api/dashboard/resumo` | Indicadores gerais |
| `GET /api/dashboard/periodo` | Abertos e concluídos por dia/semana/mês |
| `GET /api/dashboard/status` | Quantidade por situação |
| `GET /api/dashboard/tipos` | Ranking de tipos |
| `GET /api/dashboard/tecnicos` | Atendimento por técnico |
| `GET /api/dashboard/chamados` | Lista paginada |
| `GET /api/dashboard/filtros` | Opções para os filtros |

Todos aceitam `dataInicio`, `dataFim` (AAAA-MM-DD), `status`, `tipo`, `tecnico` e `prioridade`. Detalhes, exemplos e erros na [documentação](docs/DOCUMENTACAO.md#6-documentação-dos-endpoints). A coleção do Postman está em `docs/thindesk-dashboard.postman_collection.json`.

## Testes

```bash
./mvnw test
```

## Branches (gitflow)

`main` · `develop` · `feature/api-dashboard` · `feature/dashboard-web` · `feature/documentacao` · `feature/documentacao-abnt` · `release/1.1.0` · `release/1.2.0` · `hotfix/*`
