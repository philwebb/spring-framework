
package org.springframework.log;

public interface Log extends org.apache.commons.logging.Log {

	void tracef(String format, Object arg1);

	void tracef(String format, Object arg1, Object arg2);

	void tracef(String format, Object arg1, Object arg2, Object... arguments);

	void debugf(String format, Object arg);

	void debugf(String format, Object arg1, Object arg2);

	void debugf(String format, Object arg1, Object arg2, Object... arguments);

	void infof(String format, Object arg);

	void infof(String format, Object arg1, Object arg2);

	void infof(String format, Object arg1, Object arg2, Object... arguments);

	void warnf(String format, Object arg);

	void warnf(String format, Object arg1, Object arg2);

	void warnf(String format, Object arg1, Object arg2, Object... arguments);

	void errorf(String format, Object arg);

	void errorf(String format, Object arg1, Object arg2);

	void errorf(String format, Object arg1, Object arg2, Object... arguments);

	void fatalf(String format, Object arg);

	void fatalf(String format, Object arg1, Object arg2);

	void fatalf(String format, Object arg1, Object arg2, Object... arguments);

	Log withCategory(Class<?> categoryClass);

	Log withCategory(Class<?> categoryClass, Class<?>... categoryClasses);
}
