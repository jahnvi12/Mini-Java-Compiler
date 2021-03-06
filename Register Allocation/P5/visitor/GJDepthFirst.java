//
// Generated by JTB 1.3.2
//

package visitor;

import syntaxtree.*;
import java.util.*;

/**
 * Provides default methods which visit each node in the tree in depth-first
 * order. Your visitors may extend this class.
 */
public class GJDepthFirst<R, A> implements GJVisitor<R, A> {
	//
	// Auto class visitors--probably don't need to be overridden.
	//
	HashMap<String, Integer> labelToLineNo = new HashMap<String, Integer>(); // label -> lineNo for successor
	int stmtNo = 0, maxArgs = -1, nParams = 0;
	int numS = 0, numT = 0, numSpill = 0;

	public class allocInfo {

		// fnName -> (Temp -> RegAlloc)
		HashMap<String, HashMap<Integer, regAlloc>> funcRegAlloc;

		// fnName -> MaxArgsCalled
		HashMap<String, Integer> maxArgsCalled;

		// fnName -> numSpilledReg
		HashMap<String, Integer> numTempsSpilled;
		
		HashMap<String, HashMap<Integer, Integer>> start;
	}

	public class regAlloc {
		boolean spilled;
		int stackIndex;
		String register;

		public regAlloc(int t) {
			spilled = true;
			stackIndex = t;
		}

		public regAlloc(String t) {
			spilled = false;
			register = t;
		}
	}

	public class liveRange {
		int tempNo;
		int start;
		int end;

		public liveRange(int t, int s, int e) {
			tempNo = t;
			start = s;
			end = e;
		}

	}

	public class StartComparator implements Comparator<liveRange> {
		@Override
		public int compare(liveRange o1, liveRange o2) {
			if (o1.start < o2.start)
				return -1;
			if (o1.start == o2.start) {
				if (o1.end < o2.end)
					return -1;
				if (o1.end == o2.end)
					return 0;
				return 1;
			}
			return 1;
		}
	}

	public class use_def {
		Set<Integer> use, def, succ;

		use_def() {
			use = new HashSet<Integer>();
			def = new HashSet<Integer>();
			succ = new HashSet<Integer>();
		}
	}
	
	HashMap<String, HashMap<Integer, Integer>> start  = new HashMap<String, HashMap<Integer, Integer>>();
	// fnName -> (Temp -> RegAlloc)
	HashMap<String, HashMap<Integer, regAlloc>> funcRegAlloc = new HashMap<String, HashMap<Integer, regAlloc>>();

	// fnWise
	HashMap<Integer, regAlloc> RA;

	// (LineNo -> UseDefInfo)
	HashMap<Integer, use_def> lineInfo = new HashMap<Integer, use_def>();

	// fnName -> MaxArgsCalled
	HashMap<String, Integer> maxArgsCalled = new HashMap<String, Integer>();

	// fnName -> maxTReg
	HashMap<String, Integer> maxTIndex = new HashMap<String, Integer>();

	// fnName -> maxSReg
	HashMap<String, Integer> maxSIndex = new HashMap<String, Integer>();

	// fnName -> numSpilledReg
	HashMap<String, Integer> numTempsSpilled = new HashMap<String, Integer>();

	void spillAtInterval(int i, List<liveRange> intervals, List<liveRange> active, List<String> freeReg) {

		liveRange spill = active.get(active.size() - 1);
		if (spill.end > intervals.get(i).end) {
			RA.put(intervals.get(i).tempNo, RA.get(spill.tempNo));
			RA.put(spill.tempNo, new regAlloc(numSpill++));
			active.remove(spill);
			int j;
			for (j = 0; j < active.size(); j++) {
				if (active.get(j).end > intervals.get(i).end)
					break;

			}
			active.add(j, intervals.get(i));
		} else {
			RA.put(intervals.get(i).tempNo, new regAlloc(numSpill++));
		}

	}

	void expireOldIntervals(int i, List<liveRange> intervals, List<liveRange> active, List<String> freeReg) {
		for (int iter = 0; iter < active.size(); iter++) {
			if (active.get(iter).end < intervals.get(i).start) {
				freeReg.add(0, RA.get(active.get(iter).tempNo).register);
				active.remove(iter--);
			}
		}
	}

