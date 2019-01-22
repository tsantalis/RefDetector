package gr.uom.java.xmi.diff;

import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;

public class ExtractClassRefactoring implements Refactoring {
	private UMLClass extractedClass;
	private UMLClass originalClass;
	private Set<UMLOperation> extractedOperations;
	private Set<UMLAttribute> extractedAttributes;
	private UMLAttribute attributeOfExtractedClassTypeInOriginalClass;

	public ExtractClassRefactoring(UMLClass extractedClass, UMLClass originalClass,
			Set<UMLOperation> extractedOperations, Set<UMLAttribute> extractedAttributes, UMLAttribute attributeOfExtractedClassType) {
		this.extractedClass = extractedClass;
		this.originalClass = originalClass;
		this.extractedOperations = extractedOperations;
		this.extractedAttributes = extractedAttributes;
		this.attributeOfExtractedClassTypeInOriginalClass = attributeOfExtractedClassType;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(extractedClass);
		sb.append(" from class ");
		sb.append(originalClass);
		return sb.toString();
	}

	@Override
	public String getRefactoredClass () {
		return originalClass.getName();
	}

	public RefactoringType getRefactoringType() {
		if(extractedClass.isSubTypeOf(originalClass))
			return RefactoringType.EXTRACT_SUBCLASS;
		return RefactoringType.EXTRACT_CLASS;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public UMLClass getExtractedClass() {
		return extractedClass;
	}

	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public Set<UMLOperation> getExtractedOperations() {
		return extractedOperations;
	}

	public Set<UMLAttribute> getExtractedAttributes() {
		return extractedAttributes;
	}

	public UMLAttribute getAttributeOfExtractedClassTypeInOriginalClass() {
		return attributeOfExtractedClassTypeInOriginalClass;
	}
}
