package com.github.p4535992.extractor.estrattori;

import com.github.p4535992.extractor.object.support.LatLng;
import com.github.p4535992.util.http.HttpKit;
import com.github.p4535992.util.http.impl.HttpUtil;
import com.github.p4535992.util.log.SystemLog;
import com.github.p4535992.util.string.impl.StringIs;
import com.github.p4535992.util.string.impl.StringKit;
import com.github.p4535992.extractor.ManageJsonWithGoogleMaps;
import com.github.p4535992.extractor.setInfoParameterIta.SetCodicePostale;
import com.github.p4535992.extractor.setInfoParameterIta.SetNazioneELanguage;
import com.github.p4535992.extractor.setInfoParameterIta.SetProvinciaECity;
import com.github.p4535992.extractor.setInfoParameterIta.SetRegioneEProvincia;
import com.github.p4535992.extractor.object.model.GeoDocument;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by 4535992 on 15/06/2015.
 * @author 4535992
 * @version 2015-06-30
 */
@SuppressWarnings("unused")
public class ExtractorGeoDocumentSupport {

    ManageJsonWithGoogleMaps j = ManageJsonWithGoogleMaps.getInstance();

    private static ExtractorGeoDocumentSupport instance = null;
    protected ExtractorGeoDocumentSupport(){}

    public static ExtractorGeoDocumentSupport getInstance(){
        if(instance == null) {
            instance = new ExtractorGeoDocumentSupport();
        }
        return instance;
    }

    public static ExtractorGeoDocumentSupport getNewInstance(){
        instance = new ExtractorGeoDocumentSupport();
        return instance;
    }

    public GeoDocument UpgradeTheDocumentWithOtherInfo(GeoDocument geo) throws URISyntaxException {
        try{
            SystemLog.message("**************DOCUMENT*********************");
            HttpKit http = HttpKit.getInstance();
            //*************************************************************************************
            //INTEGRAZIONE FINALE CON IL DATABASE KEYWORDDB
            //SET CITY IF YOU DON'T HAVE
//            if(StringKit.setNullForEmptyString(geo.getCity())==null ){
//                //SystemLog.message("Integrazione Keyworddb");
//                geo.setCity(Docdao.selectValueForSpecificColumn("city", "url", geo.getUrl().toString()));
//            }
            //SET PROVINCIA
            if(StringKit.setNullForEmptyString(geo.getCity())!=null){
                SetProvinciaECity set = new SetProvinciaECity();
                geo.setProvincia(set.checkProvincia(geo.getCity()));
            }
            //INTEGRAZIONE DEI CAMPI CITY-PROVINCIA-REGIONE DEI GEDOCUMENT
            if(StringKit.setNullForEmptyString(geo.getCity())!=null){
                SetRegioneEProvincia set = new SetRegioneEProvincia();
                set.checkString(geo.getCity());
                geo.setRegione(set.getRegione());
                geo.setProvincia(set.getProvincia());
            }
            //INTEGRAZIONE DEL CAMPO LANGUAGE -> NAZIONE
            SetNazioneELanguage set = new SetNazioneELanguage();
            String language;
            if(!StringIs.isNullOrEmpty(geo.getNazione())) {
               language = geo.getNazione();
            }else{ language = "it";}
            String domain = http.getDomainName(geo.getUrl().toString());
            String nazione="";
            if(!StringIs.isNullOrEmpty(domain)) {
                nazione = set.checkNazioneByDomain(domain);
            }
            //con il linguaggio identificato da Tika se fallisce il controllo del
            //dominio per esempio con estensioni .com,.edu,ecc.
            if(geo.getNazione() == null && language != null){
                nazione = set.checkNazioneByLanguageIdentificatorByTika(language);
            }
            geo.setNazione(nazione);
            //PULIAMO NUOVAMENTE LA STRINGA EDIFICIO E INDIRIZZO (UTILE NEL CASO DI SearchMonkey e Tika)
            geo = pulisciDiNuovoLaStringaEdificio(geo);
            geo = pulisciDiNuovoLaStringaIndirizzo(geo);
            //AGGIUNGIAMO POSTALCODE E INDIRIZZONOCAP
            geo = new GeoDocument(geo.getUrl(), geo.getRegione(), geo.getProvincia(), geo.getCity(), geo.getIndirizzo(), geo.getIva(), geo.getEmail(), geo.getTelefono(),geo.getFax(), geo.getEdificio(),
                    geo.getLat(), geo.getLng(),geo.getNazione(),geo.getDescription(),null, null,null);
            String indirizzo = geo.getIndirizzo();
            String indirizzoNoCAP = null;
            String postalCode =null;
            String indirizzoHasNumber = null;
            SetCodicePostale setCap = new SetCodicePostale();
            if(indirizzo != null){
                indirizzoNoCAP = indirizzo.replaceAll("\\d{5,6}", "").replace("-", "");
                postalCode = setCap.GetPostalCodeByIndirizzo(indirizzo);
            }//if indrizzo not null
            if(StringIs.isNullOrEmpty(postalCode)){
                postalCode = setCap.checkPostalCodeByCitta(geo.getCity());//work
            }
            geo.setPostalCode(postalCode);
            if(indirizzoNoCAP != null){
                //indirizzoNoCAP = indirizzo.replaceAll("\\d{5,6}", "").replace("-", "");
                indirizzoHasNumber = setCap.GetNumberByIndirizzo(indirizzoNoCAP);
            }//if indrizzo not null
            if(indirizzoHasNumber!=null && StringKit.setNullForEmptyString(indirizzoHasNumber)!= null){
                geo.setIndirizzoHasNumber(indirizzoHasNumber);
                indirizzoNoCAP = indirizzoNoCAP.replace(indirizzoHasNumber,"").replaceAll("[\\^\\|\\;\\:\\,]","");
            }
            geo.setIndirizzoNoCAP(indirizzoNoCAP);
            //UPDATE THE "indirizzo" FIELD TO "indirizzoNoCAP"+","+"indirizzoHasNumber"
            if(StringIs.isNullOrEmpty(geo.getIndirizzoHasNumber()) && !StringIs.isNullOrEmpty(geo.getIndirizzoNoCAP())) {
                geo.setIndirizzo(geo.getIndirizzoNoCAP().trim());
            }else if(!StringIs.isNullOrEmpty(geo.getIndirizzoHasNumber()) && !StringIs.isNullOrEmpty(geo.getIndirizzoNoCAP())){
                geo.setIndirizzo(geo.getIndirizzoNoCAP().trim()+", "+geo.getIndirizzoHasNumber().trim());
            }else{
                geo.setIndirizzo(null);
            }
            //INTEGRAZIONE DEI CAMPI DELLE COORDINATE CON GOOGLE MAPS
            LatLng coord = j.getCoords(geo);
            geo.setLat(coord.getLat());
            geo.setLng(coord.getLng());
            SystemLog.message("COORD[LAT:" + geo.getLat() + ",LNG:" + geo.getLng() + "]");
        }catch(NullPointerException ne){
            ne.printStackTrace();
        }
        return geo;
    }


