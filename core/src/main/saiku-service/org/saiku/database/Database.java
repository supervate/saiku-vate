package org.saiku.database;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.sun.jersey.api.spring.Autowire;
import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;

import org.saiku.datasources.datasource.SaikuDatasource;
import org.saiku.service.datasource.IDatasourceManager;
import org.saiku.service.importer.LegacyImporter;
import org.saiku.service.importer.LegacyImporterImpl;
import org.saiku.service.license.Base64Coder;
import org.saiku.service.license.ILicenseUtils;

import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;


/**
 * Created by bugg on 01/05/14.
 */
public class Database {

    @Autowired
    ServletContext servletContext;

    private ILicenseUtils licenseUtils;

    private URL repoURL;

    public ILicenseUtils getLicenseUtils() {
        return licenseUtils;
    }

    public void setLicenseUtils(ILicenseUtils licenseUtils) {
        this.licenseUtils = licenseUtils;
    }

    private static final int SIZE = 2048;

    private String dbUrl;
    private String dbUsername;
    private String dbPwd;


    private MysqlDataSource ds;
    private static final Logger log = LoggerFactory.getLogger(Database.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private IDatasourceManager dsm;
    public Database() {

    }

    public void setDatasourceManager(IDatasourceManager dsm) {
        this.dsm = dsm;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void init() throws SQLException {
        initDB();
        loadUsers();
//        loadOrder();
        loadLegacyDatasources();
        importLicense();
    }

    private void initDB() {
        ds = new MysqlDataSource();
        ds.setURL(dbUrl);
        ds.setUser(dbUsername);
        ds.setPassword(dbPwd);
    }

    private void loadOrder() throws SQLException {
        JdbcDataSource ds2 = new JdbcDataSource();
        ds2.setURL(dsm.getOrderUrl());
        ds2.setUser("sa");
//        ds2.setPassword("");
        Connection c = ds2.getConnection();
        DatabaseMetaData dbm = c.getMetaData();
        ResultSet tables = dbm.getTables(null, null, "account", null);

        if (!tables.next()) {
            // Table exists
            Statement statement = c.createStatement();
            statement.execute("RUNSCRIPT FROM '"+dsm.getOrderDir()+"/ex_order.sql'");
            String schema = null;
            try {
                schema = readFile(dsm.getOrderSchema(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Can't read schema file",e);
            }
            try {
                dsm.addSchema(schema, "/datasources/ex_order.xml", null);
            } catch (Exception e) {
                log.error("Can't add schema file to repo", e);
            }
            Properties p = new Properties();
            p.setProperty("driver", "mondrian.olap4j.MondrianOlap4jDriver");
            p.setProperty("location", "jdbc:mondrian:Jdbc=jdbc:h2:"+dsm.getOrderDir()+"/ex_order;"+
                    "Catalog=mondrian:///datasources/ex_order.xml;JdbcDrivers=org.h2.Driver");
            p.setProperty("username", "sa");
            p.setProperty("password", "");
            p.setProperty("id", "4432dd20-fcae-11e3-a3ac-8888888c9a66");
            SaikuDatasource ds = new SaikuDatasource("ex_order", SaikuDatasource.Type.OLAP, p);
            try {
                dsm.addDatasource(ds);
            } catch (Exception e) {
                log.error("Can't add data source to repo", e);
            }
        } else {
            Statement statement = c.createStatement();
            statement.executeQuery("select 1");
        }
    }

    private static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    //for change the default database ,change h2 to mysql
    private void loadUsers() throws SQLException{
        Connection c = ds.getConnection();

        Statement statement = c.createStatement();

        statement.execute(" CREATE TABLE IF NOT EXISTS log ( time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, log  TEXT); ");
        statement.execute(" CREATE TABLE IF NOT EXISTS users(user_id INT(11) NOT NULL AUTO_INCREMENT, " + " username VARCHAR(45) NOT NULL UNIQUE, password VARCHAR(100) NOT NULL, email VARCHAR(100), " + " enabled TINYINT NOT NULL DEFAULT 1, PRIMARY KEY(user_id)); ");
        statement.execute(" CREATE TABLE IF NOT EXISTS user_roles ( " + " user_role_id INT(11) NOT NULL AUTO_INCREMENT,username VARCHAR(45), "  + " user_id INT(11) NOT NULL REFERENCES users(user_id), " + " ROLE VARCHAR(45) NOT NULL, " + " PRIMARY KEY (user_role_id)); ");

        ResultSet result = statement.executeQuery("select count(*) as c from log where log = 'insert users'");

        result.next();

        if (result.getInt("c") == 0) {

            statement.execute("INSERT INTO users (username,password,email, enabled) VALUES ('admin','admin', 'test@admin.com',TRUE);");
            statement.execute("INSERT INTO users (username,password,enabled) VALUES ('smith','smith', TRUE);");
            statement.execute("INSERT INTO user_roles (user_id, username, ROLE) VALUES (1, 'admin', 'ROLE_USER');");
            statement.execute("INSERT INTO user_roles (user_id, username, ROLE) VALUES (1, 'admin', 'ROLE_ADMIN');");
            statement.execute("INSERT INTO user_roles (user_id, username, ROLE) VALUES (2, 'smith', 'ROLE_USER');");
            statement.execute("INSERT INTO log (log) VALUES('insert users');");
        }

        String encrypt = servletContext.getInitParameter("db.encryptpassword");
        if (encrypt.equals("true") && !checkUpdatedEncyption()) {
            updateForEncyption();
        }
    }

    private void loadUsers_h2() throws SQLException {

        Connection c = ds.getConnection();

        Statement statement = c.createStatement();
        statement.execute("CREATE TABLE IF NOT EXISTS LOG(time TIMESTAMP AS CURRENT_TIMESTAMP NOT NULL, log CLOB);");

        statement.execute("CREATE TABLE IF NOT EXISTS USERS(user_id INT(11) NOT NULL AUTO_INCREMENT, " +
                "username VARCHAR(45) NOT NULL UNIQUE, password VARCHAR(100) NOT NULL, email VARCHAR(100), " +
                "enabled TINYINT NOT NULL DEFAULT 1, PRIMARY KEY(user_id));");

        statement.execute("CREATE TABLE IF NOT EXISTS USER_ROLES (\n"
                + "  user_role_id INT(11) NOT NULL AUTO_INCREMENT,username VARCHAR(45),\n"
                + "  user_id INT(11) NOT NULL REFERENCES USERS(user_id),\n"
                + "  ROLE VARCHAR(45) NOT NULL,\n"
                + "  PRIMARY KEY (user_role_id));");

        ResultSet result = statement.executeQuery("select count(*) as c from LOG where log = 'insert users'");
        result.next();
        if (result.getInt("c") == 0) {
            dsm.createUser("admin");
            dsm.createUser("smith");
            statement.execute("INSERT INTO users(username,password,email, enabled)\n"
                    + "VALUES ('admin','admin', 'test@admin.com',TRUE);" +
                    "INSERT INTO users(username,password,enabled)\n"
                    + "VALUES ('smith','smith', TRUE);");
            statement.execute(
                    "INSERT INTO user_roles (user_id, username, ROLE)\n"
                            + "VALUES (1, 'admin', 'ROLE_USER');" +
                            "INSERT INTO user_roles (user_id, username, ROLE)\n"
                            + "VALUES (1, 'admin', 'ROLE_ADMIN');" +
                            "INSERT INTO user_roles (user_id, username, ROLE)\n"
                            + "VALUES (2, 'smith', 'ROLE_USER');");

            statement.execute("INSERT INTO LOG(log) VALUES('insert users');");
        }

        String encrypt = servletContext.getInitParameter("db.encryptpassword");
        if(encrypt.equals("true") && !checkUpdatedEncyption()){
            log.debug("Encrypting User Passwords");
            updateForEncyption();
            log.debug("Finished Encrypting Passwords");
        }


    }

    private boolean checkUpdatedEncyption() throws SQLException{
        Connection c = ds.getConnection();
        Statement statement = c.createStatement();
        ResultSet result = statement.executeQuery("select count(*) as c from log where log = 'update passwords'");
        result.next();
        return result.getInt("c") != 0;
    }

    private boolean checkUpdatedEncyption_h2() throws SQLException{
        Connection c = ds.getConnection();

        Statement statement = c.createStatement();
        ResultSet result = statement.executeQuery("select count(*) as c from LOG where log = 'update passwords'");
        result.next();
        return result.getInt("c") != 0;
    }

    private void updateForEncyption() throws SQLException {
        Connection c = ds.getConnection();
        Statement statement = c.createStatement();
        statement.execute("ALTER TABLE users MODIFY COLUMN PASSWORD VARCHAR(100) DEFAULT NULL");
        ResultSet result = statement.executeQuery("select username, password from users");
        while (result.next()) {
            statement = c.createStatement();
            String pword = result.getString("password");
            String hashedPassword = passwordEncoder.encode(pword);
            String sql = "UPDATE users " + "SET password = '" + hashedPassword
                    + "' WHERE username = '" + result.getString("username")
                    + "'";
            statement.executeUpdate(sql);
        }
        statement = c.createStatement();
        statement.execute("INSERT INTO log (log) VALUES('update passwords');");

    }

    private void updateForEncyption_h2() throws SQLException {
        Connection c = ds.getConnection();

        Statement statement = c.createStatement();
        statement.execute("ALTER TABLE users ALTER COLUMN password VARCHAR(100) DEFAULT NULL");

        ResultSet result = statement.executeQuery("select username, password from users");

        while(result.next()){
            statement = c.createStatement();

            String pword = result.getString("password");
            String hashedPassword = passwordEncoder.encode(pword);
            String sql = "UPDATE users " +
                        "SET password = '"+hashedPassword+"' WHERE username = '"+result.getString("username")+"'";
            statement.executeUpdate(sql);
        }
        statement = c.createStatement();

        statement.execute("INSERT INTO LOG(log) VALUES('update passwords');");

    }

    private void loadLegacyDatasources() throws SQLException {
        Connection c = ds.getConnection();

        Statement statement = c.createStatement();
        ResultSet result = statement.executeQuery("select count(*) as c from LOG where log = 'insert datasources'");

        result.next();
        if (result.getInt("c") == 0) {
            LegacyImporter l = new LegacyImporterImpl(dsm);
            l.importSchema();
            l.importDatasources();
            statement.execute("INSERT INTO LOG(log) VALUES('insert datasources');");

        }
    }


    public List<String> getUsers() throws java.sql.SQLException
    {
        //Stub for EE.
        return null;
    }

    public void addUsers(List<String> l) throws java.sql.SQLException
    {
        //Stub for EE.
    }

    private void setPath(String path) {
        FileSystemManager fileSystemManager;
        try {
            fileSystemManager = VFS.getManager();
            FileObject fileObject;
            fileObject = fileSystemManager.resolveFile(path);
            if (fileObject == null) {
                throw new IOException("File cannot be resolved: " + path);
            }
            if (!fileObject.exists()) {
                throw new IOException("File does not exist: " + path);
            }
            repoURL = fileObject.getURL();
            if (repoURL == null) {
                throw new Exception(
                    "Cannot load connection repository from path: " + path);
            } else {
//load();
            }
        } catch (Exception e) {
            //LOG_EELOADER.error("Exception", e);
        }
    }
    public void importLicense() {
        setPath("res:saiku-license");
        try {
            if (repoURL != null) {
                File[] files = new File(repoURL.getFile()).listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.isHidden() && file.getName().equals("license.lic")) {

                            ObjectInputStream si = null;
                            byte[] sig;
                            byte[] data = null;
                            try {
                                si = new ObjectInputStream(new FileInputStream(file));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            try {
                                int sigLength = si.readInt();
                                sig = new byte[sigLength];
                                si.read(sig);

                                ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                                byte[] buf = new byte[SIZE];
                                int len;
                                while ((len = si.read(buf)) != -1) {
                                    dataStream.write(buf, 0, len);
                                }
                                dataStream.flush();
                                data = dataStream.toByteArray();
                                dataStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                try {
                                    si.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }


                            licenseUtils.setLicense(new String(Base64Coder.encode(data)));

                        }
                    }
                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPwd() {
        return dbPwd;
    }

    public void setDbPwd(String dbPwd) {
        this.dbPwd = dbPwd;
    }
}
