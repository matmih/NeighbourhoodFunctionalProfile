/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eukaryoticdatasetcreator;

import OntologyTools.GOTerm;
import OntologyTools.GeneOntology;
import static eukaryoticdatasetcreator.CreateReducedMappingsFile.ENCODING;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.javatuples.Pair;

  /**
 * @author Matej Mihelcic
  *@institution Rudjer Boskovic Institute, Zagreb, Croatia
  *@mail matmih1@gmail.com
  *@description class containing functions to compute the number of genes with newly predicted GO function given a precision treshold (eukaryotic organisms)
  * input parameters
 * 0 - use parallel computation
 * 1 - number of parallel threads
 * 2 - method to be used (0 - GFP, 1 - LocLog)
 * 3 - use all functions (0) or only functions with IC>4 (1)
 * 4 - precisionTreshold (numeric [0,1])
 * 5- ICTreshold (numeric)
 */
 
public class ComputingPredictionInformation {
    
    static public void main(String[] args){
        
        final ClusPredictionInfo predInfo=new ClusPredictionInfo();
        final HashMap<String,HashSet<Pair<String,String>>> geneOGMap=new HashMap<String,HashSet<Pair<String,String>>>();
              
        String geneOGMapFile = "geneOGMappingFinalOKNewnonCH.txt"; //fungy
        
            BufferedReader reader=null;
            
             try {
                     Path path =Paths.get(new File(geneOGMapFile).getAbsolutePath());
                     reader = Files.newBufferedReader(path,ENCODING);
                      String line = null;
                     
                      while ((line = reader.readLine()) != null) {
                            String tmp[] = line.split(" ");
                            String gene = tmp[0].trim();
                            
                            if(!geneOGMap.containsKey(gene)){
                                geneOGMap.put(gene, new HashSet<Pair<String,String>>());
                            }
                            
                            HashSet<Pair<String,String>> ogs=geneOGMap.get(gene);
                            
                            for(int i=1;i<tmp.length;i++){
                                String og[]=tmp[i].split(":");
                               Pair<String, String> np = new Pair(og[0].trim(),og[1].trim());
                                
                               ogs.add(np);
                            }
                            geneOGMap.put(gene, ogs);
                    }
      reader.close();
       }
         catch(Exception e){e.printStackTrace();}

        final ArrayList<HashMap<String,HashSet<Pair<String,String>>>> ogMaps=new ArrayList<>();
        
         File ogFuncFile=new File("NogFunctionsProp3_10_30NewnonCHF.txt");//fungy
        OGGOMapping cgmap=new OGGOMapping();
        
        cgmap.createCOGGOMapping(ogFuncFile);
          
        for(int i=0;i<Integer.parseInt(args[1]);i++){
            HashMap<String,HashSet<Pair<String,String>>> tmp=new HashMap<String,HashSet<Pair<String,String>>>(geneOGMap);
            ogMaps.add(tmp); 
            System.out.println("Copied map "+(i+1));
        }
       
        File predInput=null;
		
        int method=Integer.parseInt(args[2]);
        
         if(method==0){
            predInput=new File("BaselineOGsFungyNew.train.1.pred.arff");
       
         }
         else if(method==1){
            predInput=new File("DistanceLocLogD.train.1.pred.arff");
        }
         else if(method==2){
           // predInput=new File("genome5.train.1.pred.arff");
            predInput=new File("genomeFungy.train.1.pred.arff");
            
         }
         else if(method==3){
             predInput=new File("GFP.train.pred.arff");
         }
        
        predInfo.readPredictionInfo(cgmap, predInput);
        
        GOMap mapMod=new GOMap();
        mapMod.CreateGOMap(cgmap);
        File fileWithUniprotFrequencyCounts=new File("GOFrequency.txt");//new File("Uniprot-freqs-2070_organisms-Uniprot-GOA-2013-12-10.txt");//change
        mapMod.loadFrequencies(fileWithUniprotFrequencyCounts);
        
      Iterator<String> gos=cgmap.GOtoIndex.keySet().iterator();
        
       ArrayList<Integer> global=new ArrayList<>();
       global.add(0); global.add(0); global.add(0); global.add(0);
        int count=0, parallel=Integer.parseInt(args[0]), onlySpec=Integer.parseInt(args[3]);
       //params: parallel (0-no, 1-yes), method (0-GFP, 1-LocLogDist), onlySpec (0-allFunctions, 1-IC>4 only)
        
     if(parallel==0){  
          double ICThr=Double.parseDouble(args[5]);
          double positive=0.0,negative=0.0;
          
          if(ICThr>=0.0){
              positive=1.0;
              negative=0.0;
          }
          
          if(ICThr<0.0){
              positive=0.0;
              negative=1.0;
          }
          
      while(gos.hasNext()){
          String go=gos.next();
          
           if(onlySpec==1){
               if(!mapMod.frequency.containsKey(mapMod.GOmap.get(go)))
                   continue;
          double ICTest=mapMod.frequency.get(mapMod.GOmap.get(go));
             
        if(positive==1.0){  
        if(Math.abs(Math.log10(ICTest)/Math.log10(2))<=ICThr){
            ++count;
            continue;
        }
       }
        else if(negative==1.0){
             if(Math.abs(Math.log10(ICTest)/Math.log10(2))>=ICThr){
            ++count;
            continue;
        }
        }
      }
          
          System.out.println("Function number: "+(++count));
          go=go.replace("GO", "GO:");
          System.out.println("go: "+go);

        ArrayList<Double> labs=predInfo.originalLabels.get(go);
        ArrayList<Double> classScores=predInfo.classifierScores.get(go);
        System.out.println("Velicina: "+classScores.size());
        ArrayList<Double> sortedScores=predInfo.sortedScores(classScores);
         
         double maxRec=0.0, score=0.0, scorePrev=-1, precTreshold=0.5,precision=0.0;
         
         precTreshold=Double.parseDouble(args[4]);
        
          for(int i=0;i<sortedScores.size();i++){
               if(scorePrev==sortedScores.get(i)){
                   scorePrev=sortedScores.get(i);
                   continue;
               }
               else{
                ArrayList<Double> predictedClass=predInfo.predictedClass(classScores, sortedScores.get(i));
                ArrayList<Double> preRec=predInfo.precRec(predictedClass, labs);
         
                if(preRec.get(0)>=precTreshold){
                    if(preRec.get(1)> maxRec){
                        maxRec=preRec.get(1);
                        precision=preRec.get(0);
                        score=sortedScores.get(i);
                        scorePrev=sortedScores.get(i);
                    }
                }
          }
          }
          
          System.out.println("score: "+score);
          System.out.println("maxRec: "+maxRec);
          System.out.println("precision: "+precision); 
          
         ArrayList<Double> predictedClass=predInfo.predictedClass(classScores, score);
         ArrayList<Integer> difference=predInfo.numberOfNewGenes(predictedClass, labs, geneOGMap);

         System.out.println("difference: "+difference.get(0)+" "+difference.get(1)+" "+difference.get(2)+" "+difference.get(3));
         
         global.set(0, global.get(0)+difference.get(0));
         global.set(1, global.get(1)+difference.get(1));
         global.set(2, global.get(2)+difference.get(2));
         global.set(3, global.get(3)+difference.get(3));        
      }
      
      System.out.println("Global difference: ");
      System.out.println("Original: "+global.get(0)+" "+"Predicted: "+global.get(1)+" "+"Difference: "+global.get(2)+" New: "+global.get(3));
     }
     else{
        double ICThr=Double.parseDouble(args[5]);
         ExecutorService exec = Executors.newFixedThreadPool(Integer.parseInt(args[1]));
                final Lock lock = new ReentrantLock();
                final Lock lock1 = new ReentrantLock();
                final ArrayList<Integer> globResults=new ArrayList<>();
                final int numTr=Integer.parseInt(args[1]);
      
    try {
        int k=0;
       while(gos.hasNext()){
          String go=gos.next();
     
          if(onlySpec==1){
              if(!mapMod.GOmap.containsKey(go)){
                  System.out.println(go+" not contained in mapMod.GoMap!");
                  System.out.println();
                  continue;
              }
              if(!mapMod.frequency.containsKey(mapMod.GOmap.get(go))){
                  System.out.println(go);
                  System.out.println("Index: "+mapMod.GOmap.get(go));
                  System.out.println("Not contained in mapMod.freq");
                  continue;
              }
          double ICTest=mapMod.frequency.get(mapMod.GOmap.get(go));
             
        if(Math.abs(Math.log10(ICTest)/Math.log10(2))<=ICThr){
            ++count;
            continue;
        }
      }
          
          go=go.replace("GO", "GO:");
          
          final String goFin=go;
          final int count1=(++count);
          final double prec= Double.parseDouble(args[4]);
          final HashMap<String,HashSet<Pair<String,String>>> ogs=ogMaps.get((k++)%numTr);
          
          exec.submit(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("Function number: "+(count1));
                            System.out.println("go: "+goFin);
                            ArrayList<Double> labs=predInfo.originalLabels.get(goFin);

                            ArrayList<Double> classScores=predInfo.classifierScores.get(goFin);
                            System.out.println("Velicina class: "+classScores.size());
                            System.out.println("Velicina labs: "+labs.size());
                            ArrayList<Double> sortedScores=predInfo.sortedScores(classScores);
                            
                                double maxRec=0.0, score=0.0, scorePrev=-1, precTreshold=0.5,precision=0.0;
        
                                 precTreshold=prec;
                                
                            for(int i=0;i<sortedScores.size();i++){
                                  if(scorePrev==sortedScores.get(i)){
                                       scorePrev=sortedScores.get(i);
                                       continue;
                                     }
                                   else{
                                     ArrayList<Double> predictedClass=predInfo.predictedClass(classScores, sortedScores.get(i));
                                     ArrayList<Double> preRec=predInfo.precRec(predictedClass, labs);
         
                                         if(preRec.get(0)>=precTreshold){
                                               if(preRec.get(1)> maxRec){
                                                     maxRec=preRec.get(1);
                                                     precision=preRec.get(0);
                                                     score=sortedScores.get(i);
                                                     scorePrev=sortedScores.get(i);
                                              }
                                             }
                                        }
                                 }
                                                        
                             System.out.println("score: "+score);
                             System.out.println("maxRec: "+maxRec);
                             System.out.println("precision: "+precision); 
                             System.out.println("precTresh: "+precTreshold);
                        ArrayList<Double> predictedClass=new ArrayList<>(Collections.nCopies(labs.size(), 0.0));
                           if(precision>=precTreshold) 
                                 predictedClass=predInfo.predictedClass(classScores, score);
                           
                           ArrayList<Integer> difference=null;
                            
                            if(precision<precTreshold){
                                difference.set(1,0); difference.set(2, -difference.get(0)); difference.set(3, 0);
                            }
                             System.out.println("difference: "+difference.get(0)+" "+difference.get(1)+" "+difference.get(2)+" "+difference.get(3));
                             
                              try{
                                  lock.lock();
                                  try{
                                    globResults.add(difference.get(0));
                                    globResults.add(difference.get(1));
                                    globResults.add(difference.get(2));
                                    globResults.add(difference.get(3));
                                  }finally{
                                            lock.unlock();
                                        }
                              }catch( Exception e ) {
                                    e.printStackTrace();
                                    }
                        }
                          }); 
      }
      
      System.out.println("Global difference: ");
      System.out.println("Original: "+global.get(0)+" "+"Predicted: "+global.get(1)+" "+"Difference: "+global.get(2));  
         
         
     }
    catch(Exception e){
                        e.printStackTrace();
                    }
                finally {
                        exec.shutdown();
                        
                    }
                
                try {
                    exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                
                for(int i=0;i<globResults.size();i++){
                    if(i%4==0){
                        global.set(0, global.get(0)+globResults.get(i));
                    }
                    else if(i%4==1){
                        global.set(1, global.get(1)+globResults.get(i));
                    }
                    else if(i%4==2){
                        global.set(2, global.get(2)+globResults.get(i));
                    }
                     else if(i%4==3){
                        global.set(3, global.get(3)+globResults.get(i));
                    }
                }
                
                System.out.println("Global difference: ");
                System.out.println("Original: "+global.get(0)+" "+"Predicted: "+global.get(1)+" "+"Difference: "+global.get(2)+" new:"+global.get(3));           
       }    
    }
    
    
}
