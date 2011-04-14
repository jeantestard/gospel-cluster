package fr.crim.a2010;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.FileReader;

/**
 * 
 * Créé un indexe dans le répertoire spécifié
 * à partir du fichier texte csv entré
 * Crée un Indexwriter Lucene
 * Ajoute les documents à l'index.
 * 
 */
public class Indexer extends Bibluc {

  public static void main(String[] args) throws Exception {
    
	  if (args.length != 2) {
      throw new IllegalArgumentException("Usage: java " + Indexer.class.getName()
        + " <fichierSource> <repertoireIndex>");
    }

    
    Bibluc schema=new Indexer();
    String csv=args[0];
    BufferedReader br=schema.configure(new File (csv));

    String indexDir=args[1];
    Directory dir = FSDirectory.open(new File(indexDir));
    
    
    
    
    IndexWriter writer = new IndexWriter(dir, new FrenchAnalyzer(Version.LUCENE_CURRENT),
          IndexWriter.MaxFieldLength.UNLIMITED);
     writer.setInfoStream(System.out);

String line;
while ((line = br.readLine()) != null) {
Document doc = new Document();
doc.add(new Field("contents", schema.fields(line).get(TEXTE), Field.Store.YES, Field.Index.ANALYZED));
doc.add(new Field("verset", schema.fields(line).get(VERSET), Field.Store.YES, Field.Index.NOT_ANALYZED));
doc.add(new Field("livre", schema.fields(line).get(LIVRE), Field.Store.YES, Field.Index.NOT_ANALYZED));
doc.add(new Field("chapitre", schema.fields(line).get(CHAPITRE), Field.Store.YES, Field.Index.NOT_ANALYZED));

writer.addDocument(doc);
				}
    
writer.optimize();
writer.close();

  }  
    
 }   
    
    