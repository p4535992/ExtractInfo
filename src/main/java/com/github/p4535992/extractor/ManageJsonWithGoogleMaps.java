package com.github.p4535992.extractor;
import com.github.p4535992.extractor.object.model.GeoDomainDocument;
import com.github.p4535992.extractor.setInfoParameterIta.SetNazioneELanguage;
import com.github.p4535992.util.http.HttpUtil;
import com.github.p4535992.extractor.object.model.GeoDocument;
import com.github.p4535992.extractor.object.support.LatLng;
import com.github.p4535992.util.http.HttpUtilApache4;
import com.github.p4535992.util.string.StringKit;
import org.json.JSONException;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import org.json.JSONObject;
import com.github.p4535992.util.log.SystemLog;

/**
 * ManageJsonWithGoogleMaps.java.
 * @author Tenti Marco Elaborato Sistemi Distribuiti.
 * Classe per gestire la richiesta tramite url all' L'API Google Maps, e la gestione della
 * risposta della stessa, tramite Json utilizzando la libreria JSONO e creazione dei
 * GeoDocument.
 * @author 4535992.
 * @version 2015-09-29.
 */
@SuppressWarnings("unused")
public class ManageJsonWithGoogleMaps {
    
    private String API_KEY_GM;
    //private static ManageJsonWithOSMAndJP o = new ManageJsonWithOSMAndJP();
    private static SetNazioneELanguage set = new SetNazioneELanguage();
    private static ManageJsonWithGoogleMaps instance = null;
    protected ManageJsonWithGoogleMaps(){}
    protected ManageJsonWithGoogleMaps(String API_KEY_GM){
        this.API_KEY_GM=API_KEY_GM;
    }
    public static ManageJsonWithGoogleMaps getInstance(){
        if(instance == null) {
            instance = new ManageJsonWithGoogleMaps();
        }
        return instance;
    }
    public static ManageJsonWithGoogleMaps getInstance(String API_KEY_GM){
        if(instance == null) {
            instance = new ManageJsonWithGoogleMaps(API_KEY_GM);
        }
        return instance;
    }

    /**
     * Metodo per la connessione all'API Google Maps e al suo utilizzo
     * @param g il geodocument fornito di input
     * @return le coordinate GPS ricavate attraverso le informazioni ricavate dalle
     *         annotazioni di Gate con lt'utilizzo dell'API Google Maps.
     * @throws URISyntaxException error.
     */
    public LatLng getCoords(GeoDocument g) throws URISyntaxException {
        List<String> kContentList = new ArrayList<>();
        if (setNullForEmptyString(g.getIndirizzo()) != null) {
            kContentList.add(g.getRegione());
            kContentList.add(g.getIndirizzo());
            kContentList.add(g.getCity());
        } else if (setNullForEmptyString(g.getEdificio()) != null && setNullForEmptyString(g.getIndirizzo()) == null) {
            kContentList.add(g.getRegione());
            kContentList.add(g.getEdificio());
            kContentList.add(g.getCity());
        } else {
            kContentList.add(g.getRegione());
            kContentList.add(g.getCity());
        }
        String n;
        try{
            n= set.checkGMRegionByNazione(g.getNazione()).toLowerCase();
        }catch(java.lang.NullPointerException ne){n="it";}
        return getCoordinatesFromStringAddress(kContentList,n);
    }

    public LatLng getCoords(GeoDomainDocument g) throws URISyntaxException {
        List<String> kContentList = new ArrayList<>();
        if (setNullForEmptyString(g.getIndirizzo()) != null) {
            kContentList.add(g.getRegione());
            kContentList.add(g.getIndirizzo());
            kContentList.add(g.getCity());
        } else if (setNullForEmptyString(g.getEdificio()) != null && setNullForEmptyString(g.getIndirizzo()) == null) {
            kContentList.add(g.getRegione());
            kContentList.add(g.getEdificio());
            kContentList.add(g.getCity());
        } else {
            kContentList.add(g.getRegione());
            kContentList.add(g.getCity());
        }
        String n;
        try{
            n= set.checkGMRegionByNazione(g.getNazione()).toLowerCase();
        }catch(java.lang.NullPointerException ne){n="it";}
        return getCoordinatesFromStringAddress(kContentList,n);
    }

