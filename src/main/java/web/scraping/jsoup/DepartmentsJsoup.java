package web.scraping.jsoup;

import static java.util.concurrent.TimeUnit.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.assertj.core.util.Lists;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import net.minidev.json.JSONObject;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;

public class DepartmentsJsoup {
	
	private static final String URL_BASE = "https://investigacion.us.es/sisius/sis_dep.php";
	private static List<org.bson.Document> json_array = new ArrayList<org.bson.Document>();
	private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	public static List<String> getRandomKeywords() {
	    Random rand = new Random();
	    List<String> kw = Lists.newArrayList("#jquery", "#angular", "#deeplearning", "#robotics","#bigdata",
	    		"#MachineLearning","#cloud","#history","#docker","#nodejs","#NeuralNetworks","#architecture");
	 
	    int numberOfElements = 8;
	 
	    for (int i = 0; i < numberOfElements; i++) {
	        int randomIndex = rand.nextInt(kw.size());
	        String randomElement = kw.get(randomIndex);
	        kw.remove(randomIndex);
	    }
	    return kw;
	}

	
	public static String createId(String department, String departmentURL){
		
	    int sL = department.length();
	    String initials = "";
	    
	    for (int i = 0; i < sL; i++) {
	        if (department.charAt(i)!= ' ' && department.charAt(i) == Character.toUpperCase(department.charAt(i)) ) {
	            
	            initials += department.charAt(i);
	        }
	    }
	    initials = initials.toLowerCase();
	    initials = Normalizer.normalize(initials, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]","");
	    initials = initials + "-" + departmentURL.substring(departmentURL.indexOf("id_dpto=")+8);
	    
	    return initials;
	}
	
	public static void executor() {
	     final Runnable beeper = new Runnable() {
	       public void run() { 
	    	   try {
				scraping();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	       }
	     };
	     final ScheduledFuture<?> beeperHandle =
	       scheduler.scheduleAtFixedRate(beeper, 0, 24, HOURS);
	}
	
	public static void scraping() throws MalformedURLException, IOException{
		
		Document doc = Jsoup.parse(new URL(URL_BASE), 10000);
		Elements elements = doc.getElementsByClass("data");
		String departmentURL ="";
		JSONObject jsonDepartment = new JSONObject();
		System.out.println("#########Se inicia la recolección de datos#########\n\n");
		
		int i = 0;
		Iterator<Element> it = elements.get(i).getElementsByTag("a").iterator();
		while(it.hasNext()){
			Element e = it.next();
			
			departmentURL = e.attr("href");
			jsonDepartment.put("department", e.text());
			jsonDepartment.put("keywords", getRandomKeywords());
			jsonDepartment.put("idDepartment", createId(e.text(),departmentURL ));
			
			System.out.println("Cargando datos del departamento: " + e.text());
			getDepartmentInfo(departmentURL, jsonDepartment);
			System.out.println(e.text()  + " CARGADO\n");
			i= i+2;
			if(i < elements.size()) {
				it = elements.get(i).getElementsByTag("a").iterator();
			}
		}	
		System.out.println("Se establece conexión con MongoDB\n");
		MongoClientURI uri  = new MongoClientURI("mongodb://alex:alex@ds151355.mlab.com:51355/si1718-amc-departments");

	    MongoClient client = new MongoClient(uri);

	    MongoDatabase db = client.getDatabase(uri.getDatabase());
	    MongoCollection col = db.getCollection("departmentsV2");
	    
	    System.out.println("Se procede a insertar los datos en BD\n");
	    col.insertMany(json_array);
	    System.out.println("Se ha completado la carga en BD\n");


	 	client.close();
	}


	public static void main(String[] args){
		
		executor();
	
	}

	private static void getDepartmentInfo(String departmentURL, JSONObject jsonDepartment) throws MalformedURLException, IOException {
		
		final String URL_BASE_DEP = "https://investigacion.us.es";
		
		Document doc = Jsoup.parse(new URL(URL_BASE_DEP + departmentURL), 10000);
		Elements elements = doc.getElementsByTag("h5");
		
		int i = 0;
		Iterator<Element> it = elements.iterator();
		
		List<JSONObject> addressList = new ArrayList();
		
		JSONObject researchersJSON = new JSONObject();
		JSONObject rsCategoryJSON = new JSONObject();
		List<JSONObject> researchersListJSON = new ArrayList();

		
		while(it.hasNext()){
			
			Element e = it.next();
			
			List<Node> listNodes = elements.get(0).siblingNodes();
			
			if(e.html().replace(":", "").equals("Dirección")) {
							
				
				for(int l = 0; l < listNodes.size(); l++) {
					
					Element node = null;
					
					if(listNodes.get(l).getClass() != TextNode.class) {
						node = (Element)listNodes.get(l);
						Iterator<Element> itAddresses = node.getElementsByTag("table").iterator();
						int pos = 0;
						
						JSONObject addressJSON = new JSONObject();
						while(itAddresses.hasNext()) {
							
							
							Element address = itAddresses.next();
							String s_address = address.getElementsByTag("td").text();
							
							String school = "";
							String tlf = "";
							String fax = "";
							String web = "";
													
							if(pos%2 == 0) {
								if(s_address.indexOf("TELF") != -1) {
									school = s_address.substring(0, s_address.indexOf("TELF"));
									addressJSON.put("school", school);
								}
																
							}else {
								tlf = (s_address.substring(s_address.indexOf("TELF: "), s_address.indexOf("FAX")-1)).replace(".", "");
								fax = (s_address.substring(s_address.indexOf("FAX: "), s_address.indexOf("WEB"))).replace(".", "");
								web = s_address.substring(s_address.indexOf("WEB: "));
								
								addressJSON.put("tlf", tlf.replace("TELF: ", "").replace(" ", ""));
								addressJSON.put("fax", fax.replace("FAX: ", ""));
								addressJSON.put("web", web.replace("WEB: ", ""));
							}
							

							pos++;
							if(!addressJSON.isEmpty() && addressJSON.containsKey("tlf")) {
								addressList.add(addressJSON);
								addressJSON = new JSONObject();
							}
						}
					}			
				}
				
				
			}else if(e.html().replace(":", "").equals("Centros afectados")){

				for(int l = 0; l < listNodes.size(); l++) {
					
					Element node = null;
					
					if(listNodes.get(l).getClass() != TextNode.class) {
						node = (Element)listNodes.get(l);
						Iterator<Element> itCentrosAfectados = node.getElementsByTag("p").iterator();

						while(itCentrosAfectados.hasNext()) {
							Element centro = itCentrosAfectados.next();
							if(centro.childNodeSize()>=2 && (centro.childNode(1).attr("href").equals(""))) {
								
								//TODO: Contemplar si se van a cargar en BD los centros afectados
								//System.out.println(centro.text());
							}


						}
							
					}
				}
			}else {
				
				
				List<String> researchersList = new ArrayList();
				
				Elements researchers = e.nextElementSibling().getElementsByTag("a");
				
				for (Element researcher : researchers) {
					
					researchersList.add(researcher.text());

				}
				//rsCategoryJSON.put(e.text(),researchersList);
				
				for(String researcher:researchersList) {
					JSONObject researchJSON = new JSONObject();
					researchJSON.put("name", researcher);
					researchJSON.put("category", e.text());
					researchersListJSON.add(researchJSON);
					
				}
				
			}
			jsonDepartment.put("researchers", researchersListJSON);

			jsonDepartment.put("address", addressList);
			
					
		}
		org.bson.Document bson = org.bson.Document.parse(jsonDepartment.toString().replace(".", ""));
		json_array.add(bson);
	}
		
}