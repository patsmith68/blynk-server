package cc.blynk.server.db;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.web.Organization;
import cc.blynk.server.db.dao.CloneProjectDBDao;
import cc.blynk.server.db.dao.FlashedTokensDBDao;
import cc.blynk.server.db.dao.ForwardingTokenDBDao;
import cc.blynk.server.db.dao.InvitationTokensDBDao;
import cc.blynk.server.db.dao.OrganizationDBDao;
import cc.blynk.server.db.dao.PurchaseDBDao;
import cc.blynk.server.db.dao.RedeemDBDao;
import cc.blynk.server.db.dao.UserDBDao;
import cc.blynk.server.db.model.FlashedToken;
import cc.blynk.server.db.model.InvitationToken;
import cc.blynk.server.db.model.Purchase;
import cc.blynk.server.db.model.Redeem;
import cc.blynk.utils.properties.BaseProperties;
import cc.blynk.utils.properties.DBProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static cc.blynk.utils.properties.DBProperties.DB_PROPERTIES_FILENAME;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.02.16.
 */
public final class DBManager implements Closeable {

    private static final Logger log = LogManager.getLogger(DBManager.class);
    private final HikariDataSource ds;

    private final BlockingIOProcessor blockingIOProcessor;

    public final UserDBDao userDBDao;
    public final OrganizationDBDao organizationDBDao;
    public final ForwardingTokenDBDao forwardingTokenDBDao;
    final CloneProjectDBDao cloneProjectDBDao;
    private final InvitationTokensDBDao invitationTokensDBDao;
    private final RedeemDBDao redeemDBDao;
    private final PurchaseDBDao purchaseDBDao;
    private final FlashedTokensDBDao flashedTokensDBDao;

    public DBManager(BlockingIOProcessor blockingIOProcessor) {
        this(DB_PROPERTIES_FILENAME, blockingIOProcessor);
    }

    public DBManager(String propsFilename, BlockingIOProcessor blockingIOProcessor) {
        this.blockingIOProcessor = blockingIOProcessor;

        DBProperties dbProperties = new DBProperties(propsFilename);
        HikariConfig config = initConfig(dbProperties);

        log.info("DB url : {}", config.getJdbcUrl());
        log.info("DB user : {}", config.getUsername());
        log.info("Connecting to DB...");

        HikariDataSource hikariDataSource = new HikariDataSource(config);

        this.ds = hikariDataSource;
        this.userDBDao = new UserDBDao(hikariDataSource);
        this.organizationDBDao = new OrganizationDBDao(hikariDataSource);
        this.redeemDBDao = new RedeemDBDao(hikariDataSource);
        this.purchaseDBDao = new PurchaseDBDao(hikariDataSource);
        this.flashedTokensDBDao = new FlashedTokensDBDao(hikariDataSource);
        this.invitationTokensDBDao = new InvitationTokensDBDao(hikariDataSource);
        this.cloneProjectDBDao = new CloneProjectDBDao(hikariDataSource);
        this.forwardingTokenDBDao = new ForwardingTokenDBDao(hikariDataSource);

        checkDBVersion();

        log.info("Connected to database successfully.");
    }

    private void checkDBVersion() {
        try {
            int dbVersion = userDBDao.getDBVersion();
            if (dbVersion < 90500) {
                log.error("Current Postgres version is lower than minimum required 9.5.0 version. "
                        + "PLEASE UPDATE YOUR DB.");
            }
        } catch (Exception e) {
            log.error("Error getting DB version.", e.getMessage());
        }
    }

    private HikariConfig initConfig(BaseProperties serverProperties) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(org.postgresql.Driver.class.getName());
        config.setJdbcUrl(serverProperties.getProperty("jdbc.url"));
        config.setUsername(serverProperties.getProperty("user"));
        config.setPassword(serverProperties.getProperty("password"));

