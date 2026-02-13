package com.dietcoach.project.service;

import com.dietcoach.project.dto.shopping.BudgetProposalRequest;
import com.dietcoach.project.dto.shopping.BudgetProposalResponse;

public interface BudgetService {
    BudgetProposalResponse createProposal(BudgetProposalRequest request);
}
