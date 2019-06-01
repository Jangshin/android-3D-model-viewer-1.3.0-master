package org.andresoviedo.app.model3D.view;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 数据库管理类，提供连接数据库和拆解链接功能
 */
public class DBManger {
    private static String username="root";                   //定义的数据库用户名
    private static String password="123456";                   //定义的数据库连接密码
    private static String url="jdbc:mysql://192.168.43.102:3306/modelsdatabase?characterEncoding=utf8&serverTimezone=GMT";                        //定义数据库连接URL
    private static Connection connection;             //定义连接



    /**
     * 获得数据库连接对象
     *
     * @return 数据库连接对象
     */
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | SQLException ex) {
            Logger.getLogger(DBManger.class.getName()).log(Level.SEVERE, null, ex);
        }
        return connection;
    }

    /**
     * 关闭所有的数据库连接资源
     *
     * @param connection Connection 链接
     * @param statement Statement 资源
     * @param resultSet ResultSet 结果集合
     */
    public static void closeAll(ResultSet set,Connection connection, Statement statement) {
        if(set!=null){
            try {
                set.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }finally{
                if(statement!=null){
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }finally{
                        if(connection!=null){
                            try {
                                connection.close();
                            } catch (SQLException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }
}
