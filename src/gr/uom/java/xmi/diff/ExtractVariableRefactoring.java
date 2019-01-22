package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class ExtractVariableRefactoring implements Refactoring {
	private VariableDeclaration variableDeclaration;
	private UMLOperation operation;

	public ExtractVariableRefactoring(VariableDeclaration variableDeclaration, UMLOperation operation) {
		this.variableDeclaration = variableDeclaration;
		this.operation = operation;
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_VARIABLE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public VariableDeclaration getVariableDeclaration() {
		return variableDeclaration;
	}

	public UMLOperation getOperation() {
		return operation;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(variableDeclaration);
		sb.append(" in method ");
		sb.append(operation);
		sb.append(" from class ");
		sb.append(operation.getClassName());
		return sb.toString();
	}

	@Override
	public String getRefactoredClass () {
		return operation.getClassName();
	}

	/**
	 * @return the code range of the extracted variable declaration in the <b>child</b> commit
	 */
	public CodeRange getExtractedVariableDeclarationCodeRange() {
		return variableDeclaration.codeRange();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((operation == null) ? 0 : operation.hashCode());
		result = prime * result + ((variableDeclaration == null) ? 0 : variableDeclaration.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExtractVariableRefactoring other = (ExtractVariableRefactoring) obj;
		if (operation == null) {
			if (other.operation != null)
				return false;
		} else if (!operation.equals(other.operation))
			return false;
		if (variableDeclaration == null) {
			if (other.variableDeclaration != null)
				return false;
		} else if (!variableDeclaration.equals(other.variableDeclaration))
			return false;
		return true;
	}
}
