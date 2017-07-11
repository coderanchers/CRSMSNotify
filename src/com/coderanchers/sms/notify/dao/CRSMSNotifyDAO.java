package com.coderanchers.sms.notify.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.coderanchers.sms.notify.CRSMSNotify;

public class CRSMSNotifyDAO {

	private static final String DS_CONTEXT = "java:/jdbc/twilio";
	private static final String GET_CREDS = "select sid, twilio_number from wp_sms_twilio_credentials";
	private static final String GET_USER = "select user_nicename, ID, user_id from wp_usermeta, wp_users where meta_key = 'sms_twilio_mobile' and meta_value = ? and user_id = ID";
	
	public Map<String, String> getCredentials() {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Map<String, String> creds = new HashMap<String, String>();
		
		try {
			conn = getConnection();
			ps = conn.prepareStatement(GET_CREDS);
			rs = ps.executeQuery();
			
			while(rs.next()) {
				creds.put(CRSMSNotify.TWILIO_ACCOUNT_SID_KEY, rs.getString("sid"));
				creds.put(CRSMSNotify.TWILIO_NUMBER_KEY, rs.getString("twilio_number"));
			}
		} catch(Exception e) {
			System.out.println("Exception getting twilio credentials " + e);
		} finally {
			close(rs, conn, ps);
		}
		
		return creds;
	}
	
	public String getUserFromMobile(String mobile) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String user = "";
		
		try {
			conn = getConnection();
			ps = conn.prepareStatement(GET_USER);
			ps.setString(1, mobile);
			rs = ps.executeQuery();
			
			while(rs.next()) {
				user = rs.getString("user_nicename");
			}
		} catch(Exception e) {
			System.out.println("Exception getting twilio credentials " + e);
		} finally {
			close(rs, conn, ps);
		}
		
		return user;
		
	}
	private void close(ResultSet rs, Connection conn, PreparedStatement ps) {
		if(rs != null) {
			try { rs.close(); } catch (Exception e) {}
		}
		if(ps != null) {
			try { ps.close(); } catch (Exception e) {}
		}
		if(conn != null) {
			try { conn.close(); } catch (Exception e) {}
		}
	}
	
	private Connection getWPConnection() {
		Connection conn = null;
		Properties connectionProps = new Properties();
		connectionProps.put("user", "wordpress_user");
		connectionProps.put("password", "wordpress_password");
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://wordpress_host:3306/", connectionProps);
		}catch (SQLException | ClassNotFoundException e) {
			System.out.println("Exception getting WordPress connection");
		}
		return conn;
	}
	
	//Recommended approach
	private Connection getConnection() {
		Connection conn = null;
		try {
			Context initialContext = new InitialContext();
			DataSource datasource = (DataSource)initialContext.lookup(DS_CONTEXT);
			if(datasource != null) {
				conn = datasource.getConnection();
			} else {
				System.out.println("Error looking up datasource.");
			}
		} catch(NamingException | SQLException e) {
			System.out.println("Error looking up datasource.");
		}
		return conn;
	}

	public String getWordPressURL() {
		return "http://wordpress-host-name.com/wp-json/wp/v2/comments";
	}

}
