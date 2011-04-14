package fr.crim.a2010;



import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;


/**
 * Test minimal d'extension de la classe abstraite
 * Exemple d'utilisation
 * 
 * java -cp build/:lib/lucene-core-3.0-dev.jar:lib/lucene-analyzers-3.0-dev.jar fr.crim.a2010.BiblucTest -h
 *
 * @author test
 */
public class Evaluateur extends Bibluc {
  /** 
   * Instanciation d'un schéma lucene de cette classe, 
   * interprétation des arguments de la ligne de commande
   */
  public static void main(String[] args) throws Exception  {
	  
	  if (args.length != 3) {
	      throw new IllegalArgumentException("Usage: java " + Indexer.class.getName()
	        + " <Index> <Requete> <nombre de resultats>");
	    }
	  
    Bibluc schema=new Partitionneur();
//    String csv="Bibles/french_lsg_utf8.txt";
//    BufferedReader br=schema.configure(new File (csv));            

    String indexDir=args[0];
    String requete=args[1];
    int nbres=Integer.valueOf(args[2]);
    
    Directory dir = FSDirectory.open(new File(indexDir));

    
    
    schema.search(indexDir,requete,nbres);
//   ex. "propitiatoire"
    HashMap<String,Set<String>> resultats = schema.recherche(indexDir,requete);


    Partitionnement.evaluation(resultats, Partitionnement.MESURE_COSINUS);
  
    //  Partitionnement.evaluation(resultats, Partitionnement.INDICE_JACCARD);
    //TestCluster.evaluation(resultats, TestCluster.EDIT_DISTANCE);
  }

 
  
}
