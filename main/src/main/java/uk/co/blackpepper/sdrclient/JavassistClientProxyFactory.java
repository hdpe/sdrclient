package uk.co.blackpepper.sdrclient;

import java.lang.reflect.Method;
import java.net.URI;

import org.springframework.hateoas.Resource;
import org.springframework.web.client.RestTemplate;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;

class JavassistClientProxyFactory implements ClientProxyFactory {

	private static final class GetterMethodFilter implements MethodFilter {
		@Override
		public boolean isHandled(Method m) {
			return m.getName().startsWith("get") && !"getId".equals(m.getName());
		}
	}
	
	private static final MethodFilter FILTER_INSTANCE = new GetterMethodFilter();

	@Override
	public <T> T create(URI uri, Class<T> entityType, RestTemplate restTemplate) {
		return createProxyInstance(entityType, new GetterMethodHandler<T>(uri, entityType, restTemplate, this));
	}

	@Override
	public <T> T create(Resource<T> resource, Class<T> entityType, RestTemplate  restTemplate) {
		return createProxyInstance(entityType, new GetterMethodHandler<T>(resource, entityType, restTemplate, this));
	}

	private static <T> T createProxyInstance(Class<T> entityType, MethodHandler methodHandler) {
		ProxyFactory factory = new ProxyFactory();
		factory.setSuperclass(entityType);
		factory.setFilter(FILTER_INSTANCE);
		
		Class<?> clazz = factory.createClass();
		T proxy = instantiateClass(clazz);
		((Proxy) proxy).setHandler(methodHandler);
		return proxy;
	}

	private static <T> T instantiateClass(Class<?> clazz) {
		try {
			@SuppressWarnings("unchecked")
			T proxy = (T) clazz.newInstance();
			return proxy;
		}
		catch (Exception exception) {
			throw new ClientProxyException("couldn't create proxy instance of " + clazz, exception);
		}
	}
}
