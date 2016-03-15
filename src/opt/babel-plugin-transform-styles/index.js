'use strict';

/* eslint new-cap: 0, dot-notation: 0 */

// @TODO: add ability to have user-defined names for 'styles' variable via pragma comments for example
const stylesName = 'styles';

exports.__esModule = true;

exports['default'] = function (_ref) {
  const t = _ref.types;

  return {
    visitor: {
      JSXAttribute(path) {
        const node = path.node;

        if (node.name.type == 'JSXIdentifier' && node.name.name == 'styleName') {
          if (!path.scope.hasBinding(stylesName)) {
            throw new Error(`You are using "styleName" attribute but there is no imported ${stylesName} variable`);
          }

          node.name = t.JSXIdentifier('className');
          node.value = t.JSXExpressionContainer(
            t.MemberExpression(
              t.Identifier('styles'),
              node.value.type == 'StringLiteral' ? node.value : node.value.expression,
              true
            )
          );
        }
      },
    },
  };
};

module.exports = exports['default'];
