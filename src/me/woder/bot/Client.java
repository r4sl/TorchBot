package me.woder.bot;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKey;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.logging.Log;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import me.woder.gui.TorchGUI;
import me.woder.irc.IRCBridge;
import me.woder.world.Location;
import me.woder.world.World;
import me.woder.world.WorldHandler;


public class Client {
    public TorchGUI gui;
    public ChatHandler chat;
    public MetaDataProcessor proc;
    public CommandHandler chandle;
    public WorldHandler whandle;
    public MovementHandler move;
    public NetworkHandler net;
    public IRCBridge irc;
    public DataOutputStream out;
    public DataInputStream in;
    PublicKey publickey;
    SecretKey secretkey;
    SecretKey sharedkey;
    Socket clientSocket;
    boolean isInputBeingDecrypted;
    boolean isOutputEncrypted;
    public String prefix;
    public static byte gamemode;
    String leveltype;
    Location location;
    World world;
    boolean chunksloaded;
    boolean connectedirc;
    int entityID;
    byte dimension;
    byte difficulty;
    byte maxplayer;
    byte flags;
    float flyspeed;
    float walkspeed;
    long time;
    long age;
    float health;
    short food;
    float foodsat;
    float exp;
    short level;
    short lvlto;
    double stance;
    String username = "";//TODO add way to change this
    int port;
    String servername;
    String sessionId;
    public boolean running = true;
    private String password;
    public String accesstoken;
    public String clienttoken;
    public String profile;
    public String versioninfo = "TorchBot version 0.2 by woder";
    public String version = "0.2";
    List<Slot> inventory = new ArrayList<Slot>();
    //List<Player> players = new ArrayList<Player>();//Is exclusive to players
    List<Entity> entities = new ArrayList<Entity>();//Includes players
    //Credits to umby24 for the help and SirCmpwn for Craft.net
    Logger netlog = Logger.getLogger("me.woder.network");
    Logger chatlog = Logger.getLogger("me.woder.chat");
    Logger errlog = Logger.getLogger("me.woder.network");
    
    
    public void main(TorchGUI window){
        this.gui = window;
        File f = new File("config.properties");
        if(f.exists()){
            Properties prop = new Properties();                
            try {
                prop.load(new FileInputStream("config.properties"));
                username = prop.getProperty("username");
                password = prop.getProperty("password");
                servername = prop.getProperty("servername");
                System.out.println(servername);
                port = Integer.parseInt(prop.getProperty("port"));
         
            } catch (IOException ex) {
                    ex.printStackTrace();
            }
        }else{
            Properties prop = new Properties();             
            try {
                prop.setProperty("username", "unreal34");
                prop.setProperty("password", "1234");
                prop.setProperty("servername", "c.mcblocks.net");
                prop.setProperty("port", "25565");
                prop.store(new FileOutputStream("config.properties"), null);
         
            } catch (IOException ex) {
                    ex.printStackTrace();
            }
        }
        Handler fh = null, fn = null, fe = null;
        new File("logs").mkdir();
        try {
            fh = new FileHandler("logs/network.log");
            fn = new FileHandler("logs/chat.log");
            fe = new FileHandler("logs/err.log");
        } catch (SecurityException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        Logger.getLogger("me.woder.network").addHandler(fh);
        Logger.getLogger("me.woder.chat").addHandler(fn);
        Logger.getLogger("me.woder.error").addHandler(fe);
        Logger.getLogger("me.woder.network").setLevel(Level.FINEST);
        Logger.getLogger("me.woder.chat").setLevel(Level.FINEST);
        Logger.getLogger("me.woder.error").setLevel(Level.FINEST);
        prefix = "!";
        gui.addText("�3Welcome to TorchBot 0.2, press the connect button to connect to the server defined in config");
        gui.addText("�3 or press the change server button to login to a new server.");   
        pingServer(servername, port);      
        authPlayer(username, password);
    }
    
    public void startBot(String server, String port){
       this.servername = server;
       try{
         this.port = Integer.parseInt(port);
       }catch(NumberFormatException e){
         netlog.log(Level.SEVERE, "�4Port was not an integer!");
         gui.addText("�4Port was not an integer!");
       }
       startBot();
    }
    
    public void startBot(){
        chunksloaded = false;
        connectedirc = false;
        try{
          // open a socket
        System.out.println("Attempting to connect to: " + servername + " on " + port);
        clientSocket = new Socket(servername, port);
        out = new DataOutputStream(clientSocket.getOutputStream());
        in = new DataInputStream(clientSocket.getInputStream());
        //our hand shake
        int str = username.length();
        out.writeByte(0x02);
        out.writeByte(78);//74 = 1.6.2
        out.writeShort(str);
        out.writeChars(username);
        out.writeShort(servername.length());
        out.writeChars(servername);
        out.writeInt(port);
        out.flush();                             
         
        chat = new ChatHandler(this);
        proc = new MetaDataProcessor(this);
        chandle = new CommandHandler(this);
        whandle = new WorldHandler(this);
        net = new NetworkHandler(this,in,out);          
        world = whandle.getWorld();
        move = new MovementHandler(this);
        irc = new IRCBridge(this);
         
         while(true){
            //mainloop
           net.readData();//Read data
           gui.tick();
           if(chunksloaded){
            //move.applyGravity();//Apply gravity
           }
           if(connectedirc){
             irc.read();//Read text from irc, if there is some
           }
         }
         
      }catch (Exception e){
          e.printStackTrace();
      }
    }
    
    public void stopBot(String reason){
        try {
            out.writeByte(0xFF);
            out.writeShort(reason.length());
            out.writeChars(reason);
            out.close();
            in.close();
            clientSocket.close();
        } catch (IOException e) {
            gui.addText("�4Unable to disconnect! Weird error.. (check network log)");
            netlog.log(Level.SEVERE, "UNABLE TO DISCONNECT: " + e.getMessage());
        }        
    }
    
    public void pingServer(String server, int port){
        try {
            clientSocket = new Socket(server, port);
            out = new DataOutputStream(clientSocket.getOutputStream());
            in = new DataInputStream(clientSocket.getInputStream());
            out.writeByte(0xFE);
            out.writeByte(1);
            out.flush();
            out.writeByte(0xFA);
            out.writeShort(11);
            out.writeChars("MC|PingHost");
            out.writeShort(7+(2*server.length()));
            out.writeByte(75);
            out.writeShort(server.length());
            out.writeChars(server);
            out.writeInt(port);
            out.flush();
          byte id = in.readByte();
          if(id==-1){
              short lend = in.readShort();
              String[] data = getString(in, lend, 400).split("\0");
              String gamev = data[2];
              String motd = data[3];
              String online = data[4];
              String maxp = data[5];
              gui.addText("�5Game version: " + gamev + " " + motd + " " + online + "/" + maxp);
          }
          out.close();
          in.close();
          clientSocket.close();
            
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            netlog.log(Level.SEVERE, "CONNECTION ERROR: " + e.getMessage());
            gui.addText("�4CONNECTION ERROR: " + e.getMessage());
            gui.addText("�4Check server info: " + server + " on " + port);
            
        }
    }
    
    public Player findPlayer(String name){
        Player p = null;
        for(Entity s : entities){
           if(s.getEntity() instanceof Player){
            Player a = (Player)s;
            if(a.getName().equals(name)){
                p = a;
                break;
            }
           }
        }
        return p;
    }
    
    public Entity findEntityId(int id){
        Entity e = null;
        for(Entity s : entities){
            if(s.getEntityId() == id){
                e = s;
                break;
            }
        }
        return e;
    }
    
    public void activateEncryption(){
        try {
            this.out.flush();
            this.isOutputEncrypted = true;
            BufferedOutputStream var1 = new BufferedOutputStream(CryptManager.encryptOuputStream(this.sharedkey, this.clientSocket.getOutputStream()), 5120);
            this.out = new DataOutputStream(var1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void decryptInputStream(){
        this.isInputBeingDecrypted = true;
        try {
             InputStream var1;
             var1 = this.clientSocket.getInputStream();
             this.in = new DataInputStream(CryptManager.decryptInputStream(this.sharedkey, var1));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
     
    public static String getString(DataInputStream datainputstream, int length,
            int max) throws IOException {
        if (length > max)
            throw new IOException(
                    "Received string length longer than maximum allowed ("
                            + length + " > " + max + ")");
        if (length < 0) {
            throw new IOException(
                    "Received string length is less than zero! Weird string!");
        }
        StringBuilder stringbuilder = new StringBuilder();

        for (int j = 0; j < length; j++) {
            stringbuilder.append(datainputstream.readChar());
        }

        return stringbuilder.toString();
    }
     
    public String sendSessionRequest(String user, String session, String serverid)
    {
        try
        {
            URL var4 = new URL("http://session.minecraft.net/game/joinserver.jsp?user=" + urlEncode(user) + "&sessionId=" + urlEncode(session) + "&serverId=" + urlEncode(serverid));
            BufferedReader var5 = new BufferedReader(new InputStreamReader(var4.openStream()));
            String var6 = var5.readLine();
            var5.close();
            return var6;
        }
        catch (IOException var7)
        {
            return var7.toString();
        }
    }
    
    public static String toString(InputStream input)throws IOException{
            StringBuilderWriter sw = new StringBuilderWriter();
            copy(input, sw, Charset.defaultCharset());
            return sw.toString();
    }
    
    public static void copy(InputStream input, Writer output, Charset encoding)throws IOException{
         InputStreamReader in = new InputStreamReader(input, Charsets.toCharset(encoding));
         long count = copyLarge(in, output, new char[4096]);
         if (count > 2147483647L) {
            return;
         }
    }
    
    public static long copyLarge(Reader input, Writer output, char[] buffer) throws IOException{
            long count = 0L;
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
              output.write(buffer, 0, n);
              count += n;
            }
            return count;
    }

    public String authPlayer(String user, String password){
        HttpURLConnection hc = null;
        try {
            URL var4 = new URL("https://authserver.mojang.com/authenticate");
            hc = (HttpURLConnection) var4.openConnection();
            hc.setRequestProperty("content-type","application/json; charset=utf-8"); 
            hc.setRequestMethod("POST");
            hc.setDoInput(true);
            hc.setDoOutput(true);
            hc.setUseCaches(false); 
            OutputStreamWriter wr = new OutputStreamWriter(hc.getOutputStream());
            JSONObject data = new JSONObject();
            JSONObject agent = new JSONObject();
            agent.put("name", "minecraft");
            agent.put("version", "1");
            data.put("agent", agent);
            data.put("username",user);
            data.put("password", password);
            System.out.println(data.toString());
            wr.write(data.toString());
            wr.flush();
            InputStream stream = null;
            try {
              stream = hc.getInputStream();
            }           
            catch (IOException e) {
               //TODO er... handle this?
               e.printStackTrace();
            }
            JSONObject json = (JSONObject) JSONSerializer.toJSON(toString(stream));  
            accesstoken = json.getString("accessToken");
            clienttoken = json.getString("clientToken");
            System.out.println(json.toString());
            profile = json.getJSONObject("selectedProfile").getString("id");
            System.out.println("So the dick is: " + hc.getResponseMessage() + " and the puss: " + accesstoken + " and er: " + clienttoken + " profile is " + profile);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static String sendPostRequest(String data, String Adress) {
         
        String answer = "No";
         
            try {
                
                // Send the request
                URL url = new URL(Adress);
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                
                //write parameters
                writer.write(data);
                writer.flush();
                
                // Get the response
                StringBuffer enswer = new StringBuffer();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    enswer.append(line);
                }
                writer.close();
                reader.close();
                
                //Output the response
                
                answer = enswer.toString();
               
                
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return answer;
    }
    
    private static String urlEncode(String par0Str) throws IOException {
        return URLEncoder.encode(par0Str, "UTF-8");
    }
    
    
    public void writeByteArray(DataOutputStream par0DataOutputStream, byte[] par1ArrayOfByte) throws IOException
    {
        par0DataOutputStream.writeShort(par1ArrayOfByte.length);
        par0DataOutputStream.write(par1ArrayOfByte);
    }
     
     public byte[] readBytesFromStream(DataInputStream par0DataInputStream) throws IOException{
            short var1 = par0DataInputStream.readShort();

            if (var1 < 0)
            {
                throw new IOException("Key was smaller than nothing!  Weird key!");
            }
            else
            {
                byte[] var2 = new byte[var1];
                par0DataInputStream.readFully(var2);
                return var2;
            }
        }
     

}