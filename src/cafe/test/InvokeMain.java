package cafe.test;

public class InvokeMain {
    public static void main(String... args) {
		try {
		   cafe.util.LangUtils.invokeMain(args);
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
    }
}