	int linearScan(List<liveRange> intervals) {
		int spilledTemps = 0;
		List<String> freeReg = new ArrayList<String>();
		int R = 18 - numS;

		for (int i = numS; i <= 7; i++)
			freeReg.add("s" + i);
		for (int i = 0; i <= 9; i++)
			freeReg.add("t" + i);

		List<liveRange> active = new ArrayList<liveRange>();
		for (int i = 0; i < intervals.size(); i++) {
			expireOldIntervals(i, intervals, active, freeReg);
			if (active.size() == R) {
				spilledTemps++;
				spillAtInterval(i, intervals, active, freeReg);
			} else {
				RA.put(intervals.get(i).tempNo, new regAlloc(freeReg.get(0)));
				freeReg.remove(0);
				int j;
				for (j = 0; j < active.size(); j++) {
					if (active.get(j).end > intervals.get(i).end)
						break;

				}
				active.add(j, intervals.get(i));
			}

		}
		return spilledTemps;
	}

	public R visit(NodeList n, A argu) {
		R _ret = null;
		int _count = 0;
		for (Enumeration<Node> e = n.elements(); e.hasMoreElements();) {
			e.nextElement().accept(this, argu);
			_count++;
		}
		return _ret;
	}

	public R visit(NodeListOptional n, A argu) {
		if (n.present()) {
			R _ret = null;
			int _count = 0;
			LinkedList<R> list = new LinkedList<R>();

			for (Enumeration<Node> e = n.elements(); e.hasMoreElements();) {
				list.add(e.nextElement().accept(this, argu));
				_count++;
			}
			return (R) list;
		} else
			return null;
	}

	public R visit(NodeOptional n, A argu) {
		if (n.present()) {
			String label = (String) n.node.accept(this, argu);
			// labelToLineNo.put(label, stmtNo + 1);
			return (R) label;
		} else
			return null;
	}

	public R visit(NodeSequence n, A argu) {
		R _ret = null;
		int _count = 0;
		for (Enumeration<Node> e = n.elements(); e.hasMoreElements();) {
			e.nextElement().accept(this, argu);
			_count++;
		}
		return _ret;
	}

	public R visit(NodeToken n, A argu) {
		return (R) n.tokenImage;
	}

	void printSet(Set<Integer> t) {
		for (Integer i : t)
			System.out.print(i + " ");
	}