    /**
     * Metodo che ripulisce il nome edificio da usare come URI da caratteri non
     * non voluti
     * @param geo GeoDocument fornito come input
     * @return il GeoDocument con il campo Edificio settato in un nuovo modo
     */
    public GeoDocument pulisciDiNuovoLaStringaEdificio(GeoDocument geo){
        //Le seguenti righe di codice aiutano ad evitare un'errore di sintassi
        //in fase di inserimento dei record nel database.

            String set;
            if(!StringIs.isNullOrEmpty(geo.getEdificio())){
                set=geo.getEdificio();
                //set = geo.getEdificio().replaceAll("[^a-zA-Z\\d\\s:]","");
                //Accetta le lettere accentuate
                if(set.toLowerCase().contains("http://")){
                    try {
                        set = HttpUtil.getAuthorityName(set);
                        set = set.replaceAll("(https?|ftp)://", "");
                        set = set.replaceAll("(www(\\d)?)", "");
                        set = set.replace(".", " ");
                        set = set.replace("/", " ");
                    }catch (URISyntaxException e) {
                        SystemLog.warning("Edificio is a malformed url:"+set);
                    }
                }
                set = set.replaceAll("[^a-zA-Z\\u00c0-\\u00f6\\u00f8-\\u00FF\\d\\s:]","");
                set = set.replaceAll("\\s+", " ");
                //set = set.replaceAll("http", "");
                set = set.replace(":", "");
                if(set!= null){
                    geo.setEdificio(set.toUpperCase());
                }
            }
            return geo;
        }//pulisciDiNuovoLaStringaEdificio

