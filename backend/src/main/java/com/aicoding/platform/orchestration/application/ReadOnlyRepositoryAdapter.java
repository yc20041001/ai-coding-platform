package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.dto.ReadOnlyRepositoryRequest;
import com.aicoding.platform.orchestration.dto.RepositoryBranchResult;
import com.aicoding.platform.orchestration.dto.RepositoryDiffSummaryResult;
import com.aicoding.platform.orchestration.dto.RepositoryFileSnippetResult;
import com.aicoding.platform.orchestration.dto.RepositoryTreeResult;

public interface ReadOnlyRepositoryAdapter {

    RepositoryTreeResult listTree(ReadOnlyRepositoryRequest request);

    RepositoryFileSnippetResult readSnippet(ReadOnlyRepositoryRequest request);

    RepositoryBranchResult listBranches(ReadOnlyRepositoryRequest request);

    RepositoryDiffSummaryResult readDiffSummary(ReadOnlyRepositoryRequest request);
}
