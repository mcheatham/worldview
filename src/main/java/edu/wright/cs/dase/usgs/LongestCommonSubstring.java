package edu.wright.cs.dase.usgs;

/**
 *  The {@code LongestCommonSubstring} class provides a {@link SuffixArray}
 *  client for computing the longest common substring that appears in two
 *  given strings.
 *  <p>
 *  This implementation computes the suffix array of each string and applies a
 *  merging operation to determine the longest common substring.
 *  For an alternate implementation, see
 *  <a href = "http://algs4.cs.princeton.edu/63suffix/LongestCommonSubstringConcatenate.java.html">LongestCommonSubstringConcatenate.java</a>.
 *  <p>
 *  For additional documentation,
 *  see <a href="http://algs4.cs.princeton.edu/63suffix">Section 6.3</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *  <p>
 *     
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */
public class LongestCommonSubstring {

    // Do not instantiate.
    private LongestCommonSubstring() { }

    // return the longest common prefix of suffix s[p..] and suffix t[q..]
    private static String lcp(String s, int p, String t, int q) {
        int n = Math.min(s.length() - p, t.length() - q);
        for (int i = 0; i < n; i++) {
            if (s.charAt(p + i) != t.charAt(q + i))
                return s.substring(p, p + i);
        }
        return s.substring(p, p + n);
    }

    // compare suffix s[p..] and suffix t[q..]
    private static int compare(String s, int p, String t, int q) {
        int n = Math.min(s.length() - p, t.length() - q);
        for (int i = 0; i < n; i++) {
            if (s.charAt(p + i) != t.charAt(q + i))
                return s.charAt(p+i) - t.charAt(q+i);
        }
        if      (s.length() - p < t.length() - q) return -1;
        else if (s.length() - p > t.length() - q) return +1;
        else                                      return  0;
    }

    /**
     * Returns the longest common string of the two specified strings.
     *
     * @param  s one string
     * @param  t the other string
     * @return the longest common string that appears as a substring
     *         in both {@code s} and {@code t}; the empty string
     *         if no such string
     */
    public static String lcs(String s, String t) {
        SuffixArray suffix1 = new SuffixArray(s);
        SuffixArray suffix2 = new SuffixArray(t);

        // find longest common substring by "merging" sorted suffixes 
        String lcs = "";
        int i = 0, j = 0;
        while (i < s.length() && j < t.length()) {
            int p = suffix1.index(i);
            int q = suffix2.index(j);
            String x = lcp(s, p, t, q);
            if (x.length() > lcs.length()) lcs = x;
            if (compare(s, p, t, q) < 0) i++;
            else                         j++;
        }
        return lcs;
    }
    
    
    public static double lcsSim(String s, String t) {
    	String common = lcs(s, t);
    	return common.length() / (double) s.length();
    }
}