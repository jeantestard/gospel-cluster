package fr.crim.a2010;


import com.aliasi.classify.PrecisionRecallEvaluation;

import com.aliasi.cluster.HierarchicalClusterer;
import com.aliasi.cluster.ClusterScore;
import com.aliasi.cluster.CompleteLinkClusterer;
import com.aliasi.cluster.SingleLinkClusterer;
import com.aliasi.cluster.Dendrogram;
import com.aliasi.util.Counter;
import com.aliasi.util.Distance;
import com.aliasi.util.Files;
import com.aliasi.util.ObjectToCounterMap;
import com.aliasi.util.Strings;

import com.aliasi.spell.EditDistance;


import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseFilterTokenizer;

import com.aliasi.tokenizer.Tokenizer;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;




public class Partitionnement {

	/**
	 * Evalutation du partionnement en prenant comme référence 
	 * le champs chapitre de l'indexation Lucene 
	 */
	public static void evaluation(HashMap<String,Set<String>> hits, Distance<Document> mesure) throws Exception {


		Set<Set<Document>> partitionChapitres = new HashSet<Set<Document>>();


		/**
		 * ajoute pour chaque chapitres les documents contenus
		 *  dans la partition de référence
		 */


		for (String catDir : hits.keySet()) {
			Set<Document> docsForCat = new HashSet<Document>();
			partitionChapitres.add(docsForCat);
			for (String res : hits.get(catDir)) {
				File file = sauve(res);
				Document doc = new Document(file);
				docsForCat.add(doc);
			}
		}


		/**
		 * Inclus une collection contenant tous les documents renvoyés par la requête
		 */
		Set<Document> docSet = new HashSet<Document>();
		for (Set<Document> cluster : partitionChapitres)
			docSet.addAll(cluster);

		/**
		 * Constitue les partitions par liens simples et liens complexes
		 */

		HierarchicalClusterer<Document> partitionLiensComplets = new CompleteLinkClusterer<Document>(
				(double) 1.00, mesure);
		Dendrogram<Document> dendrogrammeLiensComplets = partitionLiensComplets
		.hierarchicalCluster(docSet);

		HierarchicalClusterer<Document> partitionLiensSimples = new SingleLinkClusterer<Document>(
				(double) 1.00, mesure);
		Dendrogram<Document> dendrogrammeLiensSimples = partitionLiensSimples
		.hierarchicalCluster(docSet);

		System.out.println();
		System.out.println(" pour chaque type de partition: Précision  Rappel F-mesure ");
		System.out.println("  K    Complet         Simple          Croisé        ");

		for (int k = 1; k <= docSet.size(); ++k) {
			Set<Set<Document>> nouvellepartitionLiensComplets = dendrogrammeLiensComplets
			.partitionK(k);

			Set<Set<Document>> nouvellepartitionLiensSimples = dendrogrammeLiensSimples
			.partitionK(k);
			
			System.out.println("dispertions complète:");
			System.out.println(dendrogrammeLiensSimples.withinClusterScatter(k, MESURE_COSINUS));
			System.out.println("dispertions simple:");
			System.out.println(dendrogrammeLiensComplets.withinClusterScatter(k, MESURE_COSINUS));
			
			ClusterScore<Document> scoreLiensComplets = new ClusterScore<Document>(
					partitionChapitres, nouvellepartitionLiensComplets);
			PrecisionRecallEvaluation precisionLiensComplets = scoreLiensComplets.equivalenceEvaluation();
			ClusterScore<Document> scoreLiensSimples = new ClusterScore<Document>(
					partitionChapitres, nouvellepartitionLiensSimples);
			PrecisionRecallEvaluation precisionLiensSImples = scoreLiensSimples.equivalenceEvaluation();
			ClusterScore<Document> scoreCroise = new ClusterScore<Document>(
					nouvellepartitionLiensComplets, nouvellepartitionLiensSimples);
			PrecisionRecallEvaluation precisionCroisee = scoreCroise.equivalenceEvaluation();

			System.out
			.printf(
					"| %3d | %3.2f %3.2f %3.2f | %3.2f %3.2f %3.2f | %3.2f %3.2f %3.2f |\n",
					k,
					precisionLiensComplets.precision(),
					precisionLiensComplets.recall(),
					precisionLiensComplets.fMeasure(),
					precisionLiensSImples.precision(),
					precisionLiensSImples.recall(),
					precisionLiensSImples.fMeasure(),
					precisionCroisee.precision(),
					precisionCroisee.recall(),
					precisionCroisee.fMeasure()
							);

		}

	}








