package fzj.ibg.sos.wps;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import fzj.ibg3.exceptions.IbgException;

public class PloneAuthentication {
	private static PloneAuthentication _this;
	private XmlRpcClient client;
	
	public static PloneAuthentication getInstance(String url) throws MalformedURLException{
		if(_this==null){
			_this=new PloneAuthentication(url);
		}
		return _this;
	}
	
	private PloneAuthentication(String url) throws MalformedURLException {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
	    config.setServerURL(new URL(url));
	    client = new XmlRpcClient();
	    client.setConfig(config);
	}
	
	public boolean verifyId(String cookie) 
		throws XmlRpcException{
		return true;
//		if(!cookie.equals("")){
//			return (Boolean) this.client.execute("verifyId",new Object[]{cookie});
//		}
//		return false;
	}
	
	public String getUserBySessionCookie(String cookie) throws XmlRpcException, IbgException{
//		return "sorg";
		if(!cookie.equals("")){
			return (String) this.client.execute("userFromSessionCookie",new Object[]{cookie});
		}
		//throw new IbgException("no (plone)session is active!");
		return "";
	}
	


}
