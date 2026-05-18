package br.com.semalo.hivecloud.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class JsonBuilder {

    private static final String INTEGRATION_ID = "aeb9f0b8-2236-400d-aa1c-0e570ae24fb0";

    public static String montarJsonCte(JSONObject d) {

        JSONObject json = new JSONObject();

        json.put("idIntegracao", INTEGRATION_ID);

        // ================= EMPRESA =================
        json.put("empresa", new JSONObject()
                .put("inscricaoFederal", "54331813000193")
                .put("inscricaoEstadual", "284965073"));

        // ================= REMETENTE =================
        JSONObject remetente = new JSONObject()
                .put("telefone", d.optString("REM_TELEFONE"))
                .put("inscricaoFederal", d.optString("REM_INSC_FED"))
                .put("inscricaoEstadual", d.optString("REM_INSC_ESTAD"))
                .put("nome", d.optString("REM_RAZAOSOCIAL"))
                .put("email", d.optString("REM_EMAIL"))
                .put("endereco", criarEndereco(
                        d.optString("REM_UF"),
                        d.optString("REM_LOGRADOURO"),
                        d.optString("REM_NUMEND"),
                        d.optString("REM_NOMEBAIRRO"),
                        d.optString("REM_NOMEMUN"),
                        d.optString("REM_CEP")
                ));

        json.put("remetente", remetente);

        // ================= DESTINATÁRIO =================
        json.put("destinatario", new JSONObject()
                .put("consumidorFinal",
                        "S".equalsIgnoreCase(d.optString("DEST_CONS_FINAL")))
                .put("telefone", d.optString("DEST_TELEFONE"))
                .put("inscricaoFederal", d.optString("DEST_INSC_FED"))
                .put("inscricaoEstadual", d.optString("DEST_INSC_ESTAD"))
                .put("nome", d.optString("DEST_RAZAOSOCIAL"))
                .put("email", d.optString("DEST_EMAIL"))
                .put("endereco", criarEndereco(
                        d.optString("DEST_UF"),
                        d.optString("DEST_LOGRADOURO"),
                        d.optString("DEST_NUMEND"),
                        d.optString("DEST_NOMEBAIRRO"),
                        d.optString("DEST_NOMEMUN"),
                        d.optString("DEST_CEP")
                )));

        // ================= TOMADOR =================
        json.put("tomador", new JSONObject()
                .put("atividade", "SERVICO")
                .put("telefone", d.optString("REM_TELEFONE"))
                .put("inscricaoFederal", d.optString("REM_INSC_FED"))
                .put("inscricaoEstadual", d.optString("REM_INSC_ESTAD"))
                .put("nome", d.optString("REM_RAZAOSOCIAL"))
                .put("email", d.optString("REM_EMAIL"))
                .put("endereco", criarEndereco(
                        d.optString("REM_UF"),
                        d.optString("REM_LOGRADOURO"),
                        d.optString("REM_NUMEND"),
                        d.optString("REM_NOMEBAIRRO"),
                        d.optString("REM_NOMEMUN"),
                        d.optString("REM_CEP")
                )));

        json.put("embutirImposto", false);

        // ================= FRETE =================
        json.put("frete", new JSONObject()
                .put("fretePeso", BigDecimal.ZERO)
                .put("freteValor", getBigDecimal(d, "FRETECALCULADO"))
                .put("outrosFretes", new JSONArray()
                        .put(new JSONObject()
                                .put("nomeFrete", "Taxa de Coleta")
                                .put("valorFrete", BigDecimal.ZERO))
                        .put(new JSONObject()
                                .put("nomeFrete", "Taxa de Entrega")
                                .put("valorFrete", BigDecimal.ZERO))
                ));

        // ================= CARGA =================
        json.put("carga", new JSONObject()
                .put("valorTotal", getBigDecimal(d, "NOTA_VLRNOTA"))
                .put("produtoPredominante", d.optString("PROD_PREDOMINANTE"))
                .put("medidas", new JSONObject()
                        .put("pesoBrutoKG", getBigDecimal(d, "NOTA_PESOBRUTO"))
                        .put("unidades", getBigDecimal(d, "NOTA_QTDNEG"))
                        .put("outrasMedidas", new JSONArray()
                                .put(new JSONObject()
                                        .put("nomeMedida", "PESO LIQUIDO")
                                        .put("valorMedida",
                                                getBigDecimal(d, "NOTA_PESOLIQ"))
                                        .put("tipoMedida", "KG")
                                )
                        )
                ));

        // ================= DOCUMENTOS =================
        json.put("documentos", new JSONObject()
                .put("chaveAcessoNFe",
                        d.getJSONArray("LISTA_CHAVES")));

        // ================= TIPO TRANSPORTE =================
        json.put("tipoTransporte", new JSONObject()
                .put("tipoTransporte", "RODOVIARIO"));

        // ================= ICMS =================
        BigDecimal aliquota = getBigDecimal(d, "ICMS");
        BigDecimal valorBaseCalculo =
                getBigDecimal(d, "FRETECALCULADO");

        BigDecimal valorICMS = valorBaseCalculo
                .multiply(aliquota)
                .divide(new BigDecimal("100"),
                        2,
                        RoundingMode.HALF_UP);

        JSONObject icmsNormal = new JSONObject()
                .put("aliquota", aliquota)
                .put("valorICMS", valorICMS)
                .put("valorBaseCalculo", valorBaseCalculo);

        json.put("sobrescritas", new JSONObject()
                .put("icms",
                        new JSONObject()
                                .put("normal", icmsNormal)));

        // ================= OBSERVAÇÃO =================
        String ufDestino =
                d.optString("DEST_UF", "")
                        .trim()
                        .toUpperCase();

        String observacao =
                (!ufDestino.equals("MS"))
                        ? "CREDITO PRESUMIDO CONFI CONV ICMS 106/96"
                        : "CT-e gerado automaticamente via integração Sankhya.";

        json.put("observacaoGeral", observacao);

        json.put("observacaoIntegracaoContribuinte",
                new JSONArray()
                        .put(new JSONObject()
                                .put("campo",
                                        "Lei da Transparência")
                                .put("texto",
                                        "O valor aproximado de tributos incidentes sobre o preço deste serviço é de R$ 0.0")
                        ));

        return json.toString();
    }

    private static JSONObject criarEndereco(String uf,
                                            String logradouro,
                                            String numero,
                                            String bairro,
                                            String municipio,
                                            String cep) {

        return new JSONObject()
                .put("uf", uf)
                .put("logradouro", logradouro)
                .put("numero", numero)
                .put("bairro", bairro)
                .put("municipio", municipio)
                .put("cep", cep);
    }

    private static BigDecimal getBigDecimal(JSONObject json,
                                            String key) {

        Object obj = json.opt(key);

        if (obj == null)
            return BigDecimal.ZERO;

        if (obj instanceof BigDecimal)
            return (BigDecimal) obj;

        if (obj instanceof Number)
            return BigDecimal.valueOf(
                    ((Number) obj).doubleValue());

        return new BigDecimal(obj.toString());
    }
}