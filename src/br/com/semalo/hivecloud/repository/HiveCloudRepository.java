package br.com.semalo.hivecloud.repository;


import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Objects;

public class HiveCloudRepository {
    public JSONObject buscarDadosCte(BigDecimal numControle) throws Exception {
        JdbcWrapper jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
        NativeSql sql = null;
        ResultSet rs = null;
        try {

            sql = new NativeSql(jdbc);

            sql.appendSql("SELECT * FROM V_SEMALO_CTE_INFO ");
            sql.appendSql("WHERE NFE_NUMCONTROLE = :NUMCONTROLE");

            sql.setNamedParameter("NUMCONTROLE", numControle);

            rs = sql.executeQuery();

            JSONObject dados = new JSONObject();
            JSONArray listaChaves = new JSONArray();

            boolean primeiraLinha = true;

            while (rs.next()) {

                if (primeiraLinha) {

                    dados.put("REM_TELEFONE", rs.getString("REM_TELEFONE"));
                    dados.put("REM_INSC_FED", rs.getString("REM_INSC_FED"));
                    dados.put("REM_INSC_ESTAD", rs.getString("REM_INSC_ESTAD"));
                    dados.put("REM_RAZAOSOCIAL", rs.getString("REM_RAZAOSOCIAL"));
                    dados.put("REM_EMAIL", rs.getString("REM_EMAIL"));
                    dados.put("REM_UF", rs.getString("REM_UF"));
                    dados.put("REM_LOGRADOURO", rs.getString("REM_LOGRADOURO"));
                    dados.put("REM_NUMEND", rs.getString("REM_NUMEND"));
                    dados.put("REM_NOMEBAIRRO", rs.getString("REM_NOMEBAIRRO"));
                    dados.put("REM_NOMEMUN", rs.getString("REM_NOMEMUN"));
                    dados.put("REM_CEP", rs.getString("REM_CEP"));
                    dados.put("ICMS", rs.getBigDecimal("ICMS"));
                    dados.put("DEST_CONS_FINAL", rs.getString("DEST_CONS_FINAL"));
                    dados.put("DEST_TELEFONE", rs.getString("DEST_TELEFONE"));
                    dados.put("DEST_INSC_FED", rs.getString("DEST_INSC_FED"));
                    dados.put("DEST_INSC_ESTAD", rs.getString("DEST_INSC_ESTAD"));
                    dados.put("DEST_RAZAOSOCIAL", rs.getString("DEST_RAZAOSOCIAL"));
                    dados.put("DEST_UF", rs.getString("DEST_UF"));
                    dados.put("DEST_LOGRADOURO", rs.getString("DEST_LOGRADOURO"));
                    dados.put("DEST_NUMEND", rs.getString("DEST_NUMEND"));
                    dados.put("DEST_NOMEBAIRRO", rs.getString("DEST_NOMEBAIRRO"));
                    dados.put("DEST_NOMEMUN", rs.getString("DEST_NOMEMUN"));
                    dados.put("DEST_CEP", rs.getString("DEST_CEP"));
                    String prod = rs.getString("PROD_PREDOMINANTE");
                    dados.put("PROD_PREDOMINANTE", prod != null ? prod.trim() : "");

                    dados.put("NOTA_PESOBRUTO", rs.getBigDecimal("NOTA_PESOBRUTO"));
                    dados.put("NOTA_VLRNOTA", rs.getBigDecimal("NOTA_VLRNOTA"));
                    dados.put("NOTA_QTDNEG", rs.getBigDecimal("NOTA_QTDNEG"));
                    dados.put("NOTA_PESOLIQ", rs.getBigDecimal("NOTA_PESOLIQ"));
                    dados.put("FRETECALCULADO", rs.getBigDecimal("FRETECALCULADO"));
                    primeiraLinha = false;
                }
                String status = rs.getString("STATUS");
                if (!Objects.equals(status, "Fechado")) {
                    throw new RuntimeException("Não é possível gerar CTE com o controle de frete Aberto.");
                }
                // ================= CAPTURA CHAVES (TRATA CONCATENAÇÃO) =================
                String chavesConcatenadas = rs.getString("NFE_CHAVENFE");

                if (chavesConcatenadas != null) {

                    chavesConcatenadas = chavesConcatenadas.replaceAll("[^0-9]", "");

                    int tamanho = chavesConcatenadas.length();

                    if (tamanho % 44 != 0) {
                        throw new RuntimeException(
                                "Tamanho inválido para chaves concatenadas: "
                                        + tamanho + " -> " + chavesConcatenadas
                        );
                    }

                    for (int i = 0; i < tamanho; i += 44) {
                        String chave = chavesConcatenadas.substring(i, i + 44);
                        listaChaves.put(chave);
                    }
                }
            }

            if (listaChaves.length() == 0) {
                throw new RuntimeException("Nenhuma NF-e encontrada para o controle: " + numControle);
            }

            dados.put("LISTA_CHAVES", listaChaves);

            return dados;

        } finally {
            if (rs != null) rs.close();
            if (sql != null) NativeSql.releaseResources(sql);
        }
    }
    public void inserirControleHive(BigDecimal numeroCte, String idCte, BigDecimal numControle, BigDecimal usuario) throws Exception {

        JdbcWrapper jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

        NativeSql sql = null;
        ResultSet rs = null;
        try {

            sql = new NativeSql(jdbc);

            // Buscar próximo CODREG
            sql.appendSql("SELECT ISNULL(MAX(CODREG),0) + 1 AS PROXIMO ");
            sql.appendSql("FROM AD_HIVECLOUDCTE");

            rs = sql.executeQuery();

            int proximo = 1;

            if (rs.next()) {
                proximo = rs.getInt("PROXIMO");
            }

            NativeSql.releaseResources(sql);
            rs.close();

            // Criar registro na tabela AD_HIVECLOUDCTE
            sql = new NativeSql(jdbc);

            sql.appendSql("INSERT INTO AD_HIVECLOUDCTE (CODREG, CTE, REGCTE, CODFRETE, CODUSU, DTALTER, STATUS) ");
            sql.appendSql("VALUES (:CODREG, :CTE, :REGCTE, :CODFRETE, :CODUSU, GETDATE(), 'A')");

            sql.setNamedParameter("CODREG", proximo);
            sql.setNamedParameter("CTE", numeroCte);
            sql.setNamedParameter("REGCTE", idCte);
            sql.setNamedParameter("CODFRETE", numControle);
            sql.setNamedParameter("CODUSU", usuario);
            sql.executeUpdate();

        } finally {

            if (rs != null) rs.close();
            if (sql != null) NativeSql.releaseResources(sql);
        }
    }
}