    /**
     * Metodo che ripulisce il nome indirizzo da usare come URI da caratteri non
     * non voluti
     * @param geo GeoDocument fornito come input
     * @return il GeoDocument con il campo indirizzo settato in un nuovo modo
     */
    public GeoDocument pulisciDiNuovoLaStringaIndirizzo(GeoDocument geo){
        try{
            String address = geo.getIndirizzo();
            // address = "’Orario di Lavoro - Piazza San Marco, 4 - 50121";
            if(address != null){
                if(StringKit.setNullForEmptyString(address)!=null){
                    //Rimuovi gli SpaceToken
                    //set = geo.getIndirizzo().replaceAll("[^a-zA-Z\\d\\s:]","");
                    address = address.replaceAll("\\s+", " ");
                    //set = set.replaceAll("...", "");

                    List<String> addressWords = Arrays.asList(
                            "VIA", "via", "Via", "VIALE", "viale", "Viale", "STRADA", "strada", "Strada", "ROAD",
                            "road", "Road", "Piazza", "PIAZZA",
                            "piazza", "P.zza", "p.zza", "Piazzale", "piazzale", "iazza", "Corso", "Loc.",
                            "loc.", "loc", "Loc", "località","Località", "V.", "v."
                    );
                    boolean b = false;
                    for(String s : addressWords){
                        //With Split
                        String[] listSplit = address.split(" "+s+" ");
                        if(listSplit.length > 0){
                            for(int i =0; i < listSplit.length; i++){
                                if(i > 0){
                                    address ="";
                                    address = address + listSplit[i];
                                    address = s+" "+address;
                                    b=true; break;
                                }
                                //if(i == listSplit.length-1){indirizzo = s+" "+indirizzo;}
                            }
                        }
                        if(b){
                            //"[^a-z0-9\\s]+"
                            address = address.replaceAll("[^A-Za-z0-9\\s]+","");
                            geo.setIndirizzo(address);
                        }
                    }//FOR
                }//IF indirizzo is not null
            }//IF NOT NULL
        }catch(NullPointerException ne){ne.printStackTrace();}


        return geo;
    }


    /**
     * Metodo di comparazione dei risultati attraverso GATE e Apache Tika per la modalità 1
     * @param geo GeoDocument ricavato da DOM HTML
     * @param geo2 GeoDocument ricavato da anlisi del GATE
     * @return un geodocument con il meglio dei risultati di entrambe le ricerche
     */
    public static GeoDocument compareInfo3(GeoDocument geo,GeoDocument geo2){
        if(StringKit.setNullForEmptyString(geo2.getEdificio())!=null){geo.setEdificio(geo2.getEdificio());}
        else if(StringKit.setNullForEmptyString(geo2.getEdificio())==null){geo.setEdificio(StringKit.setNullForEmptyString(geo.getUrl().toString()));}
        if(StringKit.setNullForEmptyString(geo2.getDescription())!=null){geo.setDescription(geo2.getDescription());}
        if(StringKit.setNullForEmptyString(geo2.getNazione())!=null){geo.setNazione(geo2.getNazione());}
        return geo;
    }//compareInfo3

