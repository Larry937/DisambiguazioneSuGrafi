package tesi;

import java.awt.Toolkit;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import twitter4j.IDs;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;


public class Tesi {

    
    public static void main(String[] args) throws TwitterException, FileNotFoundException, UnsupportedEncodingException, IOException{
        
        //Configurazione istanza Twitter
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
            .setOAuthConsumerKey("JJqCtqltSb8OYqUtP7UD3XsvA")
            .setOAuthConsumerSecret("Uywl9Y9kImizH5DjPxjsOuD4G171Rbd6c445F1l5k6WWjhOe3D")
            .setOAuthAccessToken("2385317408-fE7M14RCwoiWlKh86asuV7wJcjwV0hOICryNmAx")
            .setOAuthAccessTokenSecret("1HURTxXMeEXOBt7kgzH7NFAPX7girnd6t9IFJ2L06hswM");
        TwitterFactory tf = new TwitterFactory(cb.build());
        
        
        getNames(tf);
        getFriends(tf);
        int[] connesse= new Tesi.Connected().cc();
        printResult(connesse);        
        Toolkit.getDefaultToolkit().beep();
    }
    
    
    
    /**************************************************************************/
    public static void getNames(TwitterFactory tf){
        
       // file da aprire
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter=new FileNameExtensionFilter("CSV File","csv");
        chooser.addChoosableFileFilter(filter);
        chooser.showOpenDialog(null);
        File toRead=chooser.getSelectedFile();
        String readPath=toRead.getAbsolutePath();
        File iniziale=new File ("Nomi.txt");
        int cont=0;
       
        // Variabile per la riga e per la lista dei nomi iniziali
        String line = null;
        
        //Trova ID iniziali
        try {
            
            FileReader fileReader = new FileReader(readPath);
            PrintStream fileWriter= new PrintStream(iniziale);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            
            while((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
                try {
                    
                    Twitter twitter = tf.getInstance();
                    ResponseList<User> users;
                        users = twitter.searchUsers(line.replace(",", ""), 1);
                        for (User user : users) {
                               fileWriter.println(line.replace(",", "")+" ; "+user.getScreenName()+" "+user.getId());
                               cont++;
                        }    
                    
                } catch (TwitterException te) {
                    te.printStackTrace();
                    System.out.println("Failed to search users: " + te.getMessage());
                    System.exit(-1);
                }
            }
            bufferedReader.close();
            fileWriter.flush();
            fileWriter.close();
            fileReader.close();
        }
        catch(IOException ex) {
            ex.printStackTrace();
            System.out.println("Errore nella lettura del file '"+ toRead + "'");                  
        }
        System.out.println("Lettura file completata, id:"+cont); 
    }
    /*****************************************************************************/
    public static void getFriends(TwitterFactory tf) throws FileNotFoundException, IOException{
        //Definizione variabili
        ArrayList<ArrayList<String>> resultScan=scanId();
        ArrayList<String> idFile=resultScan.get(1);
        ArrayList<String> nomi=resultScan.get(0);
        System.out.println("id");
        for (int i=0;i<idFile.size();i++){
            System.out.println(idFile.get(i));
        }
        System.out.println("NOMI");
         for (int i=0;i<nomi.size();i++){
            System.out.println(nomi.get(i));
        }
        // Vector per contenere i dati del grafo
        ArrayList <ArrayList<String>> graph =new ArrayList<>();
        ArrayList <String> alUser;
        ArrayList <String> vuoto=new ArrayList();
        vuoto.add("0");
        //Ricerca degli amici
        int contID=0;
        IDs ids;
        int limit=0;
        long cursor;
        int index=0;
     
        float add=((idFile.size()*70)/60)/60;
        
        String info="Ricerca degli amici in corso. La ricerca terminerà indicativamente fra "+add+" ore";
        
        JOptionPane.showMessageDialog(null,info, "",JOptionPane.INFORMATION_MESSAGE);
        
        for (int i=0;i<nomi.size();i++){
            System.out.println("nomi"+i+":"+nomi.get(i));
        }
        
        for(int i=0; i< idFile.size(); i++){
            // Prova richiesta Twitter
            try {
                Twitter twitter = tf.getInstance(); 
                
                // Comandi da eseguire per ogni account
                alUser= new ArrayList();
                alUser.add(idFile.get(i));
                
                
                cursor = -1;
                limit=0;
                loop:do{
                    ids = twitter.getFriendsIDs(Long.parseLong(idFile.get(i)), cursor);
                    for (long id : ids.getIDs()) {    
                        //Comandi da eseguire per ogni amico trovato
                        if (limit<10000){
                            intLoop:for (int l=0;l<idFile.size();l++){
                                System.out.println(nomi.get(index));
                                //System.out.println("nome1:"+nomi.get(l)+" nome2:"+nomi.get(index));
                                if ((Long.parseLong(idFile.get(l))==id)&&(!nomi.get(l).equals(nomi.get(index)))){
                                    alUser.add(Long.toString(id));
                                break intLoop;
                                }
                            }

                            limit++;
                        }
                        else{

                            break loop;
                        }
                    }
                }while ((cursor = ids.getNextCursor()) != 0);
                index++;
                contID++;
                graph.add(alUser);
                System.out.println("ID"+contID+" fatto.");
                
                //Il programma attende 
                try {
                    TimeUnit.SECONDS.sleep(70);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("Attesa fallita");
                }
               
            } catch (TwitterException te) {
                te.printStackTrace();
                graph.add(vuoto);
                System.out.println("Richiesta fallita: " + te.getMessage());
            }
        }
        System.out.println("Graph size:"+graph.size());
        writeAdj(graph);
        writeGraphml(graph,idFile);
    }
    /****************************************************************************/
    public static ArrayList scanId() throws FileNotFoundException{
        File file= new File("Nomi.txt");
        Scanner scan=new Scanner(file);
        ArrayList<String> ids=new ArrayList();
        ArrayList<String> names=new ArrayList();
        String tempName=new String();
        int count=1;
        while(scan.hasNext()) {
            String word = scan.next();
            switch (count){
                case 1:
                    tempName=new String();
                    tempName=word;
                    count++;
                    break;
                case 2:
                    tempName=tempName+" "+word;
                    count++;
                    break;
                case 3:
                    if (word.equals(";")){
                        names.add(tempName);
                        count++;
                    }
                    else{
                      tempName=tempName+" "+word;  
                    }
                    break;
                case 5:
                    ids.add(word);
                    count=1;
                    break;
                default:
                    count++;
            }
        }
        scan.close();
        ArrayList<ArrayList<String>> res= new ArrayList<ArrayList<String>>();
        res.add(names);
        res.add(ids);
        return res;
    }
    /****************************************************************************/
    public static void printGraph(ArrayList graph){
        for(int i = 0; i < graph.size(); i++){
            ArrayList temp=(ArrayList)graph.get(i);
          for(int j = 0; j < temp.size(); j++){
              System.out.println(temp.get(j)); 
          }
        System.out.println();
        }
    }
    /***************************************************************************/
    public static void writeAdj(ArrayList graph) throws FileNotFoundException{
        File file=new File ("Adj.txt");
        PrintStream ps=new PrintStream(file);
        for(int i = 0; i < graph.size(); i++){
            ArrayList temp=(ArrayList)graph.get(i);
          for(int j = 0; j < temp.size(); j++){
            ps.println(temp.get(j));
          }
        ps.println("<end>");
        }
    }
    /****************************************************************************/
    public static ArrayList<ArrayList<String>> scanAdj() throws FileNotFoundException{
        File file= new File("Adj.txt");
        Scanner scan=new Scanner(file);
        ArrayList<ArrayList<String>> graph=new ArrayList();
        ArrayList temp=new ArrayList();
       
        while(scan.hasNext()) {
            String word=scan.next();
            if("<end>".equals(word)){
                graph.add(temp);
                temp=new ArrayList();
            }
            else{
                temp.add(word);
            }
        }
        scan.close();
        return graph;
    }
    /***************************************************************************/
    public static void writeCSV (ArrayList graph) throws IOException{
            File edge=new File ("edges.csv");
            String start;
            ArrayList<String> temp;
            try {
                PrintStream edges= new PrintStream(edge);
            
            edges.println("Source;Target;");
            
            for(int i = 0; i < graph.size(); i++){
                temp=(ArrayList)graph.get(i);
                start=temp.get(0);
                for(int j = 1; j < temp.size(); j++){
                        edges.println(start+";"+temp.get(j));
                }
            }
          
            }catch(FileNotFoundException ex) {
                ex.printStackTrace();
            System.out.println("Incapace di creare il file ");                
            }
    }
    /********************************************************************************************/
    public static void writeGraphml(ArrayList graph, ArrayList id) throws FileNotFoundException{
        File gml=new File("grafo.graphml");
        String start;
        ArrayList<String> temp;
        ArrayList <String> edges = new ArrayList();
        
        PrintStream ps=new PrintStream(gml);
        
        ps.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        ps.println("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\"> ");
        ps.println("<graph id=\"G\" edgedefault=\"directed\"> ");
        
        for(int i = 0; i < graph.size(); i++){
                temp=(ArrayList)graph.get(i);
                start=temp.get(0);
                ps.println("<node id=\" "+start+" \"/>");
                for(int j = 1; j < temp.size(); j++){
                    for (int l=0;l<id.size();l++){
                        if (temp.get(j)==id.get(l)){
                            ps.println("<node id=\" "+temp.get(j)+" \"/>");
                            break;
                        }
                    }
                    edges.add("<edge source=\" "+start+" \" target=\" "+temp.get(j)+" \"/>");
                }
        }
        
        for(int i=0;i<edges.size();i++){
            ps.println(edges.get(i));
        }
        
        ps.println("</graph>");
        ps.println("</graphml>");
        System.out.println("Graphml done!");
    }
    /**********************************************************************************************/
    public static class Connected {
        ArrayList<ArrayList<String>> graph;
        boolean[] visited;
        int[] probab;
        ArrayList<ArrayList<String>> components;
        ArrayList<ArrayList<String>> results;
        
        public int[] cc() throws FileNotFoundException {
          this.graph = scanAdj();

          visited = new boolean[graph.size()];
          probab = new int[graph.size()];
          components = new ArrayList<>();

          for (int u = 0; u < graph.size(); u++){
            if (!visited[u]) {
                  visited[u]=true;
                  ArrayList tempConn=new ArrayList();
                  tempConn.add(graph.get(u).get(0));
                  dfs(u,tempConn);
                  probab[u]+=tempConn.size();
                  components.add(tempConn);
                for (int i=0;i<visited.length;i++){
                    visited[i]=false;
                }
            }
          }
            for (int i=0;i<graph.size();i++){
                System.out.println(i+": "+components.get(i));
            }
          return probab;
        }

        void dfs(int u, ArrayList tempConn) {
          ArrayList temp=graph.get(u);
          for (int i=1;i<temp.size();i++){
              tempConn.add(temp.get(i));
              //trovare l'indice e fare la dfs
              for(int h=0;h<graph.size();h++){
                  if (temp.get(i).equals(graph.get(h).get(0))){
                      if(!visited[h]){
                          visited[h]=true;
                          probab[h]+=1;
                          dfs(h,tempConn);
                        }
                    break;
                  }
              }
          }
        }
    }
    /**************************************************************************/
    public static void printResult(int[] rating) throws FileNotFoundException{
        File file= new File("Nomi.txt");
        Scanner scan=new Scanner(file);
        File results=new File ("Risultati.csv");
        PrintStream writer= new PrintStream(results);
        int count=0;
        int index=0;
        String word="";
        String oldName="";
        String tempName="";
        String tempUsr="";
        while(scan.hasNext()){
            word=scan.next();
            switch (count){
                case 0:
                    tempName=word;
                    count+=1;
                    break;
                case 1:
                    tempName=tempName+" "+word;
                    count++;
                    break;
                case 2:
                    if (word.equals(";")){
                        count++;
                    }
                    else{
                      tempName=tempName+" "+word;  
                    }
                    break;
                case 3:
                    tempUsr=word;
                    count++;
                    break;                   
                    
                case 4:
                    if (tempName.equals(oldName)){
                       writer.println(";"+tempUsr+";"+word+";"+rating[index]+";");
                    }
                    else{
                       writer.println(tempName+";"+tempUsr+";"+word+";"+rating[index]+";");
                       oldName=tempName;
                    }
                    count=0;
                    tempName="";
                    tempUsr="";
                    index++;
                    break;
            }
        }
    }
    /**************************************************************************/
}
