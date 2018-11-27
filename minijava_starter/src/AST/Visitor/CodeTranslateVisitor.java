package AST.Visitor;

import java.util.HashMap;
import java.util.Iterator;

import AST.*;
import Symtab.*;
import sun.text.normalizer.SymbolTable;

public class CodeTranslateVisitor implements Visitor {

	SymbolTable st = null;
	private HashMap<VarSymbol, Integer> stable = new HashMap<VarSymbol, Integer>();
	public int errors = 0;
	int tabs = 0;
	int stack_pos = 0;
	int args_pos = 0;
	int label_num = 0;
	final String call_regs[] = {"%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9"};



	public void setSymtab(SymbolTable s) { st = s; }
	 
	public SymbolTable getSymtab() { return st; }

	public Integer getSymLocInteger(VarSymbol vs){
		return stable.get(vs);
	}

	public void setSymLocInteger(VarSymbol vs, int loc){
		stable.put(vs, new Integer(loc));
	}

	public void incTab(){
		tabs+=1;
	}

	public void decTab(){
		tabs-=1;
		if(tabs<0) tabs = 0;
	}

	public void printTabs(){
		String spaces="";
		for(int i =0; i < tabs; i++){
			spaces += " ";
		}
		System.out.println(spaces);
	}

	public String getTypeString(Type t){
		if(t==null){
			return "";
		}
		else if(t instanceof IntArrayType){
			return "int[]";
		}
		else if(t instanceof BooleanType){
			return "Boolean";
		}
		else if(t instanceof IntegerType){
			return "int";
		}
		else if(t instanceof IdentifierType){
			return ((IdentifierType)t).s;
		}
		else return "";
	}

	public int getObjectSize(String t, boolean is_class){
		if(t==null || t==""){
			return 0;
		}
		else if(t.equals("int")) return 8;
		else if(t.equals("int[]")) return 8;
		else if(t.equals("Boolean")||t.equals("boolean")) return 8;
		else if(is_class) return 8;

		int size = 0;
		ClassSymbol cs = (ClassSymbol)st.lookupSymbol(t, "ClassSymbol");
		if(cs != null && cs.getVariables() != null){
			List<VarSymbol> vsl = cs.getVariables();
			for(int i =0; i < vsl.size; i++){
				size += getObjectSize(vsl.get(i), true);
			}
		}
		return size;
	}

	public void printInstr(String instr, String[] args){
		printTabs();
		System.out.print(instr);
		int len = 8 - instr.length();
		if(len > 0){
			for(int t = 0; t < len; t++){
				System.out.print(" ");
			}
			for(int a = 0; a < args.length; a++){
				if(a > 0) System.out.print(" ");
			}
		}
		System.out.print("\n");
	}

	public void printInstr(String instr){
		printTabs();
		System.out.println(instr);
	}

	public String getLabel(Identifier i){
		return getLabel("", i.s);
	}

	public String getLabel(String classname, String callname){
		String label = !classname.isEmpty()? classname +"$"+callname:callname;
		String os = System.getProperty("os.name");
		if(os.contains("OS X")){
			return " " + label;
		}
		return label;
	}
	
	public void report_error(int line, String msg) {
		System.out.println(line + ": " + msg);
		++errors;
	}

