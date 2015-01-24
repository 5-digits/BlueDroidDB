/**
 * 
 */
package blue.stack.bluedroiddb.cvTest;

/**
 * @author BunnyBlue
 *
 */
public class TimeCounter {
	static long start;
	static long end;
	public static long add = 0;

	public static void start() {
		start = System.currentTimeMillis();

	}

	public static void end() {
		end = System.currentTimeMillis();
		add = add + (end - start);

	}
}