	/**
	 * 
	 *  * ajoute pour chaque chapitres les documents contenus
	 *  dans la partition de référence
	 *  * Inclus une collection contenant tous les documents renvoyés par la requête
	 *  Constitue les partitions par liens simples et liens complexes
	 * 
	 * @param hits Documents à partitionner (resultats de la recherche) 
	 * @throws Exception
	 */
	public static void partitionne(HashMap<String,Set<String>> hits) throws Exception {

		Set<Set<Document>> referencePartition = new HashSet<Set<Document>>();

		for (String chapitres : hits.keySet()) {
			Set<Document> versets = new HashSet<Document>();
			referencePartition.add(versets);
			for (String res : hits.get(chapitres)) {
				File file = sauve(res);
				Document doc = new Document(file);
				versets.add(doc);
			}
		}

		Set<Document> collectionVersets = new HashSet<Document>();
		for (Set<Document> partition : referencePartition)
			collectionVersets.addAll(partition);





		HierarchicalClusterer<Document> partitionLiensComplets = new CompleteLinkClusterer<Document>(
				(double) 1.00, MESURE_COSINUS);
		Dendrogram<Document> dendrogrammeComplet = partitionLiensComplets
		.hierarchicalCluster(collectionVersets);
		System.out.println("Dendrogramme Complet: " +
				dendrogrammeComplet.prettyPrint());

		for (int k = 1; k <= dendrogrammeComplet.size(); ++k) {
			Set<Set<Document>> kPartitionsLiensComplets = dendrogrammeComplet.partitionK(k);
			System.out.println(k);
			lisPartition(kPartitionsLiensComplets);
		}

		HierarchicalClusterer<Document> partitionLiensSimples = new SingleLinkClusterer<Document>(
				(double) 1.00, MESURE_COSINUS);
		Dendrogram<Document> dendrogrammeLiensSimples = partitionLiensSimples
		.hierarchicalCluster(collectionVersets);

		
		System.out.println("Dendrogramme Simple: " +
				dendrogrammeLiensSimples.prettyPrint());

		for (int k = 1; k <= dendrogrammeLiensSimples.size(); ++k) {
			Set<Set<Document>> kPartitionsLiensSpl= dendrogrammeLiensSimples.partitionK(k);
			System.out.println(k);
			lisPartition(kPartitionsLiensSpl);
		}
		
	} 



