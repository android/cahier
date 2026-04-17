package com.example.cahier.ui.brushgraph.model

/**
 * Represents a step in the guided tutorial.
 */
data class TutorialStep(
    val title: String,
    val message: String,
    val anchor: TutorialAnchor,
    val actionRequired: TutorialAction,
    val getTargetNode: (BrushGraph) -> GraphNode? = { null }
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
        title = "Welcome!",
        message = "We will walk through the brush graph UI and explain how to use it by building a basic brush from the ground up. We already have the building blocks of a brush here at the beginning: Family (top-level representation of a brush style, similar to a font family), a Coat (layer of ink), a Tip (geometry extruded to create the mesh), and Paint (color and texture).",
        anchor = TutorialAnchor.SCREEN_CENTER,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Test Canvas",
        message = "Tap to open the test canvas at the bottom, and draw on it.",
        anchor = TutorialAnchor.TEST_CANVAS,
        actionRequired = TutorialAction.DRAW_ON_CANVAS
    ),
    TutorialStep(
        title = "Test Canvas Options",
        message = "As we modify the brush, the test canvas will auto-update any strokes drawn here, so it is useful to have something here as we edit the brush. There's also options here to configure the canvas' color, clear it, and change the brush's color and size.",
        anchor = TutorialAnchor.TEST_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Edit Tip",
        message = "Tap the Tip node to open the inspector view.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph -> graph.nodes.find { it.data is NodeData.Tip } }
    ),
    TutorialStep(
        title = "Modify Tip Shape",
        message = "Modify the tip shape parameters. You can either use the slider, or tap the number to manually enter a value.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = "Live Strokes",
        message = "Notice how changing the parameters affects the previews on the tip, coat, and family nodes, and also the test strokes in the canvas.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Inspector Features",
        message = "Notice a few other key features in the inspector: the (?) button (show tooltips), the delete button (delete a node), and the disable button (temporarily deactivate a node).",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Exit Inspector",
        message = "Exit the inspector by clicking the (x) button or clicking on an empty part of the canvas.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EXIT_INSPECTOR
    ),
    TutorialStep(
        title = "Brush Behaviors",
        message = "In addition to defining a shape, a Tip contains behaviors. In a behavior, a 'Source' collects data from the input (e.g., pressure, speed), normalizes it, and passes it to a 'Target' which uses that data to modify a value that affect the appearance of the stroke (e.g., color, size).",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Add Behavior",
        message = "'Add...' buttons, will add a default node that produces an output compatible as input for the other node. Begin by adding a new behavior to the tip by clicking 'Add behavior...'. ",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.ADD_BEHAVIOR,
        getTargetNode = { graph -> graph.nodes.find { it.data is NodeData.Tip } }
    ),
    TutorialStep(
        title = "Error!",
        message = "Uh-oh, this causes an error! Click the error icon in the upper right corner to take a closer look.",
        anchor = TutorialAnchor.NOTIFICATION_ICON,
        actionRequired = TutorialAction.CLICK_NOTIFICATION
    ),
    TutorialStep(
        title = "Notification Pane",
        message = "There are three types of notifications: Error (brush is invalid), Warning (potential issue with brush), and Debug (a record of certain actions). Here we have one error, caused by target we created not having an input.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Navigate to Nodes with Issues",
        message = "In most cases, the error or warning message will link to the node causing it. Click the error to navigate directly to the node with the issue.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_ERROR_LINK
    ),
    TutorialStep(
        title = "Add Input",
        message = "To fix the missing input issue, let's add an input to this target. We could click 'Add input...', but let's try another way: click the floating action button (+) and select 'behavior'.",
        anchor = TutorialAnchor.FAB,
        actionRequired = TutorialAction.ADD_INPUT_FAB
    ),
    TutorialStep(
        title = "Change Node Type",
        message = "This will make a 'Target' node by default, and automatically open the inspector. Change the Node Type to 'Source' in the inspector.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_DROPDOWN,
        getTargetNode = { graph -> graph.nodes.lastOrNull() }
    ),
    TutorialStep(
        title = "Move Node",
        message = "Move the node to the left of the existing 'Target' by dragging it.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.MOVE_NODE
    ),
    TutorialStep(
        title = "Connect Nodes",
        message = "Create a connection between the two nodes by dragging from the output port (gray dot to the right of the Source node, labelled 'Out') to the input port (gray dot to the left of the Target, labelled 'Add input...').",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CONNECT_NODES
    ),
    TutorialStep(
        title = "Edit Target",
        message = "The validation error should now be fixed. You may already notice your stroke changing with this new behavior! Let's configure it futher: tap the Target node to open the inspector.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                it.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.TARGET_NODE
            }
        }
    ),
    TutorialStep(
        title = "Set Target",
        message = "Set the 'Target' to 'hue offset'.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = "Target Range Sliders",
        message = "Notice the 'range start' and 'range end' sliders. These control how much this target value will be changed based on the inputs it receives.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Set Target Range",
        message = "Let's give it a wide range so we get a lot of hue variation, say -360 to 360.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = "Select Source Node",
        message = "When you're done, tap the Source node to open the inspector.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                it.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.SOURCE_NODE
            }
        }
    ),
    TutorialStep(
        title = "Set Source Type",
        message = "Set the source to 'distance traveled'.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = "Source Range Sliders",
        message = "Source also has 'range' sliders. These control how to normalize Source data to a 0 to 1 range, determining what range of Source data to map to the upper and lower range of the Target.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Set Source Range",
        message = "Set source start to 0 and end to ~30.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = "Effects of Range",
        message = "Notice how hue offset is only applied to the beginning of the stroke, where distance traveled is between 0 and ~30. The hue offset between those values follows the range set in the Target node of -360 to 360.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Explain Behavior",
        message = "Together, these two nodes describe a behavior: map distance traveled in the range given by Source to hue offset in the range set by Target.",
        anchor = TutorialAnchor.SCREEN_CENTER,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Out of Range Behavior",
        message = "Notice 'out of range behavior'. This controls what value the Source should pass when the data  is outside of the set range.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Clamp",
        message = "By default, the out of range behavior is 'clamp': outside of the range, values are clamped to the bounds of the range, 0 or 1. This leads to no hue offset outside the Source range, because our Target range is -360 to 360 (and 360 or -360 hue offset is the same as none).",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Mirror and Repeat",
        message = "Try changing out of range behavior to 'mirror' or 'repeat' and notice what changes.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = "Moving On to a More Complex Behavior",
        message = "Next we will build a more complicated behavior. Before we proceed, we will configure the Source to best illustrate the impacts of the different nodes.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Set a Narrow Range",
        message = "Set the range of the source node narrow, with space on each side (say 20 to 80).",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = "Set Clamp",
        message = "When you're done with the range, set the Source node to use 'clamp'.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = "Move Node",
        message = "Let's make some space for another node. Move the source node to the left to make the edge longer.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.MOVE_NODE,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                it.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.SOURCE_NODE
            }
        }
    ),
    TutorialStep(
        title = "Click Edge",
        message = "Click the edge between the Source and the Target to open the edge inspector.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_EDGE
    ),
    TutorialStep(
        title = "Edge Inspector",
        message = "Like the node inspector, the edge inspector has disable and delete buttons which function similarly. It also lists the two nodes connected by the edge.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Node Navigation",
        message = "Click either node listed in the edge inspector to navigate directly to it.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.SELECT_NODE
    ),
    TutorialStep(
        title = "Back to Edge Inspector",
        message = "Reopen the edge inspector for this edge.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_EDGE
    ),
    TutorialStep(
        title = "Between Two Nodes",
        message = "Let's insert a node inbetween these nodes. Click 'add node between'.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.ADD_NODE_BETWEEN
    ),
    TutorialStep(
        title = "Edit Node Between",
        message = "By default, this adds a Response node, but let's change the Node Type to a Binary Op.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_DROPDOWN,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                it.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.RESPONSE_NODE
            }
        }
    ),
    TutorialStep(
        title = "Binary Op",
        message = "Binary Ops combine two values according to a function (e.g., sum, product). Notice there is an error since a Binary Op needs at least two values. In brush graph, a Binary Op can have more than two values; this is a UI convenience to make it easy to chain many Binary Ops together.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Add Input to Binary Op",
        message = "Resolve the error by clicking 'Add input...' on the Binary Op.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.ADD_BEHAVIOR,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                it.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE
            }
        }
    ),
    TutorialStep(
        title = "Configure New Node",
        message = "Click the new Source node added to open it in the inspector.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph -> graph.nodes.lastOrNull() }
    ),
    TutorialStep(
        title = "Other Origins of Values",
        message = "Values don't have to originate from data collected by a Source node; they can be randomly generated by a Noise node, or static from a Constant node.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Change to Constant",
        message = "Change the Node Type to a Constant node.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = "Slide Constant Value",
        message = "Try sliding the constant value around.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = "Constant Value Effects",
        message = "Notice how the color changes; we still have the same section with a hue offset affected by the distance traveled, but the color of the stroke outside of that range changes with the constant! This is because we are summing these values together, but the amount of distance traveled only impacts hue offset within the set range, since we are using the clamp out of range behavior.",
        anchor = TutorialAnchor.INSPECTOR,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Change Operation",
        message = "Let's change what the Binary Op is doing. Click to select the Binary Op node.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                it.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE
            }
        }
    ),
    TutorialStep(
        title = "Select Product",
        message = "Change the Operation to 'product'.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = "Back to Constant",
        message = "Tap the constant node to open it in the inspector.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph -> graph.nodes.lastOrNull() }
    ),
    TutorialStep(
        title = "Change the Constant",
        message = "Now that we are multiplying values in the Binary Op, try sliding the constant around and see what happens.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = "The Constant's Effect",
        message = "Notice that the area before the area affected by the Source is unaffected by any hue offset modifier, no matter how you change the constant.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Multiplying vs. Adding",
        message = "This is because values are normalized on a range of 0 to 1, so with clamp, everything outside of the Source's range is either 0 or 1, depending on which side it is on. On the lower side, that is 0, and 0 * constant = 0 passed to Target, which maps to the lower end of the range for hue offset: -360, which is equivalent to 0, or no hue offset change. On the higher side, it is clamped to 1, and 1 * constant = constant passed to Target, which can then affect hue offset.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Further Exploration",
        message = "What do you think would happen if the Source's out of range behavior was set to mirror or repeat? Feel free to explore this on your own. When you're ready to proceed, set the Source's out of range behavior back to clamp and click 'Next'.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Add Another Coat",
        message = "Let's add another coat for a 'border' effect. Long-press any node other than the Family node to enter 'selection mode'.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.LONG_PRESS_NODE
    ),
    TutorialStep(
        title = "Select Nodes",
        message = "Tap every node except the Family node to select them, or click 'Select All' (this ignores the Family node).",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Duplicate Nodes",
        message = "Click 'Duplicate' to make a copy of all of these nodes.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.DUPLICATE_NODES
    ),
    TutorialStep(
        title = "Move Duplicated Nodes",
        message = "Drag any of the selected nodes to move them all out of the way so they don't overlap the other nodes. Click 'Done' when you're ready to deselect them.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_DONE
    ),
    TutorialStep(
        title = "Warning!",
        message = "Notice a warning appears since the output of all these nodes we copied is unused.",
        anchor = TutorialAnchor.NOTIFICATION_ICON,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Connect to Family",
        message = "Draw an edge from the 'Out' port of the new coat to the 'Add coat...' port on the Family node.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CONNECT_NODES
    ),
    TutorialStep(
        title = "Modify Second Coat",
        message = "Open the Tip of the second Coat.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE
    ),
    TutorialStep(
        title = "Create Border Effect",
        message = "Increase scale x and y to make it wider and taller.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = "Add Color Function",
        message = "When you're done with the Tip, go to the Paint node of the second Coat, and click 'Add color...' to add a Color Function.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.ADD_COLOR
    ),
    TutorialStep(
        title = "Coat Order Significance",
        message = "By default this should make a 'Replace Color' function. You may see the whole stroke change color. If so, this is because of the order of the coats. Coats earlier in the order are drawn first, with later coats drawn on top.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Swap Coat Order",
        message = "We want to draw the new, larger, recolored coat beneath the smaller one. Swap the order of the coats on the Family node by dragging the handle on 'coat 1' up to swap with 'coat 0'.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SWAP_PORTS,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Family
            }
        }
    ),
    TutorialStep(
        title = "Change Order by Editing Edges",
        message = "We could also change the order by 'editing' the edges. Dragging from any input port with an edge connected allows you to move the edge. Release the edge while editing to delete, or reconnect it to another port to move it there.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Family
            }
        }
    ),
    TutorialStep(
        title = "Back Color Function",
        message = "Navigate back to the Color Function inspector by clicking it.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.ColorFunc
            }
        }
    ),
    TutorialStep(
        title = "Change Function Type",
        message = "Instead of a black outline, let's try to make the border look a bit more like a shadow. Change the Function Type to 'Opacity Multiplier'.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_DROPDOWN
    ),
    TutorialStep(
        title = "Configure Color Function",
        message = "Let's make the border be a fainter version of the inner part of the stroke. Set Opacity Multiplier ~0.4.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = "Modify the Border Color",
        message = "We can modify the constant to make the border follow the same pattern as the inner coat, but with different hues. Click the Constant Node on the border to open it in the inspector.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.SELECT_NODE
    ),
    TutorialStep(
        title = "Configure the Constant",
        message = "Try changing the value of the Constant.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.EDIT_FIELD
    ),
    TutorialStep(
        title = "Border Pattern",
        message = "Notice that the border retains the same pattern in terms of hue offset, though the base colors change.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Cleanup",
        message = "If we want the constant to be same, so the inner and outer coats have the exact same colors, we don't need the duplicated behavior nodes. Hold down on a node to enter select mode.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.LONG_PRESS_NODE
    ),
    TutorialStep(
        title = "Select Duplicated Nodes",
        message = "Tap to select the duplicated nodes making up the behavior for the border coat: Source, Constant, Binary Op, and Target.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CLICK_NEXT
    ),
    TutorialStep(
        title = "Delete Duplicated Nodes",
        message = "Click 'Delete' to get rid of them.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.DELETE_NODE
    ),
    TutorialStep(
        title = "Reuse Behavior",
        message = "Drag a new edge from the 'Out' port of the Target on the inner Coat to the 'Add behavior...' port of the Tip on the outer Coat.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CONNECT_NODES,
        getTargetNode = { graph ->
            graph.nodes.find {
                it.data is NodeData.Behavior &&
                it.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.TARGET_NODE
            }
        }
    ),
    TutorialStep(
        title = "Multiple Outputs",
        message = "Notice that now, the behavior is applied to both Tips on both Coats! The 'Out' port on nodes can connect with multiple inputs, enabling complex graphs to use fewer nodes and be easier to design.",
        anchor = TutorialAnchor.NODE_CANVAS,
        actionRequired = TutorialAction.CONNECT_NODES
    ),
    TutorialStep(
        title = "Tutorial Complete!",
        message = "That should be enough to get you started. You can find Templates in the menu, which are a great next step to understand how brushes work. Remember to click the (?) buttons for tooltips if you get stuck. Happy designing!",
        anchor = TutorialAnchor.SCREEN_CENTER,
        actionRequired = TutorialAction.CLICK_NEXT
    )
)
