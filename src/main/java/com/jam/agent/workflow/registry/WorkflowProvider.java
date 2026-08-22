package com.jam.agent.workflow.registry;

import com.jam.agent.workflow.definition.WorkflowDefinition;
import java.util.List;

/** Supplies workflow definitions to the application registry. */
public interface WorkflowProvider {

    List<WorkflowDefinition> definitions();
}
