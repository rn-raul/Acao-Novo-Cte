package br.com.semalo.hivecloud.registro;

import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import java.math.BigDecimal;
import java.sql.ResultSet;

public class ValidadorCte {
    public boolean verificarCteAutorizado(BigDecimal numControle) throws Exception {
        JdbcWrapper jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
        NativeSql sql = null;
        ResultSet rs = null;

        try {
            jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
            jdbc.openSession();

            sql = new NativeSql(jdbc);
            sql.appendSql("SELECT 1 FROM AD_HIVECLOUDCTE WHERE CODFRETE = :NUMCONTROLE AND STATUS = 'A'");
            sql.setNamedParameter("NUMCONTROLE", numControle);

            rs = sql.executeQuery();

            // Retorna true se encontrou algum registro que satisfaça a condição
            return rs.next();

        } finally {
            if (rs != null) {
                rs.close();
            }
            if (sql != null) {
                NativeSql.releaseResources(sql);
            }
            if (jdbc != null) {
                JdbcWrapper.closeSession(jdbc);
            }
        }
    }
}
