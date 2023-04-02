package org.refactoringminer.astDiff.tests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.File;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.refactoringminer.astDiff.utils.UtilMethods.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */

@RunWith(Parameterized.class)
public class AllCasesWithLocallyClonedReposTest {
	private static final String REPOS = "tmp1";
	private static GitService gitService = new GitServiceImpl();
	
    @Parameterized.Parameter(0)
    public String repo;
    @Parameterized.Parameter(1)
    public String commit;
    @Parameterized.Parameter(2)
    public String srcFileName;
    @Parameterized.Parameter(3)
    public String expected;
    @Parameterized.Parameter(4)
    public String actual;

    @Test
    public void testChecker() {
        String msg = String.format("Failed for %s/commit/%s , srcFileName: %s",repo.replace(".git",""),commit,srcFileName);
        assertEquals(msg, expected.length(),actual.length());
        assertEquals(msg, expected,actual);
    }

    @Parameterized.Parameters(name= "{index}: File: {2}, Repo: {0}, Commit: {1}")
    public static Iterable<Object[]> initDataWithClonedRepos() throws Exception {
        List<Object[]> allCases = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile = getCommitsMappingsPath() + getTestInfoFile();
        List<CaseInfo> infos = mapper.readValue(new File(jsonFile), new TypeReference<List<CaseInfo>>(){});
        for (CaseInfo info : infos) {
            List<String> expectedFilesList = new ArrayList<>(List.of(Objects.requireNonNull(new File(getFinalFolderPath(getCommitsMappingsPath(), info.getRepo(), info.getCommit())).list())));

            String repoFolder = info.getRepo().substring(info.getRepo().lastIndexOf("/"), info.getRepo().indexOf(".git"));
            Repository repo = gitService.cloneIfNotExists(REPOS + repoFolder, info.getRepo());
            Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, info.getCommit());

            makeAllCases(allCases, info, expectedFilesList, astDiffs);
        }
        return allCases;
    }

//    @Parameterized.Parameters(name= "{index}: File: {2}, Repo: {0}, Commit: {1}")
    public static Iterable<Object[]> initDataWithGithub() throws Exception {
        List<Object[]> allCases = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile = getCommitsMappingsPath() + getTestInfoFile();
        List<CaseInfo> infos = mapper.readValue(new File(jsonFile), new TypeReference<List<CaseInfo>>(){});
        for (CaseInfo info : infos) {
            List<String> expectedFilesList = new ArrayList<>(List.of(Objects.requireNonNull(new File(getFinalFolderPath(getCommitsMappingsPath(), info.getRepo(), info.getCommit())).list())));

            Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(info.getRepo(), info.getCommit(), 1000);

            makeAllCases(allCases, info, expectedFilesList, astDiffs);
        }
        return allCases;
    }
}
