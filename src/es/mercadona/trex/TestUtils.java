package es.mercadona.trex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
//import org.jboss.shrinkwrap.api.Archive;
//import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenStrategyStage;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Metodos de utilidad para las clases de test.
 * 
 * @author Capgemini Arquitecture Team [jmartima]
 * 
 */
public class TestUtils {

	/**
	 * Obtiene un array de File con los archivos que son dependencias de maven.
	 * Obtiene tambien las dependencias transitivas.
	 * 
	 * @param mavenGA
	 *            Dependencias de maven de la forma <b>
	 *            <code>groupID:artifactId</code></b>
	 * @return Array de {@link File} con los archivos de las dependencias
	 * 
	 * @see #getMavenLibs(boolean, String...)
	 */
	public static File[] getMavenLibs(String rutaPom, String... mavenGA) {
		return getMavenLibs(true, rutaPom, mavenGA);
	}

	/**
	 * Obtiene un array de File con los archivos que son dependencias de maven.
	 * Si <code>withTransitivity</code> es <b><code>true</code></b>, tambien
	 * obtiene las dependencias transitivas.
	 * 
	 * @param withTransitivity
	 *            Indica si deseamos las dependencias transitivas.
	 * @param mavenGA
	 *            Dependencias de maven de la forma <b>
	 *            <code>groupID:artifactId</code></b>
	 * @return Array de {@link File} con los archivos de las dependencias
	 * 
	 * @author Capgemini Arquitecture Team [mighered]
	 * 
	 * @see #getMavenLibs(String...)
	 */
	public static File[] getMavenLibs(boolean withTransitivity, String rutaPom, String... mavenGAs) {
		return getMavenLibs(false, withTransitivity, rutaPom, mavenGAs);
	}
	
	/**
	 * Obtiene un array de File con los archivos que son dependencias de maven.
	 * Si <code>withTransitivity</code> es <b><code>true</code></b>, tambien
	 * obtiene las dependencias transitivas.
	 * 
	 * @param offline
	 *            Resolucion de dependencias offline/online 
	 * @param withTransitivity
	 *            Indica si deseamos las dependencias transitivas.
	 * @param mavenGA
	 *            Dependencias de maven de la forma <b>
	 *            <code>groupID:artifactId</code></b>
	 * @return Array de {@link File} con los archivos de las dependencias
	 * 
	 * @author Capgemini Arquitecture Team [mighered]
	 * 
	 * @see #getMavenLibs(String...)
	 */
	public static File[] getMavenLibs(boolean offline, boolean withTransitivity, String rutaPom, String... mavenGAs) {
		File[] dependencies;				
		
		MavenStrategyStage mss;
		
		if(offline) {
			mss = Maven.resolver().offline().loadPomFromFile(rutaPom).resolve(mavenGAs);
		} else {
			mss = Maven.resolver().loadPomFromFile(rutaPom).resolve(mavenGAs);
		}
		
		if (withTransitivity) {
			dependencies = mss.withTransitivity().asFile();
		} else {
			dependencies = mss.withoutTransitivity().asFile();
		}
		
		return dependencies;
	}
	
	/**
	 * Obtiene un array de File con los archivos que son dependencias de maven.
	 * Si <code>withTransitivity</code> es <b><code>true</code></b>, tambien
	 * obtiene las dependencias transitivas.
	 * 
	 * @param verbose
	 *            Informacion del proceso por System.out
	 * @param offline
	 *            Resolucion de dependencias offline/online            
	 * @param withTransitivity
	 *            Indica si deseamos las dependencias transitivas.
	 * @param mavenGA
	 *            Dependencias de maven de la forma <b>
	 *            <code>groupID:artifactId</code></b>
	 * @return Array de {@link File} con los archivos de las dependencias
	 * 
	 * @see #getMavenLibs(String...)
	 */
	public static File[] getMavenLibs(boolean verbose, boolean offline, boolean withTransitivity, String rutaPom, String... mavenGAs) {
		File[] dependencies;
		String mavenGAsString = "";
		
		if(verbose) {
			mavenGAsString = ReflectionToStringBuilder.toString(mavenGAs, ToStringStyle.SIMPLE_STYLE);
			System.out.println("Resolving runtime dependencies" + (offline? " offline" : "") + (withTransitivity? " with transitivity" : "") + " for " + mavenGAsString + "...");
		}
		
		try {						
			dependencies = getMavenLibs(offline, withTransitivity, rutaPom, mavenGAs);
			
			if(verbose) {
				String dependenciesString = ReflectionToStringBuilder.toString(dependencies, ToStringStyle.SIMPLE_STYLE).replace(",", ",\n");			
				System.out.println("Runtime dependencies resolved for " + mavenGAsString + ":\n" + dependenciesString);
			}
			
		} catch(Throwable t) {
			if(verbose) {
				System.err.print("Error on " + mavenGAsString + " dependency resolution!");
				t.printStackTrace(System.err);
			}
			throw t;
		}
		return dependencies;
	}
	
	
	/**
	 *  Añade los recursos indicados en <code>resources</code> a un <code>WebArchive</code> 
	 * 
	 * @param webArchive
	 *           Shrinkwrap WebArchive al que serán añadidos los recursos.
	 * @param resources
	 *           Array de String representando la lista de Resources
	 * @param ignoreNotFound
	 *           Marca si los recursos no encontrados deben ignorarse. Si es false, se lanzará excepcion
	 *           cuando uno de los recursos indicados en <code>resource</code> no haya sido encontrado.
	 *           
	 * @return <code>WebArchive</code>
	 */
//	public static WebArchive addResourcesToWebArchive(WebArchive webArchive, String[] resources, Boolean ignoreNotFound) {
//		WebArchive result = webArchive;
//		for (String resourceFilename : resources) {
//			if(resourceFilename != null) {
//				try {
//					result.addAsResource(resourceFilename);
//				} catch(IllegalArgumentException e) {
//					if(!ignoreNotFound) throw e;
//				}
//			}
//		}		
//		return webArchive;
//	}

