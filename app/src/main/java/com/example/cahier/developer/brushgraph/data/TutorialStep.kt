/*
 * Copyright 2026 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.cahier.developer.brushgraph.data

import com.example.cahier.R
import ink.proto.BrushBehavior

/**
 * Represents a step in the guided tutorial.
 */
data class TutorialStep(
    val title: Int,
    val message: Int,
    val anchor: TutorialAnchor,
    val actionRequired: TutorialAction,
    val getTargetNode: (BrushGraph) -> GraphNode? = { null },
)

enum class TutorialAnchor {
    SCREEN_CENTER,
    FAB,
    NODE_CANVAS,
    INSPECTOR,
    TEST_CANVAS,
    ACTION_BAR,
    NOTIFICATION_ICON
}

enum class TutorialAction {
    CLICK_NEXT,
    CONNECT_NODES,
    SELECT_NODE,
    EDIT_FIELD,
    DRAW_ON_CANVAS,
    DELETE_NODE,
    SELECT_EDGE,
    USE_ACTION_BAR,
    CHECK_NOTIFICATIONS,
    ADD_BEHAVIOR,
    CLICK_NOTIFICATION,
    CLICK_ERROR_LINK,
    ADD_INPUT_FAB,
    MOVE_NODE,
    EXIT_INSPECTOR,
    EDIT_DROPDOWN,
    ADD_NODE_BETWEEN,
    LONG_PRESS_NODE,
    DUPLICATE_NODES,
    SWAP_PORTS,
    ADD_COLOR,
    CLICK_DONE
}

