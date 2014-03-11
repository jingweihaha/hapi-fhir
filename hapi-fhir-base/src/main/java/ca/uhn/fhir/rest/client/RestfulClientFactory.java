package ca.uhn.fhir.rest.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.rest.client.api.IRestfulClient;
import ca.uhn.fhir.rest.common.BaseMethodBinding;

public class RestfulClientFactory implements IRestfulClientFactory {

	private FhirContext myContext;
	private HttpClient myHttpClient;

	/**
	 * Constructor
	 * 
	 * @param theContext
	 *            The context
	 */
	public RestfulClientFactory(FhirContext theContext) {
		myContext = theContext;
	}

	@SuppressWarnings("unchecked")
	private <T extends IRestfulClient> T instantiateProxy(Class<T> theClientType, InvocationHandler theInvocationHandler) {
		T proxy = (T) Proxy.newProxyInstance(RestfulClientFactory.class.getClassLoader(), new Class[] { theClientType }, theInvocationHandler);
		return proxy;
	}

	/**
	 * Instantiates a new client instance
	 * 
	 * @param theClientType
	 *            The client type, which is an interface type to be instantiated
	 * @param theServerBase
	 *            The URL of the base for the restful FHIR server to connect to
	 * @return A newly created client
	 * @throws ConfigurationException
	 *             If the interface type is not an interface
	 */
	@Override
	public <T extends IRestfulClient> T newClient(Class<T> theClientType, String theServerBase){
		if (!theClientType.isInterface()) {
			throw new ConfigurationException(theClientType.getCanonicalName() + " is not an interface");
		}

		HttpClient client;
		if (myHttpClient != null) {
			client = myHttpClient;
		}else {
			PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(5000, TimeUnit.MILLISECONDS);
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setConnectionManager(connectionManager);
			client = builder.build();
		}
		
		String serverBase = theServerBase;
		if (!serverBase.endsWith("/")) {
			serverBase = serverBase + "/";
		}
		
		ClientInvocationHandler theInvocationHandler = new ClientInvocationHandler(client, myContext, serverBase);

		for (Method nextMethod : theClientType.getMethods()) {			
			BaseMethodBinding binding = BaseMethodBinding.bindMethod(null, nextMethod);
			theInvocationHandler.addBinding(nextMethod, binding);
		}

		T proxy = instantiateProxy(theClientType, theInvocationHandler);

		return proxy;
	}

	
	/**
	 * Sets the Apache HTTP client instance to be used by any new restful clients created by
	 * this factory. If set to <code>null</code>, which is the default, a new HTTP client with 
	 * default settings will be created.
	 *  
	 * @param theHttpClient An HTTP client instance to use, or <code>null</code>
	 */
	@Override
	public void setHttpClient(HttpClient theHttpClient) {
		myHttpClient = theHttpClient;
	}

}
