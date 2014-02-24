/**
 * 用于管理iptables规则和状态的类
 * 
 * @author hz<khzliu@163.com>
 */

 package com.cafe.servlet;

import java.io.InputStreamReader;  
import java.io.LineNumberReader;  
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;


public class CaptivePortal extends HttpServlet{
	// 驱动程序名
    private final String driver = "com.mysql.jdbc.Driver";

    // URL指向要访问的数据库名wb
    private final String url = "jdbc:mysql://localhost:3306/cafe";

	// MySQL配置时的用户名
    private final String sql_user = "root"; 
  
	// MySQL配置时的密码
	private final String sql_passwd = "526156";

	public void service(HttpServletRequest req,HttpServletResponse res) 
		throws ServletException,IOException
	{
		process(req,res);
	}
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);	
	}

	public void destroy() {
		//清空所有iptables表
		String cmd = "iptables -X";
		executeIptabels(cmd);
			
		cmd = "iptables -F -t nat";
		executeIptabels(cmd);
			
		cmd = "ipset -X";
		executeIptabels(cmd);
          
    }  

	private void process(HttpServletRequest req,HttpServletResponse res)
		throws IOException
	{
		String ip = req.getRemoteAddr();//获得客户端IP地址;
		String mac = getMACAddress(ip);
	
		String passwd = req.getParameter("password");
		int flag = userAuthentication(passwd); //认证调用
		PrintWriter out=res.getWriter();
		if (flag == 1)
		{	
			addUser(ip,mac);
			internetAccessLog();
			out.print(1);
		}
		else
		{	
			out.print(0);
		}

	}
    /**
     * 添加接入Internet的用户
     */
    public void addUser(String ip,String mac) {
			String cmd = "ipset -A nat_tables "+ip;
			executeIptabels(cmd);

			try {
			// 加载驱动程序
			Class.forName(driver);

			// 连续数据库
			Connection conn = DriverManager.getConnection(url, sql_user, sql_passwd);

			if(!conn.isClosed())
				System.out.println("Succeeded connecting to the Database!");
					

			// statement用来执行SQL语句
			Statement statement = conn.createStatement();
				
				
			// 要执行的SQL语句
			String sql = "select mac from ipMacLoginTables where ip='"+ip+"'";
				
			// 结果集
			ResultSet rs = statement.executeQuery(sql);

			if (rs.next())
				{
					
					sql="update ipMacLoginTables set mac='"+mac+"' where ip='"+ip+"'";
					
					statement.executeUpdate(sql);
					
				}
				rs.close();
				conn.close();

			} catch(ClassNotFoundException e) {


				System.out.println("Sorry,can`t find the Driver!"); 
				e.printStackTrace();


           } catch(SQLException e) {


            e.printStackTrace();


           }
  
    }

	   /**
        * 用户认证
        * 
        * @return 认证结果
        */

	    private int userAuthentication(String pwd) {
			int flag = 0;
			try {
				// 加载驱动程序
				Class.forName(driver);

				// 连续数据库
				Connection conn = DriverManager.getConnection(url, sql_user, sql_passwd);

				if(!conn.isClosed())
					System.out.println("Succeeded connecting to the Database!");
					

				// statement用来执行SQL语句
				Statement statement = conn.createStatement();
				
				
				// 要执行的SQL语句
				String sql = "select password from internetPassword";
				
				// 结果集
				ResultSet rs = statement.executeQuery(sql);

				

				if(rs.next())
				{
					if(rs.getString("password").equals(pwd))
						flag = 1;
				}
				
			}catch (SQLException e) {  
				e.printStackTrace();
				return 0;
			}catch (ClassNotFoundException e) {  
				e.printStackTrace();
				return 0;
			}

			return flag;

	    }


		
        // 统计某日正常接入Internet的用户人数

		private void internetAccessLog()
		{
			 try { 
				// 加载驱动程序
				Class.forName(driver);

				// 连续数据库
				Connection conn = DriverManager.getConnection(url, sql_user, sql_passwd);

				if(!conn.isClosed())
					System.out.println("Succeeded connecting to the Database!");
					

				// statement用来执行SQL语句
				Statement statement = conn.createStatement();
				
				//获取当前日期
				String date = getCal();
				
				// 要执行的SQL语句
				String sql = "select times from internetAuthro where Date_id ='"+date+"'";
				
				// 结果集
				ResultSet rs = statement.executeQuery(sql);
				
				if (rs.next())
				{
					
					int count = rs.getInt("times")+1;
					
					sql="update internetAuthro set times="+count+" where Date_id='"+date+"'";
					
					statement.executeUpdate(sql);
					
					
				}else {
					
					sql="insert into internetAuthro(Date_id,times) values('"+date+"',1)";
					statement.executeUpdate(sql);
	
				}
				
				rs.close();
				conn.close();

			} catch(ClassNotFoundException e) {


				System.out.println("Sorry,can`t find the Driver!"); 
				e.printStackTrace();


           } catch(SQLException e) {


            e.printStackTrace();


           } catch(Exception e) {


            e.printStackTrace();


           } 

		}

		//获取当前日期
		private String getCal() {
			int y=0,m=0,d=0;
			String sm="",sd="";
			
			// 加载驱动程序
				
			NumberFormat nf = new DecimalFormat("00");
				
			Calendar cal=Calendar.getInstance(); 
				
			y=cal.get(Calendar.YEAR);
				
			m=cal.get(Calendar.MONTH);
				
			sm = nf.format(m);
				
			d=cal.get(Calendar.DATE);
				
			sd = nf.format(d);
			
			
			return y+sm+sd;
		}

		//执行命令
		private void executeIptabels(String c) {
			try
			{
				Process p = Runtime.getRuntime().exec(c);
				p.waitFor();
			}catch (IOException e) {  
             e.printStackTrace();  
			}catch (InterruptedException e) {  
             e.printStackTrace();  
			}
		}

		//获取mac地址
		public String getMACAddress(String ip) {
        String str = "";
        String macAddress = "";
        try {
            Process p = Runtime.getRuntime().exec("arp -n");
            InputStreamReader ir = new InputStreamReader(p.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
			p.waitFor();
			boolean flag = true;
            while(flag) {
                str = input.readLine();
                if (str != null) {
                    if (str.indexOf(ip) > 1) {
						int temp = str.indexOf("at");
                        macAddress = str.substring(
                                temp + 3, temp + 20);
                        break;
                    }
                } else
					flag = false;
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        } catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
        return macAddress;
    }

	//获取dns
	public String getDNSAddress() {
		String line = "";
		String DNSString = "8.8.8.8";
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader("/etc/resolv.conf"));
			line = br.readLine();
			String arrays[] = line.split(" ");
			if(arrays[1]==null)
				return DNSString;
			else
				DNSString = arrays[1];
			br.close();
		} catch (FileNotFoundException e) {	
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("读取文件失败");
		} 

        return DNSString;
	}

}