	public String getExp(ASTNode n, boolean dst){
		if(n==null){
			return "";
		}
		else if(n instanceof IntegerLiteral){
			IntegerLiteral i = (IntegerLiteral)n;
			i.accept(this);
			return "%rax";
		}
		else if(n instanceof True){
			True i = (True)m;
			i.accept(this);
			return "%rax";
		}
		else if(n instanceof False){
			False f = (True)n;
			i.accept(this);
			return "%rax";
		}
		else if(n instanceof IdentifierExp){
			IdentifierExp i = (IdentifierExp)n;
			Symbol s = st.getSymbol(i.s);
			if(s != null && s instanceof VarSymbol){
				VarSymbol vs = (VarSymbol) s;
				String operand = getSymLocInteger(vs) + "(%rbp)";
				if(dst) return operand;
				printInstr("movq", new String[]{operand, "%rax"});
				return "%rax";
			}

			ASTNode p = n;
			while(p != null && !(p instanceof ClassDecl)){
				p = p.getParent();
			}

			if(p != null && p instanceof ClassDeclExtends) {
				ClassDeclExtends c = (ClassDeclExtends)p;
				ClassSymbol cs = (ClassSymbol)st.lookupSymbol(c.j.s);
				if(cs != null){
					VarSymbol vs = cs.getVariables(i.s);
					if(vs != null){
						printInstr("movq", new String[] { "-8(%rbp", "%rax"});
						String operand = getSymLocInteger(vs) + "(%rax)";
						if (dst) return operand;
						printInstr("movq", new String[]{operand, "%rax"});
						return "%rax";
					}
				}
			}
		}
		else if(n instanceof Identifier){
			Identifier i = (Identifier)n;
			Symbol s = st.getSymbol(i.s);
			if(s != null && s instanceof VarSymbol){
				VarSymbol vs = (VarSymbol) s;
				String operand = getSymLocInteger(vs) + "(%rbp)";
				if(dst) return operand;
				printInstr("movq", new String[]{operand, "%rax"});
				return "%rax";
			}
			s = st.lookupSymbol(i.s, VarSymbol);
			if(s != null && s instanceof VarSymbol){
				VarSymbol vs = (VarSymbol) s;
				printInstr("movq", new String[]{"-8 (%rbp)", "%rax"});
				String operand = getSymLocInteger(vs) + "(%rax)";
				if(dst) return operand;
				printInstr("movq", new String[]{operand, "%rax"});
				return "%rax";
			}

			ASTNode p = n;
			while(p != null && !(p instanceof ClassDecl)){
				p = p.getParent();
			}

			if(p != null && p instanceof ClassDeclExtends) {
				ClassDeclExtends c = (ClassDeclExtends)p;
				ClassSymbol cs = (ClassSymbol)st.lookupSymbol(c.j.s);
				if(cs != null){
					VarSymbol vs = cs.getVariables(i.s);
					if(vs != null){
						printInstr("movq", new String[] { "-8(%rbp", "%rax"});
						String operand = getSymLocInteger(vs) + "(%rax)";
						if (dst) return operand;
						printInstr("movq", new String[]{operand, "%rax"});
						return "%rax";
					}
				}
			}
		}
		else if( n instanceof ArrayLookup){
			ArrayLookup e = (ArrayLookup) n;
			e.accept(this);
			return "%rax";
		}
		else if(n instanceof Exp){
			Exp e = (Exp)n;
			e.accept(this);
			return "%rax";
		}
		report_error(n.getLineNo(), "Undefined Expression");
		return""; 
	}

	// Display added for toy example language. Not used in regular MiniJava
	public void visit(Display n) {
		n.e.accept(this);
	}

	// MainClass m;
	// ClassDeclList cl;
	public void visit(Program n) {
		System.out.println(".text");
		System.out.println(".glob" + getLabel("", "asm_main"));
		n.m.accept(this);
		if(n.cl != null){
			for (int i = 0; i < n.cl.size(); i++) {
				n.cl.get(i).accept(this);
			}
		}
		
	}

	// Identifier i1,i2;
	// Statement s;
	public void visit(MainClass n) {
		n.i1.accept(this);
		st = st.findScope(n.il.toString());
		n.i2.accept(this);
		st = st.findScope("main");
		stack_pos = 0;
		args_pos = 0;
		System.out.println("");
		System.out.println(getLabel("", "asm_main")+ ":");
		incTab();
		printInstr("pushq", new String[]{"%rbp"});
		printInstr("movq", new String[]{"%rsp", "%rbp"});
		n.s.accept(this);
		printInstr("leave");
		printInstr("ret");
		decTab();
		st = st.exitScope();
		st = st.exitScope();
	}

	// Identifier i;
	// VarDeclList vl;
	// MethodDeclList ml;
	public void visit(ClassDeclSimple n) {
		n.i.accept(this);
		st = st.findScope(n.i.toString());
		stack_pos = 0;
		args_pos = 0;
		for (int i = 0; i < n.vl.size(); i++) {
			n.vl.get(i).accept(this);
		}
		for (int i = 0; i < n.ml.size(); i++) {
			n.ml.get(i).accept(this);
		}
		st = st.exitScope();
	}

