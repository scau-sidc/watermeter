package com.github.scausidc.watermeter;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

import org.apache.http.*;
//import org.apache.http.client.*;
import org.apache.http.client.fluent.*;
import com.alibaba.fastjson.*;

@WebServlet("/254/statistic.json")
public class Meter254 extends HttpServlet
{
    protected String realm;

    @Override
    public void init()
    {
        try
        {
            this.realm = new BufferedReader(
                new InputStreamReader(
                    this.getClass().getResourceAsStream("/realm254")
                )
            ).readLine();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return;
    }

    public Map<String, Map<String, String>> fetch()
        throws IOException
    {
        Map<String, Map<String, String>> root = new HashMap<String, Map<String, String>>();

            String resp1 = Request.Get("http://10.50.9.254/userRpm/SystemStatisticRpm.htm")
                .setHeader("Authorization", "Basic "+this.realm)
                .viaProxy(new HttpHost("localhost", 8888))
                .execute()
                .returnContent()
                .asString();

            String resp2 = Request.Get("http://10.50.9.254/userRpm/AssignedIpAddrListRpm.htm")
                .setHeader("Authorization", "Basic "+this.realm)
                .execute()
                .returnContent()
                .asString();

            Pattern patt1 = Pattern.compile("<table width=\"617\" border=\"1\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" class=\"space\">(.*?)</table>", Pattern.DOTALL);
            Matcher mat1 = patt1.matcher(resp1);
            mat1.find();
            String data1 = mat1.group(1);

            Pattern patt11 = Pattern.compile("<tr>(.*?)</tr>", Pattern.DOTALL);
            Matcher mat11 = patt11.matcher(data1);
            mat11.find();
            mat11.find();
            while (mat11.find())
            {
                //System.out.println(mat11.group(1));

                String[] items = mat11.group(1).split("<.+?>");
                //for (int i=0; i<items.length; i++)
                    //System.out.println(i+":"+items[i]);

                String ip = items[2];
                Map<String, String> sub = new HashMap<String, String>(8);

                sub.put("accumulated_packets",  items[ 5].substring(0, items[ 5].length()-6));
                sub.put("accumulated_bytes",    items[ 7].substring(0, items[ 7].length()-6));
                sub.put("transient_packets",    items[ 9].substring(0, items[ 9].length()-6));
                sub.put("transient_bytes",      items[11].substring(0, items[11].length()-6));
                sub.put("icmp_tx",              items[13].substring(0, items[13].length()-6));
                sub.put("udp_tx",               items[15].substring(0, items[15].length()-6));
                sub.put("tcp_syn_tx",           items[17].substring(0, items[17].length()-6));

                root.put(ip, sub);
            }
            Pattern patt2 = Pattern.compile("<table width=\"500\" border=\"1\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" class=\"space\">(.*?)</table>", Pattern.DOTALL);
            Matcher mat2 = patt2.matcher(resp2);
            mat2.find();
            String data2 = mat2.group(1);
            //System.out.println(data2);

            Pattern patt21 = Pattern.compile("<tr>(.*?)</tr>", Pattern.DOTALL);
            Matcher mat21 = patt21.matcher(data2);
            while (mat21.find())
            {
                //System.out.println(mat21.group(1));

                String[] items = mat21.group(1).split("<.+?>");
                //for (int i=0; i<items.length; i++)
                    //System.out.println(i+":"+items[i]);

                String ip = items[7];
                Map<String, String> sub = root.get(ip);
                if (sub == null)
                {
                    sub = new HashMap<String, String>();
                    root.put(ip, sub);
                }

                sub.put("name",  items[ 3]);
            }

            return(root);

    }

    public static void main(String[] args)
    {
        try
        {
            Meter254 m = new Meter254();
            m.init();
            Map<String, Map<String, String>> root = m.fetch();

            JSONObject json = (JSONObject)JSON.toJSON(root);
            System.out.println(json.toJSONString());

            return;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
