package fr.crim.a2010;


import java.io.File;
import java.io.IOException;
import com.aliasi.matrix.ProximityMatrix;
import com.aliasi.util.Distance;
import com.aliasi.util.Files;

public class MatriceSimilarite implements Distance<Integer> {

  private final String REPERTOIRE_PART;
  private ProximityMatrix matrice;
  private String[] champs;

  /**
   * Specifie le repertoire où sont stockées les partitions
   * 
   * @param repPart
   */
  public MatriceSimilarite(String repPart) {
    REPERTOIRE_PART = repPart;
  }

  /**
   * Renvoie la mesure de similarité entre deux documents
   * 
 * @see com.aliasi.util.Distance#distance(java.lang.Object, java.lang.Object)
 */
public double distance(Integer i, Integer j) {
    if ((i.intValue() < 0) || (i.intValue() > matrice.numRows()))
      return (0.0);
    if ((j.intValue() < 0) || (j.intValue() > matrice.numColumns()))
      return (0.0);
    return (matrice.value(i.intValue(), j.intValue()));
  }


  /**
   * Repmplissage des entrées de la matrice
   * 
   * 
   * @throws IOException
   */
  public void constructionMatrice() throws IOException {
    File file = new File(REPERTOIRE_PART);
    File[] testFiles = file.listFiles();
    int dimension = testFiles.length;

    double[][] similarites = new double[dimension][dimension];
    for (int i = 0; i < dimension; i++)
      for (int j = 0; j < dimension; j++)
        similarites[i][j] = 0.0;
    for (int i = 0; i < dimension; ++i) {
      LOOP: for (int j = i; j < dimension; j++) {
        if (i == j)continue LOOP;
        similarites[i][j] = 1.0 - Vecteur.comparaison(Vecteur.getVecteur(Files
            .readFromFile(testFiles[i])), Vecteur.getVecteur(Files
            .readFromFile(testFiles[j])));
        similarites[j][i] = similarites[i][j];
      } 
    } 

    champs = new String[dimension];
    for (int i = 0; i < dimension; i++)
      champs[i] = testFiles[i].getName();
    matrice = new ProximityMatrix(dimension);
    for (int i = 0; i < dimension; i++)
      for (int j = 0; j < dimension; j++)
        if (i != j)
          matrice.setValue(i, j, similarites[i][j]);

    return;
  }

}