	// Identifier i;
	// Identifier j;
	// VarDeclList vl;
	// MethodDeclList ml;
	public void visit(ClassDeclExtends n) {
		n.i.accept(this);
		n.j.accept(this);
		st = st.findScope(n.i.toString());
		stack_pos = 0;
		args_pos = 0;
		for (int i = 0; i < n.vl.size(); i++) {
			n.vl.get(i).accept(this);
		}
		for (int i = 0; i < n.ml.size(); i++) {
			n.ml.get(i).accept(this);
		}
		st = st.exitScope();
	}

	// Type t;
	// Identifier i;
	public void visit(VarDecl n) {
		if(n.i == null) return;
		Symbol sym = st.getVarTable().get(n.i.s);
		if(sym!=null && sym instanceof VarSymbol){
			VarSymbol vs = (VarSymbol) sym;
			stack_pos -= 8;
			setSymLocInteger(vs, stack_pos);
		}
	}

	// Type t;
	// Identifier i;
	// FormalList fl;
	// VarDeclList vl;
	// StatementList sl;
	// Exp e;
	public void visit(MethodDecl n) {
		ASTNode p = st.getScope();
		st = st.findScope(n.i.toString());
		stack_pos = 0;
		args_pos = 0;

		String func_name = n.i.s;
		String class_name = "";
		if(p!=null && p instanceof ClassDeclSimple){
			ClassDeclSimple c = (ClassDeclSimple) p;
			class_name = c.i.s;
		}
		System.out.println(getLabel(class_name, func_name) + ":");
		incTab();
		printInstr("pushq", new String[]{"%rbp"});
		printInstr("movq", new String[]{"%rsp", "%rbp"});

		int stack_size = 8 * (n.fl.size() + n.vl.size() + 1);
		if(stack_size > 0){
			stack_size += (stack_size % 16);
			printInstr("subq", new String[] {"$" + Integer.toString(stack_size), "%rsp"});
		}
		stack_pos -= 8;
		String stack_loc = Integer.toString(stack_pos) + "(%rbp)";
		printInstr("movq", new String[] {call_regs[args_pos++], stack_loc});

		for (int i = 0; i < n.fl.size(); i++) {
			n.fl.get(i).accept(this);
		}
		for (int i = 0; i < n.vl.size(); i++) {
			n.vl.get(i).accept(this);
		}
		for (int i = 0; i < n.sl.size(); i++) {
			n.sl.get(i).accept(this);
		}

		if(n.e != null){
			String ret = getExp(n.e, false);
			if(ret != "%rax"){
				printInstr("movq", new String[] {ret, "%rax"});
			}
		}
		st = st.exitScope();
		printInstr("leave");
		printInstr("ret");
		decTab();
	}

	// Type t;
	// Identifier i;
	public void visit(Formal n) {
		if (n.i == null) return;
		Symbol sym = st.getVarTable().get(n.i.s);
		if(sym!=null && sym instanceof VarSymbol){
			VarSymbol vs = (VarSymbol) sym;
			stack_pos -= 8;
			setSymLocInteger(vs, stack_pos);
			String stack_loc = Integer.toString(stack_pos)+ "(%rbp)";
			printInstr("movq", new String[] {call_regs[args_pos++], stack_loc});
		}
	}

	public void visit(IntArrayType n) {
	}

	public void visit(BooleanType n) {
	}

	public void visit(IntegerType n) {
	}

	// String s;
	public void visit(IdentifierType n) {
	}

	// StatementList sl;
	public void visit(Block n) {
		for (int i = 0; i < n.sl.size(); i++) {
			n.sl.get(i).accept(this);
		}
	}

	// Exp e;
	// Statement s1,s2;
	public void visit(If n) {
		String label = getLabel("", "L"+label_num++);
		String cond = getExp(n.e, false);
		printInstr("cmp", new String[]{"$0", cond});
		printInstr("je", new String[]{label});
		n.s1.accept(this);

		if(n.s2 != null){
			String label2 = getLabel("", "L"+label_num++);
			printInstr("jmp", new String[] {label2});
			System.out.println(label+ ":");
			n.s2.accept(this);
			label = label2;
		}
		System.out.println(label+ ":");
	}

