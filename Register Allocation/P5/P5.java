import syntaxtree.*;
import visitor.*;

public class P5 {
	public static void main(String[] args) {
		try {
			Node root = new microIRParser(System.in).Goal();

			Object table = root.accept(new MapLabelsToLine(), null);
			Object alloc = root.accept(new GJDepthFirst(), table); // Your assignment part is invoked here.
			root.accept(new GenerateCode(), alloc);
			// root.accept(new GJDepthFirstPass2(), table);
		} catch (ParseException e) {
			System.out.println(e.toString());
		}
	}
}
