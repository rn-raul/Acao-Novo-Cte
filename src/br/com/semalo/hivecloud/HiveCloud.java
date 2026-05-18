package br.com.semalo.hivecloud;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.semalo.hivecloud.client.HiveCloudClient;
import br.com.semalo.hivecloud.model.ApiResponse;
import br.com.semalo.hivecloud.repository.HiveCloudRepository;
import br.com.semalo.hivecloud.service.JsonBuilder;
import br.com.semalo.hivecloud.registro.ValidadorCte;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;


public class HiveCloud implements AcaoRotinaJava {
    private final HiveCloudRepository repository = new HiveCloudRepository();
    private final HiveCloudClient hiveCloudClient = new HiveCloudClient();
    private final ValidadorCte validadorCte = new ValidadorCte();
    @Override
    public void doAction(ContextoAcao ctx) {
        Registro[] linhas = ctx.getLinhas();
        BigDecimal usuario = ctx.getUsuarioLogado();
        if (linhas == null || linhas.length == 0) {
            ctx.setMensagemRetorno("Selecione um Controle de Frete");
            return;
        }

        StringBuilder log = new StringBuilder();

        for (Registro linha : linhas) {

            try {

                BigDecimal numControle = (BigDecimal) linha.getCampo("NUMCONTROLE");

                if (numControle == null) {
                    throw new RuntimeException("NUMCONTROLE não informado.");
                }

                boolean cteJaAutorizado = validadorCte.verificarCteAutorizado(numControle);

                if (cteJaAutorizado) {
                    throw new RuntimeException("Já possui um CT-e emitido e autorizado para esse controle de frete.");
                }
                // ================= BUSCA DADOS =================
                JSONObject dados = repository.buscarDadosCte(numControle);

                // ================= MONTA JSON =================
                String jsonCriacao = JsonBuilder.montarJsonCte(dados);

                // ================= 1º CHAMADA - CRIAR =================

                ApiResponse criacao = hiveCloudClient.enviarParaHive(jsonCriacao, "/cte");

                if (criacao.status != 201) {
                    throw new RuntimeException(
                            "Erro ao criar CT-e: HTTP "
                                    + criacao.status + " - " + criacao.body);
                }

                JSONObject respostaCriacao = new JSONObject(criacao.body);

                String idCte = respostaCriacao.getString("id");
                int numeroCte = respostaCriacao.getInt("numero");

                // ================= 2º CHAMADA - EMITIR =================
                JSONObject emitirJson = new JSONObject()
                        .put("averbarCte", true)
                        .put("idList", new JSONArray().put(idCte))
                        .put("enviarEmail", false);

                ApiResponse emissao = hiveCloudClient.enviarParaHive(
                        emitirJson.toString(),
                        "/ctes/emitir"
                );

                if (emissao.status != 200 && emissao.status != 201) {
                    throw new RuntimeException(
                            "Erro ao emitir CT-e: HTTP "
                                    + emissao.status + " - " + emissao.body);
                }
                Thread.sleep(11000); // espera 11 segundos para garantir que o CT-e esteja processado antes de tentar imprimir
                // ================= 3º CHAMADA - IMPRIMIR DOCUMENTO =================
                JSONObject imprimirJson = new JSONObject()
                        .put("idCteList", new JSONArray().put(idCte))
                        .put("ordenarPorIntegracao", true);

                ApiResponse impressao = hiveCloudClient.enviarParaHive(
                        imprimirJson.toString(),
                        "/ctes/imprimir-documento-cte"
                );

                if (impressao.status != 200 && impressao.status != 201) {
                    throw new RuntimeException(
                            "Erro ao imprimir CT-e: HTTP "
                                    + impressao.status + " - " + impressao.body);
                }
                JSONObject respPrint = new JSONObject(impressao.body);
                String urlDownload = respPrint.optString("url");
                repository.inserirControleHive(
                        BigDecimal.valueOf(numeroCte),
                        idCte,
                        numControle,
                        usuario
                );
                // ================= SUCESSO =================
                log.append("CT-e emitido com sucesso!\n")
                        .append("Número: ")
                        .append(numeroCte)
                        .append("\n")
                        .append("<a href='")
                        .append(urlDownload)
                        .append("' target='_blank'>Clique para baixar o PDF do CTE.</a>")
                        .append("\n")
                        .append("Obs: Não esqueça de emitir o MDF-e no Portal.");


            } catch (Exception e) {

                log.append("Erro no controle ")
                        .append(linha.getCampo("NUMCONTROLE"))
                        .append(":\n")
                        .append(e.getMessage())
                        .append("\n\n");
            }
        }
        ctx.setMensagemRetorno(log.toString());
    }
}