        config.setAutoCommit(false);
        config.setConnectionTimeout(serverProperties.getLongProperty("connection.timeout.millis"));
        config.setMaximumPoolSize(5);
        config.setMaxLifetime(0);
        config.setConnectionTestQuery("SELECT 1");
        return config;
    }

    public void deleteUser(String email) {
        if (isDBEnabled() && email != null) {
            blockingIOProcessor.executeDB(() -> userDBDao.deleteUser(email));
        }
    }

    public void saveUsers(ArrayList<User> users) {
        if (isDBEnabled() && users.size() > 0) {
            blockingIOProcessor.executeDB(() -> userDBDao.save(users));
        }
    }

    public void deleteOrganization(int orgId) {
        if (isDBEnabled()) {
            blockingIOProcessor.executeDB(() -> organizationDBDao.deleteOrganization(orgId));
        }
    }

    public void saveOrganizations(List<Organization> organizations) {
        if (isDBEnabled() && organizations.size() > 0) {
            blockingIOProcessor.executeDB(() -> organizationDBDao.save(organizations));
        }
    }

    public Redeem selectRedeemByToken(String token) throws Exception {
        if (isDBEnabled()) {
            return redeemDBDao.selectRedeemByToken(token);
        }
        return null;
    }

    public boolean updateRedeem(String email, String token) throws Exception {
        return redeemDBDao.updateRedeem(email, token);
    }

    public void insertRedeems(List<Redeem> redeemList) {
        if (isDBEnabled() && redeemList.size() > 0) {
            redeemDBDao.insertRedeems(redeemList);
        }
    }

    public FlashedToken selectFlashedToken(String token) {
        if (isDBEnabled()) {
            return flashedTokensDBDao.selectFlashedToken(token);
        }
        return null;
    }

    public boolean activateFlashedToken(String token) {
        return flashedTokensDBDao.activateFlashedToken(token);
    }

    public boolean insertFlashedTokens(FlashedToken... flashedTokenList) throws Exception {
        if (isDBEnabled() && flashedTokenList.length > 0) {
            flashedTokensDBDao.insertFlashedTokens(flashedTokenList);
            return true;
        }
        return false;
    }

    public void insertPurchase(Purchase purchase) {
        if (isDBEnabled()) {
            purchaseDBDao.insertPurchase(purchase);
        }
    }

    public void insertInvitation(InvitationToken invitationToken) throws Exception {
        if (isDBEnabled()) {
            invitationTokensDBDao.insert(invitationToken);
        }
    }

    public boolean insertClonedProject(String token, String projectJson) throws Exception {
        if (isDBEnabled()) {
            cloneProjectDBDao.insertClonedProject(token, projectJson);
            return true;
        }
        return false;
    }

    public String selectClonedProject(String token) throws Exception {
        if (isDBEnabled()) {
            return cloneProjectDBDao.selectClonedProjectByToken(token);
        }
        return null;
    }

    public boolean isDBEnabled() {
        return !(ds == null || ds.isClosed());
    }

    public void executeSQL(String sql) throws Exception {
        try (Connection connection = ds.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            connection.commit();
        }
    }

    public String getUserServerIp(String email) {
        if (isDBEnabled()) {
            return userDBDao.getUserServerIp(email);
        }
        return null;
    }

    public String getServerByToken(String token) {
        if (isDBEnabled()) {
            return forwardingTokenDBDao.selectHostByToken(token);
        }
        return null;
    }

    public void assignServerToToken(String token, String serverIp, String email, int deviceId) {
        if (isDBEnabled()) {
            blockingIOProcessor.executeDB(() ->
                    //todo fix dashId
                    forwardingTokenDBDao.insertTokenHost(token, serverIp, email, 0, deviceId));
        }
    }

    public void removeToken(String... tokens) {
        if (isDBEnabled() && tokens.length > 0) {
            blockingIOProcessor.executeDB(() -> forwardingTokenDBDao.deleteToken(tokens));
        }
    }

    public Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    @Override
    public void close() {
        if (isDBEnabled()) {
            System.out.println("Closing Blynk DB...");
            ds.close();
        }
    }

}
