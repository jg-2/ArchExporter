package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SizeChecker {
    public Connection conn;
    public Logger logger;
    SizeChecker(Connection conn){
        this.conn = conn;
        logger = LogManager.getRootLogger();
    }
    public void updateSizes(){
        String sql = "SELECT * From results";
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()){
                int docId = rs.getInt("docid");
                String pdfPatch = rs.getString("pdfFile");

                        Statement st2 = conn.createStatement();
                String queryPages = "SELECT * from pages where docid = "+docId;
                ResultSet rs2 = st2.executeQuery(queryPages);
                long tiffSize = 0;
                int pages=0;
                while (rs2.next()){

                    String tiffPatch = rs2.getString("tiffFile");
                    File plik = new File(tiffPatch);

                    if (plik.exists()) {
                        pages++;
                        tiffSize += plik.length();
                        logger.debug("File: {}",plik.getName());
                        logger.debug("File size: {} ", tiffSize );
                    } else {
                        System.out.println("Plik nie istnieje.");
                    }
                }
                File plik = new File(pdfPatch);
                long pdfSize = 0;
                if (plik.exists()) {
                    pdfSize = plik.length();
                    logger.debug("File: {}",plik.getName());
                    logger.debug("File size: {} ", pdfSize );
                } else {
                    System.out.println("Plik nie istnieje.");
                }

                String sqlU = "UPDATE results set tiffSize="+tiffSize+", pdfsize="+pdfSize+", pages="+pages+" WHERE docid="+docId;
                logger.debug("SQL UPDATE: {}",sqlU);
                st2.executeUpdate(sqlU);


            }

        } catch (SQLException e){
            e.printStackTrace();
        }

    }

}