	// Exp e;
	// Statement s;
	public void visit(While n) {
		String label1 = getLabel("", "L"+label_num++);
		String label2 = getLabel("", "L"+label_num++);
		System.out.println(label1 + ":");

		String cond = getExp(n.e, false);
		printInstr("cmp", new String[]{"$0", cond});
		printInstr("je", new String[]{label2});

		n.s.accept(this);
		printInstr("jmp", new String[] {label1});
		System.out.println(label2+ ":");
	}

	// Exp e;
	public void visit(Print n) {
		String operand = getExp(n.e, false);
		printInstr("movq", new String[]{operand, "%rdi"});
		printInstr("call", new String[]{ getLabel("", "put")});
	}

	// Identifier i;
	// Exp e;
	public void visit(Assign n) {
		String e = getExp(n.e, false);
		printInstr("pushq", new String[] {e});

		String i = getExp(n.i, true);
		printInstr("popq", new String[] {"%rdi"});

		printInstr("movq", new String[] {"%rdi", i});
	}

	// Identifier i;
	// Exp e1,e2;
	public void visit(ArrayAssign n) {
		String val = getExp(n.e2, false);
		printInstr("pushq", new String[] {val});

		String arr = getExp(n.i, true);
		printInstr("pushq", new String[] {arr});

		String index = getExp(n.e1, false);
		printInstr("popq", new String[] {"%rdi"});
		printInstr("popq", new String[] {"%rdx"});
		printInstr("movq", new String[] {"%rdx", "16(%rdi, " + index + ",8)"});
	}

	// Exp e1,e2;
	public void visit(And n) {
		String opl = getExp(n.e1, false);
		printInstr("pushq", new String[] {"%rax"});

		String op2 = getExp(n.e2, false);
		printInstr("popq", new String[] {"%rdi"});

		opl = "%rdi";
		printInstr("andq", new String[] {op1, op2});
	}

	// Exp e1,e2;
	public void visit(LessThan n) {
		String opl = getExp(n.e1, false);
		printInstr("pushq", new String[] {"%rax"});

		String op2 = getExp(n.e2, false);
		printInstr("popq", new String[] {"%rdi"});
		opl = "%rdi";

		printInstr("cmpq", new String[] {op1, op2});
		printInstr("setq", new String[] {"%al"});
		printInstr("movezbq", new String[] {"%al", "%rax"});
		
	}

	// Exp e1,e2;
	public void visit(Plus n) {
		String opl = getExp(n.e1, false);
		printInstr("pushq", new String[] {"%rax"});

		String op2 = getExp(n.e2, false);
		printInstr("popq", new String[] {"%rdi"});

		opl = "%rdi";
		printInstr("addq", new String[] {op1, op2});
	}

	// Exp e1,e2;
	public void visit(Minus n) {
		String op2 = getExp(n.e2, false);
		printInstr("pushq", new String[] {"%rax"});

		String opl = getExp(n.e1, false);
		printInstr("popq", new String[] {"%rdi"});
		op2 = "%rdi";

		printInstr("subq", new String[] {op2, op1});
	}

	// Exp e1,e2;
	public void visit(Times n) {
		String opl = getExp(n.e1, false);
		printInstr("pushq", new String[] {"%rax"});

		String op2 = getExp(n.e2, false);
		printInstr("popq", new String[] {"%rdi"});

		opl = "%rdi";
		printInstr("imulq", new String[] {op1, op2});
	}

	// Exp e1,e2;
	public void visit(ArrayLookup n) {
		String arr = getExp(n.e1, false);
		printInstr("pushq", new String[] {arr});

		String index = getExp(n.e2, false);
		printInstr("popq", new String[] {"%rdi"});
		printInstr("movq", new String[] {"16(%rdi, " + index + ",8)", "%rax"});
	}

	// Exp e;
	public void visit(ArrayLength n) {
		String arr = getExp(n.e, false);
		printInstr("movq", new String[] {"0(" + arr + ")", "%rax"});
	}

