package org.example;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.RunScript;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.sql.Connection;
import java.util.Properties;


public class Main {


    public static void main(String[] args) {
        System.setProperty("log4j2.configurationFile", "cfg/log4j.config.xml");

        Logger logger = LogManager.getRootLogger();

        logger.trace("Configuration File Defined to : {} ", System.getProperty("log4j2.configurationFile"));

        logger.info("Starting ArchExporter");
        logger.info("Trying load properties resource");
         Properties appProps;


             appProps = new Properties();
            try {
                appProps.load(new FileInputStream( "cfg/ArchExporter.properties"));
                logger.info("Loaded properties file: {}", "cfg/ArchExporter.properties");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }



        try {
            logger.info("Creating H2 database instance");
            String jdbcURL = "jdbc:h2:mem:test";
            Connection connH2 = DriverManager.getConnection(jdbcURL);
            logger.info("Connected to H2 in-memory database.");
            logger.info("Trying to get property: h2generateScript");
            String h2ScriptFile = appProps.getProperty("h2generateScript");
            logger.info("Property h2ScriptFile: {}",h2ScriptFile);
            logger.info("Executing SQLScript file: {}",h2ScriptFile);
            RunScript.execute(connH2, new FileReader(h2ScriptFile));
            logger.info("Executed successful");
            Statement statement = connH2.createStatement();


            String sql = "show tables";

            try (Statement stmt = connH2.createStatement()) {
                ResultSet rs = stmt.executeQuery(sql);
                StringBuilder sb = new StringBuilder();
                sb.append("(");

                while (rs.next()) {
                    String tabName = rs.getString("TABLE_NAME");
                    sb.append(tabName).append(",");

                }
                sb.append(")");
                logger.info("Tables in database: {}",sb);
            } catch (SQLException e) {
                e.printStackTrace();
            }

           /* sql = "Insert into documents ( pages) values (1)";

            int rows = statement.executeUpdate(sql);
            rows = statement.executeUpdate(sql);
            rows = statement.executeUpdate(sql);
            rows = statement.executeUpdate(sql);
            rows = statement.executeUpdate(sql);
            rows = statement.executeUpdate(sql);

           */
            logger.info("Reading property SQLServerURL");
            String sqlServerUrl = appProps.getProperty("SQLServerURL");
            logger.info("Property SQLServerURL: {}",sqlServerUrl);

            DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
            logger.info("Trying to connect to SQLServer database");
            Connection conn = DriverManager.getConnection(sqlServerUrl);
            logger.info("Connection OK");
            Statement st = conn.createStatement();
            String Sql = "select docid,pages from v_document where 1=1 group by docid, pages";
            logger.info("SQLSERVER statement: {}",Sql);
            ResultSet rs = st.executeQuery(Sql);

            while (rs.next()) {
                int rsDocId = rs.getInt("docid");
                int rsPages = rs.getInt("pages");

                logger.debug("Docid: {} ", rsDocId);
                String sql2 = "select * from v_document where docid="+rsDocId;
                logger.debug("SQLSERVER statement: {}",sql2);
                Statement st2 = conn.createStatement();
                ResultSet rs2 = st2.executeQuery(sql2);
                /*
                String sqlH2 = "INSERT INTO documents (isdone, pages, docIdorg) values (0, "+rsPages+","+rsDocId+")";
                logger.debug(sqlH2);
                Statement stH2 = connH2.createStatement();
                int z = stH2.executeUpdate(sqlH2);
                logger.debug("Result: {}",z);
                */
                String sqlH2 = "INSERT INTO documents (isdone, pages, docIdorg) values (0, "+rsPages+","+rsDocId+")";
                PreparedStatement stH2 =
                        connH2.prepareStatement(sqlH2,Statement.RETURN_GENERATED_KEYS);

                int affectedRows = stH2.executeUpdate();

                ResultSet generatedKeys = stH2.getGeneratedKeys();
                long id = 0;
                if (generatedKeys.next()) {
                    id = generatedKeys.getLong(1);
                } else {
                    System.out.println("Pysto");
                    // Throw exception?
                }


                while  (rs2.next()){
                    String rs2Tiff = rs2.getString("patch");
                    logger.debug(rs2Tiff);
                    sqlH2 = "INSERT into pages values("+id+","+rs2.getString("pageNum")+",'"+rs2Tiff+"')";
                    logger.debug(sqlH2);
                    stH2 = connH2.prepareStatement(sqlH2);
                    stH2.executeUpdate();
                }


            }

            /*
            logger.info("Trying to get property: Threads");
            int threads = Integer.parseInt(appProps.getProperty("Threads"));

            logger.info("Property Threads: {} ",threads);
            logger.info("Creating threads");

            Thread[] threadsTab = new Thread[threads];
            Runnable[] runnersTab = new Runnable[threads];

            for (int i=0;i<threads;i++){
                String threadName = "Thread_"+i;
                logger.info("Creating runner: {}",threadName);
                runnersTab[i] = new ArchRunner(threadName,connH2);
            }

            for (int i=0;i<threads;i++){
                String threadName = "Thread_"+i;
                logger.info("Creating thread: {}",threadName);
                threadsTab[i] = new Thread(runnersTab[i]);
            }

            for (int i=0;i<threads;i++){
                String threadName = "Thread_"+i;
                logger.info("Starting tread {}",threadName);
                threadsTab[i].start();
                Thread.sleep(100);
            }

            */
            logger.info("Loading XMLTemplate ... ");


            StringBuilder sb = new StringBuilder();


                try (BufferedReader reader = new BufferedReader(new FileReader("cfg/xmlTemplate.xml"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }




            String fileContent = sb.toString();
            //System.out.println(fileContent);

            ArchConverter ac = new ArchConverter(connH2);
            ac.setAppProp(appProps);
            ac.setSb(sb);
            ac.convert();

            logger.info("Checking sizes of output/input files");

            SizeChecker sc = new SizeChecker(connH2);
            sc.updateSizes();

            logger.info("Creating report");
            RapCreator rc = new RapCreator(connH2);
            rc.setProp(appProps);
            rc.createReport();
            logger.info("Closing DB connections");
            conn.close();
            connH2.close();
        } catch (SQLException | FileNotFoundException e) {

            e.printStackTrace();
        }

        logger.info("ArchExporter END");
     /*
     Załaduj dane z bazy sqlservera
     Zmienna z liczbami wątków (parametr bądź properties)
     Utwórz wątki
     Czytaj z bazy top 1
     CZyli select top 1 where done = false;

      */

    }
}