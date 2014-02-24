
import java.io.IOException;
import java.io.*;
import java.net.*;
import java.sql.*;

public class DnsConfig{
    // 驱动程序名
    private final String driver = "com.mysql.jdbc.Driver";

    // URL指向要访问的数据库名wb
    private final String url = "jdbc:mysql://localhost:3306/cafe";

    // MySQL配置时的用户名
    private final String sql_user = "root"; 
  
    // MySQL配置时的密码
    private final String sql_passwd = "526156";
    /**
     * 初始化iptables规则
     */
    private void initDNSSet() {
        
        //清空所有iptables表
	String cmd = "iptables -X";
	executeIptabels(cmd);
			
	cmd = "iptables -F -t nat";
	executeIptabels(cmd);
			
	cmd = "ipset -X";
	executeIptabels(cmd);

	//nat_tables表为允许接入网络的ip表
	cmd = "ipset -N nat_tables iphash --timeout 14400";
	executeIptabels(cmd);
			
	cmd = "iptables -t nat -A PREROUTING -i wlan0 -m set ! --match-set nat_tables src -p tcp --dport 80 -j DNAT --to 192.168.5.1";
	executeIptabels(cmd);
                        
	//设置443端口通过，可以通过google查询然后跳转
	cmd = "iptables -t nat -A POSTROUTING -o eth0 -d 74.125.128.199 -p tcp --dport 443 -j MASQUERADE";
	executeIptabels(cmd);
	//设置nat_tables内可以访问任何地址
	cmd = "iptables -t nat -A POSTROUTING -o eth0 -m set --match-set nat_tables src -j MASQUERADE";
	executeIptabels(cmd);

        //初始化网路连通性记录
        setIsConnective(0);
        //测试连通性
        String baidu = "www.baidu.com";
        String google = "www.google.ca";
        while(true){
            if(isConnective(baidu)==1)
                break;
            else
            {
                if(isConnective(google)==1)
                    break;
                else
                {
                    try {
                        Thread.sleep(5000);//括号里面的5000代表5000毫秒，也就是5秒，可以该成你需要的时间
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }    
            }       
        }
        String dnsStr = getDNSAddress();
        //设置内网所有ip的53端口dns查询转到resolv.conf下的nds服务器
        cmd = "iptables -t nat -A PREROUTING -i wlan0 -d 192.168.5.1 -p udp --dport 53 -j DNAT --to "+dnsStr;
        executeIptabels(cmd);
			
        cmd = "iptables -t nat -A POSTROUTING -o eth0 -d "+dnsStr+" -p udp --dport 53 -j MASQUERADE";
        executeIptabels(cmd);
        //设置连通性
        setIsConnective(1);
      
	}
    /**
     * 设置当前网络连通状态
     */
    private void setIsConnective(int flag){
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
            String sql = "select id from internetState";
				
            // 结果集
            ResultSet rs = statement.executeQuery(sql);
            
            sql="update internetState set id="+flag+" where id<3";
      
            if(rs.next())	
                statement.executeUpdate(sql);
        } catch (SQLException e) {                    
            e.printStackTrace();
        } catch (ClassNotFoundException e) {  
            e.printStackTrace();
        }
    
    }
	/**
     * 判断当前网络连通性
     * 
     * @return 当前网络连通性
     */
    private int isConnective(String testURL) {
       try {  
           InetAddress ad = InetAddress.getByName(testURL);  
           boolean state = ad.isReachable(5000);//测试是否可以达到该地址  
           if(state){  
                System.out.println("连接成功" + ad.getHostAddress());
                return 1;
           }
           else{  
                System.err.println("连接失败");
                return 0;
           }
        } catch(Exception e){  
             System.err.println("连接失败");
        }
       return 0;
   }


	//执行命令
	private void executeIptabels(String c) {
            try{
		Process p = Runtime.getRuntime().exec(c);
		p.waitFor();
            }catch (IOException e) {  
                e.printStackTrace();  
            }catch (InterruptedException e) {  
                e.printStackTrace();  
            }
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
        
        public static void main(String args[])
        {
            DnsConfig initDns = new DnsConfig();
            initDns.initDNSSet();
	}

}