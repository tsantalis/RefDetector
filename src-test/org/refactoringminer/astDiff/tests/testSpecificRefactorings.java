package org.refactoringminer.astDiff.tests;

import com.github.gumtreediff.matchers.Mapping;
import org.junit.Test;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.util.Set;

import static org.junit.Assert.assertTrue;

/* Created by pourya on 2023-02-28 4:48 p.m. */
public class testSpecificRefactorings {

    @Test
    public void testRenameParameter() {

        String m1 = "SingleVariableDeclaration [3886,3897] -> SingleVariableDeclaration [2778,2797]";
        String m2 = "PrimitiveType: long [3886,3890] -> PrimitiveType: long [2778,2782]";
        String m3 = "SimpleName: millis [3891,3897] -> SimpleName: durationMillis [2783,2797]";
        String url = "https://github.com/apache/commons-lang/commit/5111ae7db08a70323a51a21df0bbaf46f21e072e";
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);
        Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, commit, 1000);
        for (ASTDiff astDiff : astDiffs) {
            System.out.println();
            if (!astDiff.getSrcPath().equals("src/java/org/apache/commons/lang/time/DurationFormatUtils.java"))
                continue;
            Set<Mapping> mappings = astDiff.getMultiMappings().getMappings();
            boolean m1Check = false, m2Check = false, m3Check = false;
            for (Mapping mapping : mappings) {
                if (mapping.toString().equals(m1)) m1Check = true;
                if (mapping.toString().equals(m2)) m2Check = true;
                if (mapping.toString().equals(m3)) m3Check = true;
            }
            assertTrue("SingleVariableDeclaration For RenameParameter Refactoring ",m1Check);
            assertTrue("PrimitiveType Long For RenameParameter Refactoring ",m2Check);
            assertTrue("SimpleName For RenameParameter Refactoring ",m3Check);
        }

    }

}