	void doRegAlloc(String fnName, int stmtStart, int stmtEnd) {

		HashMap<Integer, Set<Integer>> in = new HashMap<Integer, Set<Integer>>();
		HashMap<Integer, Set<Integer>> out = new HashMap<Integer, Set<Integer>>();

		// Initialize
		for (int counter = stmtStart; counter <= stmtEnd; counter++) {
			in.put(counter, new HashSet<Integer>());
			out.put(counter, new HashSet<Integer>());
		}

		Set<Integer> prevIn = new HashSet<Integer>();
		Set<Integer> prevOut = new HashSet<Integer>();
		// Loop1:
		while (true) {
			boolean done = true;

			// Loop2:
			for (int counter = stmtStart; counter <= stmtEnd; counter++) {
				prevIn = new HashSet<Integer>(in.get(counter));
				prevOut = new HashSet<Integer>(out.get(counter));

				Set<Integer> temp1 = new HashSet<Integer>();
				for (Integer s : lineInfo.get(counter).succ) {
					if (s != stmtEnd + 1)
						temp1.addAll(in.get(s));
				}
				out.put(counter, temp1);

				Set<Integer> temp2 = new HashSet<Integer>();
				temp2.addAll(out.get(counter)); // out[n]
				temp2.removeAll(lineInfo.get(counter).def); // out[n] - def[n]
				temp2.addAll(lineInfo.get(counter).use); // use[n] U (out[n] - def[n])
				in.put(counter, temp2);

				// System.out.print(yy+" ********* "+counter+" ***********\nin ");
				// printSet(in.get(counter));
				// System.out.print("\nout ");
				// printSet(out.get(counter));
				// System.out.println();
				done = (in.get(counter)).containsAll(prevIn) && prevIn.containsAll(in.get(counter))
						&& prevOut.containsAll(out.get(counter)) && (out.get(counter)).containsAll(prevOut) && done;
				// System.out.println((in.get(counter)).containsAll(prevIn));
				// System.out.println(prevIn.containsAll(in.get(counter)));
				// System.out.println(prevOut.contains(out.get(counter)));
				// System.out.println((out.get(counter)).containsAll(prevOut));

			}

			if (done)
				break;

		}

		RA = new HashMap<Integer, regAlloc>();
		HashMap<Integer, Integer> curStart = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> curEnd = new HashMap<Integer, Integer>();

		for (int counter = stmtStart; counter <= stmtEnd; counter++) {
			for (Integer s : lineInfo.get(counter).def) {
				if (out.get(counter).contains(s))
					if (curStart.containsKey(s))
						curEnd.put(s, counter);
					else {
						curStart.put(s, counter);
						curEnd.put(s, counter);
					}
			}
			for (Integer s : in.get(counter)) {
				if (out.get(counter).contains(s))
					curEnd.put(s, counter);
			}
		}

		numS = numT = numSpill = 0;

		for (Integer i = 4; i < nParams; i++) {
			RA.put(i, new regAlloc(numSpill++));
			curStart.remove(i);
			curEnd.remove(i);
		}
		start.put(fnName, curStart);

		List<liveRange> liveIntervals = new ArrayList<liveRange>();
		for (Integer s : curStart.keySet()) {
			liveIntervals.add(new liveRange(s, curStart.get(s), curEnd.get(s)));
		}

		Collections.sort(liveIntervals, new StartComparator());

		// for (int i = 0; i < liveIntervals.size(); i++)
		// System.out.println(
		// liveIntervals.get(i).tempNo + " " + liveIntervals.get(i).start + " " +
		// liveIntervals.get(i).end);

		int spilledTemps = linearScan(liveIntervals);

		numTempsSpilled.put(fnName, numSpill);
		funcRegAlloc.put(fnName, RA);
		// for (Integer s : RA.keySet()) {
		// if (RA.get(s).spilled)
		// System.out.println(s + " " + RA.get(s).stackIndex);
		// else
		// System.out.println(s + " " + RA.get(s).register);
		// }

	}

	//
	// User-generated visitor methods below
	//

	/**
	 * f0 -> "MAIN" f1 -> StmtList() f2 -> "END" f3 -> ( Procedure() )* f4 -> <EOF>
	 */
	public R visit(Goal n, A argu) {
		R _ret = null;
		labelToLineNo = (HashMap<String, Integer>) argu;
		maxArgs = -1;
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		n.f2.accept(this, argu);
		// for(Integer key: lineInfo.keySet()) {
		// System.out.println("<------------- "+key+" ---------------->");
		// System.out.print("def ");
		// printSet(lineInfo.get(key).def);
		// System.out.print("\nuse ");
		// printSet(lineInfo.get(key).use);
		// System.out.print("\nsucc ");
		// printSet(lineInfo.get(key).succ);
		// System.out.println();
		// }
		maxArgsCalled.put("MAIN", maxArgs);
		doRegAlloc("MAIN", 1, stmtNo);
		n.f3.accept(this, argu);
		n.f4.accept(this, argu);
		allocInfo t = new allocInfo();
		t.maxArgsCalled = maxArgsCalled;
		t.funcRegAlloc = funcRegAlloc;
		t.numTempsSpilled = numTempsSpilled;
		t.start = start;
		return (R) t;
	}

	/**
	 * f0 -> ( ( Label() )? Stmt() )*
	 */
	public R visit(StmtList n, A argu) {
		R _ret = null;
		n.f0.accept(this, argu);
		return _ret;
	}

