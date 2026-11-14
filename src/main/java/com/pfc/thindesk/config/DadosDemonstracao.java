package com.pfc.thindesk.config;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.StatusChamado;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

/**
 * Popula a coleção com chamados fictícios para demonstrar o dashboard.
 *
 * Só roda com thindesk.seed.enabled=true e quando a coleção tem poucos registros,
 * para nunca misturar dados de teste com uma base já em uso.
 */
@Component
@Order(3)
@ConditionalOnProperty(name = "thindesk.seed.enabled", havingValue = "true")
public class DadosDemonstracao implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracao.class);
    private static final int LIMITE_BASE_VAZIA = 20;
    private static final int QUANTIDADE = 400;
    private static final int DIAS = 120;

    private static final String[][] TIPOS = {
            {"Rede", "Sem acesso à internet na estação", "Queda de conexão no Wi-Fi do setor", "Rede lenta no período da tarde"},
            {"Impressora", "Impressora não imprime", "Papel enroscando com frequência", "Instalar impressora no computador novo"},
            {"Acesso e senha", "Senha do sistema bloqueada", "Liberar acesso à pasta compartilhada", "Criar usuário para colaborador novo"},
            {"Instalação de software", "Instalar pacote Office", "Atualizar o leitor de PDF", "Instalar sistema de folha de pagamento"},
            {"Hardware", "Computador não liga", "Monitor piscando", "Teclado com teclas falhando"},
            {"Manutenção", "Limpeza preventiva do computador", "Troca de fonte", "Computador muito lento"},
            {"E-mail", "Não recebe e-mails externos", "Caixa de e-mail cheia", "Configurar e-mail no celular"},
    };
    // Peso de cada tipo (Rede e Acesso aparecem mais, como num suporte real)
    private static final int[] PESO_TIPO = {22, 14, 20, 12, 10, 12, 10};
    private static final String[] TECNICOS = {"João", "Mariana", "Carlos", "Fernanda", "Rafael"};
    private static final String[] USUARIOS = {"Alberto", "Beatriz", "Camila", "Diego", "Eduarda", "Felipe",
            "Gabriela", "Henrique", "Isabela", "Lucas", "Marcos", "Natália", "Otávio", "Paula"};
    // Considerações técnicas na mesma ordem de TIPOS
    private static final String[][] CONSIDERACOES = {
            {"Cabo de rede substituído.", "Roteador do setor reiniciado e canal do Wi-Fi alterado."},
            {"Driver reinstalado e teste de impressão realizado.", "Rolete de tração limpo."},
            {"Senha redefinida; usuário orientado a trocar no primeiro acesso.", "Permissão concedida após aprovação do gestor."},
            {"Instalação concluída e licença ativada.", "Software atualizado para a versão homologada."},
            {"Fonte substituída; equipamento em observação.", "Periférico trocado por um do estoque."},
            {"Limpeza interna e troca de pasta térmica.", "Arquivos temporários removidos e disco verificado."},
            {"Regra de spam ajustada no servidor.", "Conta configurada e testada com o usuário."},
    };

    private final MongoTemplate mongoTemplate;

    public DadosDemonstracao(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        long existentes = mongoTemplate.count(new Query(), Chamado.class);
        if (existentes >= LIMITE_BASE_VAZIA) {
            log.info("Seed ignorado: a coleção já tem {} chamados.", existentes);
            return;
        }
        List<Chamado> chamados = gerar(new Random(42), LocalDateTime.now());
        mongoTemplate.insertAll(chamados);
        log.info("Seed: {} chamados de demonstração inseridos.", chamados.size());
    }

    static List<Chamado> gerar(Random r, LocalDateTime agora) {
        List<Chamado> lista = new ArrayList<>();
        for (int i = 0; i < QUANTIDADE; i++) {
            int indiceTipo = sortearTipo(r);
            String[] tipo = TIPOS[indiceTipo];
            LocalDateTime abertura = agora.minusDays(r.nextInt(DIAS))
                    .withHour(8 + r.nextInt(10)).withMinute(r.nextInt(60)).withSecond(0).withNano(0);
            if (abertura.isAfter(agora)) {
                abertura = agora.minusHours(1 + r.nextInt(5));
            }

            String prioridade = switch (r.nextInt(10)) {
                case 0, 1 -> "Alta";
                case 2, 3, 4, 5 -> "Média";
                default -> "Baixa";
            };

            Chamado c = new Chamado();
            c.setTipo(tipo[0]);
            c.setDescricao(tipo[1 + r.nextInt(tipo.length - 1)]);
            c.setUsuario(USUARIOS[r.nextInt(USUARIOS.length)]);
            c.setTecnico(TECNICOS[r.nextInt(TECNICOS.length)]);
            c.setPrioridade(prioridade);
            c.setDataAbertura(abertura);

            // Quanto mais antigo, maior a chance de já estar concluído
            long idadeDias = java.time.Duration.between(abertura, agora).toDays();
            double chanceConcluido = idadeDias > 14 ? 0.96 : idadeDias > 3 ? 0.75 : 0.3;
            double sorteio = r.nextDouble();
            StatusChamado status;
            if (sorteio < 0.04) {
                status = StatusChamado.CANCELADO;
            } else if (sorteio < chanceConcluido) {
                status = StatusChamado.CONCLUIDO;
            } else if (r.nextBoolean()) {
                status = StatusChamado.EM_ANDAMENTO;
            } else {
                status = StatusChamado.ABERTO;
            }
            c.setStatus(status.getRotulo());

            if (status == StatusChamado.CONCLUIDO) {
                int horasMax = prioridade.equals("Alta") ? 12 : prioridade.equals("Média") ? 40 : 96;
                LocalDateTime fechamento = abertura.plusHours(1 + r.nextInt(horasMax)).plusMinutes(r.nextInt(60));
                if (fechamento.isAfter(agora)) {
                    fechamento = agora.minusMinutes(5);
                }
                c.setDataFechamento(fechamento);
                c.setDataAtualizacao(fechamento);
                c.setConsideracoesTecnicas(CONSIDERACOES[indiceTipo][r.nextInt(2)]);
            } else if (status == StatusChamado.EM_ANDAMENTO) {
                c.setDataAtualizacao(abertura.plusHours(1));
                c.setConsideracoesTecnicas("Em análise pelo técnico.");
            } else {
                c.setDataAtualizacao(abertura);
            }
            lista.add(c);
        }
        return lista;
    }

    private static int sortearTipo(Random r) {
        int total = 0;
        for (int p : PESO_TIPO) total += p;
        int alvo = r.nextInt(total);
        for (int i = 0; i < PESO_TIPO.length; i++) {
            alvo -= PESO_TIPO[i];
            if (alvo < 0) return i;
        }
        return 0;
    }
}
