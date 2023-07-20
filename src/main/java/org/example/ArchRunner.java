package org.example;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ArchRunner implements Runnable{

    private String threadName;
    private Connection conn;
    public ArchRunner(String threadName, Connection conn){
        this.threadName = threadName;
        this.conn = conn;
    };

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public Connection getConn() {
        return conn;
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void run() {
        while (true){
            System.out.println(this.threadName);

            try {
                Statement st2 = conn.createStatement();
                String sqlH2 = "SELECT * from documents where isDone = false limit 1";
                System.out.println(threadName+": "+sqlH2);
                ResultSet rs = st2.executeQuery(sqlH2);
                rs.next();
                int docid = rs.getInt("docId");
                String updH2 = "UPDATE documents set isdone = 'true' where docId="+docid;
                System.out.println(threadName+": "+updH2);
                st2.executeUpdate(updH2);
                sqlH2 = "SELECT * from page where docId="+docid;
                ResultSet rs3 = st2.executeQuery(sqlH2);
                while (rs3.next()){

                    System.out.println(); rs3.getString("tiffFile");
                }


            } catch (SQLException e) {
                throw new RuntimeException(e);
            }


        }
    }
}
