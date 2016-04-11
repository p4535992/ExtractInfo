package com.github.p4535992.extractor.estrattori.gtfs;

import com.github.p4535992.extractor.estrattori.gtfs.database.support.GTFSModel2;
import com.github.p4535992.extractor.estrattori.gtfs.tordf.helper.TransformerPicker;
import com.github.p4535992.extractor.estrattori.gtfs.tordf.transformer.Transformer;
import com.github.p4535992.extractor.estrattori.util2.Jena2Utilities;
import com.github.p4535992.util.database.sql.SQLUtilities;
import com.github.p4535992.util.file.FileUtilities;
import com.github.p4535992.util.file.archive.ZtZipUtilities;

import com.github.p4535992.util.log.logback.LogBackUtil;
import com.github.p4535992.util.string.StringUtilities;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;

import java.io.File;
import java.io.IOException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 4535992 on 30/11/2015.
 * @author 4535992.
 * @version 2015-11-30.
 */
public class GTFSUtilities {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(GTFSUtilities.class);

    protected GTFSUtilities(){}

    private static GTFSUtilities instance = null;

    public static GTFSUtilities getInstance(){
        if(instance == null){
            instance = new GTFSUtilities();
        }
        return instance;
    }

    public static GTFSUtilities getNewInstance(){
        instance = new GTFSUtilities();
        return instance;
    }

