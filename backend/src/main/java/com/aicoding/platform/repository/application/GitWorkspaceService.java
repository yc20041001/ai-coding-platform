package com.aicoding.platform.repository.application;

import com.aicoding.platform.repository.dto.RepositoryBranchResponse;
import com.aicoding.platform.repository.dto.RepositoryDiffResponse;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GitWorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(GitWorkspaceService.class);

    private final String workspaceRoot;

    public GitWorkspaceService(WorkspaceProperties properties) {
        this.workspaceRoot = properties.getRootPath();
    }

    public Path getRepoPath(Long projectId) {
        return Path.of(workspaceRoot, "projects", projectId.toString(), "repo");
    }

    public void cloneRepository(String cloneUrl, Path repoPath, String branch, boolean force)
            throws GitAPIException, IOException {
        File repoDir = repoPath.toFile();
        if (repoDir.exists()) {
            if (force) {
                deleteDirectory(repoDir);
            } else {
                throw new IOException("仓库目录已存在: " + repoPath + "，请设置 force=true 强制覆盖");
            }
        }

        log.info("Cloning {} branch {} into {}", cloneUrl, branch, repoPath);
        try (Git git = Git.cloneRepository()
                .setURI(cloneUrl)
                .setDirectory(repoDir)
                .setBranch(branch)
                .call()) {
            log.debug("Cloned git directory: {}", git.getRepository().getDirectory());
            log.info("Clone completed: {}", repoPath);
        }
    }

    public void pullRepository(Path repoPath, String branch) throws GitAPIException, IOException {
        try (Git git = Git.open(repoPath.toFile())) {
            git.checkout().setName(branch).call();
            git.pull().call();
            log.info("Pull completed: {} branch {}", repoPath, branch);
        }
    }

    public List<RepositoryBranchResponse> listBranches(Path repoPath) throws GitAPIException, IOException {
        try (Git git = Git.open(repoPath.toFile())) {
            List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();

            List<RepositoryBranchResponse> localBranches = new ArrayList<>();
            List<RepositoryBranchResponse> remoteBranches = new ArrayList<>();

            for (Ref ref : branches) {
                String fullName = ref.getName();
                String shortName;
                boolean isLocal;

                if (fullName.startsWith("refs/heads/")) {
                    shortName = fullName.substring("refs/heads/".length());
                    isLocal = true;
                } else if (fullName.startsWith("refs/remotes/")) {
                    String remaining = fullName.substring("refs/remotes/".length());
                    int slashIdx = remaining.indexOf('/');
                    shortName = slashIdx >= 0 ? remaining.substring(slashIdx + 1) : remaining;
                    isLocal = false;
                } else {
                    continue;
                }

                RepositoryBranchResponse branch = new RepositoryBranchResponse();
                branch.setName(shortName);
                ObjectId objectId = ref.getObjectId();
                if (objectId != null) {
                    branch.setCommitHash(objectId.getName());
                }
                if (isLocal) {
                    localBranches.add(branch);
                } else {
                    remoteBranches.add(branch);
                }
            }

            Set<String> seen = new HashSet<>();
            List<RepositoryBranchResponse> result = new ArrayList<>();

            for (RepositoryBranchResponse b : localBranches) {
                seen.add(b.getName());
                result.add(b);
            }
            for (RepositoryBranchResponse b : remoteBranches) {
                if (!seen.contains(b.getName())) {
                    seen.add(b.getName());
                    result.add(b);
                }
            }

            return result;
        }
    }

    public RepositoryDiffResponse getDiff(Path repoPath, String base, String head)
            throws GitAPIException, IOException {
        try (Git git = Git.open(repoPath.toFile());
             Repository repo = git.getRepository()) {

            AbstractTreeIterator oldTree = prepareTreeParser(repo, base);
            AbstractTreeIterator newTree = prepareTreeParser(repo, head);

            List<DiffEntry> diffs = git.diff()
                    .setOldTree(oldTree)
                    .setNewTree(newTree)
                    .call();

            RepositoryDiffResponse response = new RepositoryDiffResponse();
            response.setBase(base);
            response.setHead(head);
            List<RepositoryDiffResponse.DiffFile> files = new ArrayList<>();

            for (DiffEntry entry : diffs) {
                RepositoryDiffResponse.DiffFile diffFile = new RepositoryDiffResponse.DiffFile();
                diffFile.setPath(entry.getNewPath());
                diffFile.setChangeType(entry.getChangeType().name());
                diffFile.setPatch(formatPatch(entry));

                files.add(diffFile);
            }
            response.setFiles(files);
            return response;
        }
    }

    private AbstractTreeIterator prepareTreeParser(Repository repo, String ref) throws IOException {
        try (RevWalk walk = new RevWalk(repo)) {
            Ref refObj = repo.exactRef("refs/heads/" + ref);
            if (refObj == null) {
                refObj = repo.exactRef("refs/remotes/origin/" + ref);
            }
            if (refObj == null) {
                throw new IOException("引用不存在: " + ref);
            }
            RevCommit commit = walk.parseCommit(refObj.getObjectId());
            RevTree tree = walk.parseTree(commit.getTree().getId());
            CanonicalTreeParser parser = new CanonicalTreeParser();
            try (ObjectReader reader = repo.newObjectReader()) {
                parser.reset(reader, tree.getId());
            }
            return parser;
        }
    }

    private String formatPatch(DiffEntry entry) {
        // Return a short patch indicator since full patch can be very large
        return "@@ " + entry.getOldPath() + " -> " + entry.getNewPath() + " @@" +
                " (type: " + entry.getChangeType().name() + ")";
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }
}
