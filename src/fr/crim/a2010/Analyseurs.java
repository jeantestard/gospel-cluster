package fr.crim.a2010;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Term;


public class Analyseurs {
  
  /**
   * Renvoie une liste de tokens à partir d'un chaine en spécifiant un analyseur
 * @param Analyze analyseur
 * @param String texte
 * @return Token[] Liste de Tokens
 * @throws IOException
 */
public static Token[] analyseur(Analyzer analyseur, String texte)
      throws IOException {
    TokenStream stream = analyseur.tokenStream("contents",
        new StringReader(texte));
    
    
    ArrayList<Token> listeTokens = new ArrayList<Token>();
    Token token = null;
    while (stream.incrementToken())

    listeTokens.add(token);
    Token[] tokens = new Token[listeTokens.size()];
    for (int i = 0; i < tokens.length; i++)
      tokens[i] = listeTokens.get(i);
    return (tokens);
  }
  
  
  
}