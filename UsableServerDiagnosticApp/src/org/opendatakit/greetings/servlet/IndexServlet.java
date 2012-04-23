package org.opendatakit.greetings.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Servlet implementation class IndexServlet
 */
public class IndexServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
    public IndexServlet() {
    }

    private String formatPathProperty(String name) {
    	StringBuilder b = new StringBuilder();
    	b.append("<tr><td>").append(name).append("</td><td>");
    	String pathListProperty = System.getProperty(name);
    	if ( pathListProperty != null ) {
        	String[] values = pathListProperty.split(System.getProperty("path.separator"));
        	for ( String s : values ) {
        		b.append( StringEscapeUtils.escapeHtml(s) ).append("<br>");
        	}
    	}
    	b.append("</td></tr>");
    	return b.toString();
    }
    
    class LoaderWrapper extends ClassLoader {
    	LoaderWrapper(ClassLoader parent) {
    		super(parent);
    	}
    	
    	Package[] allPackages() {
    		return getPackages();
    	}
    }
    
    private String classDetails() {
    	// ClassLoader loader = this.getClass().getClassLoader();
    	// LoaderWrapper wrp = new LoaderWrapper(loader);
    	// Package[] pkgs = wrp.allPackages();
    	Package[] pkgs = Package.getPackages();
    	List<Package> porder = Arrays.asList(pkgs);
    	Collections.sort(porder, new Comparator<Package>() {

			@Override
			public int compare(Package arg0, Package arg1) {
				return arg0.getName().compareTo(arg1.getName());
			}});
    	
    	StringBuilder b = new StringBuilder();
    	b.append("<tr><td>Packages</td><td>");
    	for ( Package p : porder ) {
    		b.append(StringEscapeUtils.escapeHtml(p.getName()))
    		.append(" [").append(p.getSpecificationVersion())
    		.append(", ").append(p.getImplementationVersion())
    		.append("] impl(").append(p.getImplementationTitle())
    		.append(", ").append(p.getImplementationVendor())
    		.append(") spec(").append(p.getSpecificationTitle())
    		.append(", ").append(p.getSpecificationVendor())
    		.append(")<br>");
    	}
    	b.append("</td></tr>");
    	return b.toString();
    }
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html; charset=utf-8");
		response.setCharacterEncoding("UTF-8");
	    PrintWriter out = response.getWriter();
	    out.write("<!DOCTYPE html>\n<html><head></head><body>");
	    out.write("<p>Table of server info:</p><table>");
	    ServletContext ctxt = getServletContext();
	    out.write("<tr><td>ServletContext version</td><td>" + ctxt.getMajorVersion() + "." + ctxt.getMinorVersion() + "</td></tr>");
	    out.write("<tr><td>ServerInfo</td><td>" + ctxt.getServerInfo() + "</td></tr>");
	    
//	    out.write("<tr><td>ServletContext effective version</td><td>" + ctxt.getEffectiveMajorVersion() + "." + ctxt.getEffectiveMinorVersion() + "</td></tr>");
	    out.write("<tr><td>ServletInfo</td><td>" + getServletInfo() + "</td></tr>");
	    ServletConfig cfg = getServletConfig();
	    out.write("<tr><td>ServletName</td><td>" + cfg.getServletName() + "</td></tr>");
	    Enumeration<String> names = cfg.getInitParameterNames();
	    while ( names.hasMoreElements() ) {
	    	String name = names.nextElement();
	    	String value = cfg.getInitParameter(name);
		    out.write("<tr><td>" + name + "</td><td>" + value + "</td></tr>");
	    }
	    out.write("</table>");
	    out.write("<p>Classpath info:</p><table bordercolor=\"black\" border=\"2\">");
	    out.write(formatPathProperty("java.class.path"));
	    out.write(formatPathProperty("java.ext.dirs"));
	    out.write(formatPathProperty("java.library.path"));
	    out.write(classDetails());
	    out.write("</table></body></html>");
	    response.setStatus(HttpServletResponse.SC_ACCEPTED);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

}
