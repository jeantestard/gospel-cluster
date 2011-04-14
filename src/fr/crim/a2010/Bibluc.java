package fr.crim.a2010;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


/**
 * Exploiter des bibles http://unbound.biola.edu/ avec lucene, une classe d'indexation et de requête,
 * afin d'assurer le partage d'un même analyseur.
 * La classe est abstraite, imitant les constructions de Lucene (ex Analyzer).
 * Elle fournit des méthodes toutes prêtes mais ne peut pas être instanciée.
 */

public abstract class Bibluc {
  /** Analyseur en cours */
  Analyzer analyzer=analyzer();
  /** 
   * Création d'un analyseur par défaut, peut être surchargé.
   */
  public Analyzer analyzer() {
    this.analyzer = new SimpleAnalyzer();
    return this.analyzer;
  }
  


  /** 
   * Pour appel en ligne de commande
   * Interprétation des arguments par introspection de la classe selon la syntaxe
   * (-method "Argument 1"? "Argument 2"?)* 
   * 
   * @throws IllegalAccessException    Si la méthode est protégée
   * @throws IllegalArgumentException  Au cas où une méthode serait invoquée avec une erreur d'argument (testé)
   * @throws InvocationTargetException Emballage des exceptions lancées par les méthodes invoquées
   */
  public void cli(String[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException  {
    String option;
    Method method;
    for (int i=0; i < args.length ; i++) {
      option=args[i];
      // ce n'est pas une option, faire quelque chose ?
      if (!option.startsWith("-")) continue;
      option=option.substring(1);
      // option sans argument
      try {
        method=this.getClass().getMethod(option, null);
        method.invoke(this, null);
        continue;
      }
      // la méthode n'existe pas sans argument
      catch(NoSuchMethodException e){};
      // option avec un argument
      try {
        method=this.getClass().getMethod(option, String.class);
        String[] val={args[i+1]};
        method.invoke(this, (Object[])val);
        i++;
        continue;
      } 
      // la méthode n'existe pas avec un argument
      catch(NoSuchMethodException e){}
      // plus d'argument à prendre
      catch(ArrayIndexOutOfBoundsException e) {
        System.out.println("-"+option+" l'option attend un paramètre.");
      }
      // option avec 2 arguments
      try {
        Class[] cl={String.class, String.class};
        method=this.getClass().getMethod(option, cl);
        String[] val={args[i+1], args[i+2]};
        method.invoke(this, (Object[])val);
        i++; i++;
        continue;
      }         
      // la méthode n'est pas connue, alerter l'utilisateur
      catch(NoSuchMethodException e){
        System.out.println();
        System.out.println("-"+option+" n'est pas une option connue.");
        System.out.println();
        this.h();
        System.exit(2);
      }
      // moins de 2 argument
      catch(ArrayIndexOutOfBoundsException e) {
        System.out.println("-"+option+" l'option attend deux paramètres.");
      }
    }
  }

  /**
   * Aide pour la cli
   */
  public void h() {
    System.out.println(this.getClass().getSimpleName()+" Aide");
    System.out.println("Analyseur : " + this.analyzer);
    System.out.println(" -h ce message.");
  }

  /**
   * Méthode de test à 1 argument
   */
  public void echo(String coucou) {
    System.out.println(coucou);
  }

  /**
   * Méthode de test à 2 arguments
   */
  public void repeat(String coucou, String fois) {
    try {
      int i=Integer.parseInt(fois);
      for(;i>0;i--) System.out.println(coucou);
    }
    // le deuxième argument n'est pas un nombre
    catch (NumberFormatException e) {
      System.out.println(coucou);
      System.out.println(fois);      
    }
  }

  /**
   * Indexer un fichier de Bible
   * @throws IOException 
   */
  public void i(String csv) throws IOException {
    BufferedReader in=configure(new File(csv));
    System.out.println(fields(in.readLine()));
  }
  /** Correspondance nom de champ colonne du fichier csv  */
  HashMap<String,Integer> field_col;
  /** Langue courante */
  String lang;
  /** Nom du fichier courant */
  String source;
  /** Formater les dates */
  final DateFormat dateForm = new SimpleDateFormat("yyyy-MM-dd");
  /** Date de l'indexation */
  String modified;
  /** Formater les nombres pour chapitres et versets */
  final NumberFormat numForm = new DecimalFormat( "000" );
  /** noms de champs */  
  static final String LIVRE="livre";
  static final String CHAPITRE="chapitre";
  static final String VERSET="verset";
  static final String TEXTE="texte";
  static final String MODIFIED="modified";
  static final String SOURCE="source";
  static final String TYPE="type";
  static final String LANGUE="langue";
  /**
   * Configurer l'indexeur sur les commentaires d'un fichier de Bible.
   * Considérer final pour ne pas être surcharger.
   * @throws IOException 
   */
  public final BufferedReader configure(File csv) throws IOException {
    // boucler sur les lignes
    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream (csv), "UTF-8" ));
    source=csv.getName();
    modified=dateForm.format(new Date());
    field_col=new HashMap<String,Integer>();
    field_col.put(LIVRE, new Integer(0));
    field_col.put(CHAPITRE, new Integer(1));
    field_col.put(VERSET, new Integer(2));
    field_col.put(TEXTE, new Integer(3));
    String[] cells;
    String line;
    while ((line = in.readLine()) != null) {
      // ce n'est plus une ligne de commentaire, remettre le pointeur au bon endroit et sortir de la boucle 
      if (!line.startsWith("#")) {
        in.reset();
        break;
      }
      // marquer la position du pointeur
      in.mark(10000);
      cells=line.split("\t");
      // parser la ligne pour les entêtes
      if (cells[0].startsWith("#language")) {
        if("ger".equals(cells[1])) lang="de"; 
        else if("fre".equals(cells[1])) lang="fr";
        else if("frn".equals(cells[1])) lang="fr";
        else if("eng".equals(cells[1])) lang="en";
        else lang=cells[1];
        continue;
      }
      else if (cells[0].startsWith("#columns")) {
        for(int i=cells.length -1; i>-1 ; i--) {
          // orig_book_index  orig_chapter  orig_verse
          if("text".equals(cells[i])) field_col.put(TEXTE, new Integer(i-1));
          else if("orig_book_index".equals(cells[i])) field_col.put(LIVRE, new Integer(i-1));
          else if("orig_chapter".equals(cells[i])) field_col.put(CHAPITRE, new Integer(i-1));
          else if("orig_verse".equals(cells[i])) field_col.put(VERSET, new Integer(i-1));
        }
        System.out.println(field_col);
        continue;
      }
    }
    return in;
  }
  /**
   * Sort les champs d'une ligne csv.
   * la méthode est considérée finale, de manière à ce que tous les index partagent 
   * les mêmes manières d'interpréter les fichiers
   * @return
   */
  final public HashMap<String,String> fields(String line) {
    if (line == null || "".equals(line)) return null;
    String[] cells=line.split("\t");
    // pas assez de cellule
    if (cells.length < field_col.get(TEXTE) +1) return null;
    HashMap<String,String> fields=new HashMap<String,String>();
    fields.put(SOURCE, source);
    fields.put(MODIFIED, modified);
    fields.put(LANGUE, lang);
    fields.put(LIVRE, cells[field_col.get(LIVRE)]);
    // identifiant de chapitre normalement unique
    fields.put(CHAPITRE,
        cells[field_col.get(LIVRE)]
          +"_"+numForm.format(Integer.parseInt(cells[field_col.get(CHAPITRE)]))
     );
    // identifiant de verset normalement unique
    fields.put(VERSET,
      cells[field_col.get(LIVRE)]
        +"_"+numForm.format(Integer.parseInt(cells[field_col.get(CHAPITRE)]))
        +"_"+numForm.format(Integer.parseInt(cells[field_col.get(VERSET)]))
     );
    // texte du verset
    fields.put(TEXTE, cells[field_col.get(TEXTE)]);
    return fields;
  }


