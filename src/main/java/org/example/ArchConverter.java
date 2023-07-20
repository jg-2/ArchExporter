package org.example;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.*;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchConverter {
    public Connection conn;
    public Logger logger;
    public Properties appProp;

    public StringBuilder sb;

    public StringBuilder getSb() {
        return sb;
    }

    public void setSb(StringBuilder sb) {
        this.sb = sb;
    }

    public Connection getConn() {
        return conn;
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public Properties getAppProp() {
        return appProp;
    }

    public void setAppProp(Properties appProp) {
        this.appProp = appProp;
    }

    ArchConverter(Connection conn){
        this.conn = conn ;
        logger = LogManager.getRootLogger();
    }
    public int convert(){
        boolean go=true;
        while(go) {
            try {
                Timestamp beginTime = new Timestamp(System.currentTimeMillis());
                Statement st2 = conn.createStatement();
                String sqlH2 = "SELECT * from documents where isDone = false limit 1";
                logger.debug("SQL QUERY: {}", sqlH2);
                ResultSet rs = st2.executeQuery(sqlH2);
                rs.next();
                int docid = rs.getInt("docId");
                String updH2 = "UPDATE documents set isdone = 'true' where docId=" + docid;
                logger.debug("SQL UPDATE: {}", updH2);
                st2.executeUpdate(updH2);
                sqlH2 = "SELECT * from pages where docId=" + docid+ " order by pageNum";
                logger.debug("SQL SELECT: {}", sqlH2);
                ResultSet rs3 = st2.executeQuery(sqlH2);
                Timestamp afterSelectTime = new Timestamp(System.currentTimeMillis());
                Document document = new Document();
                String pdfFileName= appProp.getProperty("outputPath")+""+docid+".pdf";
                    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfFileName));
                    document.open();

                while (rs3.next()) {
                    String tiff =  rs3.getString("tiffFile");
                    Image image = Image.getInstance(tiff);
                    document.newPage();
                    document.add(image);

                    logger.debug("Getting tiff image: {}", tiff);
                }
                document.close();
                writer.close();
                Timestamp afterConvertTime = new Timestamp(System.currentTimeMillis());
                logger.debug("Creating XMLFile");

                String sss= sb.toString();


                String out = sss.replace("$kategoria","Jakas dluga kategoria");


                BufferedWriter XMLwriter = new BufferedWriter(new FileWriter(appProp.getProperty("xmlPath")+""+docid+".xml"));
                XMLwriter.write(out);
                logger.debug("Stringbuilder written to file");
                XMLwriter.close();

                Timestamp afterXMLTime = new Timestamp(System.currentTimeMillis());

                sqlH2 = "INSERT INTO results (docid, pdffile, beginTime, afterSelectTime,afterConvertTime, afterXmlTime)" +
                        " VALUES ("+docid+",'"+pdfFileName+"','"+beginTime+"','"+afterSelectTime+"','"+afterConvertTime+"','"+afterXMLTime+"')";
                logger.debug("QUERY: "+sqlH2);
                st2.executeUpdate(sqlH2);

            } catch (SQLException e) {
                if (e.getErrorCode() == 2000) {
                    logger.info("No more data");
                    go = false;
                } else {

                    throw new RuntimeException(e);
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (BadElementException e) {
                throw new RuntimeException(e);
            } catch (DocumentException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
return 1;
    }
}