	/**
	 * Sauve les documents dans des fichiers temporaries
	 * en leur assignant un UUID
	 * 
	 * @param String verset
	 * @return fichier temporaire
	 */
	private static File sauve(String verset){

		new File("partition").mkdir();
		File aFile = new File("partition/"+UUID.randomUUID());   
		FileOutputStream outputFile = null;  
		try {
			outputFile = new FileOutputStream(aFile, true);
		} catch (FileNotFoundException e) {
			e.printStackTrace(System.err);
		} 

		FileChannel outChannel = outputFile.getChannel();

		ByteBuffer buf = ByteBuffer.allocate(1024);

		for (char ch : verset.toCharArray()) {
			buf.putChar(ch);
		}
		buf.flip();

		try {
			outChannel.write(buf);
			outputFile.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		return aFile;
	}

	/**
	 * Lis le contenu de la collection de versets pour 
	 * montrer les partitions de documents possibles
	 * 
	 * @param collection de versets
	 */
	private static void lisPartition(Set<Set<Document>> partitionVersets) {

		int i =1;
		for(Set<Document> groupe : partitionVersets){
			System.out.println("Groupes de résultat numéro:"+i);
			i++;
			for(Document verset : groupe){
				File aFile = new File(verset.toString());

				StringBuilder contenu = new StringBuilder();

				try {
					BufferedReader input =  new BufferedReader(new FileReader(aFile));
					try {
						String line = null;
						while (( line = input.readLine()) != null){
							contenu.append(line);
							contenu.append(System.getProperty("line.separator"));
						}
					}
					finally {
						//aFile.delete();
						input.close();
					}
					
				}
				catch (IOException ex){
					ex.printStackTrace();
				}

				System.out.println(contenu.toString());
			}
		}
	}


	private static class Document {
		final File fichier;
		final String texte;
		final ObjectToCounterMap<String> nbTOkens = new ObjectToCounterMap<String>();
		final double longueur;

		/**
		 * Tokenize un fichier
		 * 
		 * @param fichier
		 * @throws IOException
		 */
		Document(File fichierTemporaire) throws IOException {
			fichier = fichierTemporaire;
			texte = Files.readFromFile(fichierTemporaire, Strings.UTF8);
			Tokenizer tokenizer = appelTokenizer(texte.toCharArray());
			String token;
			while ((token = tokenizer.nextToken()) != null) {
				nbTOkens.increment(token.toLowerCase());
			}
			longueur = longueur(nbTOkens);
		}

		/**
		 * renvoie la mesure de similartié de type Cosinus
		 *  avec le document passé en argument
		 *  
		 * @param Document docCompare
		 * @return double mesure de similarité cosinus
		 */
		double cosine(Document docCompare) {
			return product(docCompare) / (longueur * docCompare.longueur);
		}


		/**
		 * renvoie la somme des carrés des termes en commun
		 *  avec le document passé en argument
		 *  
		 * @param docCompare
		 * @return somme
		 */
		double product(Document docCompare) {
			double somme = 0.0;
			for (String token : nbTOkens.keySet()) {
				int res = docCompare.nbTOkens.getCount(token);
				if (res == 0)
					continue;
				somme += Math.sqrt(res * nbTOkens.getCount(token));
			}
			return somme;
		}


		/**
		 * renvoie la somme des carrés des fréquences totales des termes 
		 * 
		 *
		 * @param compteurTermes
		 * @return somme des carrés
		 */
		double longueur(ObjectToCounterMap<String> compteurTermes) {
			double somme = 0.0;
			for (Counter counter : compteurTermes.values()) {
				double compte = counter.doubleValue();
				somme += compte; 
			}
			return Math.sqrt(somme);
		}

		/**
		 * renvoie la mesure de similartié de type indice de Jaccard
		 *  avec le document passé en argument
		 *  
		 *  
		 *
		 * @param docCompare
		 * @return similarité
		 */
		double indiceJaccard(Document docCompare) {
			double intersection = intersection(docCompare);
			double union = nbTOkens.keySet().size()
			+ docCompare.nbTOkens.keySet().size() - intersection;
			return intersection / union;
		}

		/**
		 * @param docCompare
		 * @return somme des termes communs
		 */
		double intersection(Document docCompare) {
			double somme = 0.0;
			for (String token : nbTOkens.keySet())
				somme += (docCompare.nbTOkens.getCount(token) == 0) ? 0 : 1;
			return somme;
		}

		/**
		 * renvoie un tokeniseur Lingpipe simple
		 * associé à un tableau à tokeniser
		 * 
		 * @param texte tableau
		 * @return tokenizer
		 */
		static Tokenizer appelTokenizer(char[] texte) {
			Tokenizer tokenizer = IndoEuropeanTokenizerFactory.FACTORY.tokenizer(texte,
					0, texte.length);
			tokenizer = new LowerCaseFilterTokenizer(tokenizer);

			return tokenizer;
		}

		/**
		 * Donne le nom du fichier
		 * 
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return fichier.getParentFile().getName() + "/" + fichier.getName();
		}

	} 

	/**
	 * mesure de similartié par Cosinus
	 */

	static Distance<Document> MESURE_COSINUS = new Distance<Document>() {
		public double distance(Document doc1, Document doc2) {
			return 1.0 - doc1.cosine(doc2);
		}
	};

	/**
	 * mesure de similartié par indice de Jaccard 
	 */


	static Distance<Document> INDICE_JACCARD = new Distance<Document>() {
		public double distance(Document doc1, Document doc2) {
			return 1.0 - doc1.indiceJaccard(doc2);
		}
	};

	/**
	 * mesure de similartié par distance d'édition
	 */


	static Distance<Document> DISTANCE_EDITION = new Distance<Document>() {
		public double distance(Document doc1, Document doc2) {
			return EditDistance.editDistance(doc1.texte.subSequence(0, (doc1.texte
					.length() - 1)),
					doc2.texte.subSequence(0, (doc2.texte.length() - 1)), false);
		}
	};
}