    private String prepareRawAddress(List<String>  kContentList,String nation){
        String fua = "";
        for (String s : kContentList) {
            if(s!=null){
                String address = manageArrayString(s);
                fua = fua +"+"+ address+"+";
                fua = reduceString(fua);
            }
        }
        fua = removeFirstAndLast(fua, "+");
        String prefix = "https://maps.googleapis.com/maps/api/geocode/json?";
        String address= "address="+fua;
        String apiKey = API_KEY_GM;
        String suffix = "&sensor=false";
        String region ="";
        if(!StringKit.isNullOrEmpty(nation))region ="&region="+nation;
        return prefix+address+region+suffix;
    }

    private LatLng getCoordinatesFromStringAddress(List<String> rawAddress,String nation){
        JSONObject json;
        try {
            URL url = new URL(prepareRawAddress(rawAddress,nation));
            SystemLog.message("URL for GM:" + url.toString());
            json =  temporizzatorePerGoogleMaps(url);
            if (json != null) {
                if(!(json.toString().contains("\"status\":\"ZERO_RESULTS\"")) ||
                        !(json.toString().contains("\"status\":\"OVER_QUERY_LIMIT\""))){
                    try{
                        Double lat=(Double) json.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").get("lat");
                        Double lng=(Double) json.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").get("lng");
                        if(lat!=null && lng!=null){
                            return new LatLng(lat,lng);
                        }
                    }catch(JSONException je){
                        SystemLog.warning("JSON:" + json.toString());
                        return new LatLng(null,null);
                    }
                    //Se Google Maps ha raggiunto il massimo numero di query possibili
                    //prova con OpenStreetMap
    //            }else if(json.toString().contains("\"status\":\"OVER_QUERY_LIMIT\"")){
    //                 try{
    //                    LatLng ll = o.tryWithOpenStreetMap(fua);
    //                    Double lat =(Double) ll.getLat();
    //                    Double lng =(Double) ll.getLng();
    //                    if(lat!=null && lng!=null){
    //                        geoDoc.setLat(lat);
    //                        geoDoc.setLng(lng);
    //                    }
    //                }catch(Exception ex){}
                }else{
                    if(json.toString().contains("\"status\":\"OVER_QUERY_LIMIT\"")){
                        SystemLog.error(json.toString());
                    }
                }
            }//json !=null
        } catch (IOException|java.lang.NullPointerException ex){
            SystemLog.exception(ex);
        }
        return new LatLng(null,null);
    }

