/**
 *  Copyright (c) 2003,2004, Stefan Haustein, Oberhausen, Rhld., Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. 
 *
 * Contributor(s): John D. Beatty, Dave Dash, F. Hunter, Alexander Krebs, 
 *                 Lars Mehrmann, Sean McDaniel, Thomas Strang, Renaud Tognelli 
 * */
package org.ksoap2.transport;

import java.io.*;
import java.net.Proxy;

import org.ksoap2.*;
import org.xmlpull.v1.*;

import org.ksoap2.cookiemanagement.*;

/**
 * A J2SE based HttpTransport layer.
 */
public class HttpTransportSE extends Transport {

    private ServiceConnection connection;

    /**
     * Creates instance of HttpTransportSE with set url
     * 
     * @param url
     *            the destination to POST SOAP data
     */
    public HttpTransportSE(String url) {
        super(null, url);
    }
    
    /**
     * Creates instance of HttpTransportSE with set url and defines a
     * proxy server to use to access it
     * 
     * @param proxy
     * 				Proxy information or <code>null</code> for direct access
     * @param url
     * 				The destination to POST SOAP data
     */
    public HttpTransportSE(Proxy proxy, String url) {
    	super(proxy, url);
    }

    /**
     * set the desired soapAction header field
     * 
     * @param soapAction
     *            the desired soapAction
     * @param envelope
     *            the envelope containing the information for the soap call.
     * @throws MalformedCookieException 
     */
    public void call(String soapAction, SoapEnvelope envelope) throws IOException, XmlPullParserException {
    	
    	call(soapAction, envelope, null);
    }

	/**
	 * 
     * set the desired soapAction header field
     * 
     * @param soapAction
     *            	the desired soapAction
     * @param envelope
     *            	the envelope containing the information for the soap call.
     * @param cookieJar
     * 				           
     * @return <code>CookieJar</code> with any cookies sent by the server
	 * @throws MalformedCookieException Cookie is invalid
	 */
    public CookieJar call(String soapAction, SoapEnvelope envelope, CookieJar cookieJar) 
		throws IOException, XmlPullParserException {

		if (soapAction == null)
			soapAction = "\"\"";

		byte[] requestData = createRequestData(envelope);
	    
		requestDump = debug ? new String(requestData) : null;
	    responseDump = null;
	    
	    connection = getServiceConnection();
	    
	    connection.setRequestProperty("User-Agent", "kSOAP/2.0");
	    connection.setRequestProperty("SOAPAction", soapAction);
	    connection.setRequestProperty("Content-Type", "text/xml");
	    connection.setRequestProperty("Connection", "close");
	    connection.setRequestProperty("Content-Length", "" + requestData.length);
	    connection.setRequestMethod("POST");
	    
	    // Only send cookies if we appear to have some
	    if (cookieJar != null)
	    	connection.sendCookies(cookieJar);
	    
	    connection.connect();
    

	    OutputStream os = connection.openOutputStream();
   
	    os.write(requestData, 0, requestData.length);
	    os.flush();
	    os.close();
	    requestData = null;
	    InputStream is;
	    try {
	    	connection.connect();
	    	is = connection.openInputStream();
	    	
	    	if (cookieJar != null)
	    		connection.saveCookies(cookieJar);
	    	
	    } catch (IOException e) {
	    	is = connection.getErrorStream();

	    	if (is == null) {
	    		connection.disconnect();
	    		throw (e);
	    	}
	    }
    
		if (debug) {
	        ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        byte[] buf = new byte[256];
	        
	        while (true) {
	            int rd = is.read(buf, 0, 256);
	            if (rd == -1)
	                break;
	            bos.write(buf, 0, rd);
	        }
	        
	        bos.flush();
	        buf = bos.toByteArray();
	        responseDump = new String(buf);
	        is.close();
	        is = new ByteArrayInputStream(buf);
	    }
   
	    parseResponse(envelope, is);
	    return cookieJar;
	}

	public ServiceConnection getConnection() {
		return (ServiceConnectionSE) connection;
	}

    protected ServiceConnection getServiceConnection() throws IOException {
        return new ServiceConnectionSE(proxy, url);
    }
}