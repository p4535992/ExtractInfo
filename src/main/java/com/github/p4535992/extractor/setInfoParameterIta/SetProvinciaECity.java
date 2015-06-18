package com.github.p4535992.extractor.setInfoParameterIta;



import java.util.HashMap;
import java.util.Map;

/**
 * Created by Marco on 18/04/2015.
 */
public class SetProvinciaECity {

    private static Map<String,String> simpleMap =  new HashMap<String,String>();

    public SetProvinciaECity(){
        simpleMap.put("bagno a ripoli","Firenze");
        simpleMap.put("barberino di mugello","Firenze");
        simpleMap.put("barberino val d'elsa","Firenze");
        simpleMap.put("borgo san lorenzo","Firenze");
        simpleMap.put("calenzano","Firenze");
        simpleMap.put("campi bisenzio","Firenze");
        simpleMap.put("capraia e limite","Firenze");
        simpleMap.put("castelfiorentino","Firenze");
        simpleMap.put("cerreto guidi","Firenze");
        simpleMap.put("certaldo","Firenze");
        simpleMap.put("dicomano","Firenze");
        simpleMap.put("empoli","Firenze");
        simpleMap.put("fiesole","Firenze");
        simpleMap.put("figline valdarno","Firenze");
        simpleMap.put("firenze","Firenze");
        simpleMap.put("firenzuola","Firenze");
        simpleMap.put("fucecchio","Firenze");
        simpleMap.put("gambassi terme","Firenze");
        simpleMap.put("greve in chianti","Firenze");
        simpleMap.put("impruneta","Firenze");
        simpleMap.put("incisa in val d'arno","Firenze");
        simpleMap.put("lastra a signa","Firenze");
        simpleMap.put("londa","Firenze");
        simpleMap.put("marradi","Firenze");
        simpleMap.put("montaione","Firenze");
        simpleMap.put("montelupo fiorentino","Firenze");
        simpleMap.put("montespertoli","Firenze");
        simpleMap.put("palazzuolo sul senio","Firenze");
        simpleMap.put("pelago","Firenze");
        simpleMap.put("pontassieve","Firenze");
        simpleMap.put("reggello","Firenze");
        simpleMap.put("rignano sull'arno","Firenze");
        simpleMap.put("rufina","Firenze");
        simpleMap.put("san casciano in val di pesa","Firenze");
        simpleMap.put("san godenzo","Firenze");
        simpleMap.put("san piero a sieve","Firenze");
        simpleMap.put("scandicci","Firenze");
        simpleMap.put("scarperia","Firenze");
        simpleMap.put("sesto fiorentino","Firenze");
        simpleMap.put("signa","Firenze");
        simpleMap.put("tavarnelle val di pesa","Firenze");
        simpleMap.put("vaglia","Firenze");
        simpleMap.put("vicchio","Firenze");
        simpleMap.put("vinci","Firenze");
    }

    public String checkProvincia(String city){
        SetProvinciaECity spc = new  SetProvinciaECity();
        String provincia = "";
        for(Map.Entry<String, String>  entry : simpleMap.entrySet()){
            if(entry.getKey().toLowerCase().contains(city.toLowerCase().trim())) {
                provincia = entry.getValue();
                break;
            }
        }
        return provincia;
    }

//    public static void  main(String args[]){
//        String provincia = checkProvincia("Figline Valdarno");
//    }

}