  /**
   * Passer du texte à l'analyseur, renvoyer à la console de quoi le comprendre
   * fonctionnera en ligne de commande avec l'option
   * -a "" 
   * TODO implémenter
   *
   * @author Jugurtha 
   */
  public void a(String text) {
    System.out.println("Analyser "+text+" avec "+this.analyzer);
  }

  /**
   * Indexer les chapitres d'une bible CSV
   * TODO implémenter
   *
   * @author Richard 
   */
  public void indexChapitre(BufferedReader csv, IndexWriter index) {
	 // Indexe1(csv,index);
  }

  /**
   * Indexer les versets d'une bible CSV
   * TODO implémenter
   *
   * @author Marco
   */
  public void indexVerset(BufferedReader csv, IndexWriter index) {
	 // Indexe2(csv,index);
  }

  /**
   * Lancer une requête en ligne de commande
   * 
   * @author Jean
 * @throws IOException 
 * @throws ParseException 
   */
  public void cherche(String indexDir,String requete) throws IOException, ParseException {
	  
//	  Directory dir = new FSDirectory(new File(indexDir));
	  FSDirectory dir = FSDirectory.open(new File(indexDir));

	  
	  IndexSearcher is = new IndexSearcher(dir);
	  
	  
	  //l'analyseur du QueryParser doit être le même que celui utilisé pour l'index.
	  QueryParser parser = new QueryParser(null, "contents", this.analyzer()); 
	  Query query = parser.parse(requete);	
	  TopDocs hits = is.search(query, 10); 
	  System.err.println("Found " + hits.totalHits + " document(s) for query '" + requete + "':");
	  for(int i=0;i<hits.scoreDocs.length;i++) { 
		  ScoreDoc scoreDoc = hits.scoreDocs[i];
		  Document doc = is.doc(scoreDoc.doc);
		  System.out.println(doc.get("filename"));
	  } 
	  is.close();
  }
  
