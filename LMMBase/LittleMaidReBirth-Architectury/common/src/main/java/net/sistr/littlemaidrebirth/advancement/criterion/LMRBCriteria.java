package net.sistr.littlemaidrebirth.advancement.criterion;

import net.minecraft.advancements.CriteriaTriggers;

public class LMRBCriteria {
    public static ContractMaidCriterion CONTRACT_MAID;
    public static ResurrectMaidCriterion RESURRECT_MAID;

    public static void init() {
        CONTRACT_MAID = CriteriaTriggers.register(ContractMaidCriterion.ID.toString(), new ContractMaidCriterion());
        RESURRECT_MAID = CriteriaTriggers.register(ResurrectMaidCriterion.ID.toString(), new ResurrectMaidCriterion());
    }
}