    /**
     * Metodo per strutturare il contenuto dell'annotazione
     * @param address il contenuto dell'annotazione sottoforma di stringa
     * @return il contenuto struttturato
     */
    private String manageArrayString(String address){
         //String sgm="";
         address = address.replaceAll("\\W","+");
         address = address.replaceAll("(\\+)\\1+","+");       
         return address;
   }
   /**
    * Metodo che strttura la stringa fornita per essere utilizzata
    * come input dall'API Google Maps.
    * @param fua la stringa da ridurre
    * @return ritorna la stringa ridotta
    */ 
   private String reduceString(String fua){
       String reduce="";
       List<String> list = new ArrayList<>();

       //removeFirstAndLast(fua, "+");

       StringTokenizer stringtokenizer = new StringTokenizer(fua, "+");
        while (stringtokenizer.hasMoreElements()) {
             String s= stringtokenizer.nextToken();
             if(!(list.contains(s)) && setNullForEmptyString(s)!=null){list.add(s);}
        }
       for (String s : list) { reduce = reduce+"+"+s;}
       return reduce;
   }
   /**
    * Rimuove il simbolo separatore del contentuo se presente all'inzio o
    * alla fine della stringa
    * @param fua il contentuo dell'annotazione sottoforma di stringa
    * @param symbol il simbolo separatore del contenuto
    * @return la stringa del contenuto senza simboli separatori all'inizio e alla fine
    */
   private String removeFirstAndLast(String fua,String symbol){
       if(!StringKit.isNullOrEmpty(fua)){
            fua = fua.replaceAll("(\\"+symbol+")\\1+",symbol); 
            if(fua.substring(0,1).contains(symbol)){                
                fua = fua.substring(1,fua.length());
            }
            if(fua.substring(fua.length()-1,fua.length()).contains(symbol)){                
                fua = fua.substring(0,fua.length()-1);
            }
         }
       return fua;
   }
   /**
    * Setta a null se verifica che la stringa non è
    * nulla, non è vuota e non è composta da soli spaceToken (white space)
    * @param s stringa di input
    * @return  il valore della stringa se null o come è arrivata
    */
    private String setNullForEmptyString(String s){
        //verifica che la stringa non è nulla, non è vuota e non è composta da soli spaceToken (white space)
        if(s!=null && !s.isEmpty() && !s.trim().isEmpty()){return s;}
        else{return null;}
    }        
    
    
    /////////////////////////////////////////////////////////////////////////////////////
    ///// METODI PER INVOCARE UN NUMERO INDEFINITO DI QUERY ALL'API GOOGLE MAPS//////////
    /////////////////////////////////////////////////////////////////////////////////////
    /**
     * Metodo che invoca ogni tot secondi in modo random le richieste GET HTTP
     * all'API Google Maps per evitare di superare il numero di query giornaliere
     * disponibili con la licenza freeware, è un semplice ritardo random tra una
     * richiesta e lt'altra verso lt'API Google Maps.
     * @param url identifica la richiesta all'API Google Maps .
     * @return ritorna il risultato della richiesta ma con un ritardo.
     * @throws IOException error.
     */
    public JSONObject temporizzatorePerGoogleMaps(URL url) throws IOException{
         ManageJsonWithGoogleMaps mgm = new ManageJsonWithGoogleMaps(API_KEY_GM);            
         //boolean result =false;
         //String last="";
         JSONObject json = new JSONObject();
         String jsonText = "";
         try{
            try{
               HttpUtil.waiter();
               //FUNZIONA
               jsonText = HttpUtil.get(url.toString());
            } catch (InterruptedException e) {
                SystemLog.warning(e.getMessage());
            } finally{
                //UN SECONDO TENTATIVO IN CASO DI FALLIMENTO (TIMEOUT,ECC.)
                if(StringKit.isNullOrEmpty(jsonText)){
                    jsonText = HttpUtilApache4.GETWithRetry(url.toString());
                }
            }
         json = new JSONObject(jsonText);
        }finally{
            //SE SUCCEDE QUALUNQUE COSA CON HTTP SI USA JSOUP IN EXTREMIS
            try{
                if(StringKit.isNullOrEmpty(jsonText)){
                    try {
                        jsonText = org.jsoup.Jsoup.connect(url.toString()).ignoreContentType(true).execute().body();
                        json = new JSONObject(jsonText);
                    }catch(org.jsoup.HttpStatusException e){
                        json = new JSONObject("{\"results\" : [],\"status\" : \"ZERO_RESULTS\"}");
                    }
                }
            }finally{
                if(StringKit.isNullOrEmpty(json.toString()))json = null;
            }
        }
        return json;
                                 
    }

   
    
    /**
     * Semplice metodo che estare il domino di appartenenza dell'url analizzato.
     * @param u url di ingresso in fromato stringa.
     * @return il dominio dell'url in formato stringa.
     * @throws URISyntaxException error.
     */
    public String getDomainName(String u) throws URISyntaxException {     
            URI uri = new URI(u);
            return uri.getHost();
    }//getDomainName
    
}//ManageJsonWithGoogleMaps.java.