    public GeoDocument pulisciDiNuovoGeoDocument(GeoDocument geo){
        if(geo.getRegione()!=null){geo.setRegione(geo.getRegione().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        if(geo.getProvincia()!=null){geo.setProvincia(geo.getProvincia().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        if(geo.getCity()!=null){geo.setCity(geo.getCity().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        if(geo.getIndirizzo()!=null){geo.setIndirizzo(geo.getIndirizzo().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        if(geo.getIva()!=null){geo.setIva(geo.getIva().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        if(geo.getEmail()!=null){geo.setEmail(geo.getEmail().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        if(geo.getTelefono()!=null){geo.setTelefono(geo.getTelefono().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        if(geo.getEdificio()!=null){geo.setEdificio(geo.getEdificio().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        if(geo.getNazione()!=null){geo.setNazione(geo.getNazione().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        //geo.setLat(geo.getLat().replaceAll("\\r\\n|\\r|\\n","").trim());
        //geo.setLng(geo.getLng().replaceAll("\\r\\n|\\r|\\n","").trim());
        if(geo.getDescription()!=null){geo.setDescription(geo.getDescription().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        if(geo.getIndirizzoNoCAP()!=null){geo.setIndirizzoNoCAP(geo.getIndirizzoNoCAP().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        if(geo.getPostalCode()!=null){geo.setPostalCode(geo.getPostalCode().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        if(geo.getFax()!=null){geo.setFax(geo.getFax().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        if(geo.getIndirizzoHasNumber()!=null){geo.setIndirizzoHasNumber(geo.getIndirizzoHasNumber().replaceAll("\\r\\n|\\r|\\n","").replace("\\n\\r", "").replace("\\n","").replace("\\r","").trim()); }
        return geo;

    }


    /**
     * Ripulisce la stringa edificio da caratteri non voluti
     * @param edificio la stringa da "ripulire"
     * @return la stringa "ripulita"
     */
    public String pulisciStringaEdificio(String edificio){
        List<String> badWords = Arrays.asList("INDEX","index", "home/home", "HOME", "homepage","HOMEPAGE",
                "page","PAGE","Homepage","Page","Home","Chi siamo","Chi Siamo","Portale","portale",
                "NEWS","News","Benvenuto nel","benvenuto nel","Benvenuto","benvenuto","CHI SIAMO"
        );
        for(String s: badWords){
            if(edificio.contains(s)){
                //System.out.println("STAMPA:"+s);
                edificio = edificio.replaceAll(s," ");
            }
        }
        //if(linkString.contains("|")){st = new StringTokenizer(linkString, "|");}
        //org.apache.commons.lang3.StringUtils.containsIgnoreCase("ABCDEFGHIJKLMNOP", "gHi");
//        StringTokenizer st = null;
//
//        st = new StringTokenizer(edificio, "|");
//        while (st.hasMoreTokens()) {
//                edificio = st.nextToken().toString();
//                if(setNullForEmptyString(edificio)==null){
//                    continue;
//                }else{break;}
//        }

        if(edificio.contains("...")){edificio = edificio.replace("...", "");}
        if(edificio.contains(".")){edificio = edificio.replace(".", ""); }
        if(edificio.contains("::")){edificio = edificio.replace(".", "");}
        if(edificio.contains(":")){edificio = edificio.replace(".", ""); }
        //edificio = getTheFirstTokenOfATokenizer(edificio, "|");
        edificio = getTheFirstTokenOfATokenizer(edificio, "...");
        //edificio = getTheFirstTokenOfATokenizer(edificio, ",");
        edificio = getTheFirstTokenOfATokenizer(edificio, ".");
        edificio = getTheFirstTokenOfATokenizer(edificio, ";");
        //edificio = getTheFirstTokenOfATokenizer(edificio, "-");

        //System.out.println("TIKA:"+edificio);
        return edificio;
    }

    /**
     * Metodo che "taglia" la descrizione dell'edificio al minimo indispensabile.
     * @param content stringa del contenuto da tokenizzare.
     * @param symbol simbolo del tokenizer.
     * @return la stringa tokenizzata.
     */
    public static String getTheFirstTokenOfATokenizer(String content,String symbol){
        StringTokenizer st = new StringTokenizer(content, symbol);
        while (st.hasMoreTokens()) {
            content = st.nextToken();
            if(!StringIs.isNullOrEmpty(content)){
                break;
            }
        }
        return content;
    }

    /**
     * Method for update all record with the Coordinates null or empty.
     */
   /* public void reloadNullCoordinatesGeoDocumentOnTheDatabase(
        GeoDocumentDaoImpl geoDocumentDao,String[] column,String[] columns_where,String tableName){
        ManageJsonWithGoogleMaps j = ManageJsonWithGoogleMaps.getInstance();
        //String[] columns_where = new String[]{"latitude","longitude"};
        Object[] values_where = new Object[]{null,null};
        List<org.jooq.Condition> conditions = new ArrayList<>();
        for(String coord : columns_where) {
            *//*org.jooq.Condition condition1 = SQLJooqKit.createField(coord).isNull(); //is NULL...
            org.jooq.Condition condition2 = SQLJooqKit.createField(coord).length().eq(0); //is empty
            org.jooq.Condition condition3 = SQLJooqKit.createField(coord).notEqual("0"); //is not "0"
            org.jooq.Condition condition4 = SQLJooqKit.createField(coord).greaterOrEqual("1"); //greater than 0*//*
            conditions.add(SQLJooqKit.createField(coord).isNull());
            conditions.add(SQLJooqKit.createField(coord).length().eq(0));
            conditions.add(SQLJooqKit.createField(coord).notEqual("0"));
            conditions.add(SQLJooqKit.createField(coord).greaterOrEqual("1"));
        }
        try {
            values_where = new Object[]{null,null};
            List<List<Object[]>> listGeoDoc =
                    geoDocumentDao.select(new String[]{"*"},columns_where, values_where,conditions);

            values_where = new Object[]{"",""};
            List<GeoDocument> listGeoDoc2 =
                    geoDocumentDao.select(new String[]{"*"},columns_where,values_where,conditions );
            listGeoDoc.addAll(listGeoDoc2);

            values_where = new Object[]{0,0};
            listGeoDoc2 = geoDocumentDao.select(new String[]{"*"},columns_where,values_where, LIMIT, OFFSET, "AND");
            listGeoDoc.addAll(listGeoDoc2);
            for (GeoDocument geo : listGeoDoc) {
                LatLng coord = j.getCoords(geo);
                if(coord.getLat() == 0 && coord.getLng()==0){
                    values_where = new Object[]{null,null};
                }else{
                    values_where = new Object[]{coord.getLat(),coord.getLng()};

                }
                geoDocumentDao.update(columns_where, values_where, "url", geo.getUrl().toString());
            }
        }catch(URISyntaxException e){
            SystemLog.exception(e);
        }
    }*/
}