	/**
	 * f0 -> Label() f1 -> "[" f2 -> IntegerLiteral() f3 -> "]" f4 -> StmtExp()
	 */
	public R visit(Procedure n, A argu) {
		stmtNo++;
		maxArgs = -1;
		R _ret = null;
		int begin = stmtNo;
		String fnName = (String) n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		nParams = Integer.parseInt(n.f2.accept(this, argu) + "");
		use_def t = new use_def();
		t.succ.add(stmtNo + 1);
		for (int i = 0; i < nParams; i++)
			t.def.add(i);
		lineInfo.put(stmtNo, t);
		n.f3.accept(this, argu);
		n.f4.accept(this, argu);
		int end = stmtNo;
		maxArgsCalled.put(fnName, maxArgs);
		doRegAlloc(fnName, begin, end);
		return _ret;
	}

	/**
	 * f0 -> NoOpStmt() | ErrorStmt() | CJumpStmt() | JumpStmt() | HStoreStmt() |
	 * HLoadStmt() | MoveStmt() | PrintStmt()
	 */
	public R visit(Stmt n, A argu) {
		stmtNo++;
		R _ret = null;
		n.f0.accept(this, argu);
		return _ret;
	}

	/**
	 * f0 -> "NOOP"
	 */
	public R visit(NoOpStmt n, A argu) {
		R _ret = null;
		n.f0.accept(this, argu);
		use_def t = new use_def();
		t.succ.add(stmtNo + 1);
		lineInfo.put(stmtNo, t);
		return _ret;
	}

	/**
	 * f0 -> "ERROR"
	 */
	public R visit(ErrorStmt n, A argu) {
		R _ret = null;
		n.f0.accept(this, argu);
		use_def t = new use_def();
		t.succ.add(stmtNo + 1);
		lineInfo.put(stmtNo, t);
		return _ret;
	}

	/**
	 * f0 -> "CJUMP" f1 -> Temp() f2 -> Label()
	 */
	public R visit(CJumpStmt n, A argu) {
		R _ret = null;
		n.f0.accept(this, argu);
		Integer temp = Integer.parseInt(n.f1.accept(this, argu) + "");
		String label = (String) n.f2.accept(this, argu);
		use_def t = new use_def();
		t.use.add(temp);
		t.succ.add(stmtNo + 1);
		t.succ.add(labelToLineNo.get(label));
		lineInfo.put(stmtNo, t);
		return _ret;
	}

	/**
	 * f0 -> "JUMP" f1 -> Label()
	 */
	public R visit(JumpStmt n, A argu) {
		R _ret = null;
		n.f0.accept(this, argu);
		String label = (String) n.f1.accept(this, argu);
		use_def t = new use_def();
		t.succ.add(labelToLineNo.get(label));
		lineInfo.put(stmtNo, t);
		return _ret;
	}

	/**
	 * f0 -> "HSTORE" f1 -> Temp() f2 -> IntegerLiteral() f3 -> Temp()
	 */
	public R visit(HStoreStmt n, A argu) {
		R _ret = null;
		n.f0.accept(this, argu);
		Integer temp1 = Integer.parseInt(n.f1.accept(this, argu) + "");
		n.f2.accept(this, argu);
		Integer temp2 = Integer.parseInt(n.f3.accept(this, argu) + "");
		use_def t = new use_def();
		t.succ.add(stmtNo + 1);
		t.use.add(temp1);
		t.use.add(temp2);
		lineInfo.put(stmtNo, t);
		return _ret;
	}

	/**
	 * f0 -> "HLOAD" f1 -> Temp() f2 -> Temp() f3 -> IntegerLiteral()
	 */
	public R visit(HLoadStmt n, A argu) {
		R _ret = null;
		n.f0.accept(this, argu);
		Integer temp1 = Integer.parseInt(n.f1.accept(this, argu) + "");
		Integer temp2 = Integer.parseInt(n.f2.accept(this, argu) + "");
		n.f3.accept(this, argu);
		use_def t = new use_def();
		t.succ.add(stmtNo + 1);
		t.def.add(temp1);
		t.use.add(temp2);
		lineInfo.put(stmtNo, t);
		return _ret;
	}