	// Exp e;
	// Identifier i;
	// ExpList el;
	public void visit(Call n) {
		String obj = getExp(n.e, false);
		printInstr("movq", new String[] {obj, "%rdi"});

		String func_name = n.i.s;
		String class_name = "";

		for (int i = 0; i < n.el.size(); i++) {
			String e = getExp(n.e1.get(i), false);
			if(e.equals(call_regs[i+1])) continue;
			printInstr("movq", new String[] {e, call_regs[i+1]});
		}

		if(n.e!=null){
			if(n.e instanceof IdentifierExp){
				IdentifierExp i = (IdentifierExp)n.e;
				Symbol s = st.getSymbol(i.s);
				if(s != null && s instanceof ClassSymbol){
					ClassSymbol csym = (ClassSymbol) s;
					class_name = csym.getName();
				}
				else if(s != null && s instanceof VarSymbol){
					VarSymbol vsym = (VarSymbol) s;
					class_name = vsym.getName();
				}
			}
			else if(n.e instanceof Call){
				Call c = (Call) n.e;
				MethodSymbol msym = (MethodSymbol) st.lookupSymbol(c.i.s, "MethodSymbol");
				if(msym != null){
					class_name = msym.getType();
				}
			}
			else if(n.e instanceof This){
				This t = (This)n.e;
				ASTNode p = t.getParent();
				while(p!=null && !(p instanceof ClassDecl)) p = p.getParent();
				if(p!=null && p instanceof ClassDeclSimple){
					ClassDeclSimple c = (ClassDeclSimple)p;
					MethodSymbol ms = null;
					ClassSymbol csym = (ClassSymbol)st.lookupSymbol(c.i.s, "ClassSymbol");
					if(csym!=null && csym.getMethod(func_name) != null){
						class_name = csym.getName();
					}
				}	
				else if(p!=null && p instanceof ClassDeclExtends){
					ClassDeclExtends c = (ClassDeclExtends) p;
					MethodSymbol ms = null;
					ClassSymbol csym = (ClassSymbol) st.lookupSymbol(c.i.s, "ClassSymbol");
					if(csym!= null){
						ms = csym.getMethod(func_name);
						class_name = csym.getName();
					} 
				
					ClassSymbol csym_ext = (ClassSymbol) st.lookupSymbol(c.j.s, "ClassSymbol");
					if(csym_ext != null){
						MethodSymbol mse = csym_ext.getMethod(func_name);
						if(class_name==null || ms==mse){
							class_name = csym_ext.getName();
						}
					} 
				}
			}
			else if(n.e instanceof NewObject){
				NewObject o = (NewObject) n.e;
				class_name = o.i.s;
			}
		}

		printInstr("call", new String[] { getLabel(class_name, func_name)});
	}

	// int i;
	public void visit(IntegerLiteral n) {
		String i = "$" + Integer.toString(n.i);
		printInstr("movq", new String[] {i, "%rax"});
	}

	public void visit(True n) {
		printInstr("movq", new String[] {"$1", "%rax"});
	}

	public void visit(False n) {
		printInstr("movq", new String[] {"$0", "%rax"});
	}

	// String s;
	public void visit(IdentifierExp n) {
	}

	public void visit(This n) {
		printInstr("movq", new String[] {"-8(%rbp)", "%rax"});
	}

	// Exp e;
	public void visit(NewArray n) {
		String e = getExp(n.e, false);
		printInstr("pushq", new String[] {e});
		printInstr("imulq", new String[] {"$8", e});
		printInstr("addq", new String[] {"$16", e});
		printInstr("movq", new String[] {e, "%rdi"});
		printInstr("call", new String[] {getLabel("", "mjcalloc")});
		printInstr("popq", new String[] {"0(%rax)"});
		printInstr("movq", new String[] {"$16", "8(%rax)"});
	}

	// Identifier i;
	public void visit(NewObject n) {
		String obj = n.i.s;
		int objSize = getObjectSize(obj, false);
		objSize += (objSize % 16);
		printInstr("movq", new String[] {"$"+objSize, "%rdi"});
		printInstr("call", new String[] {getLabel("", "mjcalloc")});
	}

	// Exp e;
	public void visit(Not n) {
		String op = getExp(n.e, false);
		printInstr("cmpq", new String[] {"$0", op});
		printInstr("sete", new String[] {"%al"});
		printInstr("movzbq", new String[] {"%al", "%rax"});
	}

	// String s;
	public void visit(Identifier n) {
	}
}