    private Map<String,String> setPrefixes(){
        Map<String,String>  map = new HashMap<>();
        map.put("gtfs","http://vocab.gtfs.org/terms#");
        map.put("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        map.put("foaf","http://xmlns.com/foaf/0.1/");
        map.put("dct","http://purl.org/dc/terms/");
        map.put("rdfs","http://www.w3.org/2000/01/rdf-schema#");
        map.put("owl","http://www.w3.org/2002/07/owl#");
        map.put("xsd","http://www.w3.org/2001/XMLSchema#");
        map.put("vann","http://purl.org/vocab/vann/");
        map.put("skos","http://www.w3.org/2004/02/skos/core#");
        map.put("schema","http://schema.org/");
        map.put("dcat","http://www.w3.org/ns/dcat#");
        return map;
    }

    private List<List<Statement>> mapper(File zipFile, String baseUri) throws IOException {
        //List<File> files = ArchiveUtilities.extractAllFromZip(zipFile,FileUtilities.getDirectoryFullPath(zipFile));
        List<File> files =
                FileUtilities.getFilesFromDirectory(
                        ZtZipUtilities.extractDirectoryFromZip(zipFile, new File(FileUtilities.getAndCreateDirectoryFullPath(zipFile))
                        )
                );
        List<List<Statement>> listStmt = new ArrayList<>();
        logger.info("Start conversion GTFS to RDF...");
        for(File file : files){
            String nameFile = FileUtilities.getFilenameWithoutExt(file.getAbsolutePath());
            Transformer specificTransformer = TransformerPicker.getNewInstance(nameFile).getTransformer();
            List<Statement> stmts = specificTransformer._Transform(file, StringUtilities.UTF_8, baseUri);
            listStmt.add(stmts);
        }
        logger.info("...end conversion GTFS to RDF");
        return listStmt;
    }

    private Model prepareModel(File zipFile, String baseUri) throws IOException {
        Model jModel = Jena2Utilities.createModel();
        jModel.setNsPrefixes(setPrefixes());
        for(List<Statement> list : mapper(zipFile,baseUri)){
            jModel.add(list);
        }
        return jModel;
    }

    public File convertGTFSFileToRDF(File file,String baseUri,File fileRDFOutput, Lang outputFormat){
        return convertGTFSFileToRDF(file,baseUri,fileRDFOutput,outputFormat.getName());
    }

    public File convertGTFSFileToRDF(File file,String baseUri,File fileRDFOutput, RDFFormat outputFormat){
        return convertGTFSFileToRDF(file,baseUri,fileRDFOutput,outputFormat.getLang().getName());
    }

    public File convertGTFSFileToRDF(File file,String baseUri,File fileRDFOutput, String outputFormat){
        Model jModel = Jena2Utilities.createModel();
        jModel.setNsPrefixes(setPrefixes());
        String nameFile = FileUtilities.getFilenameWithoutExt(file.getAbsolutePath());
        Transformer specificTransformer = TransformerPicker.getNewInstance(nameFile).getTransformer();
        List<Statement> stmts = specificTransformer._Transform(file, StringUtilities.UTF_8, baseUri);
        jModel.add(stmts);
        Jena2Utilities.write(fileRDFOutput,jModel,outputFormat);
        return fileRDFOutput;
    }

    public List<Statement> getRDFFromGTFSFile(File file,String baseUri){
        Model jModel = Jena2Utilities.createModel();
        jModel.setNsPrefixes(setPrefixes());
        String nameFile = FileUtilities.getFilenameWithoutExt(file.getAbsolutePath());
        Transformer specificTransformer = TransformerPicker.getNewInstance(nameFile).getTransformer();
        return specificTransformer._Transform(file, StringUtilities.UTF_8, baseUri);
    }

    public File convertGTFSZipToRDF(File zipFile,String baseUri,File fileOutput, String outputFormat) throws IOException {
        Model model = prepareModel(zipFile,baseUri);
        Jena2Utilities.write(fileOutput,model,outputFormat);
        return fileOutput;
    }

    public File convertGTFSZipToRDF(File zipFile,String baseUri,File fileOutput, RDFFormat outputFormat) throws IOException {
        Model model = prepareModel(zipFile,baseUri);
        Jena2Utilities.write(fileOutput,model,outputFormat);
        return fileOutput;
    }

    public File convertGTFSZipToRDF(File zipFile,String baseUri,File fileOutput, Lang outputFormat) throws IOException {
        Model model = prepareModel(zipFile,baseUri);
        Jena2Utilities.write(fileOutput,model,outputFormat);
        return fileOutput;
    }

    /**
     * href:https://github.com/SparkandShine/data_analysis/blob/master/traces/tisseo_toulouse_gtfs/load.sql
     * @param zipFile the {@link File} to the Zip File.
     * @param conn the {@link Connection} to the SQL Database.
     * @param database the {@link String} name of the dataabase/schema.
     * @param createDatabase the {@link Boolean} is true if drop and create the database.
     * @param createTables the {@link Boolean} is true if drop and create the tables.
     * @return the {@link Boolean} is true if all the operations are done.
     */
    public Boolean importGTFSZipToDatabase(File zipFile,Connection conn,String database,boolean createDatabase,boolean createTables){
        try {
            SQLUtilities.setConnection(conn);
            if(createDatabase){
                //-- CREATE DATABASE IF NOT EXISTS gtfs;
                SQLUtilities.executeSQL(
                        "DROP DATABASE IF EXISTS "+database+";");
                SQLUtilities.executeSQL("CREATE DATABASE "+database+" " +
                        "DEFAULT CHARACTER SET utf8 " +
                        "DEFAULT COLLATE utf8_general_ci;"
                );
            }
            //Map<String,String[]> map = SQLUtilities.getTableAndColumn(conn,database);
            Map<String,File> files2 = getGTFSFilesFromZipFile(zipFile);
            if (createTables) {
                SQLUtilities.executeSQL("USE gtfs;");
                //"-- agency_id,agency_name,agency_url,agency_timezone,agency_phone,agency_lang\n" +
                SQLUtilities.executeSQL("DROP TABLE IF EXISTS agency;");
                SQLUtilities.executeSQL("CREATE TABLE `agency` ("
                        + GTFSModel2.prepareColumn(files2.get("agency"),GTFSFileType2.agency)+");");
                //"-- shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence\n" +
                SQLUtilities.executeSQL("DROP TABLE IF EXISTS shapes;");
                SQLUtilities.executeSQL("CREATE TABLE `shapes` ("
                        + GTFSModel2.prepareColumn(files2.get("shapes"),GTFSFileType2.shapes)+");");
                //"-- service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date\n" +
                SQLUtilities.executeSQL("DROP TABLE IF EXISTS calendar;");
                SQLUtilities.executeSQL("CREATE TABLE `calendar` ("
                        + GTFSModel2.prepareColumn(files2.get("calendar"),GTFSFileType2.calendar)+");");
                //"-- service_id,date,exception_type\n" +
                SQLUtilities.executeSQL("SET FOREIGN_KEY_CHECKS=0;");
                SQLUtilities.executeSQL("DROP TABLE IF EXISTS calendar_dates;");
                SQLUtilities.executeSQL("CREATE TABLE `calendar_dates` ("
                        + GTFSModel2.prepareColumn(files2.get("calendar_dates"),GTFSFileType2.calendar_dates)+ ");");
                SQLUtilities.executeSQL("SET FOREIGN_KEY_CHECKS=1;");
                //"-- route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color\n" +
                SQLUtilities.executeSQL("DROP TABLE IF EXISTS routes;");
                SQLUtilities.executeSQL("CREATE TABLE `routes` ("
                        + GTFSModel2.prepareColumn(files2.get("routes"),GTFSFileType2.routes)+");");
                //"-- trip_id,service_id,route_id,trip_headsign,direction_id,shape_id\n" +
                SQLUtilities.executeSQL("DROP TABLE IF EXISTS trips;");
                SQLUtilities.executeSQL("CREATE TABLE `trips` ("
                        + GTFSModel2.prepareColumn(files2.get("trips"),GTFSFileType2.trips)+");");
                //"-- stop_id,stop_code,stop_name,stop_lat,stop_lon,location_type,parent_station,wheelchair_boarding\n" +
                SQLUtilities.executeSQL("DROP TABLE IF EXISTS stops;");
                SQLUtilities.executeSQL("CREATE TABLE `stops` ("
                        + GTFSModel2.prepareColumn(files2.get("stops"),GTFSFileType2.stops)+");");
                //"-- trip_id,stop_id,stop_sequence,arrival_time,departure_time,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled\n" +
                SQLUtilities.executeSQL("SET FOREIGN_KEY_CHECKS=0;");
                SQLUtilities.executeSQL("DROP TABLE IF EXISTS stop_times;");
                SQLUtilities.executeSQL("CREATE TABLE `stop_times` ("
                        + GTFSModel2.prepareColumn(files2.get("stop_times"),GTFSFileType2.stop_times)+");");
                SQLUtilities.executeSQL("SET FOREIGN_KEY_CHECKS=1;");
                //"-- trip_id,start_time,end_time,headway_secs\n" +
                SQLUtilities.executeSQL("SET FOREIGN_KEY_CHECKS=0;");
                SQLUtilities.executeSQL("DROP TABLE IF EXISTS frequencies;");
                SQLUtilities.executeSQL("CREATE TABLE `frequencies` ("
                        + GTFSModel2.prepareColumn(files2.get("frequencies"),GTFSFileType2.frequencies)+");");
                SQLUtilities.executeSQL("SET FOREIGN_KEY_CHECKS=1;");
            }
            //load data
            for(Map.Entry<String,File> entry : files2.entrySet()){
                SQLUtilities.importData(entry.getValue(),',',database,entry.getKey());
            }
            return  true;
        } catch (SQLException e) {
            logger.error(e.getMessage(),e);
            return false;
        }
    }

    public Boolean exportGTFSFileFromTable(Connection conn,String database,String tableName,File fileOutput){
        return SQLUtilities.exportData(conn,fileOutput.getAbsolutePath(),database+"."+tableName);
    }

    public Boolean exportGTFSZipFromDatabase(Connection conn,String database,File fileZipOutput){
        List<File> files = new ArrayList<>();
        try {
            List<String> tables = SQLUtilities.getTablesFromConnection(conn, database);
            File dir = FileUtilities.getParentFile(fileZipOutput);

            for(String table : tables){
                for (GTFSFileType2 ft : GTFSFileType2.values()) {
                    if(table.equals(ft.name())){
                            File file = new File(dir+File.separator+ft.name()+".txt");
                        if(exportGTFSFileFromTable(conn,database,table,file)){
                            files.add(file);
                            break;
                        }
                    }
                }
            }
            ZtZipUtilities.compressToZip(fileZipOutput,files);
            return true;
        }catch (SQLException e){
            logger.error(e.getMessage(),e);
            return false;
        }
    }

    public Boolean exportGTFSDatabaseToRDF(Connection conn,File fileRDFOutput){
        return exportGTFSDatabaseToRDF(conn,fileRDFOutput,"http://gtfstest/",Lang.TURTLE);
    }

    public Boolean exportGTFSDatabaseToRDF(Connection conn,File fileRDFOutput,Lang lang){
        return exportGTFSDatabaseToRDF(conn,fileRDFOutput,"http://gtfstest/",Lang.TURTLE);
    }

    public Boolean exportGTFSDatabaseToRDF(Connection conn,File fileRDFOutput,String baseUri,Lang lang){
        List<Statement> stmt = new ArrayList<>();
        try {
            String database = SQLUtilities.getDatabaseName(conn);
            List<String> tables = SQLUtilities.getTablesFromConnection(conn, database);
            File dir = FileUtilities.getParentFile(fileRDFOutput);
            for(String table : tables){
                for (GTFSFileType2 ft : GTFSFileType2.values()) {
                    //We use only the table match the name standard of the GTFS file....
                    if(table.equals(ft.name())){
                        File file = new File(dir+File.separator+ft.name()+".txt");
                        //We save the content of the tsble on a temporary csv file...
                        if(exportGTFSFileFromTable(conn,database,table,file)){
                            stmt.addAll(getRDFFromGTFSFile(file,baseUri));
                            FileUtilities.delete(file.toPath());
                            break;
                        }
                    }
                }
            }
            Model jModel = Jena2Utilities.createModel();
            jModel.setNsPrefixes(setPrefixes());
            jModel.add(stmt);
            Jena2Utilities.write(fileRDFOutput,jModel,lang);
            logger.info("Converted the database GTFS:"+conn.getMetaData().getURL()+ " to "+ stmt.size()+" Statement RDF" +
                    " on directory "+dir);
            return true;
        }catch (SQLException e){
            logger.error(e.getMessage(),e);
            return false;
        }
    }

    private static Map<String,File> getGTFSFilesFromZipFile(File zipFile){
        Map<String,File> map = new HashMap<>();
        //List<File> files = ArchiveUtilities.extractFilesFromZip(zipFile);
        List<File> files = ZtZipUtilities.extractFilesFromZip(zipFile);
        for (File file : files) {
            if (FileUtilities.isFileExists(file)) {
                //String nameTable = database+"."+FileUtilities.getFilenameWithoutExt(file);
                String nameTable = FileUtilities.getFilenameWithoutExt(file);
                String path = file.getAbsolutePath();
                path = path.replace("\\", "\\\\");
                map.put(nameTable, new File(path));
            }
        }
        return map;
    }

    public enum GTFSFileType2{agency,shapes,calendar,calendar_dates,routes,stops,trips,frequencies,stop_times}

    public static void main(String[] args) throws IOException, SQLException {
        LogBackUtil.console();
        //WORK
       /* File zip = new File(System.getProperty("user.dir")+File.separator+"" +
                "utility\\src\\main\\resources\\fileForTest\\ac-transit_20150218_1708.zip");*/
        File zip = new File("C:\\Users\\Utente\\Desktop\\ac-transit_20150218_1708.zip");
        //File zip2 = new File("C:\\Users\\Utente\\Desktop\\ac-transit_20150218_170822.zip");
        //NOT WORK
        //File zip = FileUtilities.getFromResourceAsFile("fileForTest/ac-transit_20150218_1708.zip",GTFSUtilities.class);

        //IMPORT TO DATABASE - EXPORT FROM DATABASE
        /* List<File> ss = ZtZipUtilities.extractFilesByPattern(zip,"agency");
         String srr = ss.get(0).getAbsolutePath();
         srr = FileUtilities.toString(ss.get(0));
        Connection conn = SQLUtilities.getMySqlConnection("localhost","3306","geodb","siimobility","siimobility",false,false,false);
        GTFSUtilities.getNewInstance().importGTFSZipToDatabase(zip,conn,"gtfs",true,true);*/

        //EXPORT SINGLE FILE FROM DATABASE
        //Connection conn = SQLUtilities.getMySqlConnection("localhost","3306","gtfs","siimobility","siimobility",false,false,false);
        //GTFSUtilities.getNewInstance().exportGTFSFileFromTable(conn,"gtfs","agency",new File("C:\\Users\\Utente\\Desktop\\agency.txt"));

        //List<String> list  = SQLUtilities.getTablesFromConnection(conn,"gtfs");
        /*for(String table : list){
            String directory = "C:\\Users\\Utente\\Desktop\\GTFS\\";
            GTFSUtilities.getNewInstance().exportGTFSFileFromTable(conn,"gtfs",table,
                    new File(directory+File.separator+table+".txt"));
        }*/
        //GTFSUtilities.getNewInstance().exportGTFSZipFromDatabase(conn,"gtfs",zip2);

        //CONVERT TO RDF
        //File output2 = new File("C:\\Users\\Utente\\Desktop\\ac-transit_20150218_1708.n3");
        //GTFSUtilities.getInstance().convertGTFSZipToRDF(zip,"http://baseuri#",output2,RDFFormat.TURTLE);

        //Convert database to RDF files
        File output2 = new File("C:\\Users\\Utente\\Desktop\\gtfsTest.ttl");
        Connection conn = SQLUtilities.getMySqlConnection("localhost","3306","gtfs","siimobility","siimobility",false,false,false);
        GTFSUtilities.getInstance().exportGTFSDatabaseToRDF(conn,output2);

    }






}
