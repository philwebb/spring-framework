
package org.springframework.log;

public abstract class LogFactory {

	public static Log getLog(Class<?> clazz) {
		return LogWrapper.get(clazz);
	}

	public static Log getLog(String name) {
		return LogWrapper.get(name);

		// org.springframework.log.categories=true

		// org.springframework.jdbc.log.SQL=
		//   org.springframework.jdbc.core.JdbcTemplate.DEBUG->INFO,
		//   org.springframework.jdbc.core.StatementCreatorUtils.TRACE->INFO
	}

	//FIXME how to configure log categories.  Should be opt-in and off by default
}
