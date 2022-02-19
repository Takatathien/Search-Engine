package search.analyzers;

import datastructures.concrete.ChainedHashSet;
import datastructures.concrete.KVPair;
import datastructures.concrete.dictionaries.ChainedHashDictionary;
import datastructures.interfaces.IDictionary;
import datastructures.interfaces.IList;
import datastructures.interfaces.ISet;
import search.models.Webpage;

import java.net.URI;

/**
 * This class is responsible for computing how "relevant" any given document is
 * to a given search string.
 *
 * See the spec for more details.
 */
public class TfIdfAnalyzer {
    // This field must contain the IDF score for every single word in all
    // the documents.
    private IDictionary<String, Double> idfScores;

    // This field must contain the TF-IDF vec for each webpage you were given
    // in the constructor.
    //
    // We will use each webpage's page URI as a unique key.
    private IDictionary<URI, IDictionary<String, Double>> documentVec;

    // Feel free to add extra fields and helper methods.
    private IDictionary<URI, Double> normVec;
    
    public TfIdfAnalyzer(ISet<Webpage> webpages) {
        // Implementation note: We have commented these method calls out so your
        // search engine doesn't immediately crash when you try running it for the
        // first time.
        //
        // You should uncomment these lines when you're ready to begin working
        // on this class.

        this.idfScores = this.computeIdfScores(webpages);
        this.documentVec = this.computeAllDocumentTfIdfVectors(webpages);
        this.normVec = this.helper();
    }

    // Note: this method, strictly speaking, doesn't need to exist. However,
    // we've included it so we can add some unit tests to help verify that your
    // constructor correctly initializes your fields.
    public IDictionary<URI, IDictionary<String, Double>> getDocumentTfIdfVectors() {
        return this.documentVec;
    }

    // Note: these private methods are suggestions or hints on how to structure your
    // code. However, since they're private, you're not obligated to implement exactly
    // these methods: feel free to change or modify these methods however you want. The
    // important thing is that your 'computeRelevance' method ultimately returns the
    // correct answer in an efficient manner.

    /**
     * Return a resultionary mapping every single unique word found
     * in every single document to their IDF score.
     */
    private IDictionary<String, Double> computeIdfScores(ISet<Webpage> pages) {
        IDictionary<String, Double> result = new ChainedHashDictionary<>();
        int size = pages.size();
        for (Webpage page : pages) {
            IList<String> words = page.getWords();
            ISet<String> wordCounts = new ChainedHashSet<>();
            for (String word : words) {
                if (!wordCounts.contains(word)) {
                    wordCounts.add(word);
                    double pageCount = (result.containsKey(word)) ? result.get(word) : 0.0;
                    result.put(word, pageCount + 1.0); 
                }
            }
        }
        for (KVPair<String, Double> idfScore : result) {
            result.put(idfScore.getKey(), Math.log(size/idfScore.getValue()));
        }
        return result;
    }

    /**
     * Returns a resultionary mapping every unique word found in the given list
     * to their term frequency (TF) score.
     *
     * The input list represents the words contained within a single document.
     */
    private IDictionary<String, Double> computeTfScores(IList<String> words) {
        IDictionary<String, Double> result = new ChainedHashDictionary<>();
        int size = words.size();
        for (String next : words) {
            if (result.containsKey(next)) {
                result.put(next, result.get(next)+1);
            } else {
                result.put(next, 1.0);
            }
        }
        for (KVPair<String, Double> idfScore : result) {
            result.put(idfScore.getKey(), idfScore.getValue() / size);
        }
        return result;
    }

    /**
     * See spec for more details on what this method should do.
     */
    private IDictionary<URI, IDictionary<String, Double>> computeAllDocumentTfIdfVectors(ISet<Webpage> pages) {
        // Hint: this method should use the idfScores field and
        // call the computeTfScores(...) method.
        IDictionary<URI, IDictionary<String, Double>> result = new ChainedHashDictionary<>();
        for (Webpage page : pages) {
            URI name = page.getUri();
            IList<String> words = page.getWords();
            IDictionary<String, Double> tfScores = computeTfScores(words);
            IDictionary<String, Double> scoreWords = new ChainedHashDictionary<>();
            for (String next : words) {
               double pageScore = (idfScores.containsKey(next)) ? tfScores.get(next) * idfScores.get(next) : 0.0;
               scoreWords.put(next, pageScore);
            }
            result.put(name, scoreWords);
        }
        return result;
    }

    /**
     * Returns the cosine similarity between the TF-IDF vector for the given string and the
     * URI's document.
     *
     * Precondition: the given uri must have been one of the uris within the list of
     *               webpages given to the constructor.
     */
    public Double computeRelevance(IList<String> queries, URI uri) {
        // Note: The pseudocode we gave you is not very efficient. When implementing,
        // this method, you should:
        //
        // 1. Figure out what information can be precomputed in your constructor.
        //    Add a third field containing that information.
        //
        // 2. See if you can combine or merge one or more loops.
        IDictionary<String, Double> docVec = documentVec.get(uri);
        IDictionary<String, Double> qVector = new ChainedHashDictionary<>();
        double num = 0.0;
        for (String word : queries) {
            double tfidf = (idfScores.containsKey(word)) ? computeTfScores(queries).get(word)*idfScores.get(word) : 0.0;
            qVector.put(word, tfidf);
            Double docWordScore = (docVec.containsKey(word)) ? docVec.get(word) : 0.0;
            Double stringWordScore = qVector.get(word);
            num += stringWordScore * docWordScore;
        }
        Double denom = norme(qVector) * normVec.get(uri);
        return (denom < 0.0 || denom > 0.0) ? num / denom : 0.0;

    }
    
    private IDictionary<URI, Double> helper() {
        IDictionary<URI, Double> norme = new ChainedHashDictionary<>();
        for (KVPair<URI, IDictionary<String, Double>> idfScore: documentVec) {
            norme.put(idfScore.getKey(), norme(idfScore.getValue()));
        }
        return norme;
    }
    
    private Double norme(IDictionary<String, Double> vec) {
        Double result = 0.0;
        for (KVPair<String, Double> idfScore : vec) {
            Double score = idfScore.getValue();
            result += score * score;
        }
        return Math.sqrt(result);
    }
}