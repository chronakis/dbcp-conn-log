package net.chronakis.tomcat;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class DBCPConnLogger {
	
	/**
	 * Turn this off!
	 */
	public static final boolean OFF = 
			Boolean.parseBoolean(System.getenv().getOrDefault("DBCPLOG_OFF", "false"));
	
	/**
	 * The first trace elements are:
	 * 1. The getStackTrace() call itself
	 * 2. The Aspect method
	 * 3. The actual method we are wrapping.
	 * Better skip those
	 */
	private static final int SKIP_FIRST =
			Integer.parseInt(System.getenv().getOrDefault("DBCPLOG_SKIP_FIRST", "3"));
	
	/**
	 * How many callers to trace back.
	 * This limits the stack traces to a more readable format
	 * and also prevents all the container methods to be printed.
	 * We will rarely need more than 5 steps to identify the culprit,
	 * but if you need, change it.
	 * 
	 * More elegant method would be to pass a system property
	 * with a list of packages you want to be included in the trace
	 */
	private static final int MAX_TRACE =
			Integer.parseInt(System.getenv().getOrDefault("DBCPLOG_MAX_TRACE", "5"));;
	
	
	/**
	 * Do not use package names, we rarely use the same names
	 */
	public static final boolean NO_PACKAGE_NAMES = 
			Boolean.parseBoolean(System.getenv().getOrDefault("DBCPLOG_NO_PACKAGE_NAMES", "true"));

	
	/**
	 * Exclude calls within the package from the trace
	 */
	private static final boolean EXC_DBCP_PACKAGE =
			Boolean.parseBoolean(System.getenv().getOrDefault("DBCPLOG_EXC_DBCP_PACKAGE", "true"));
	
	/**
	 * The apache tomcat dbcp package
	 */
	private static final String DBCP_PACKAGE = "org.apache.tomcat.dbcp.dbcp2";
	

	/**
	 * Wrap around the getConnection and print the trace in a compact format
	 */
	@Around("execution(* org.apache.tomcat.dbcp.dbcp2.PoolingDataSource.getConnection(..))")
	public Object logGetConnection(ProceedingJoinPoint invocation) throws Throwable {
		Object con = invocation.proceed();
		if (!OFF) {
			System.out.println("--- getConnection("
					+ Integer.toHexString(con.hashCode()) + "): "
					+ oneLineTrace(Thread.currentThread().getStackTrace()));
		}
		return con;
	}
	
	/**
	 * Wrap around the connection close
	 */
	@Around("execution(* org.apache.tomcat.dbcp.dbcp2.PoolingDataSource.PoolGuardConnectionWrapper.close(..))")
	public Object logCloseConnection(ProceedingJoinPoint invocation) throws Throwable {
		Object con = invocation.getTarget();
		if (!OFF) {
			System.out.println("--- retConnection("
					+ Integer.toHexString(con.hashCode()) + "): "
					+ oneLineTrace(Thread.currentThread().getStackTrace()));
		}
		return invocation.proceed();
	}

	
	/**
	 * Brief, one line stack traces. Feel free to change it to anyway you like
	 */
	public static String oneLineTrace(StackTraceElement[] trace) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (int i = SKIP_FIRST, max = MAX_TRACE + SKIP_FIRST ; i < trace.length && i < max - 1 ; i++) {
			StackTraceElement elm = trace[i];
			String className = elm.getClassName();
			if (EXC_DBCP_PACKAGE && className.startsWith(DBCP_PACKAGE))
				continue;
			if (NO_PACKAGE_NAMES)
				className = className.substring(className.lastIndexOf(".") + 1, className.length());
			
			if (first)
				first = false;
			else
				sb.append(" > ");
			
			sb.append(className)
			  .append(".")
			  .append(elm.getMethodName())
			  .append("(")
			  .append(elm.getLineNumber())
			  .append(")");
			
		}
		return sb.toString();
	}
	
}
