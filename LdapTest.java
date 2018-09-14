import java.io.StringReader;
import java.io.StringWriter;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPathExpression;
import org.xml.sax.InputSource;

import com.vitria.fc.directory.DirConfig;
import com.vitria.fc.directory.DirLib;
import com.vitria.xquery.XQueryLib;
import com.vitria.xquery.dom.DocumentImpl;
import com.vitria.xquery.dom.NodeImpl;

public class LdapTest {
	public static void main(String[] args) {
		boolean flag = parseArgument(args);
		if (flag) {
			try {
				if (args[0].equals("-print")) {
					System.out.println(getvtsecuritymodel());
				} else if (args[0].equals("-update-port")) {
					setvtsercuritmodel(args[1]);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static boolean parseArgument(String[] args) {
		if (args.length <= 0) {
			System.out.println("=============================================================================");
			System.out.println("========= java LdapTest <[-print | -update-port <port(number)>]> ===========");
			System.out.println("=============================================================================");
			return false;
		} else {
			if(!args[0].equals("-print") && !args[0].equals("-update-port")) {
				System.out.println("=============================================================================");
				System.out.println("========= java LdapTest <[-print | -update-port <port(number)>]> ===========");
				System.out.println("=============================================================================");
				return false;
			}
			
			if(args[0].equals("-update-port")) {
				try {
					Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					System.out.println("=============================================================================");
					System.out.println("========= java LdapTest <[-print | -update-port <port(number)>]> ===========");
					System.out.println("=============================================================================");
					return false;
				}
			}
			return true;
		}
	}
	
	private static Document strToXml(String str) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;;
		Document doc = null;
		try {
			builder = factory.newDocumentBuilder();
			doc = builder.parse(new InputSource(new StringReader(str)));
			doc.getDocumentElement().normalize();
			//blank text node remove
			XPathExpression xpath = XPathFactory.newInstance().newXPath().compile("//text()[normalize-space(.) = '']");
			NodeList blankTextNodes = (NodeList) xpath.evaluate(doc, XPathConstants.NODESET);
			for (int i = 0; i < blankTextNodes.getLength(); i++) {
			     blankTextNodes.item(i).getParentNode().removeChild(blankTextNodes.item(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return doc;
	}
	
	private static String xmlToStr(Document doc) throws Exception {
		String ret = null;
		try {
			StringWriter sw = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.transform(new DOMSource(doc), new StreamResult(sw));
			ret = sw.toString();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return ret;
	}
	
	//setvtsercuritmodel
	private static void setvtsercuritmodel(String port) throws Exception {
		Context ctx = null;
		DirConfig dirConfig = null;
		try {
			ctx = DirLib.createInitialContext();
			dirConfig = (DirConfig) ctx.lookup("CN=Config");
			
			String[] authPlugin = dirConfig.getAuthenticationPlugin();
			String[] authorityPlugin = dirConfig.getAuthorizationPlugin();
			
			String ldapUrl = getValueXmlByXPath(authPlugin[1], "config/ldap/url/text()");
			ldapUrl = ldapUrl.substring(0, ldapUrl.lastIndexOf(":") + 1);
			ldapUrl = ldapUrl + port;
			
			authPlugin[1] = setValueXMLByXPath(authPlugin[1], "config/ldap/url/text()", ldapUrl);
			authorityPlugin[1] = setValueXMLByXPath(authorityPlugin[1], "config/ldap/url/text()", ldapUrl);
			
			dirConfig.setAuthenticationPlugin(authPlugin[0], authPlugin[1]);
			dirConfig.setAuthorizationPlugin(authorityPlugin[0],authorityPlugin[1]);
			
			ctx.rebind("CN=Config", dirConfig);
			System.out.println("success set securitymodel");
			System.out.println("============================= vtsecuritymodel =============================");
			String ret = getvtsecuritymodel();
			System.out.println(ret);
		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			try {
				if(ctx != null) {
					ctx.close();
				}
			} catch (Exception e) {
				System.err.println("DirContext closing error");
				e.printStackTrace();
			}
		}
	} 
	
	private static String getValueXmlByXPath(String data, String path) throws Exception {
		String ret = null;
		try {
			DocumentImpl localDocumentImpl = XQueryLib.parseXML(data);
			NodeImpl[] arrayOfNodeImpl = XQueryLib.getFromXPath(localDocumentImpl, path);
			ret = arrayOfNodeImpl[0].toString();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return ret;
	}
	
	private static String setValueXMLByXPath(String data, String path, String value) throws Exception {
		String ret = null;
		try {
			DocumentImpl localDocumentImpl = XQueryLib.parseXML(data);
			XQueryLib.setUsingXPath(localDocumentImpl, path, value);
			ret = XQueryLib.unparseXML(localDocumentImpl);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return ret;
	} 
	
	private static String getvtsecuritymodel() throws Exception {
		String ret = null;
		Context ctx = null;
		DirConfig dirConfig = null;
		try {
			ctx = DirLib.createInitialContext();
			dirConfig = (DirConfig) ctx.lookup("CN=Config");
			Attributes attrs = dirConfig.getAllAttributes();
			NamingEnumeration<? extends Attribute> ne = attrs.getAll();
			
			while(ne.hasMore()) {
				Attribute attr = ne.next();
				if (attr.getID().equalsIgnoreCase("vtSecurityModel")) {
					ret = (String)attr.get();
					break;
				}
			}
		
			if (ret != null) {
				Document document = strToXml(ret);
				ret = xmlToStr(document);
			}
		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			try {
				if(ctx != null) {
					ctx.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

}
