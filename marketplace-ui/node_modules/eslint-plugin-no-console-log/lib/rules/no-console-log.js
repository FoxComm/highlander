/**
 * @fileoverview Warns on usage of `console.log`, but allows other `console` methods.
 * @author Joey Baker
 * @copyright 2015 Joey Baker. All rights reserved.
 * See LICENSE file in root directory for full license.
 * via https://github.com/eslint/eslint/issues/2621#issuecomment-105961888
 */
"use strict";

//------------------------------------------------------------------------------
// Rule Definition
//------------------------------------------------------------------------------

module.exports = function(context) {

    // variables should be defined here

    //--------------------------------------------------------------------------
    // Helpers
    //--------------------------------------------------------------------------

    // any helper functions should go here or else delete this section

    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------

    return {

        "MemberExpression": function(node) {

            if (node.object.name === "console" && node.property.name === "log") {
                context.report(node, "Unexpected console.log statement.");
            }

        }
    };
};

module.exports.schema = [];