val TUTORIAL_STEPS = listOf(
    TutorialStep(
        title = R.string.bg_tutorial_welcome_title,
        message = R.string.bg_tutorial_welcome_message,
        anchor = TutorialAnchor.SCREEN_CENTER,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_test_canvas_title,
        message = R.string.bg_tutorial_test_canvas_message,
        anchor = TutorialAnchor.TEST_CANVAS,
        actionRequired = TutorialAction.DRAW_ON_CANVAS
    ),
    TutorialStep(
        title = R.string.bg_tutorial_test_canvas_options_title,
        message = R.string.bg_tutorial_test_canvas_options_message,
        anchor = TutorialAnchor.TEST_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_edit_tip_title,
        message = R.string.bg_tutorial_edit_tip_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph -> graph.nodes.find { it.data is NodeData.Tip } }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_modify_tip_shape_title,
        message = R.string.bg_tutorial_modify_tip_shape_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = R.string.bg_tutorial_live_strokes_title,
        message = R.string.bg_tutorial_live_strokes_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_inspector_features_title,
        message = R.string.bg_tutorial_inspector_features_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_exit_inspector_title,
        message = R.string.bg_tutorial_exit_inspector_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EXIT_INSPECTOR
    ),
    TutorialStep(
        title = R.string.bg_tutorial_brush_behaviors_title,
        message = R.string.bg_tutorial_brush_behaviors_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_add_behavior_title,
        message = R.string.bg_tutorial_add_behavior_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.ADD_BEHAVIOR,
        getTargetNode = { graph -> graph.nodes.find { it.data is NodeData.Tip } }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_error_title,
        message = R.string.bg_tutorial_error_message,
        anchor = TutorialAnchor.NOTIFICATION_ICON,
        actionRequired = TutorialAction.CLICK_NOTIFICATION
    ),
    TutorialStep(
        title = R.string.bg_tutorial_notification_pane_title,
        message = R.string.bg_tutorial_notification_pane_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_navigate_to_nodes_with_issues_title,
        message = R.string.bg_tutorial_navigate_to_nodes_with_issues_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_ERROR_LINK
    ),
    TutorialStep(
        title = R.string.bg_tutorial_add_input_title,
        message = R.string.bg_tutorial_add_input_message,
        anchor = TutorialAnchor.FAB,
        actionRequired = TutorialAction.ADD_INPUT_FAB
    ),
    TutorialStep(
        title = R.string.bg_tutorial_change_node_type_title,
        message = R.string.bg_tutorial_change_node_type_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_DROPDOWN,
        getTargetNode = { graph -> graph.nodes.lastOrNull() }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_move_node_title,
        message = R.string.bg_tutorial_move_node_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.MOVE_NODE
    ),
    TutorialStep(
        title = R.string.bg_tutorial_connect_nodes_title,
        message = R.string.bg_tutorial_connect_nodes_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CONNECT_NODES
    ),
    TutorialStep(
        title = R.string.bg_tutorial_edit_target_title,
        message = R.string.bg_tutorial_edit_target_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                        it.data.node.nodeCase == BrushBehavior.Node.NodeCase.TARGET_NODE
            }
        }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_set_target_title,
        message = R.string.bg_tutorial_set_target_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = R.string.bg_tutorial_target_range_sliders_title,
        message = R.string.bg_tutorial_target_range_sliders_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_set_target_range_title,
        message = R.string.bg_tutorial_set_target_range_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = R.string.bg_tutorial_select_source_node_title,
        message = R.string.bg_tutorial_select_source_node_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                        it.data.node.nodeCase == BrushBehavior.Node.NodeCase.SOURCE_NODE
            }
        }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_set_source_type_title,
        message = R.string.bg_tutorial_set_source_type_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = R.string.bg_tutorial_source_range_sliders_title,
        message = R.string.bg_tutorial_source_range_sliders_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_set_source_range_title,
        message = R.string.bg_tutorial_set_source_range_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = R.string.bg_tutorial_effects_of_range_title,
        message = R.string.bg_tutorial_effects_of_range_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_explain_behavior_title,
        message = R.string.bg_tutorial_explain_behavior_message,
        anchor = TutorialAnchor.SCREEN_CENTER,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_out_of_range_behavior_title,
        message = R.string.bg_tutorial_out_of_range_behavior_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_clamp_title,
        message = R.string.bg_tutorial_clamp_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_mirror_and_repeat_title,
        message = R.string.bg_tutorial_mirror_and_repeat_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = R.string.bg_tutorial_moving_on_to_complex_behavior_title,
        message = R.string.bg_tutorial_moving_on_to_complex_behavior_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_set_narrow_range_title,
        message = R.string.bg_tutorial_set_narrow_range_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = R.string.bg_tutorial_set_clamp_title,
        message = R.string.bg_tutorial_set_clamp_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = R.string.bg_tutorial_move_node_space_title,
        message = R.string.bg_tutorial_move_node_space_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.MOVE_NODE,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                        it.data.node.nodeCase == BrushBehavior.Node.NodeCase.SOURCE_NODE
            }
        }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_click_edge_title,
        message = R.string.bg_tutorial_click_edge_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_EDGE
    ),
    TutorialStep(
        title = R.string.bg_tutorial_edge_inspector_title,
        message = R.string.bg_tutorial_edge_inspector_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_node_navigation_title,
        message = R.string.bg_tutorial_node_navigation_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.SELECT_NODE
    ),
    TutorialStep(
        title = R.string.bg_tutorial_back_to_edge_inspector_title,
        message = R.string.bg_tutorial_back_to_edge_inspector_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_EDGE
    ),
    TutorialStep(
        title = R.string.bg_tutorial_between_two_nodes_title,
        message = R.string.bg_tutorial_between_two_nodes_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.ADD_NODE_BETWEEN
    ),
    TutorialStep(
        title = R.string.bg_tutorial_edit_node_between_title,
        message = R.string.bg_tutorial_edit_node_between_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_DROPDOWN,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                        it.data.node.nodeCase == BrushBehavior.Node.NodeCase.RESPONSE_NODE
            }
        }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_binary_op_title,
        message = R.string.bg_tutorial_binary_op_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_add_input_to_binary_op_title,
        message = R.string.bg_tutorial_add_input_to_binary_op_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.ADD_BEHAVIOR,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                        it.data.node.nodeCase == BrushBehavior.Node.NodeCase.BINARY_OP_NODE
            }
        }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_configure_new_node_title,
        message = R.string.bg_tutorial_configure_new_node_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph -> graph.nodes.lastOrNull() }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_other_origins_of_values_title,
        message = R.string.bg_tutorial_other_origins_of_values_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_change_to_constant_title,
        message = R.string.bg_tutorial_change_to_constant_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = R.string.bg_tutorial_slide_constant_value_title,
        message = R.string.bg_tutorial_slide_constant_value_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = R.string.bg_tutorial_constant_value_effects_title,
        message = R.string.bg_tutorial_constant_value_effects_message,
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_change_operation_title,
        message = R.string.bg_tutorial_change_operation_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                        it.data.node.nodeCase == BrushBehavior.Node.NodeCase.BINARY_OP_NODE
            }
        }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_select_product_title,
        message = R.string.bg_tutorial_select_product_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = R.string.bg_tutorial_back_to_constant_title,
        message = R.string.bg_tutorial_back_to_constant_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph -> graph.nodes.lastOrNull() }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_change_constant_value_title,
        message = R.string.bg_tutorial_change_constant_value_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = R.string.bg_tutorial_constant_effect_title,
        message = R.string.bg_tutorial_constant_effect_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_multiplying_vs_adding_title,
        message = R.string.bg_tutorial_multiplying_vs_adding_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_further_exploration_title,
        message = R.string.bg_tutorial_further_exploration_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_add_another_coat_title,
        message = R.string.bg_tutorial_add_another_coat_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.LONG_PRESS_NODE
    ),
    TutorialStep(
        title = R.string.bg_tutorial_select_nodes_title,
        message = R.string.bg_tutorial_select_nodes_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_duplicate_nodes_title,
        message = R.string.bg_tutorial_duplicate_nodes_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.DUPLICATE_NODES
    ),
    TutorialStep(
        title = R.string.bg_tutorial_move_duplicated_nodes_title,
        message = R.string.bg_tutorial_move_duplicated_nodes_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_DONE
    ),
    TutorialStep(
        title = R.string.bg_tutorial_warning_title,
        message = R.string.bg_tutorial_warning_message,
        anchor = TutorialAnchor.NOTIFICATION_ICON,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_connect_to_family_title,
        message = R.string.bg_tutorial_connect_to_family_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CONNECT_NODES
    ),
    TutorialStep(
        title = R.string.bg_tutorial_modify_second_coat_title,
        message = R.string.bg_tutorial_modify_second_coat_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE
    ),
    TutorialStep(
        title = R.string.bg_tutorial_create_border_effect_title,
        message = R.string.bg_tutorial_create_border_effect_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = R.string.bg_tutorial_add_color_function_title,
        message = R.string.bg_tutorial_add_color_function_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.ADD_COLOR
    ),
    TutorialStep(
        title = R.string.bg_tutorial_coat_order_significance_title,
        message = R.string.bg_tutorial_coat_order_significance_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_swap_coat_order_title,
        message = R.string.bg_tutorial_swap_coat_order_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SWAP_PORTS,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Family
            }
        }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_change_order_by_editing_edges_title,
        message = R.string.bg_tutorial_change_order_by_editing_edges_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Family
            }
        }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_back_color_function_title,
        message = R.string.bg_tutorial_back_color_function_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.ColorFunction
            }
        }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_change_function_type_title,
        message = R.string.bg_tutorial_change_function_type_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = R.string.bg_tutorial_configure_color_function_title,
        message = R.string.bg_tutorial_configure_color_function_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = R.string.bg_tutorial_modify_border_color_title,
        message = R.string.bg_tutorial_modify_border_color_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE
    ),
    TutorialStep(
        title = R.string.bg_tutorial_configure_constant_title,
        message = R.string.bg_tutorial_configure_constant_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = R.string.bg_tutorial_border_pattern_title,
        message = R.string.bg_tutorial_border_pattern_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_cleanup_title,
        message = R.string.bg_tutorial_cleanup_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.LONG_PRESS_NODE
    ),
    TutorialStep(
        title = R.string.bg_tutorial_select_duplicated_nodes_title,
        message = R.string.bg_tutorial_select_duplicated_nodes_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = R.string.bg_tutorial_delete_duplicated_nodes_title,
        message = R.string.bg_tutorial_delete_duplicated_nodes_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.DELETE_NODE
    ),
    TutorialStep(
        title = R.string.bg_tutorial_reuse_behavior_title,
        message = R.string.bg_tutorial_reuse_behavior_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CONNECT_NODES,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                        it.data.node.nodeCase == BrushBehavior.Node.NodeCase.TARGET_NODE
            }
        }
    ),
    TutorialStep(
        title = R.string.bg_tutorial_multiple_outputs_title,
        message = R.string.bg_tutorial_multiple_outputs_message,
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CONNECT_NODES
    ),
    TutorialStep(
        title = R.string.bg_tutorial_complete_title,
        message = R.string.bg_tutorial_complete_message,
        anchor = TutorialAnchor.SCREEN_CENTER,
        actionRequired = TutorialAction.CLICK_NEXT
    )
)
