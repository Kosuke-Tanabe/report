package utils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import constants.JpaConst;

public class DBUtil {

    private static EntityManagerFactory emf;

    // EntityManagerインスタンスを生成
    public static EntityManager createEntityManager() {
        return _geEntityManagerFactory().createEntityManager();
    }

    // EntityManagerFactoryインスタンスを生成
    private static EntityManagerFactory _geEntityManagerFactory() {
        if (emf == null) {
            emf = Persistence.createEntityManagerFactory(JpaConst.PERSISTENCE_UNIT_NAME);
        }
        return null;
    }
}
