package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
import org.refactoringminer.astDiff.actions.ASTDiff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class ProjectASTDiffer
{
	private final static Logger logger = LoggerFactory.getLogger(ProjectASTDiffer.class);
	private final boolean CHECK_COMMENTS = false;
	private UMLModelDiff modelDiff;
	private List<AbstractCodeMapping> lastStepMappings;
	private List<Refactoring> modelDiffRefactorings;
	private Set<ASTDiff> diffSet = new LinkedHashSet<>();

	public ProjectASTDiffer(UMLModelDiff modelDiff) throws RefactoringMinerTimedOutException {
		this.modelDiff = modelDiff;
		diff();
	}

	public Set<ASTDiff> getDiffSet() {
		return diffSet;
	}

	private void diff() throws RefactoringMinerTimedOutException {
		long start = System.currentTimeMillis();
		this.modelDiffRefactorings = modelDiff.getRefactorings();
		long finish = System.currentTimeMillis();
		logger.info("ModelDiff.getRefactorings() execution time: " + (finish - start)/ 1000 + " seconds");
		long diff_execution_started = System.currentTimeMillis();
		makeASTDiff(modelDiff.getCommonClassDiffList());
		makeASTDiff(modelDiff.getClassRenameDiffList());
		//makeASTDiff(modelDiff.getClassMoveDiffList());
		long diff_execution_finished =  System.currentTimeMillis();
		logger.info("Diff execution: " + (diff_execution_finished - diff_execution_started)/ 1000 + " seconds");
		computeAllEditScripts();
	}

	private void computeAllEditScripts() {
		long editScript_start = System.currentTimeMillis();
		for (ASTDiff diff : diffSet) {
			diff.computeEditScript(modelDiff.getParentModel().getTreeContextMap(), modelDiff.getChildModel().getTreeContextMap());
		}
		long editScript_end = System.currentTimeMillis();
		logger.info("EditScript execution: " + (editScript_end - editScript_start)/ 1000 + " seconds");
	}

	private void makeASTDiff(List<? extends UMLClassBaseDiff> umlClassBaseDiffList) throws RefactoringMinerTimedOutException {
		for (UMLClassBaseDiff classDiff : umlClassBaseDiffList) {
			ASTDiff classASTDiff = process(classDiff, findTreeContexts(classDiff));
			//ASTDiff classASTDiff = processWithGTGreedy(classDiff, findTreeContexts(classDiff));
			//ASTDiff classASTDiff = processWithGTSimple(classDiff, findTreeContexts(classDiff));
			if (diffSet.contains(classASTDiff)) {
				for (ASTDiff diff : diffSet) {
					if (diff.equals(classASTDiff)) {
						diff.getMultiMappings().mergeMappings(classASTDiff.getMultiMappings());
						break;
					}
				}
			}
			else {
				diffSet.add(classASTDiff);
			}
		}
	}

	private Pair<TreeContext, TreeContext> findTreeContexts(UMLClassBaseDiff classDiff) {
		return new Pair<>(modelDiff.getParentModel().getTreeContextMap().get(classDiff.getOriginalClass().getSourceFile()),
				modelDiff.getChildModel().getTreeContextMap().get(classDiff.getNextClass().getSourceFile()));
	}

	private ASTDiff process(UMLClassBaseDiff classDiff, Pair<TreeContext, TreeContext> treeContextPair) throws RefactoringMinerTimedOutException {
		TreeContext srcTreeContext = treeContextPair.first;
		TreeContext dstTreeContext = treeContextPair.second;
		Tree srcTree = srcTreeContext.getRoot();
		Tree dstTree = dstTreeContext.getRoot();
		ExtendedMultiMappingStore mappingStore = new ExtendedMultiMappingStore(srcTreeContext,dstTreeContext);
		mappingStore.addMapping(srcTree,dstTree);
		this.lastStepMappings = new ArrayList<>();

		processPackageDeclaration(srcTree,dstTree,classDiff,mappingStore);
		processImports(srcTree,dstTree,classDiff.getImportDiffList(),mappingStore);
		processEnumConstants(srcTree,dstTree,classDiff.getCommonEnumConstants(),mappingStore);
		for (UMLEnumConstantDiff umlEnumConstantDiff : classDiff.getEnumConstantDiffList()) {
			UMLAnonymousClassDiff anonymousClassDiff = umlEnumConstantDiff.getAnonymousClassDiff().isPresent() ? umlEnumConstantDiff.getAnonymousClassDiff().get() : null;
			assert anonymousClassDiff != null;
			List<UMLOperationBodyMapper> operationBodyMapperList = anonymousClassDiff.getOperationBodyMapperList();
			for (UMLOperationBodyMapper umlOperationBodyMapper : operationBodyMapperList) {
				processMethod(srcTree,dstTree,umlOperationBodyMapper,mappingStore);
			}
		}
		processClassDeclarationMapping(srcTree,dstTree,classDiff,mappingStore);
		processAllMethods(srcTree,dstTree,classDiff.getOperationBodyMapperList(),mappingStore);
		processRefactorings(srcTree,dstTree,classDiff.getRefactorings(),mappingStore);
		processModelDiffRefactorings(srcTree,dstTree,classDiff,mappingStore);
		processLastStepMappings(srcTree,dstTree,mappingStore);
		//if (CHECK_COMMENTS) addAndProcessComments(treeContextPair.first, treeContextPair.second,mappingStore);
		return new ASTDiff(classDiff.getOriginalClass().getLocationInfo().getFilePath(),
				classDiff.getNextClass().getLocationInfo().getFilePath(), treeContextPair.first, treeContextPair.second, mappingStore);
	}

	private void processLastStepMappings(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
		for (AbstractCodeMapping lastStepMapping : lastStepMappings) {
			if (lastStepMapping.getFragment1().getLocationInfo().getFilePath().equals(lastStepMapping.getFragment2().getLocationInfo().getFilePath())) {
				Tree srcExp = TreeUtilFunctions.findByLocationInfo(srcTree, lastStepMapping.getFragment1().getLocationInfo());
				Tree dstExp = TreeUtilFunctions.findByLocationInfo(dstTree, lastStepMapping.getFragment2().getLocationInfo());
				new LeafMatcher(true).match(srcExp, dstExp, lastStepMapping, mappingStore);
			}
		}
	}

	private void processEnumConstants(Tree srcTree, Tree dstTree, Set<org.apache.commons.lang3.tuple.Pair<UMLEnumConstant, UMLEnumConstant>> commonEnumConstants, ExtendedMultiMappingStore mappingStore) {
		for (org.apache.commons.lang3.tuple.Pair<UMLEnumConstant, UMLEnumConstant> commonEnumConstant : commonEnumConstants) {
			LocationInfo locationInfo1 = commonEnumConstant.getLeft().getLocationInfo();
			LocationInfo locationInfo2 = commonEnumConstant.getRight().getLocationInfo();
			Tree srcEnumConstant = TreeUtilFunctions.findByLocationInfo(srcTree,locationInfo1);
			Tree dstEnumConstant = TreeUtilFunctions.findByLocationInfo(dstTree,locationInfo2);
			new LeafMatcher(false).match(srcEnumConstant,dstEnumConstant,null,mappingStore);
		}
	}

	private void processModelDiffRefactorings(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		for(Refactoring refactoring : modelDiffRefactorings)
		{
			List<String> beforeRefactoringClasses = refactoring.getInvolvedClassesBeforeRefactoring().stream().map(ImmutablePair::getRight).collect(Collectors.toList());
			List<String> afterRefactoringClasses = refactoring.getInvolvedClassesAfterRefactoring().stream().map(ImmutablePair::getRight).collect(Collectors.toList());
			if (refactoring instanceof MoveOperationRefactoring) {
				MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
				String srcPath = moveOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath();
				String dstPath = moveOperationRefactoring.getMovedOperation().getLocationInfo().getFilePath();
				Tree srcTotalTree = modelDiff.getParentModel().getTreeContextMap().get(srcPath).getRoot();
				Tree dstTotalTree = modelDiff.getChildModel().getTreeContextMap().get(dstPath).getRoot();
				processMethod(srcTotalTree, dstTotalTree, moveOperationRefactoring.getBodyMapper(), mappingStore);
			}
			if (refactoring instanceof MoveAttributeRefactoring)
			{
				MoveAttributeRefactoring moveAttributeRefactoring = (MoveAttributeRefactoring) refactoring;

				String srcPath = moveAttributeRefactoring.getOriginalAttribute().getLocationInfo().getFilePath();
				String dstPath = moveAttributeRefactoring.getMovedAttribute().getLocationInfo().getFilePath();
				Tree srcTotalTree = modelDiff.getParentModel().getTreeContextMap().get(srcPath).getRoot();
				Tree dstTotalTree = modelDiff.getChildModel().getTreeContextMap().get(dstPath).getRoot();
				processFieldDeclaration(srcTotalTree,dstTotalTree,moveAttributeRefactoring.getOriginalAttribute(),moveAttributeRefactoring.getMovedAttribute(),mappingStore);
			}
			if (afterRefactoringClasses.contains(classDiff.getNextClass().getName()))
			{
				if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION))
				{
					PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
					String otherFileName = pushDownOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath();
					Tree otherFileTree = modelDiff.getParentModel().getTreeContextMap().get(otherFileName).getRoot();
					processMethod(otherFileTree,dstTree,pushDownOperationRefactoring.getBodyMapper(),mappingStore);
				}
				else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_OPERATION))
				{
					PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) refactoring;
					String otherFileName = pullUpOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath();
					Tree otherFileTree = modelDiff.getParentModel().getTreeContextMap().get(otherFileName).getRoot();
					processMethod(otherFileTree,dstTree,pullUpOperationRefactoring.getBodyMapper(),mappingStore);
				}
				else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_ATTRIBUTE))
				{
					PushDownAttributeRefactoring pushDownAttributeRefactoring = (PushDownAttributeRefactoring) refactoring;
					String otherFileName = pushDownAttributeRefactoring.getOriginalAttribute().getLocationInfo().getFilePath();
					Tree otherFileTree = modelDiff.getParentModel().getTreeContextMap().get(otherFileName).getRoot();
					processFieldDeclaration(otherFileTree,dstTree,pushDownAttributeRefactoring.getOriginalAttribute(),pushDownAttributeRefactoring.getMovedAttribute(),mappingStore);
				}
				else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_ATTRIBUTE))
				{
					PullUpAttributeRefactoring pullUpAttributeRefactoring = (PullUpAttributeRefactoring) refactoring;
					String otherFileName = pullUpAttributeRefactoring.getOriginalAttribute().getLocationInfo().getFilePath();
					Tree otherFileTree = modelDiff.getParentModel().getTreeContextMap().get(otherFileName).getRoot();
					processFieldDeclaration(otherFileTree,dstTree,pullUpAttributeRefactoring.getOriginalAttribute(),pullUpAttributeRefactoring.getMovedAttribute(),mappingStore);
				}

			}
			else if (beforeRefactoringClasses.contains(classDiff.getOriginalClass().getName()))
			{
				if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION))
				{
					PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
					String otherFileName = pushDownOperationRefactoring.getMovedOperation().getLocationInfo().getFilePath();
					Tree otherFileTree = modelDiff.getChildModel().getTreeContextMap().get(otherFileName).getRoot();
					processMethod(srcTree,otherFileTree,pushDownOperationRefactoring.getBodyMapper(),mappingStore);
				}
				else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_OPERATION))
				{
					PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) refactoring;
					String otherFileName = pullUpOperationRefactoring.getMovedOperation().getLocationInfo().getFilePath();
					Tree otherFileTree = modelDiff.getChildModel().getTreeContextMap().get(otherFileName).getRoot();
					processMethod(srcTree,otherFileTree,pullUpOperationRefactoring.getBodyMapper(),mappingStore);
				}
				else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_ATTRIBUTE))
				{
					PushDownAttributeRefactoring pushDownAttributeRefactoring = (PushDownAttributeRefactoring) refactoring;
					String otherFileName = pushDownAttributeRefactoring.getMovedAttribute().getLocationInfo().getFilePath();
					Tree otherFileTree = modelDiff.getChildModel().getTreeContextMap().get(otherFileName).getRoot();
					processFieldDeclaration(srcTree,otherFileTree,pushDownAttributeRefactoring.getOriginalAttribute(),pushDownAttributeRefactoring.getMovedAttribute(),mappingStore);
				}
				else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_ATTRIBUTE))
				{
					PullUpAttributeRefactoring pullUpAttributeRefactoring = (PullUpAttributeRefactoring) refactoring;
					String otherFileName = pullUpAttributeRefactoring.getMovedAttribute().getLocationInfo().getFilePath();
					Tree otherFileTree = modelDiff.getChildModel().getTreeContextMap().get(otherFileName).getRoot();
					processFieldDeclaration(srcTree,otherFileTree,pullUpAttributeRefactoring.getOriginalAttribute(),pullUpAttributeRefactoring.getMovedAttribute(),mappingStore);
				}
			}
		}
	}

	/*
	private void addAndProcessComments(TreeContext firstTC, TreeContext secondTC, MultiMappingStore mappingStore) {
		Pair<List<Tree>, List<Tree>> addedCommentsPair = addComments(firstTC,secondTC);
		processComments(addedCommentsPair,mappingStore);
	}
	*/

	private void processAllMethods(Tree srcTree, Tree dstTree, List<UMLOperationBodyMapper> operationBodyMapperList, ExtendedMultiMappingStore mappingStore) {
		for(UMLOperationBodyMapper umlOperationBodyMapper : new ArrayList<>(operationBodyMapperList))
			processMethod(srcTree,dstTree,umlOperationBodyMapper,mappingStore);
	}

	private void processMethod(Tree srcTree, Tree dstTree, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore)
	{
		processOperationDiff(srcTree,dstTree,umlOperationBodyMapper,mappingStore);
		processMethodParameters(srcTree,dstTree,umlOperationBodyMapper.getMatchedVariables(),mappingStore);
		if (umlOperationBodyMapper.getOperation1() != null & umlOperationBodyMapper.getOperation2() != null) {
			processMethodJavaDoc(srcTree, dstTree, umlOperationBodyMapper.getOperation1().getJavadoc(), umlOperationBodyMapper.getOperation2().getJavadoc(), mappingStore);
			Tree srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getOperation1().getLocationInfo());
			Tree dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getOperation2().getLocationInfo());
			mappingStore.addMapping(srcOperationNode, dstOperationNode);
			processMethodSignature(srcOperationNode, dstOperationNode, umlOperationBodyMapper, mappingStore);
			fromRefMiner(srcTree, dstTree, umlOperationBodyMapper, mappingStore);
		}
		else {
			//Static Initializers
			Tree srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getContainer1().getLocationInfo());
			Tree dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getContainer2().getLocationInfo());
			mappingStore.addMapping(srcOperationNode, dstOperationNode);
			if (umlOperationBodyMapper.getContainer1() instanceof UMLInitializer &&  umlOperationBodyMapper.getContainer2() instanceof UMLInitializer)
				if (((UMLInitializer)umlOperationBodyMapper.getContainer1()).isStatic() && ((UMLInitializer)umlOperationBodyMapper.getContainer2()).isStatic())
				{
					Tree srcInitializer = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getContainer1().getLocationInfo());
					Tree dstInitializer = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getContainer2().getLocationInfo());
					mappingStore.addMapping(srcInitializer.getChild(0),dstInitializer.getChild(0));
				}
			processMethodSignature(srcOperationNode, dstOperationNode,umlOperationBodyMapper,  mappingStore);
			fromRefMiner(srcTree, dstTree, umlOperationBodyMapper, mappingStore);
		}
	}

	private void processMethodParameters(Tree srcTree, Tree dstTree, Set<org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration>> matchedVariables, ExtendedMultiMappingStore mappingStore) {
		for (org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration> matchedPair: matchedVariables) {
			VariableDeclaration leftPair = matchedPair.getLeft();
			VariableDeclaration rightPair = matchedPair.getRight();
			Tree leftTree =  TreeUtilFunctions.findByLocationInfo(srcTree,leftPair.getLocationInfo());
			Tree rightTree = TreeUtilFunctions.findByLocationInfo(dstTree,rightPair.getLocationInfo());
			if (leftTree.getParent().getType().name.equals("MethodDeclaration") &&
					rightTree.getParent().getType().name.equals("MethodDeclaration"))
				if (rightTree.isIsomorphicTo(leftTree))
					mappingStore.addMappingRecursively(leftTree,rightTree);
		}
	}

	private void fromRefMiner(Tree srcTree, Tree dstTree, UMLOperationBodyMapper bodyMapper, ExtendedMultiMappingStore mappingStore) {
		if (bodyMapper.getAnonymousClassDiffs() != null) {
			for (UMLAnonymousClassDiff anonymousClassDiff : bodyMapper.getAnonymousClassDiffs()) {
				processClassAttributes(srcTree,dstTree,anonymousClassDiff,mappingStore);
				for (UMLOperationBodyMapper umlOperationBodyMapper : anonymousClassDiff.getOperationBodyMapperList()) {
					processMethod(srcTree,dstTree,umlOperationBodyMapper,mappingStore);
				}
			}
		}
		Set<AbstractCodeMapping> mappingSet = bodyMapper.getMappings();
		ArrayList<AbstractCodeMapping> mappings = new ArrayList<>(mappingSet);
		for (AbstractCodeMapping abstractCodeMapping : mappings)
		{
			if (abstractCodeMapping instanceof LeafMapping)
				processLeafMapping(srcTree,dstTree,abstractCodeMapping,mappingStore);
			else if (abstractCodeMapping instanceof CompositeStatementObjectMapping)
				processCompositeMapping(srcTree,dstTree,abstractCodeMapping,mappingStore);
		}
	}

	private void processCompositeMapping(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
		CompositeStatementObjectMapping compositeStatementObjectMapping = (CompositeStatementObjectMapping) abstractCodeMapping;
		Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,compositeStatementObjectMapping.getFragment1().getLocationInfo());
		Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,compositeStatementObjectMapping.getFragment2().getLocationInfo());
		// TODO: 8/2/2022 Need to rethink regarding the logic asap, with this logic , we might see a huge drop in performance
		//if (srcStatementNode.getMetrics().hash == dstStatementNode.getMetrics().hash)
		//{
		//	mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
		//}
		//else
		{
			mappingStore.addMapping(srcStatementNode,dstStatementNode);
			if ( (srcStatementNode.getType().name.equals("TryStatement") && dstStatementNode.getType().name.equals("TryStatement")) ||
					(srcStatementNode.getType().name.equals("CatchClause") && dstStatementNode.getType().name.equals("CatchClause"))) {
				matchBlocks(srcStatementNode, dstStatementNode, mappingStore);
				if  (srcStatementNode.getType().name.equals("CatchClause") && dstStatementNode.getType().name.equals("CatchClause"))
					new CompositeMatcher().match(srcStatementNode,dstStatementNode,abstractCodeMapping,mappingStore);
			}
			else if (!srcStatementNode.getType().name.equals("Block") && !dstStatementNode.getType().name.equals("Block")) {
				new CompositeMatcher().match(srcStatementNode, dstStatementNode, abstractCodeMapping , mappingStore);
			}
		}
	}

	private void processLeafMapping(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
		LeafMapping leafMapping = (LeafMapping) abstractCodeMapping;
		Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,leafMapping.getFragment1().getLocationInfo());
		Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,leafMapping.getFragment2().getLocationInfo());

		//if (leafMapping.getFragment2().toString().equals(leafMapping.getFragment1().toString())) {
		//	mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
		//}
		//else {
		//	if (srcStatementNode.isIsoStructuralTo(dstStatementNode))
		//	{
		//		mappingStore.addMappingRecursively(srcStatementNode,dstStatementNode);
		//	}
		//	else
		{
			if (srcStatementNode.getType().name.equals(dstStatementNode.getType().name))
				mappingStore.addMapping(srcStatementNode, dstStatementNode);

			if (abstractCodeMapping.getFragment1() instanceof AbstractExpression || abstractCodeMapping.getFragment2() instanceof AbstractExpression)
				lastStepMappings.add(abstractCodeMapping);
			else
				new LeafMatcher(false).match(srcStatementNode,dstStatementNode,abstractCodeMapping,mappingStore);
		}
	}

	private void processClassAnnotations(Tree srcTree, Tree dstTree, UMLAnnotationListDiff annotationListDiff, ExtendedMultiMappingStore mappingStore) {
		for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation> umlAnnotationUMLAnnotationPair : annotationListDiff.getCommonAnnotations()) {
			Tree srcClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo());
			Tree dstClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo());
			if (srcClassAnnotationTree.isIsoStructuralTo(dstClassAnnotationTree))
				mappingStore.addMappingRecursively(srcClassAnnotationTree,dstClassAnnotationTree);
		}
	}

	private void matchBlocks(Tree srcStatementNode, Tree dstStatementNode, ExtendedMultiMappingStore mappingStore) {
		String searchingType = "Block";
		Pair<Tree, Tree> matched = matchBasedOnType(srcStatementNode,dstStatementNode, searchingType);
		if (matched != null)
			mappingStore.addMapping(matched.first,matched.second);
	}

	private void processComments(Pair<List<Tree>, List<Tree>> addedCommentsPair, ExtendedMultiMappingStore mappingStore) {
		List<Tree> srcComments = addedCommentsPair.first;
		List<Tree> dstComments = addedCommentsPair.second;
		Map<Tree,List<Tree>> candidates = new HashMap<>();
		for (Tree srcComment : srcComments)
		{
			List<Tree> candidateList = new ArrayList<>();
			for (Tree dstComment : dstComments)
			{
				if (srcComment.getMetrics().hash == dstComment.getMetrics().hash)
					candidateList.add(dstComment);
			}
			if (!candidateList.isEmpty())
				candidates.put(srcComment,candidateList);
		}
		for (Map.Entry<Tree,List<Tree>> entry : candidates.entrySet())
		{
			Tree srcTree = entry.getKey();
			List<Tree> matches = entry.getValue();
			if (matches.size() == 1) {
				mappingStore.addMappingRecursively(srcTree, matches.get(0));
			}
			else
			{
				//TODO: ignore at the moment
			}
		}
	}

	/*
	private Pair<List<Tree> , List<Tree>> addComments(TreeContext first, TreeContext second) {
		CommentVisitor firstCommentVisitor = new CommentVisitor(first);
		firstCommentVisitor.addCommentToProperSubtree();
		CommentVisitor secondCommentVisitor = new CommentVisitor(second);
		secondCommentVisitor.addCommentToProperSubtree();
		return new Pair<>(firstCommentVisitor.getComments(),secondCommentVisitor.getComments());
	}
	*/

	private void processClassJavaDocs(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		UMLJavadoc javadoc1 = classDiff.getOriginalClass().getJavadoc();
		UMLJavadoc javadoc2 = classDiff.getNextClass().getJavadoc();
		if (javadoc1 != null && javadoc2 != null) {
			Tree srcJavaDocNode = TreeUtilFunctions.findByLocationInfo(srcTree, javadoc1.getLocationInfo());
			Tree dstJavaDocNode = TreeUtilFunctions.findByLocationInfo(dstTree, javadoc2.getLocationInfo());
			if (javadoc1.equalText(javadoc2))
				mappingStore.addMappingRecursively(srcJavaDocNode,dstJavaDocNode);
		}
	}

	private void processMethodJavaDoc(Tree srcTree, Tree dstTree, UMLJavadoc javadoc1, UMLJavadoc javadoc2, ExtendedMultiMappingStore mappingStore) {
		if (javadoc1 != null && javadoc2 != null) {
			if (javadoc1.equalText(javadoc2))
			{
				Tree srcJavaDocNode = TreeUtilFunctions.findByLocationInfo(srcTree,javadoc1.getLocationInfo());
				Tree dstJavaDocNode = TreeUtilFunctions.findByLocationInfo(dstTree,javadoc2.getLocationInfo());
				mappingStore.addMappingRecursively(srcJavaDocNode,dstJavaDocNode);
			}
		}
	}

	private void processOperationDiff(Tree srcTree, Tree dstTree, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore) {
		UMLOperationDiff umlOperationDiff = umlOperationBodyMapper.getOperationSignatureDiff().isPresent() ? umlOperationBodyMapper.getOperationSignatureDiff().get() : null;
		if (umlOperationDiff == null) return;
		UMLTypeParameterListDiff umlTypeParameterListDiff = umlOperationDiff.getTypeParameterListDiff();
		for (org.apache.commons.lang3.tuple.Pair<UMLTypeParameter, UMLTypeParameter> commonTypeParamSet : umlTypeParameterListDiff.getCommonTypeParameters()) {
			Tree srcTypeParam = TreeUtilFunctions.findByLocationInfo(srcTree, commonTypeParamSet.getLeft().getLocationInfo());
			Tree dstTypeParam = TreeUtilFunctions.findByLocationInfo(dstTree, commonTypeParamSet.getRight().getLocationInfo());
			mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
		}
		for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation>  umlAnnotationUMLAnnotationPair : umlOperationDiff.getAnnotationListDiff().getCommonAnnotations())
		{
			Tree srcClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo());
			Tree dstClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo());
			mappingStore.addMappingRecursively(srcClassAnnotationTree,dstClassAnnotationTree);
		}
		Set<org.apache.commons.lang3.tuple.Pair<UMLType, UMLType>> commonExceptionTypes = umlOperationDiff.getCommonExceptionTypes();
		if (commonExceptionTypes != null)
		{
			for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> matchedException : commonExceptionTypes) {
				Tree srcExceptionNode =TreeUtilFunctions.findByLocationInfo(srcTree, matchedException.getLeft().getLocationInfo());
				Tree dstExceptionNode =TreeUtilFunctions.findByLocationInfo(dstTree, matchedException.getRight().getLocationInfo());
				mappingStore.addMappingRecursively(srcExceptionNode,dstExceptionNode);
			}
		}
		if (umlOperationDiff.getRemovedOperation().getReturnParameter() != null && umlOperationDiff.getAddedOperation().getReturnParameter() != null ) {
			LocationInfo srcLocationInfo = umlOperationDiff.getRemovedOperation().getReturnParameter().getType().getLocationInfo();
			LocationInfo dstLocationInfo = umlOperationDiff.getAddedOperation().getReturnParameter().getType().getLocationInfo();
			Tree srcNode =TreeUtilFunctions.findByLocationInfo(srcTree, srcLocationInfo);
			Tree dstNode =TreeUtilFunctions.findByLocationInfo(dstTree, dstLocationInfo);
			if (srcNode.isIsoStructuralTo(dstNode))
				mappingStore.addMappingRecursively(srcNode,dstNode);
		}
	}

	private void processRefactorings(Tree srcTree, Tree dstTree, List<Refactoring> refactoringList, ExtendedMultiMappingStore mappingStore){
		for (Refactoring refactoring : refactoringList)
		{
			if (refactoring instanceof ReplaceLoopWithPipelineRefactoring)
			{
				//TODO: Kinda completed but assertations must be removed
				ReplaceLoopWithPipelineRefactoring replaceLoopWithPipelineRefactoring = (ReplaceLoopWithPipelineRefactoring) (refactoring);
				assert replaceLoopWithPipelineRefactoring.getCodeFragmentsAfter().size() == 1;
				AbstractCodeFragment next = replaceLoopWithPipelineRefactoring.getCodeFragmentsAfter().iterator().next();
				List<LambdaExpressionObject> lambdas = next.getLambdas();
				AbstractCodeFragment enhancedFor = null;
				for (AbstractCodeFragment abstractCodeFragment : replaceLoopWithPipelineRefactoring.getCodeFragmentsBefore()) {
					if (abstractCodeFragment.getLocationInfo().getCodeElementType().toString().equals("ENHANCED_FOR_STATEMENT")) {
						enhancedFor = abstractCodeFragment;
						break;
					}
				}
				assert enhancedFor != null;
				for (LambdaExpressionObject lambda : lambdas) {
					for (VariableDeclaration parameter : lambda.getParameters()) {
						String variableName = parameter.getVariableName();
						VariableDeclaration variableDeclaration = enhancedFor.getVariableDeclaration(variableName);
						Tree srcNode = TreeUtilFunctions.findByLocationInfo(srcTree,variableDeclaration.getLocationInfo());
						Tree dstNode = TreeUtilFunctions.findByLocationInfo(dstTree,parameter.getLocationInfo());
						new LeafMatcher(false).match(srcNode,dstNode,null,mappingStore);
					}
				}
				Tree srcSt = TreeUtilFunctions.findByLocationInfo(srcTree,enhancedFor.getLocationInfo());
				Tree dstSt = TreeUtilFunctions.findByLocationInfo(dstTree,next.getLocationInfo());
				mappingStore.addMapping(srcSt,dstSt);
			}

			if (refactoring instanceof ReplacePipelineWithLoopRefactoring)
			{
				//TODO : ongoing problem
				ReplacePipelineWithLoopRefactoring replaceLoopWithPipelineRefactoring = (ReplacePipelineWithLoopRefactoring) (refactoring);
				assert replaceLoopWithPipelineRefactoring.getCodeFragmentsBefore().size() == 1;
				AbstractCodeFragment next = replaceLoopWithPipelineRefactoring.getCodeFragmentsBefore().iterator().next();
				List<LambdaExpressionObject> lambdas = next.getLambdas();
				AbstractCodeFragment enhancedFor = null;
				for (AbstractCodeFragment abstractCodeFragment : replaceLoopWithPipelineRefactoring.getCodeFragmentsAfter()) {
					if (abstractCodeFragment.getLocationInfo().getCodeElementType().toString().equals("ENHANCED_FOR_STATEMENT")) {
						enhancedFor = abstractCodeFragment;
						break;
					}
				}
				assert enhancedFor != null;
				for (LambdaExpressionObject lambda : lambdas) {
					for (VariableDeclaration parameter : lambda.getParameters()) {
						String variableName = parameter.getVariableName();
						VariableDeclaration variableDeclaration = enhancedFor.getVariableDeclaration(variableName);
						if (variableDeclaration == null)
							continue;
						Tree srcNode = TreeUtilFunctions.findByLocationInfo(srcTree,parameter.getLocationInfo());
						Tree dstNode = TreeUtilFunctions.findByLocationInfo(dstTree,variableDeclaration.getLocationInfo());
						new LeafMatcher(false).match(srcNode,dstNode,null,mappingStore);
					}
				}
				Tree srcSt = TreeUtilFunctions.findByLocationInfo(srcTree,next.getLocationInfo());
				Tree dstSt = TreeUtilFunctions.findByLocationInfo(dstTree,enhancedFor.getLocationInfo());
				mappingStore.addMapping(srcSt,dstSt);
			}
			if (refactoring instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) (refactoring);
				UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
				fromRefMiner(srcTree,dstTree,bodyMapper,mappingStore);
			}
			if (refactoring instanceof InlineOperationRefactoring) {
				InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) (refactoring);
				UMLOperationBodyMapper bodyMapper = inlineOperationRefactoring.getBodyMapper();
				fromRefMiner(srcTree,dstTree,bodyMapper,mappingStore);
			}
			else if (refactoring instanceof RenameAttributeRefactoring) {
				RenameAttributeRefactoring renameAttributeRefactoring = (RenameAttributeRefactoring) (refactoring);
				Tree srcAttrTree =TreeUtilFunctions.findByLocationInfo(srcTree,renameAttributeRefactoring.getOriginalAttribute().getLocationInfo()).getParent(); //Super Risky
				Tree dstAttrTree =TreeUtilFunctions.findByLocationInfo(dstTree,renameAttributeRefactoring.getRenamedAttribute().getLocationInfo()).getParent(); //Super Risky
				//if (dstAttrTree.isIsomorphicTo(srcAttrTree))
				//	mappingStore.addMappingRecursively(srcAttrTree.getParent(),dstAttrTree.getParent());
				processFieldDeclaration(srcAttrTree,dstAttrTree,renameAttributeRefactoring.getOriginalAttribute(),renameAttributeRefactoring.getRenamedAttribute(),mappingStore);
			}
			else if (refactoring instanceof ExtractVariableRefactoring) {
				//ExtractVariableRefactoring extractVariableRefactoring = (ExtractVariableRefactoring)refactoring;
				//AbstractExpression initializer = extractVariableRefactoring.getVariableDeclaration().getInitializer();
				//for (AbstractCodeMapping reference : extractVariableRefactoring.getReferences()) {
				//
				//}
			}
			else if (refactoring instanceof MergeVariableRefactoring)
			{
				MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring)refactoring;
				Set<VariableDeclaration> mergedVariables = mergeVariableRefactoring.getMergedVariables();
				VariableDeclaration newVariable = mergeVariableRefactoring.getNewVariable();
				Tree dstVariableType =TreeUtilFunctions.findByLocationInfo(dstTree,newVariable.getType().getLocationInfo());
				Tree dstVariableDeclaration =TreeUtilFunctions.findByLocationInfo(dstTree,newVariable.getLocationInfo());
				List<Tree> dstChildrenList = dstVariableDeclaration.getChildren();
				Tree dstVarName = dstChildrenList.get(dstChildrenList.size() - 1);
				for (VariableDeclaration variableDeclaration : mergedVariables)
				{
					Tree srcVariableDeclaration =TreeUtilFunctions.findByLocationInfo(srcTree,variableDeclaration.getLocationInfo());
					Tree srcVariableType =TreeUtilFunctions.findByLocationInfo(srcTree,variableDeclaration.getType().getLocationInfo());
					List<Tree> srcChildrenList = srcVariableDeclaration.getChildren();
					Tree srcVarName = srcChildrenList.get(srcChildrenList.size() - 1);
					//mappingStore.addMapping(srcVariableDeclaration,dstVariableDeclaration);
					mappingStore.addMapping(srcVariableType,dstVariableType.getChild(0));
					mappingStore.addMapping(srcVarName,dstVarName);
				}
			}
			else if (refactoring instanceof SplitConditionalRefactoring)
			{
				SplitConditionalRefactoring splitConditionalRefactoring = (SplitConditionalRefactoring) refactoring;
				Set<AbstractCodeFragment> splitConditionals = splitConditionalRefactoring.getSplitConditionals();
				for (AbstractCodeFragment splitConditional : splitConditionals) {
					new CompositeMatcher().match(srcTree,dstTree, (AbstractStatement) splitConditionalRefactoring.getOriginalConditional(), (AbstractStatement) splitConditional,mappingStore);
				}
			}
			else if (refactoring instanceof MergeConditionalRefactoring)
			{
				MergeConditionalRefactoring mergeConditionalRefactoring = (MergeConditionalRefactoring) refactoring;
				Set<AbstractCodeFragment> mergedConditionals = mergeConditionalRefactoring.getMergedConditionals();
				for (AbstractCodeFragment eachMerged : mergedConditionals) {
					new CompositeMatcher().match(srcTree,dstTree,
							(AbstractStatement) eachMerged, (AbstractStatement) mergeConditionalRefactoring.getNewConditional()
							,mappingStore);
				}
			}
			else if (refactoring instanceof MergeCatchRefactoring)
			{
				MergeCatchRefactoring mergeCatchRefactoring = (MergeCatchRefactoring) refactoring;
				for (AbstractCodeFragment eachMerged : mergeCatchRefactoring.getMergedCatchBlocks()) {
					new CompositeMatcher().match(srcTree,dstTree,
							(AbstractStatement) eachMerged, (AbstractStatement) mergeCatchRefactoring.getNewCatchBlock()
							,mappingStore);
				}
			}
		}
	}

	private void processClassImplementedInterfaces(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		List<UMLType> srcImplementedInterfaces = classDiff.getOriginalClass().getImplementedInterfaces();
		List<UMLType> dstImplementedInterfaces = classDiff.getNextClass().getImplementedInterfaces();
		List<UMLType> removedOnes = classDiff.getRemovedImplementedInterfaces();
		for (UMLType srcUmlType : srcImplementedInterfaces) {
			if (!removedOnes.contains(srcUmlType))
			{
				Tree srcInterfaceTree =TreeUtilFunctions.findByLocationInfo(srcTree,srcUmlType.getLocationInfo());
				for (UMLType dstUmlType : dstImplementedInterfaces) {
					if (dstUmlType.getClassType().equals(srcUmlType.getClassType()))
					{
						Tree dstInterfaceTree =TreeUtilFunctions.findByLocationInfo(dstTree,dstUmlType.getLocationInfo());
						mappingStore.addMappingRecursively(srcInterfaceTree, dstInterfaceTree);
						break;
					}
				}
			}
		}
	}

	private void processClassAttributes(Tree srcTree, Tree dstTree, UMLAbstractClassDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		Set<org.apache.commons.lang3.tuple.Pair<UMLAttribute, UMLAttribute>> pairs = classDiff.getCommonAtrributes();
		for (org.apache.commons.lang3.tuple.Pair<UMLAttribute, UMLAttribute> pair : pairs) {
			processFieldDeclaration(srcTree,dstTree,pair.getLeft(),pair.getRight(),mappingStore);
		}
	}

	private List<Pair<UMLAttribute, UMLAttribute>> findMatchedAttributesPair(UMLClassBaseDiff classDiff) {
		List<Pair<UMLAttribute,UMLAttribute>> pairs = new ArrayList<>();
		List<UMLAttribute> srcAttributes = classDiff.getOriginalClass().getAttributes();
		List<UMLAttribute> dstAttributes = classDiff.getNextClass().getAttributes();
		List<UMLAttribute> removedOnes = classDiff.getRemovedAttributes();
		for (UMLAttribute srcUmlType : srcAttributes) {
			if (!removedOnes.contains(srcUmlType))
			{
				for (UMLAttribute dstUmlType : dstAttributes) {
					if (dstUmlType.getName().equals(srcUmlType.getName()))
					{
						pairs.add(new Pair<>(srcUmlType,dstUmlType));
						break;
					}
				}
			}
		}
		return pairs;
	}

	private void processFieldDeclaration(Tree srcTree, Tree dstTree, UMLAttribute srcUMLAttribute,UMLAttribute dstUMLAttribute, ExtendedMultiMappingStore mappingStore)
	{
		Tree srcFieldDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLAttribute.getLocationInfo()).getParent(); //TODO
		Tree dstFieldDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLAttribute.getLocationInfo()).getParent(); //TODO
		if (srcFieldDeclaration.getMetrics().hash == dstFieldDeclaration.getMetrics().hash ||
				srcFieldDeclaration.isIsoStructuralTo(dstFieldDeclaration))
		{
			// TODO: 8/3/2022 isoStructural can't be a good idea here, i.e anonymous class
			mappingStore.addMappingRecursively(srcFieldDeclaration,dstFieldDeclaration);
			return;
		}
		mappingStore.addMapping(srcFieldDeclaration,dstFieldDeclaration);
		matchFieldAllModifiers(srcFieldDeclaration,dstFieldDeclaration,srcUMLAttribute,dstUMLAttribute,mappingStore);
		Tree srcType = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLAttribute.getType().getLocationInfo());
		Tree dstType = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLAttribute.getType().getLocationInfo());
		if (srcType.isIsoStructuralTo(dstType)) mappingStore.addMappingRecursively(srcType,dstType);
		Tree srcVarDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLAttribute.getVariableDeclaration().getLocationInfo());
		Tree dstVarDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLAttribute.getVariableDeclaration().getLocationInfo());
		mappingStore.addMapping(srcVarDeclaration,dstVarDeclaration);
		new LeafMatcher(false).match(srcVarDeclaration,dstVarDeclaration,null,mappingStore);
		processAttributeJavaDoc(srcTree,dstTree,srcUMLAttribute.getJavadoc(),dstUMLAttribute.getJavadoc(),mappingStore);
		mappingStore.addMapping(srcVarDeclaration.getChild(0),dstVarDeclaration.getChild(0));
	}

	private void processAttributeJavaDoc(Tree srcTree,Tree dstTree, UMLJavadoc srcJavaDoc, UMLJavadoc dstJavaDoc, ExtendedMultiMappingStore mappingStore) {
		if (srcJavaDoc == null || dstJavaDoc == null) return;
		Tree srcJavaDocTree = TreeUtilFunctions.findByLocationInfo(srcTree,srcJavaDoc.getLocationInfo());
		Tree dstJavaDocTree = TreeUtilFunctions.findByLocationInfo(dstTree,dstJavaDoc.getLocationInfo());
		if (srcJavaDocTree != null && dstJavaDocTree != null)
			if (srcJavaDocTree.isIsoStructuralTo(dstJavaDocTree))
				mappingStore.addMappingRecursively(srcJavaDocTree,dstJavaDocTree);
	}

	private void matchFieldAllModifiers(Tree srcFieldDeclaration, Tree dstFieldDeclaration, UMLAttribute srcUMLAttribute, UMLAttribute dstUMLAttribute, ExtendedMultiMappingStore mappingStore) {
		//Pair<Tree, Tree> attributeAccessModifierPair = findAttributeAccessModifierPair(srcFieldDeclaration, dstFieldDeclaration, srcUMLAttribute, dstUMLAttribute);
		//if (attributeAccessModifierPair.first != null && attributeAccessModifierPair.second != null)
		//	mappingStore.addMapping(attributeAccessModifierPair.first, attributeAccessModifierPair.second);

		String visibility1 = srcUMLAttribute.getVisibility();
		String visibility2 = dstUMLAttribute.getVisibility();
		if (visibility1 != null && visibility2 != null)
		{
			matchModifierForFeild(srcFieldDeclaration,dstFieldDeclaration,visibility1,mappingStore);
		}
		if (srcUMLAttribute.isFinal() && dstUMLAttribute.isFinal())
			matchModifierForFeild(srcFieldDeclaration,dstFieldDeclaration,"final",mappingStore);
		if (srcUMLAttribute.isFinal() && dstUMLAttribute.isVolatile())
			matchModifierForFeild(srcFieldDeclaration,dstFieldDeclaration,"volatile",mappingStore);
		if (srcUMLAttribute.isFinal() && dstUMLAttribute.isStatic())
			matchModifierForFeild(srcFieldDeclaration,dstFieldDeclaration,"static",mappingStore);
		if (srcUMLAttribute.isFinal() && dstUMLAttribute.isTransient())
			matchModifierForFeild(srcFieldDeclaration,dstFieldDeclaration,"transient",mappingStore);
	}

	private void matchModifierForFeild(Tree srcFieldDeclaration, Tree dstFieldDeclaration, String modifier, ExtendedMultiMappingStore mappingStore) {
		Tree srcModifierTree = findAttributeModifierByLabel(srcFieldDeclaration, modifier);
		Tree dstModifierTree = findAttributeModifierByLabel(dstFieldDeclaration, modifier);
		if (srcModifierTree != null && dstModifierTree != null)
			mappingStore.addMapping(srcModifierTree,dstModifierTree);
	}

	private Pair<Tree, Tree> findAttributeAccessModifierPair(Tree srcFieldDeclaration, Tree dstFieldDeclaration, UMLAttribute srcUMLAttribute, UMLAttribute dstUMLAttribute) {
		Tree srcAccessModifier = findAttributeAccessModifier(srcFieldDeclaration);
		Tree dstAccessModifier = findAttributeAccessModifier(dstFieldDeclaration);
		return new Pair<>(srcAccessModifier,dstAccessModifier);
	}

	private Tree findAttributeAccessModifier(Tree anyFieldDeclaration) {
		if (anyFieldDeclaration.getChildren().size() > 0)
		{
			for (Tree child : anyFieldDeclaration.getChildren())
			{
				if (child.getType().name.equals("AccessModifier"))
					return child;
				if (child.getType().name.equals("VariableDeclarationFragment"))
					break;
			}
		}
		return null;
	}

	private Tree findAttributeModifierByLabel(Tree anyFieldDeclaration,String label) {
		if (anyFieldDeclaration.getChildren().size() > 0)
		{
			for (Tree child : anyFieldDeclaration.getChildren())
			{
				if (child.getLabel().equals(label))
					return child;
				if (child.getType().name.equals("VariableDeclarationFragment"))
					break;
			}
		}
		return null;
	}
	private Tree findAttributeTreeByType(Tree anyFieldDeclaration,String type) {
		if (anyFieldDeclaration.getChildren().size() > 0)
		{
			for (Tree child : anyFieldDeclaration.getChildren())
			{
				if (child.getType().name.equals(type))
					return child;
				if (child.getType().name.equals("VariableDeclarationFragment"))
					break;
			}
		}
		return null;
	}

	private void processSuperClass(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		UMLType srcParentUML = classDiff.getOldSuperclass();
		UMLType dstParentUML = classDiff.getNewSuperclass();
		if (srcParentUML != null && dstParentUML != null) {
			Tree srcParentClassTree =TreeUtilFunctions.findByLocationInfo(srcTree, srcParentUML.getLocationInfo());
			Tree dstParentClassTree =TreeUtilFunctions.findByLocationInfo(dstTree, dstParentUML.getLocationInfo());
			if (srcParentClassTree.isIsomorphicTo(dstParentClassTree))
				mappingStore.addMappingRecursively(srcParentClassTree,dstParentClassTree);
		}
	}

	private void processPackageDeclaration(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		//TODO: In current implementation, I assumed that these two package-statements are matched since they both belong to the same class
		//TODO : Question: Can single file have multiple package declaration? if yes, I have to use list of pairs
		Tree srcPackageDeclaration = findPackageDeclaration(srcTree);
		Tree dstPackageDeclaration = findPackageDeclaration(dstTree);
		if (srcPackageDeclaration != null && dstPackageDeclaration != null)
			mappingStore.addMappingRecursively(srcPackageDeclaration,dstPackageDeclaration);
	}

	private Tree findPackageDeclaration(Tree inputTree) {
		String searchingType = "PackageDeclaration";
		if (!inputTree.getChildren().isEmpty())
		{
			List<Tree> children = inputTree.getChildren();
			for(Tree child: children)
			{
				if (child.getType().name.equals(searchingType))
					return child;
			}
		}
		return null;
	}

	private void processImports(Tree srcTree, Tree dstTree, UMLImportListDiff importDiffList, ExtendedMultiMappingStore mappingStore) {
		if (importDiffList == null) return;
		Set<UMLImport> commonImports = importDiffList.getCommonImports();
		if (commonImports.isEmpty())
			return;
		String searchingType = "ImportDeclaration";
		List<Tree> srcChildren = srcTree.getChildren();
		List<Tree> dstChildren = dstTree.getChildren();

		for(UMLImport label : commonImports){
			Tree srcImportStatement = findImportByTypeAndLabel(srcChildren,searchingType,label);
			Tree dstImportStatement = findImportByTypeAndLabel(dstChildren,searchingType,label);
			if (srcImportStatement != null && dstImportStatement != null)
				mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
		}
	}

	private Tree findImportByTypeAndLabel(List<Tree> inputTree, String searchingType, UMLImport label) {
		for (Tree srcStatement: inputTree) {
			if (srcStatement.getType().name.equals(searchingType)) {
				if (srcStatement.getChild(0).getLabel().equals(label.getName())) //TODO getChild 0 will cause a lot of problem
					return srcStatement;
			}
		}
		return null;
	}

	private void processMethodSignature(Tree srcOperationNode, Tree dstOperationNode, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore) {
		List<String> searchingTypes = new ArrayList<>();
		//searchingTypes.add("AccessModifier");
		searchingTypes.add("SimpleName");
		searchingTypes.add("PrimitiveType");
		searchingTypes.add("Block");
		for (String type : searchingTypes) {
			Pair<Tree,Tree> matched = matchBasedOnType(srcOperationNode,dstOperationNode,type);
			if (matched != null)
				mappingStore.addMapping(matched.first,matched.second);
		}
		if (umlOperationBodyMapper.getOperation1() != null && umlOperationBodyMapper.getOperation2() != null) {
			if (umlOperationBodyMapper.getOperation1().isStatic() && umlOperationBodyMapper.getOperation2().isStatic())
				matchModifier(srcOperationNode, dstOperationNode, "static", mappingStore);
			if (umlOperationBodyMapper.getOperation1().isFinal() && umlOperationBodyMapper.getOperation2().isFinal())
				matchModifier(srcOperationNode, dstOperationNode, "final", mappingStore);
			if (umlOperationBodyMapper.getOperation1().isAbstract() && umlOperationBodyMapper.getOperation2().isAbstract())
				matchModifier(srcOperationNode, dstOperationNode, "abstract", mappingStore);
			if (umlOperationBodyMapper.getOperation1().isSynchronized() && umlOperationBodyMapper.getOperation2().isSynchronized())
				matchModifier(srcOperationNode, dstOperationNode, "synchronized", mappingStore);
			if (umlOperationBodyMapper.getOperation1().getVisibility() != null || umlOperationBodyMapper.getOperation1().getVisibility().equals("==")
					&&
					umlOperationBodyMapper.getOperation2().getVisibility() != null || umlOperationBodyMapper.getOperation2().getVisibility().equals("=="))
			{
				String v1 = umlOperationBodyMapper.getOperation1().getVisibility();
				String v2 = umlOperationBodyMapper.getOperation2().getVisibility();
				Tree tree1 = TreeUtilFunctions.findChildByTypeAndLabel(srcOperationNode, "Modifier", v1);
				Tree tree2 = TreeUtilFunctions.findChildByTypeAndLabel(dstOperationNode, "Modifier", v2);
				if (tree1 != null && tree2 != null)
					mappingStore.addMappingRecursively(tree1,tree2);
			}
		}
	}

	private Pair<Tree, Tree> matchBasedOnType(Tree srcOperationNode, Tree dstOperationNode, String searchingType) {
		Tree srcModifier = TreeUtilFunctions.findChildByType(srcOperationNode,searchingType);
		Tree dstModifier = TreeUtilFunctions.findChildByType(dstOperationNode,searchingType);
		if (srcModifier != null && dstModifier != null)
			return new Pair<>(srcModifier, dstModifier);
		return null;
	}

	private void processClassDeclarationMapping(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		String AST_type = "TypeDeclaration";
		if (classDiff.getOriginalClass().isEnum()) AST_type = "EnumDeclaration";
		Tree srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),AST_type);
		Tree dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),AST_type);
		mappingStore.addMapping(srcTypeDeclaration,dstTypeDeclaration);
		List<String> searchingTypes = new ArrayList<>();

		if (classDiff.getOriginalClass().getVisibility() != null || classDiff.getOriginalClass().getVisibility().equals("==")
				&&
				classDiff.getNextClass().getVisibility() != null || classDiff.getNextClass().getVisibility().equals("=="))
		{
			String v1 = classDiff.getOriginalClass().getVisibility();
			String v2 = classDiff.getNextClass().getVisibility();
			Tree tree1 = TreeUtilFunctions.findChildByTypeAndLabel(srcTypeDeclaration, "Modifier", v1);
			Tree tree2 = TreeUtilFunctions.findChildByTypeAndLabel(dstTypeDeclaration, "Modifier", v2);
			if (tree1 != null && tree2 != null)
				mappingStore.addMappingRecursively(tree1,tree2);
		}

		searchingTypes.add("SimpleName");
		searchingTypes.add("TYPE_DECLARATION_KIND");
		for (String type : searchingTypes) {
			Pair<Tree,Tree> matched = matchBasedOnType(srcTypeDeclaration,dstTypeDeclaration,type);
			if (matched != null)
				mappingStore.addMapping(matched.first,matched.second);
		}
		if (classDiff.getOriginalClass().isStatic() && classDiff.getNextClass().isStatic())
			matchModifier(srcTypeDeclaration,dstTypeDeclaration,"static",mappingStore);
		if (classDiff.getOriginalClass().isFinal() && classDiff.getNextClass().isFinal())
			matchModifier(srcTypeDeclaration,dstTypeDeclaration,"final",mappingStore);
		if (classDiff.getOriginalClass().isAbstract() && classDiff.getNextClass().isAbstract())
			matchModifier(srcTypeDeclaration,dstTypeDeclaration,"abstract",mappingStore);

		for (org.apache.commons.lang3.tuple.Pair<UMLTypeParameter, UMLTypeParameter> commonTypeParamSet : classDiff.getTypeParameterDiffList().getCommonTypeParameters()) {
			Tree srcTypeParam = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, commonTypeParamSet.getLeft().getLocationInfo());
			Tree dstTypeParam = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, commonTypeParamSet.getRight().getLocationInfo());
			mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
		}
		processSuperClass(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
		processClassImplementedInterfaces(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
		processClassAttributes(srcTree,dstTree,classDiff,mappingStore);
		processClassJavaDocs(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
		processClassAnnotations(srcTypeDeclaration,dstTypeDeclaration,classDiff.getAnnotationListDiff(),mappingStore);

	}

	private void matchModifier(Tree srcTypeDeclaration, Tree dstTypeDeclaration, String modifier, ExtendedMultiMappingStore mappingStore) {
		// TODO: 8/3/2022 Search should be limited to the method signature before the method name
		String type = "Modifier";
		Tree srcTree = TreeUtilFunctions.findChildByTypeAndLabel(srcTypeDeclaration,type,modifier);
		Tree dstTree = TreeUtilFunctions.findChildByTypeAndLabel(dstTypeDeclaration,type,modifier);
		if (srcTree != null && dstTree != null){
			mappingStore.addMapping(srcTree,dstTree);
		}
	}
}
