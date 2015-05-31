package cafe.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TestBase {
	// Obtain a logger instance
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	public abstract void test(String ... args) throws Exception;
}
