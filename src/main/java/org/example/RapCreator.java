package org.example;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

public class RapCreator {
    Connection conn;
      Logger logger;
      Properties prop;

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

    public Properties getProp() {
        return prop;
    }

    public void setProp(Properties prop) {
        this.prop = prop;
    }

    public RapCreator(Connection conn) {
        this.conn = conn;
        logger  = LogManager.getRootLogger();
    }

    public String convertTimestamp(long timestamp){
        Calendar cal = Calendar.getInstance();
        String allTime;
        if (timestamp > 0 ) {
            cal.setTimeInMillis(timestamp);

            // Nie wiem dlaczego na razie tak
            cal.add(Calendar.HOUR, -1);

            allTime =
                    new SimpleDateFormat("HH:mm:ss.SSS").format(cal.getTime());
        } else {
            allTime =
                    new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(cal.getTime());
        }
       return allTime;
    }

    public void createReport() throws FileNotFoundException {

        StringBuilder sb = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new FileReader( "cfg/rapTemplate.txt"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }



        ;
        //System.out.println(sb.toString());
        try {
            Statement stm = conn.createStatement();
            String sql = "select min(beginTime) fromTime, max(afterXmlTime) toTime, " +
                    "TIMESTAMPDIFF(MILLISECOND,min(beginTime), max(afterXmlTime)) allTime " +
                    ", SUM(TIMESTAMPDIFF(MILLISECOND,afterSelectTime, afterConvertTime)) convTime " +
                    ", SUM(TIMESTAMPDIFF(MILLISECOND,afterConvertTime, afterXMlTime)) xmlTime" +
                    ", AVG(TIMESTAMPDIFF(MILLISECOND,beginTime, afterXMlTime)) avgTime" +
                    ", COUNT(*) allDocs" +
                    ", SUM(pages) allPages " +
                    ", SUM(tiffSize) tiffSize " +
                    ", SUM(pdfSize) pdfSize " +
                    " from results";
            ResultSet rs = stm.executeQuery(sql);
            while (rs.next()){
                String fromTime = rs.getTimestamp("fromTime").toString();
                String toTime = rs.getTimestamp("toTime").toString();


                 String allTime = convertTimestamp(rs.getLong("allTime"));

                 logger.debug("allTime: {}", allTime);
                 String convertTime = convertTimestamp(rs.getLong("convTime"));

                 logger.debug("convTime: {}", convertTime);
                String xmlTime = convertTimestamp(rs.getLong("xmlTime"));

                logger.debug("xmlTime: {}",xmlTime);

                String avgTime = convertTimestamp(rs.getLong("avgTime"));

                logger.debug("avgTime: {}",avgTime);

                int pages = rs.getInt("allPages");
                logger.debug("Pages: {}",pages);
                int docs = rs.getInt("allDocs");
                logger.debug("Docs: {}", docs);

                long tiffSize = rs.getLong("tiffSize");
                logger.debug("Tiff size: {}", tiffSize);

                long pdfSize = rs.getLong("pdfSize");
                logger.debug("PDF Size: {}", pdfSize);

                int avgTiffSize = (int)tiffSize/docs;
                logger.debug("avgTiffSize: {}", avgTiffSize);

                int avgPDFSize = (int)pdfSize/docs;
                logger.debug("avgPDFSize: {}", avgPDFSize);



                String currTime = convertTimestamp(0);
                logger.debug(currTime);




                int index = sb.indexOf("$current");
                sb.replace(index, index + "$current".length(), currTime);

                index = sb.indexOf("$from");
                sb.replace(index, index + "$current".length(), fromTime);

                index = sb.indexOf("$to");
                sb.replace(index, index + "$to".length(), toTime);

                index = sb.indexOf("$time");
                sb.replace(index, index + "$time".length(), allTime);

                index = sb.indexOf("$pdfTime");
                sb.replace(index, index + "$pdfTime".length(), convertTime);

                index = sb.indexOf("$alldocs");
                sb.replace(index, index + "$allDocs".length(), docs+"");

                index = sb.indexOf("$pages");
                sb.replace(index, index + "$pages".length(), pages+"");

                index = sb.indexOf("$ppd");
                sb.replace(index, index + "$ppd".length(), pages/docs+"."+pages%docs);

                index = sb.indexOf("$xmlTime");
                sb.replace(index, index + "$xmlTime".length(), xmlTime);

                index = sb.indexOf("$avgTime");
                sb.replace(index, index + "$avgTime".length(), avgTime);

                index = sb.indexOf("$tiffSizes");
                sb.replace(index, index + "$tiffSizes".length(), tiffSize+"");

                index = sb.indexOf("$pdfSizes");
                sb.replace(index, index + "$pdfSizes".length(), pdfSize+"");

                index = sb.indexOf("$avgTiff");
                sb.replace(index, index + "$avgTiff".length(), avgTiffSize+"");

                index = sb.indexOf("$avgPdf");
                sb.replace(index, index + "$avgPdf".length(), avgPDFSize+"");



                String wynik = sb.toString();
               // System.out.println(wynik);

                Document document = new Document();

                try {
                    String path = prop.getProperty("outputPath");
                    PdfWriter.getInstance(document, new FileOutputStream(path+"report.pdf"));

                    document.open();

                    Paragraph paragraph = new Paragraph(wynik);
                    document.add(paragraph);

                    document.close();

                    // System.out.println("Plik PDF został pomyślnie zapisany.");
                } catch (DocumentException | FileNotFoundException e) {
                    e.printStackTrace();
                }

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }
    };