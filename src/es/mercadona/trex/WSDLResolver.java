package es.mercadona.trex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;

public class WSDLResolver {

	final static Logger logger = Logger.getLogger(WSDLResolver.class);
	
	public static String POM = null;
	
	public static String RUTA_DEST = ".";
	
	public static List<String[]> imports = new ArrayList<String[]>();
	
	public static void main(String[] args) {
		
		try {
			final String rutaTempBase = System.getProperty("java.io.tmpdir")+ "/saurio";
			// si no existe la ruta, la creo
			File rutaBase = new File(rutaTempBase);
			if (!rutaBase.exists()) {
				FileUtils.mkdir(rutaTempBase);
			}
			POM  = rutaTempBase + "/pom.xml";
			// si el pom no existe, lo creo
			final File rutaPom = new File(POM);
			if (!rutaPom.exists()) {
				rutaPom.createNewFile();
				// escribo contenido
				final FileWriter writer = new FileWriter(rutaPom);
				final StringBuilder pomStr = new StringBuilder();
				pomStr.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("\t<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
				.append("\txsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n")
				.append("\t<modelVersion>4.0.0</modelVersion>\n")
				.append("\t<groupId>es.mercadona.trex</groupId>\n")
				.append("\t<artifactId>WSDL-Saurio4000</artifactId>\n")
				.append("\t<packaging>jar</packaging>\n")
				.append("\t<version>0.0.1</version>\n</project>");
				writer.write(pomStr.toString());
				writer.close();
			}

			// pendiente obtener desde config
			final File config = new File("./config.properties");
			// si no existe, error
			if (!config.exists() || !config.isFile()) {
				debug("No existe el fichero de configuracion");
				return;
			}
			// paso a string
			final StringBuilder configStr = readFileAsString(config.getAbsolutePath());
			
			final Pattern patronRuta = Pattern.compile("RUTA_DES=(.+)");
			final Matcher matchRuta = patronRuta.matcher(configStr);
			// si no existe
			if (matchRuta.find()) {
				// obtengo valor
				RUTA_DEST = matchRuta.group(1).replace("\\", "/");
				// si no termina en \ lo agrego
				if (!"/".equals(RUTA_DEST.charAt(RUTA_DEST.length() -1))) {
					RUTA_DEST += "/";
				}
			} else {
				debug("No se ha encontrado la ruta destino, se utilizara la ruta por defecto");
			}
			// ruta dest
			debug("Ruta destino: " + RUTA_DEST);
			final File dest = new File(RUTA_DEST);
			
			// si no existe tiro error
			if (!dest.exists() || !dest.isDirectory()) {
				debug("Error: No existe la ruta destino");
				return;
			}
			// modo, false por defecto
			boolean actualizar = false;
			// obtengo el modo
			final Pattern modo = Pattern.compile("ACTUALIZAR=(.+)");
			final Matcher matchModo = modo.matcher(configStr);
			//si encuentr
			if (matchModo.find()) {
				// obtengo valor
				actualizar = Boolean.parseBoolean(matchModo.group(1));
			}
			
			// obtengo el pom
			final Pattern patronPom = Pattern.compile("# INICIO POM((?s).+)# FIN POM");
			final Matcher matchPom = patronPom.matcher(configStr);
			
			// obtener groupId
			String groupId = null; //"es.mercadona.contratos.wsdl";
			// obtener artifactId
			String artifactId = null; //"ED_InventarioDispositivo";
			// obtener version
			String version = null; //"1.0.4";
			// obtener classifier
			String classifier = null; //wsdl
			
			if (matchPom.find()) {
				final String pom = matchPom.group(0);
				// obtengo el groupId
				final Pattern groupIdPatr = Pattern.compile(
						"# INICIO POM(?s).+<groupId>(.+)</groupId>.+" // grupo 1
					  + "<artifactId>(.+)</artifactId>.+" // grupo 2
					  + "<version>(.+)</version>" // grupo 3
					  + "(.+<classifier>(.+)</classifier>)?.+# FIN POM"); // grupo4.grupo1 - opcional
				final Matcher groupIdMatch = groupIdPatr.matcher(pom);
				// si hay match
				if (groupIdMatch.find()) {
					// obtengo el groupId
					groupId = groupIdMatch.group(1);
					// obtengo el artifactId
					artifactId = groupIdMatch.group(2);
					// obtengo el version
					version = groupIdMatch.group(3);
					// si existe el grupo 4, es porque hay classifier
					if (groupIdMatch.group(4) != null) {
						// obtengo el classifier
						classifier = groupIdMatch.group(5);
					}
					
				} else {
					// si no, error
					debug("El pom indicando no cumple el formato: " + pom);
					return;
				}
			} else {
				// si no, error
				debug("El fichero de configuracion no incluye ningun pom: " + configStr);
				return;
			}
			// contrato a descargar
			debug("groupId: " + groupId);
			debug("artifactId: " + artifactId);
			debug("version: " + version);
			debug("classifier: " + classifier);
			
			File[] dependenciesFiles = null;
			
			if (classifier != null) {
				// crear array con classifier
				final String[] dependenciesGAVs = { 
						groupId + ":" + artifactId + ":jar:" + classifier + ":" + version
				};
				// obtener desde maven
				dependenciesFiles = TestUtils.getMavenLibs(true, false, true, POM, dependenciesGAVs);
			} else {
				// crear array sin classifier
				final String[] dependenciesGAVs = { 
					groupId + ":" + artifactId + ":" + version
				};
				// obtener desde maven
				dependenciesFiles = TestUtils.getMavenLibs(true, false, true, POM, dependenciesGAVs);
			}
			// si se desea actualizar
			if (actualizar) {
				debug("Actualizar desde nexus = true, se procede a eliminar los contratos en local.");
				// elimino contratos y descargo de nuevo
				for (final File file : dependenciesFiles) {
					FileUtils.deleteDirectory(file.getParent());
				}
				if (classifier != null) {
					// crear array con classifier
					final String[] dependenciesGAVs = { 
							groupId + ":" + artifactId + ":jar:" + classifier + ":" + version
					};
					// obtener desde maven
					dependenciesFiles = TestUtils.getMavenLibs(true, false, true, POM, dependenciesGAVs);
				} else {
					// crear array sin classifier
					final String[] dependenciesGAVs = { 
						groupId + ":" + artifactId + ":" + version
					};
					// obtener desde maven
					dependenciesFiles = TestUtils.getMavenLibs(true, false, true, POM, dependenciesGAVs);
				}
			}
						
			if (logger.isDebugEnabled()) {
				for (final File file : dependenciesFiles) {
					logger.debug("Dependencia obtenidas desde nexus: " + file);
				}
			}
			// si no hay dependencias lanzo error
			if (dependenciesFiles == null || dependenciesFiles.length == 0) {
				debug("Error, no se han encontrado dependencias para el POM");
				return;
			}
			// mover cada uno de los jar a mi ruta para procesarlos
			final File tempDirectory = new File(rutaTempBase +  "/temp/");
			// si no existe, lo creo
			if (!tempDirectory.exists()) {
				FileUtils.mkdir(tempDirectory.getAbsolutePath());
			}
			FileUtils.cleanDirectory(tempDirectory);
			final File wsdlDirectory = new File(rutaTempBase + "/wsdl/");
			// si no existe, lo creo
			if (!wsdlDirectory.exists()) {
				FileUtils.mkdir(wsdlDirectory.getAbsolutePath());
			}
			FileUtils.cleanDirectory(wsdlDirectory);
			
			for (final File file : dependenciesFiles) {
	
				FileUtils.copyFileToDirectory(file, tempDirectory);			
			}
			// abrir el contrato
			File[] files = new File(rutaTempBase + "/temp/").listFiles();
			for (File file : files) {
				debug("Procesando fichero: " + file.getName());
		        if (!file.isDirectory() && file.getName().matches(".+\\.jar")) {
		        	descomprimir(file.getAbsolutePath(), rutaTempBase + "/wsdl/");
		        }
			}
			// limpio temporales
			FileUtils.cleanDirectory(tempDirectory);
			// recorro descomprimido
			files = new File(rutaTempBase + "/wsdl/").listFiles();
			
			copyTemps(files, tempDirectory);
			
			FileUtils.cleanDirectory(wsdlDirectory);
			// regex
			final Pattern patron = Pattern.compile("schemaLocation=\"(.+/)(.+\\.(xsd|wsdl))\"");
			
			// por cada
			files = new File(rutaTempBase + "/temp/").listFiles();
			for (final File file : files) {
				imports.clear();
				debug("Procesando imports de: " + file);
				final StringBuilder fichero = readFileAsString(file.getAbsolutePath());
				final Matcher matches = patron.matcher(fichero);
				//  por cada
				while (matches.find()) {
					debug("\tmatch: " + matches.group());
					debug("\truta: " + matches.group(1));
					debug("\tfile: " + matches.group(2));
					// reemplazo el grupo 1 con nada para que 
					// los imports sean en la misma ruta siempre
					final String[] sinRuta = {matches.group(), 
							matches.group().replace(matches.group(1), "")};
					
					debug("\tSin ruta: " + sinRuta[1]);
					
					imports.add(sinRuta);
				}
				// string del fichero
				String ficheroStr = fichero.toString();
				// por cada
				for (final String[] replace : imports) {
					ficheroStr = ficheroStr.replace(replace[0], replace[1]);
				}
				// escribo el contenido
				final FileWriter writer = new FileWriter(file);
				writer.write(ficheroStr);
				writer.close();
			}
			dest.setExecutable(true);
			dest.setWritable(true);
			dest.setReadable(true);
			// muevo finalmente a la ruta final
			for (final File file : files) {
				FileUtils.copyFileToDirectory(file, dest);
			}
			debug("Ficheros guardados en la ruta: " + dest);
			// limpio temp
			FileUtils.cleanDirectory(tempDirectory);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("Todo se derrumbo: ", e);
		} catch (Exception e) {
			logger.error("Todo se derrumbo: ", e);
		}
	}
	
	public static void copyTemps(final File[] files, final File tempDirectory) throws IOException {
		for (File file : files) {
			if (file.isDirectory()) {
				copyTemps(file.listFiles(), tempDirectory);
			}
			// si es un .wsdl o .xsd
			if (file.isFile() && (file.getName().matches(".+\\.wsdl")
					|| file.getName().matches(".+\\.xsd"))) {
				// lo copio
				debug("Copiando fichero:" + file.getName());
				FileUtils.copyFileToDirectory(file, tempDirectory);
			} 
		}
	}
	
	public static void descomprimir(String jarFile, String destDir) throws IOException {
		
		final java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile);
		final java.util.Enumeration enumEntries = jar.entries();
		
		while (enumEntries.hasMoreElements()) {
		    java.util.jar.JarEntry file = (java.util.jar.JarEntry) enumEntries.nextElement();
		    java.io.File f = new java.io.File(destDir + java.io.File.separator + file.getName());
		    if (file.isDirectory()) { // if its a directory, create it
		        f.mkdir();
		        continue;
		    }
		    java.io.InputStream is = jar.getInputStream(file); // get the input stream
		    java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
		    while (is.available() > 0) {  // write contents of 'is' to 'fos'
		        fos.write(is.read());
		    }
		    fos.close();
		    is.close();
		}
	}

	public static void debug(String mensaje) {
		if (logger.isDebugEnabled()) {
			logger.debug(mensaje);
		}
	}
	
	/**
	 * Lee el texto de un archivo.
	 * @param filePath ruta del fichero
	 * @return string con el texto del archivo
	 * @throws IOException error de IO
	 */
	public static StringBuilder readFileAsString(String filePath) throws IOException {
		final StringBuilder fileData = new StringBuilder();
		final BufferedReader reader = new BufferedReader(new FileReader(filePath));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
		}
		reader.close();
		return fileData;
	}
}
