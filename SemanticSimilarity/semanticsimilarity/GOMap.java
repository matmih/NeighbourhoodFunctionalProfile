package semanticsimilarity;


import eukaryoticdatasetcreator.OGGOMapping;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import recursivefilesearch.COGGOMap;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Matej Mihelcic
  *@institution Rudjer Boskovic Institute, Zagreb, Croatia
  *@mail matmih1@gmail.com
  *@description class to store information about and print GOMap
 */
public class GOMap {
    final static Charset ENCODING = StandardCharsets.UTF_8;
      public HashMap<String,Integer> GOmap=new HashMap<>();
      public HashMap<Integer,String> GOmapNumeric=new HashMap<>();
      public HashMap<Integer,Double> frequency=new HashMap<>();
      public String hierarchy;
      
      public void CreateGOMap(File input){
               BufferedReader reader;
          try{
            Path path =Paths.get(input.getAbsolutePath());
      reader = Files.newBufferedReader(path,ENCODING);
      String line = null;
      
      while ((line = reader.readLine()) != null) {
          if(line.contains("class hierarchical")){
              String tmp[]=line.split(" ");
            String functions=tmp[3];
            hierarchy=tmp[3];
            String f[]=functions.split(",");
            
            int count=0;
            for(int i=0;i<f.length;i++){
                String fun[]=f[i].split("/");
                for(int j=0;j<fun.length;j++){
                    if(fun[j].length()<7 && !fun[j].contains("root")){
                    while(fun[j].length()<7)
                        fun[j]="0"+fun[j];
                    fun[j]="GO"+fun[j];
                    }
                    else{
                        if(!fun[j].contains("GO") && !fun[j].contains("root"))
                       fun[j]="GO"+fun[j]; 
                    }
                if(!GOmap.containsKey(fun[j])){
                    GOmap.put(fun[j], count);
                    GOmapNumeric.put(count, fun[j]);
                    ++count;
                }
                }
            }
            break;
           }
        }
      reader.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
      }
      
      public void CreateGOMap(COGGOMap map){
          
         int count=0;
         
         for(int i=0;i<map.GOtoIndex.keySet().size();i++){
             String go=map.IndexToGO.get(i);//it.next();
             count=i;
             if(!GOmap.containsKey(go)){
                    GOmap.put(go, count);
                    GOmapNumeric.put(count, go);
                }
             
         }
          
      }
      
       public void CreateGOMap(OGGOMapping map){
          
         int count=0;
         
         for(int i=0;i<map.GOtoIndex.keySet().size();i++){
             String go=map.IndexToGO.get(i);//it.next();
             count=i;
             if(!GOmap.containsKey(go)){
                    GOmap.put(go, count);
                    GOmapNumeric.put(count, go);
                }
             
         }
          
      }
      
      public void printGOMap(){
          Iterator<String> it=GOmap.keySet().iterator();
          
          while(it.hasNext()){
              String GO=it.next();
              System.out.println("GO: "+GO+" Numeric key: "+GOmap.get(GO));
          }
      }
      
      public void printGOMapNumeric(){
          Iterator<Integer> it=GOmapNumeric.keySet().iterator();
          
          while(it.hasNext()){
              int GONum=it.next();
              System.out.println("Numeric key: "+GONum+" GO: "+GOmapNumeric.get(GONum));
          }
      }
      
           public void saveGOMap(File output){
            try
            {
                FileWriter fw = new FileWriter(output);
                
                Iterator<String> it=GOmap.keySet().iterator();
                
                while(it.hasNext()){
                    String go=it.next();
                    int id=GOmap.get(go);
                    fw.write(go+"\t"+id+"\n");
                }
                
                fw.close();
            }
           catch(Exception e){
                e.printStackTrace();
            }
   }
      
      public void saveGO(File output){
            try
            {
                FileWriter fw = new FileWriter(output);
                
                Iterator<String> it=GOmap.keySet().iterator();
                
                while(it.hasNext()){
                    String go=it.next();
                    go=go.substring(2);
                    fw.write(go+"\n");
                }
                
                fw.close();
            }
           catch(Exception e){
                e.printStackTrace();
            }
   }
      
      public void loadFrequencies(File fileWithUniprotFrequencyCounts){
          BufferedReader bufRdr=null;
           try 
        {
            bufRdr = new BufferedReader(new FileReader(fileWithUniprotFrequencyCounts));
            String line;
            
            while ((line = bufRdr.readLine()) != null)
            {
                if (line.startsWith("#")) //header
                    continue;
                
                String[] parts = line.split("\t");
                
                if(parts[0].length()<7)
                    while(parts[0].length()<7)
                        parts[0]="0"+parts[0];
                
                 parts[0]="GO"+parts[0];   
                 
                int index=0;
                
                if(GOmap.containsKey(parts[0]))
                    index=GOmap.get(parts[0]);
                else continue;
                
                double generality = Double.parseDouble(parts[1]);
                frequency.put(index, generality);
            }
            frequency.put(0, 1.0);
            bufRdr.close();
        }
           catch(Exception e){
               e.printStackTrace();
           }
      }
      
       public void loadFrequencies1(File fileWithUniprotFrequencyCounts){
          BufferedReader bufRdr=null;
           try 
        {
            bufRdr = new BufferedReader(new FileReader(fileWithUniprotFrequencyCounts));
            String line;
            
            while ((line = bufRdr.readLine()) != null)
            {
                if (line.startsWith("#")) //header
                    continue;
                
                String[] parts = line.split("\t");
                
                if(parts[0].length()<7)
                    while(parts[0].length()<7)
                        parts[0]="0"+parts[0];
                
                 parts[0]="GO"+parts[0];   
                 
                int index=0;
                    
                if(GOmap.containsKey(parts[0]))
                    index=GOmap.get(parts[0]);
                else continue;
                
                double generality = Double.parseDouble(parts[1]);
                frequency.put(index, generality);
            }
            bufRdr.close();
        }
           catch(Exception e){
               e.printStackTrace();
           }
      }
      
      public void printFrequencies(){
          Iterator<Integer> it=frequency.keySet().iterator();
          
          while(it.hasNext()){
              int GONum=it.next();
              System.out.println("Numeric key: "+GONum+" GO: "+GOmapNumeric.get(GONum)+" Frequency: "+frequency.get(GONum));
          }
      }
      
      double computeScore(int index){
          
          if(!frequency.containsKey(index))
              System.out.println("not contained index: "+index);
          
          return -(Math.log10(frequency.get(index))/Math.log10(2));//base 2 log
      }
      
}
