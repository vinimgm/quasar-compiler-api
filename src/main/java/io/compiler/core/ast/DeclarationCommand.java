package io.compiler.core.ast;

import io.compiler.types.Types;
import io.compiler.types.Var;

public class DeclarationCommand extends Command{
	private Var var;
	
	public DeclarationCommand(Var v) {
		this.var = v;
	}

	@Override
	public String generateTarget() {
		
		StringBuilder sb = new StringBuilder();
		
		switch (this.var.getType()) {
			case NUMBER:  	sb.append("int"); 		break;
			case REALNUMBER:	sb.append("double");	break;
			case TEXT:		sb.append("String");	break;
		}
		sb.append(" " + var.getId());
		sb.append(";\n");
		return sb.toString();
	}
	
}