	/**
	 * Dado un <code>InputStream</code> no nulo, lee todo el contenido y
	 * devuelve el contenido como un <code>String</code>.
	 * 
	 * @param in
	 *            InputStream a leer
	 * @return Cadena de caracteres con el contenido del
	 *         <code>InputStream</code>
	 * @throws IOException
	 *             si se produce un error al cerrar el <code>InputStream</code>
	 */
	public static String readAllAndClose(InputStream in) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int read;

		try {
			while ((read = in.read()) != -1) {
				out.write(read);
			}

		} finally {
			try {
				in.close();

			} catch (IOException e) {
				// do nothing
			}
		}

		return out.toString();
	}

	/**
	 * TODO: Add method description...
	 * 
	 */
	public static void pause(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			// do nothing
		}
	}

	/**
	 * Cambia la configuracion del log programáticamente, utilizando un nuevo
	 * fichero de configuracion, que debe ser un recurso del classpath.
	 * 
	 * @param resource
	 *            Nombre del recurso que debe ser un archivo válido de
	 *            configuracion de <i>logback</i>.
	 */
	public static void setLogConfig(String resource) {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			context.reset();
			configurator.doConfigure(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource));

		} catch (JoranException je) {
			// StatusPrinter will handle this
		}

		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}

	/**
	 * Visualizamos en tiempo de test el contenido del <i>deployment</i>
	 * generado.
	 * 
	 * @param archive
	 *            Nombre del archivo de configuracion del servidor, caso de
	 *            necesitarlo.
	 */
//	public static void printDeployment(Archive<?> archive) {
//		print("\n-- TEST DEPLOYMENT -----------------------------");
//		print(archive.toString(true));
//		print("------------------------------------------------\n");
//	}

	/**
	 * Muestra por consola todas las propiedades del sistema.
	 * 
	 */
	public static void printSystemProperties() {
		printSystemProperties(".*");
	}

	/**
	 * Muestra por consola las propiedades del sistema cuyas claves cumplen la
	 * expresion regular.
	 * 
	 * @param regex
	 *            Expresion regular que filtra las claves de las propiedades.
	 */
	public static void printSystemProperties(String regex) {
		printSystemProperties(regex, true);
	}

	/**
	 * Muestra por consola las propiedades del sistema cuyas claves cumplen la
	 * expresion regular si el valor del segundo parámetro es cierto. Muestra
	 * las que no la cumplen si es falso.
	 * 
	 * @param regex
	 *            Expresion regular que filtra las claves de las propiedades.
	 * @param include
	 *            si es <code>true</code>, muestra las que cumplen. Si es
	 *            <code>false</code>, muestra las que no cumplen.
	 */
	public static void printSystemProperties(String regex, boolean include) {
		Enumeration<?> propertyNames = System.getProperties().propertyNames();

		print("\n+ SYSTEM PROPERTIES ----------------------------");

		Set<String> sortedSet = new TreeSet<>();

		while (propertyNames.hasMoreElements()) {
			String propertyName = (String) propertyNames.nextElement();
			boolean match = propertyName.matches(regex);

			if (match && include || !match && !include) {
				sortedSet.add(propertyName);
			}
		}

		for (String propertyName : sortedSet) {
			String propertyValue = System.getProperty(propertyName);
			print("| %s: %s", propertyName, propertyValue);
		}

		print("+-----------------------------------------------\n");
	}

	/**
	 * Mensaje con parámetros.
	 * 
	 * @param format
	 *            Formato del mensaje
	 * @param params
	 *            Parámetros del mensaje.
	 * 
	 * @see String#format(String, Object...)
	 */
	public static void print(String format, Object... params) {
		if (params != null && params.length > 0) {
			System.out.println(String.format(format, params));

		} else {
			System.out.println(format);
		}
	}

	/**
	 * Escribe en la consola.
	 * 
	 * @param msg
	 *            Mensaje a escribir.
	 */
	public static void print(Object msg) {
		System.out.println(msg);
	}

	public static String getBeansXML(String content) {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><beans xmlns=\"http://java.sun.com/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/beans_1_0.xsd\">"
				+ content + "</beans>";
	}

	public static <T> String createInterceptors(Class<?>... interceptors) {
		if (interceptors == null || interceptors.length == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("<interceptors><class>");

		for (Class<?> interceptor : interceptors) {
			if (interceptor.getDeclaringClass() != null) {
				sb.append(interceptor.getDeclaringClass().getName());
				
			} else {
				sb.append(interceptor.getName());
			}
		}

		return sb.append("</class></interceptors>").toString();
	}

}
