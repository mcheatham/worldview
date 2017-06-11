package edu.wright.cs.dase.usgs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXWikiWSParser extends DefaultHandler {

	private ArrayList<String> linkList = new ArrayList<String>();
	private String content = "" ;
	private StringBuilder builder;
	private boolean getText = false;
	private boolean ambiguousPage = false;
	private boolean missingPage = false;
	private boolean getTerm = false;
	private boolean error = false;
	private String title = "" ;
	
	public SAXWikiWSParser(String content) {
		this.content = content;
//		System.out.println(content);
	}

	
	private void writeToFile(){
		try {
		    BufferedWriter out = new BufferedWriter(new FileWriter("data/wikipedia-category.xml"));
		    out.write(this.content);
		    out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Parse the search result of Wikipedia Web Service result page and return a list of 
	 * categories to user.
	 * 
	 * @return a list containing categories
	 */
	public ArrayList<String> parse() {
		writeToFile();
		parseDocument();
		linkList.add(title);
		return linkList;
	}

	
	private void parseDocument() {
		
		SAXParserFactory spf = SAXParserFactory.newInstance();
		
		try {
			File file = new File("data/wikipedia-category.xml");
			InputStream inputStream = new FileInputStream(file);
			Reader reader = new InputStreamReader(inputStream, "UTF-8");
			
			InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");
			
			SAXParser sp = spf.newSAXParser();
			sp.parse(is, this);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) 
		throws SAXException {
		if(qName.equalsIgnoreCase("page")){
			if (attributes.getValue("title") != null){
				title = attributes.getValue("title");
			}
			if (attributes.getValue("missing") != null){
				linkList.add(0, "PAGE-STATUS::missing");
				missingPage = true;
			}
		} else if(qName.equalsIgnoreCase("extract")){ //if extract present
			builder = new StringBuilder();
			getText = true;
		} else if (qName.equalsIgnoreCase("pageprops")){ // check if ambiguous
				linkList.add("PAGE-STATUS::disambiguation");	
				ambiguousPage = true;
		} else if(qName.equalsIgnoreCase("pl")){
				linkList.add(attributes.getValue("title"));
		} else if(qName.equalsIgnoreCase("rd")){ // page redirects
			linkList.add(attributes.getValue("title"));
			
		} else if(qName.equalsIgnoreCase("term")){ // alias
			this.getTerm = true;
			builder = new StringBuilder();
		} else if(qName.equalsIgnoreCase("r")){ //redirected
			linkList.add(0, "PAGE-STATUS::redirected");
		} else if(qName.equalsIgnoreCase("p")){ //search results
			linkList.add("T:" + attributes.getValue("title"));
			linkList.add("S:" + attributes.getValue("snippet"));
		} else if(qName.equalsIgnoreCase("error")){
			this.error = true;
			linkList.add("PAGE-STATUS::API-ERROR");
		}
		
	}
	
	@Override
	public void characters(char[] ch, int start, int length) {
		if(getText && !ambiguousPage && !missingPage && !error){
		   builder.append(ch,start,length);
		} 
		if(this.getTerm && !error){
			builder.append(ch,start,length);
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) 
		throws SAXException {
		if(qName.equalsIgnoreCase("extract")){
			//end the string Builder and return 
			if(getText && !ambiguousPage && !missingPage){
				linkList.add(builder.toString());
				getText = false;
			}
		} else if(qName.equalsIgnoreCase("term")){
			linkList.add(builder.toString());
			this.getTerm = false;
		}
	}
	
}
