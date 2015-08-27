package fzj.ibg.sos.wps;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.persistence.Id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
	protected static HibernateUtil utilInstance = null;
	protected static int batchSize = 20;
	private static SessionFactory sessionFactory = null;
	private static final Logger LOG = LoggerFactory.getLogger(HibernateUtil.class);
	private static ArrayList<Session> activeSessions = new ArrayList<Session>();

	public static HibernateUtil getInstance() {
		if (utilInstance == null) {
			utilInstance = new HibernateUtil();
		}
		return utilInstance;
	}

	private HibernateUtil() {
		try {
			sessionFactory = new AnnotationConfiguration().configure("/hibernate.tereno22.xml").buildSessionFactory();
		} catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			LOG.error("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public Object loadObject(Object object, Long primaryKey) {
		Session s = sessionFactory.openSession();
		Transaction tx = null;
		Object obj = null;
		try {
			tx = s.beginTransaction();
			obj = s.get(object.getClass(), primaryKey);
			activeSessions.add(s);
			s.getTransaction().commit();
		} catch (HibernateException e) {
			if (tx != null)
				tx.rollback();
			e.printStackTrace();
		}
		return obj;
	}

	private static Object getPrimaryKeyValue(Object object) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Class clazz = object.getClass();
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			if (method.getAnnotation(Id.class) != null) {
				return method.invoke(object);

			}
		}
		throw new IllegalArgumentException("object has no primary key:\n" + object.toString());
	}

	
}
