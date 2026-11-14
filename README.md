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

Requisitos: JDK 23 ou mais novo (funciona no JDK 25) e um MongoDB. Por padrão a aplicação conecta em `mongodb://localhost:27017` (MongoDB Community instalado na máquina). Para usar o Atlas, defina a variável `MONGODB_URI`.

```bash
./mvnw spring-boot:run
```

Acesse http://localhost:8080, entre com `admin` / `thindesk` e clique em **Dashboard** no menu.

Variáveis de ambiente opcionais:

| Variável | Para quê |
|---|---|
| `MONGODB_URI` | Usar outro banco, ex.: a string de conexão de um cluster do Atlas |
| `THINDESK_SEED=true` | Inserir ~400 chamados fictícios dos últimos 120 dias (só quando a coleção tem menos de 20 registros) |

Exemplo com dados de demonstração (PowerShell):

```powershell
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
