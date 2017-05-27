package edu.wright.cs.dase.usgs;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * @author brooke mccurdy
 */
public class WikipediaOperations {
	
	private static String GET_ARTICLE_URL = "https://en.wikipedia.org/w/api.php?action=query&prop=pageprops|"
			+ "extracts&format=xml&explaintext=&exsectionformat=plain&ppprop=disambiguation&rawcontinue=&titles=";
	private static String REDIRECTS_ALIAS_URL = "https://en.wikipedia.org/w/api.php?action=query&prop=pageprops|"
			+ "pageterms|redirects&format=xml&ppprop=disambiguation&wbptterms=alias&rdprop=title&rdnamespace=0&"
			+ "rdlimit=max&rawcontinue=&titles=";
	private static String OPENSEARCH_URL = "https://en.wikipedia.org/w/api.php?srsearch=";
	private static String ARTICLE_SNIPPET_URL = "https://en.wikipedia.org/w/api.php?action=query&prop=pageprops|"
			+ "extracts&format=xml&exsentences=2&exintro=&explaintext=&exsectionformat=plain&ppprop=disambiguation&"
			+ "rawcontinue=&titles=";
	
	
	public static String getClosestArticle(String term) {
		
		String result = getArticle(term);
		
		if (result.startsWith("page-status::found")) {
			return result.replaceAll("page-status::found", "").trim();
		}
		
		if (result.startsWith("page-status::redirected")) {
			return result.replaceAll("page-status::redirected", "").trim();
		}
		
		if (result.startsWith("page-status::disambiguation")) {
			// this prevents an infinite loop
			if (term.contains("water ")) {
				System.err.println(term + " is ambiguous");
				return result.replaceAll("page-status::disambiguation", "").trim();
			}
			
			return getClosestArticle("water " + term);	
		}
		
		if (result.startsWith("page-status::missing")) {
			String article = getSearchOptions(term);
			result = getArticle(article);
			return result.replaceAll("page-status::found", "").trim();
		}
		
		return result;
	}
	
	
	public static String getArticle(String term) {
		try {
			term = URLEncoder.encode(term, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String url = GET_ARTICLE_URL + term + "&redirects=&maxlag=5";
		String result = "";
		ArrayList<String> data = invokeService(url);
		for (String d: data) {
			result += " " + d + " ";
		}
		return result.trim().toLowerCase();
	}
	
	public static ArrayList<String> getRedirectsAlias(String term) {
		try {
			term = URLEncoder.encode(term, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String url = REDIRECTS_ALIAS_URL + term + "&redirects=&maxlag=5";
		return invokeService(url);
	}
			
	public static String getSearchOptions(String term) {
		try {
			term = URLEncoder.encode(term, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String url = OPENSEARCH_URL + term + 
				"&srlimit=50&srinfo=suggestion&srwhat=text&format=xml&action=query&srprop=snippet&list=search";
		//System.out.println(url);
		ArrayList<String> temp = invokeService(url);
		if (!temp.isEmpty() && temp.get(0).contains("PAGE-STATUS::found")){
			temp.remove(0);
			temp.add(0, "PAGE-STATUS::search");
		}

		return temp.get(1).toLowerCase().replaceAll("t:", "").trim();
	}
	
	public static ArrayList<String> getArticleSnippet(String term) {
		try {
			term = URLEncoder.encode(term, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String url = ARTICLE_SNIPPET_URL + term + "&redirects=&maxlag=5";
		return invokeService(url);
	}
	
	private static ArrayList<String> invokeService(String serviceURL) {
	
		InvokeWikipediaWebService invokeWS = new InvokeWikipediaWebService(serviceURL);
		String content = invokeWS.invokeWebService();
		
		SAXWikiWSParser saxParser = new SAXWikiWSParser(content);
		ArrayList<String> data = saxParser.parse();
		// disambiguation
		if (data.size() > 1 && data.get(1).contains("PAGE-STATUS::disambiguation")){
			data.remove(0);
		}
		// found
		if (!data.isEmpty() && !data.get(0).contains("PAGE-STATUS::")){
			data.add(0, "PAGE-STATUS::found");
		}
		// not found
		if(data.isEmpty()){
			data.add(0, "INTERNAL-ERROR");
		}
		return data;
	}
	
	public static void main(String[] args){
		//TESTING
//		ArrayList<String> temp = getRedirectsAlias("Conference");
//		ArrayList<String> temp = getRedirectsAlias("dam or weir");
//		ArrayList<String> temp = getSearchOptions("administrator");
//		ArrayList<String> temp = getSearchOptions("dam or weir");
//		ArrayList<String> temp = getArticleSnippet("Paper");
//		ArrayList<String> temp = getArticle("Ultisol");
//		ArrayList<String> temp = getArticle("one edition of a conference");
		System.out.println(getClosestArticle("queen"));
//		for (String s : temp){
//			System.out.println("Have: " + s);
//		}
	}

}

