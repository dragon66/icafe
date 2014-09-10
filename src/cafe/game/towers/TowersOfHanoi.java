package cafe.game.towers;

/** Towers of Hanoi problem solver.
 * @author Lars Vogel
 */
public class TowersOfHanoi {
	private static int no = 0;
	
	public static void move(int n, int startPole, int endPole) {
		if (n== 0){
			return; 
		}
		int intermediatePole = 6 - startPole - endPole;
		move(n-1, startPole, intermediatePole);
		System.out.println("Move " +n + " from " + startPole + " to " +endPole);
		no++;
		System.out.println("number of moves: " + no);
		move(n-1, intermediatePole, endPole);
	}
  
	public static void main(String[] args) {
		move(5, 1, 3);
	}  
} 
