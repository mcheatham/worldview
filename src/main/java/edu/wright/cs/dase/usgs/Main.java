package edu.wright.cs.dase.usgs;

import static spark.Spark.*;

public class Main {
	
	public static void main(String[] args) {
		port(8080);
		staticFiles.location("/public");
		get("/hello", (req, res) -> "Hello World");
		
		get("/example", (req, res) -> 
			example(Integer.parseInt(req.queryParams("first")), 
					Integer.parseInt(req.queryParams("second"))));
	}
	
	
	public static int example(int a, int b) {
		return a + b;
	}
}