  public static void search(String indexDir, String q, int nbres)
  throws IOException, ParseException {

  Directory dir = FSDirectory.open(new File(indexDir)); 
  IndexSearcher is = new IndexSearcher(dir, true);   

  QueryParser parser = new QueryParser(Version.LUCENE_CURRENT,"contents", new FrenchAnalyzer(Version.LUCENE_CURRENT));  
  Query query = parser.parse(q);      
  
  TopDocs hits = is.search(query,nbres); 

  System.err.println( hits.totalHits +    
    " document(s");                                   

  for(int i=0;i<hits.scoreDocs.length;i++) {
    ScoreDoc scoreDoc = hits.scoreDocs[i];        
    Document doc = is.doc(scoreDoc.doc);                     
    System.out.println(doc.get("contents"));  
  }

  is.close();                                
}
  
  
  public static HashMap<String,Set<String>> recherche(String indexDir, String q)
throws IOException, ParseException {

Directory dir = FSDirectory.open(new File(indexDir)); 
IndexSearcher is = new IndexSearcher(dir, true);   

QueryParser parser = new QueryParser(Version.LUCENE_CURRENT,"contents", new FrenchAnalyzer(Version.LUCENE_CURRENT));  
Query query = parser.parse(q);              
TopDocs hits = is.search(query,40);

HashMap<String,Set<String>> docSet = new HashMap<String,Set<String>>();
//Set<Set<String>> biblePartition = new HashSet<Set<String>>();

for(int i=0;i<hits.scoreDocs.length;i++) {
  ScoreDoc scoreDoc = hits.scoreDocs[i];        
//  Set<String> livres = new HashSet<String>();
  Document doc = is.doc(scoreDoc.doc);                     
  System.out.println(doc.get("contents"));
//  docSet.add(doc.get("contents"));
//  biblePartition.
  if (docSet.get(doc.get("livre"))!=null){
  Set<String> liste = docSet.get(doc.get("livre"));
//  System.out.println(liste.toString());
  liste.add(doc.get("contents"));
  docSet.put(doc.get("livre"), liste);
  }
  else{
	  Set<String> liste = new HashSet<String>();
	  liste.add(doc.get("contents"));
	  docSet.put(doc.get("livre"), liste); 
  }
}

is.close();                                
return docSet;
}

}