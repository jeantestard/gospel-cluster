package fr.crim.a2010;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.standard.StandardAnalyzer;


public class Vecteur {

  /**
   * Compare deux vecteurs pour la mesure de similarité Cosinus/Jaccard/Diestance d'édition.
   * 
   * @param v1
   * @param v2
   * @return double mesure cosinus
   * @throws IOException
   */
  public static double comparaison(HashMap<String, Double> v1,
      HashMap<String, Double> v2) throws IOException {
    double denominateur = Math.sqrt(ponderation(v1))
        * Math.sqrt(ponderation(v2));

    double numerateur = 0.0;
    Set<String> v1Keys = v1.keySet();
    Iterator<String> iterator = v1Keys.iterator();
    while (iterator.hasNext()) {
      String key = iterator.next();
      if (v2.containsKey(key))
        numerateur += (v1.get(key) * v2.get(key));
    }
    return (numerateur / denominateur);
  }

  /**
   * Tokenise la chaine passée en argument avec un analyseur  Lucene analyzer pour construire
   * un vecteur de fréquences de termes
   * 
   * @param texte: chaine à vectoriser 
   * @return HashMap associant une chaine à son score (double)
   * @throws IOException
   */
  public static HashMap<String, Double> getVecteur(String text)
      throws IOException {
    Token[] tokens = Analyseurs.analyseur(new StandardAnalyzer(null),
        text);
    HashMap<String, Double> vector = new HashMap<String, Double>();
    int tlen = tokens.length;
    for (int i = 0; i < tlen; i++) {
      String token = new String(tokens[i].termBuffer(), 0, tokens[i].termLength());
      if (vector.get(token) == null)
        vector.put(token, new Double(1.0 / tlen));
      else {
        vector.put(token, new Double(vector.get(token).doubleValue() + 1.0
            / tlen));
      }
    }
    return (vector);
  }


  /**
 *  renvoie la somme des carrés de poids.
 *  
 * @param vecteur
 * @return
 */
private static double ponderation(HashMap<String, Double> vecteur) {
    Set<Map.Entry<String, Double>> keys = vecteur.entrySet();
    Iterator<Map.Entry<String, Double>> iterator = keys.iterator();
    double wt = 0.0;
    while (iterator.hasNext()) {
      Map.Entry<String, Double> element = iterator.next();
      wt += (element.getValue() * element.getValue());
    }
    return (wt);
  }

}