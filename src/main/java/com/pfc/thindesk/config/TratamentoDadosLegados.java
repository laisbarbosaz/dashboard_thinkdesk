package com.pfc.thindesk.config;

import com.pfc.thindesk.entity.StatusChamado;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Corrige registros antigos da coleção "chamados" ao iniciar a aplicação (RF07).
 *
 * - campo "Tipo" (com T maiúsculo) é renomeado para "tipo";
 * - status gravados em formatos diferentes ("ABERTO", "EM_ANDAMENTO") passam a usar o rótulo oficial;
 * - chamados sem data de abertura recebem a data de criação do próprio ObjectId.
 *
 * É seguro rodar várias vezes: só altera o que ainda estiver fora do padrão.
 */
@Component
@Order(2)
public class TratamentoDadosLegados implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TratamentoDadosLegados.class);
    private static final String COLECAO = "chamados";

    private final MongoTemplate mongoTemplate;

    public TratamentoDadosLegados(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            long renomeados = mongoTemplate.updateMulti(
                    new Query(Criteria.where("Tipo").exists(true).and("tipo").exists(false)),
                    new Update().rename("Tipo", "tipo"), COLECAO).getModifiedCount();

            int statusCorrigidos = 0;
            int datasPreenchidas = 0;
            for (Document doc : mongoTemplate.getCollection(COLECAO).find()) {
                Object id = doc.get("_id");
                Update update = new Update();
                boolean alterar = false;

                String status = doc.getString("status");
                StatusChamado normalizado = StatusChamado.normalizar(status);
                if (normalizado != null && !normalizado.getRotulo().equals(status)) {
                    update.set("status", normalizado.getRotulo());
                    statusCorrigidos++;
                    alterar = true;
                }

                if (doc.get("dataAbertura") == null && id instanceof ObjectId oid) {
                    update.set("dataAbertura",
                            LocalDateTime.ofInstant(oid.getDate().toInstant(), ZoneId.systemDefault()));
                    datasPreenchidas++;
                    alterar = true;
                }

                if (alterar) {
                    mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), update, COLECAO);
                }
            }

            if (renomeados + statusCorrigidos + datasPreenchidas > 0) {
                log.info("Tratamento de dados: {} campo(s) 'Tipo' renomeado(s), {} status corrigido(s), "
                        + "{} data(s) de abertura preenchida(s).", renomeados, statusCorrigidos, datasPreenchidas);
            }
        } catch (Exception e) {
            // Não impede a aplicação de subir; o dashboard continua normalizando na leitura
            log.warn("Não foi possível tratar os dados legados de chamados: {}", e.getMessage());
        }
    }
}
