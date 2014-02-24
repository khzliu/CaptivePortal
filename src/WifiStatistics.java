/**
 * 用于管理iptables规则和状态的类
 * 
 * @author hz<khzliu@163.com>
 */

  
import java.io.*;
import java.sql.*;
import java.util.*;
import java.text.*;

    
public class WifiStatistics{

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		WifiStatistics tmp = new WifiStatistics();
		tmp.run();
	}


	String filename = "/root/leto_software/wifi.txt";

	// 驱动程序名
    private final String driver = "com.mysql.jdbc.Driver";

    // URL指向要访问的数据库名wb
    private final String url = "jdbc:mysql://127.0.0.1:3306/cafe";

	// MySQL配置时的用户名
    private final String sql_user = "root"; 
  
	// MySQL配置时的密码
	private final String sql_passwd = "526156";
   

    private void state(){
  
        //获取当前日期
        String currentDate = getCal();
        
		//获取目前接入的pc总数
        String[] wifiNum = new String[]{"", "0", "0"};
        wifiNum = getUpdate();
        wifiNum[0] = currentDate;

        try{
			// 加载驱动程序
			Class.forName(driver);

			// 连续数据库
			Connection conn = DriverManager.getConnection(url, sql_user, sql_passwd);

			if(conn.isClosed())
				System.out.println("Failed connecting to the Database!");
					

			// statement用来执行SQL语句
			Statement statement = conn.createStatement();
		    //执行SQL,更新当前数据
			String sql = "select times from wifiAuthro where Date_id =\'"+currentDate+"\'";
		    // 结果集
			ResultSet rs = statement.executeQuery(sql);
			if(rs.next()){
				sql = "update wifiAuthro set times="+wifiNum[1]+" ,total="+wifiNum[2]+" where Date_id=\'"+currentDate+"\'";
	            statement.executeUpdate(sql);
		    }else{
			    sql = "insert into wifiAuthro(Date_id,times,total) values(\'"+wifiNum[0]+"\',"+wifiNum[1]+","+wifiNum[2]+")";
				System.out.println(sql);
				statement.executeUpdate(sql);
			}
			//关闭游标，释放资源
			rs.close();
			//关闭连接，释放资源
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
					
		NumberFormat nf = new DecimalFormat("00");
				
		Calendar cal=Calendar.getInstance(); 
				
		y=cal.get(Calendar.YEAR);
				
		m=cal.get(Calendar.MONTH)+ 1;
				
		sm = nf.format(m);
				
		d=cal.get(Calendar.DATE);
				
		sd = nf.format(d);
				
		return y+sm+sd;
	}

	public String[] getUpdate(){
		String[] result = new String[]{"", "0", "0"};
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filename));
			result[0] = br.readLine();
			result[1] = br.readLine();
            result[2] = br.readLine();
			br.close();
		} catch (FileNotFoundException e) {	
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("读取文件失败");
		} 

        return result;
	}	
    //删除已经离开的的设备ip
    private void deleteIp(String ip){
        String cmd = "ipset -D nat_tables "+ip;
		executeIptabels(cmd);
	}

    private void updateTable(){
        List<String[]> arpList = new ArrayList<String[]>(); //arp ip-mac表
        List<String[]> sqlList = new ArrayList<String[]>(); //数据库中已经登录的ip-mac表
        List<String> natList = new ArrayList<String>(); //nat_tables表
        List<String> arpIpList = new ArrayList<String>(); //arp 中ip表
        //获取数据库表
		try{
			// 加载驱动程序
			Class.forName(driver);

			// 连续数据库
			Connection conn = DriverManager.getConnection(url, sql_user, sql_passwd);

			if(conn.isClosed())
				System.out.println("Failed connecting to the Database!");
					
			// statement用来执行SQL语句
			Statement statement = conn.createStatement();

		    //执行SQL,更新当前数据
			String sql = "select * from ipMacLoginTables";
		    // 结果集
			ResultSet rs = statement.executeQuery(sql);
			while(rs.next()){
				String[] s = new String[] {"",""};
				s[0] = rs.getString("ip");
				s[1] = rs.getString("mac");
				//System.out.println("sqlIpMac: ip:"+s[0]+"mac:"+s[1]);
				sqlList.add(s);
		    }

			//关闭游标，释放资源
			rs.close();
			//关闭连接，释放资源
			conn.close();
			
        } catch(ClassNotFoundException e) {
			System.out.println("Sorry,can`t find the Driver!"); 
			e.printStackTrace();
        } catch(SQLException e) {
		    e.printStackTrace();
		} catch(Exception e) {
            e.printStackTrace();
        } 

    
        //获取arp表和arpIp表
		try{
			Process p = Runtime.getRuntime().exec("arp -n");
            InputStreamReader ir = new InputStreamReader(p.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
			p.waitFor();
			String line = "";
			List<String> reList = new ArrayList<String>();
			while ((line = input.readLine()) != null) {
				reList.add(line);
			}

            for(String str : reList) {
                if (str != null) {
                    if (str.indexOf("192.168.5.") > 1) {
						int num_at = str.indexOf("at");
						String[] s = new String[]{"",""};
						s[0] = str.substring(str.indexOf("(")+1,str.indexOf(")"));
                        s[1] = str.substring(num_at + 3, num_at + 20);
						arpList.add(s);
						arpIpList.add(s[0]);
						//System.out.println("arpIpMac: ip:"+s[0]+"mac:"+s[1]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        } catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
        
        //获取nat_tables表
		try {
            Process p = Runtime.getRuntime().exec("ipset -L nat_tables");
            InputStreamReader ir = new InputStreamReader(p.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
			p.waitFor();
			String line = "";
			List<String> reList = new ArrayList<String>();
			while ((line = input.readLine()) != null) {
				reList.add(line);
			}
            for(String str : reList) {
                if (str != null) {
                    if (str.indexOf("192") >= 0) {
						int num_at = str.indexOf("at");
						natList.add(str.substring(0,str.indexOf("time")-1));
						//System.out.println("nat_tables: ip:"+str.substring(0,str.indexOf("time")-1));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        } catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}

        //对比arp表和nat_tables表
        for (String ip : natList){
            if(!arpIpList.contains(ip))
			{
                deleteIp(ip);
				System.out.println("noArPIpList:"+ip);
			}
		}
        // 对比arp表和数据库表
        for(String[] arp_ipmac : arpList){
			String str = arp_ipmac[0];
			String substr = str.substring(10); 
			int index = Integer.parseInt(substr) - 1;
			String[] sql_ipmac = sqlList.get(index);
			str = sql_ipmac[1];
            if(!str.equals(arp_ipmac[1]))
			{
                deleteIp(arp_ipmac[0]);
				//System.out.println("sqlMacChanged:"+arp_ipmac[0]+"arp:"+arp_ipmac[1]+" sql:"+str);
			}
		}
	}

	//执行命令
	private void executeIptabels(String c) {
		try	{
			Process p = Runtime.getRuntime().exec(c);
			p.waitFor();
		}catch (IOException e) {  
            e.printStackTrace();  
		}catch (InterruptedException e) {  
            e.printStackTrace();  
		}
	}
    

      
	private void run(){
		while(true){
			state();
		
			updateTable();
			
			try
			{
				Thread.currentThread().sleep(2000);//毫秒 
			}catch(Exception e){
				System.out.println("sleep error");
			}
		}
	}
}
 