	/**
	 * f0 -> "MOVE" f1 -> Temp() f2 -> Exp()
	 */
	public R visit(MoveStmt n, A argu) {
		R _ret = null;
		n.f0.accept(this, argu);
		Integer temp1 = Integer.parseInt(n.f1.accept(this, argu) + "");
		use_def t = new use_def();
		t.succ.add(stmtNo + 1);
		t.def.add(temp1);
		lineInfo.put(stmtNo, t);
		n.f2.accept(this, argu);
		return _ret;
	}

	/**
	 * f0 -> "PRINT" f1 -> SimpleExp()
	 */
	public R visit(PrintStmt n, A argu) {
		R _ret = null;
		use_def t = new use_def();
		t.succ.add(stmtNo + 1);
		lineInfo.put(stmtNo, t);
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		return _ret;
	}

	/**
	 * f0 -> Call() | HAllocate() | BinOp() | SimpleExp()
	 */
	public R visit(Exp n, A argu) {
		R _ret = null;
		n.f0.accept(this, argu);
		return _ret;
	}

	/**
	 * f0 -> "BEGIN" f1 -> StmtList() f2 -> "RETURN" f3 -> SimpleExp() f4 -> "END"
	 */
	public R visit(StmtExp n, A argu) {
		R _ret = null;
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		n.f2.accept(this, argu);
		stmtNo++;
		use_def t = new use_def();
		t.succ.add(stmtNo + 1);
		lineInfo.put(stmtNo, t);
		n.f3.accept(this, argu);
		n.f4.accept(this, argu);
		return _ret;
	}

	/**
	 * f0 -> "CALL" f1 -> SimpleExp() f2 -> "(" f3 -> ( Temp() )* f4 -> ")"
	 */
	public R visit(Call n, A argu) {
		R _ret = null;
		use_def t = lineInfo.get(stmtNo);
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		n.f2.accept(this, argu);
		R list = n.f3.accept(this, argu);
		n.f4.accept(this, argu);
		if (list != null) {
			LinkedList<String> args = (LinkedList<String>) list;
			if (args.size() > maxArgs)
				maxArgs = args.size();
			for (int i = 0; i < args.size(); i++)
				t.use.add(Integer.parseInt(args.get(i)));
		}
		if (maxArgs == -1)
			maxArgs = 0;
		return _ret;
	}

	/**
	 * f0 -> "HALLOCATE" f1 -> SimpleExp()
	 */
	public R visit(HAllocate n, A argu) {
		R _ret = null;
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		return _ret;
	}

	/**
	 * f0 -> Operator() f1 -> Temp() f2 -> SimpleExp()
	 */
	public R visit(BinOp n, A argu) {
		R _ret = null;
		use_def t = lineInfo.get(stmtNo);
		n.f0.accept(this, argu);
		Integer temp = Integer.parseInt(n.f1.accept(this, argu) + "");
		t.use.add(temp);
		n.f2.accept(this, argu);
		return _ret;
	}

	/**
	 * f0 -> "LE" | "NE" | "PLUS" | "MINUS" | "TIMES" | "DIV"
	 */
	public R visit(Operator n, A argu) {
		R _ret = null;
		n.f0.accept(this, argu);
		return _ret;
	}

	/**
	 * f0 -> Temp() | IntegerLiteral() | Label()
	 */
	public R visit(SimpleExp n, A argu) {
		R _ret = null;
		_ret = n.f0.accept(this, argu);
		if (n.f0.which == 0) {
			use_def t = lineInfo.get(stmtNo);
			t.use.add(Integer.parseInt(_ret + ""));
		}
		return _ret;
	}

	/**
	 * f0 -> "TEMP" f1 -> IntegerLiteral()
	 */
	public R visit(Temp n, A argu) {
		R _ret = null;
		n.f0.accept(this, argu);
		_ret = n.f1.accept(this, argu);
		return _ret;
	}

	/**
	 * f0 -> <INTEGER_LITERAL>
	 */
	public R visit(IntegerLiteral n, A argu) {
		R _ret = null;
		_ret = n.f0.accept(this, argu);
		return _ret;
	}

	/**
	 * f0 -> <IDENTIFIER>
	 */
	public R visit(Label n, A argu) {
		R _ret = null;
		_ret = n.f0.accept(this, argu);
		return _ret;
	}

}
