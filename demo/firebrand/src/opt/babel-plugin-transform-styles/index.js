'use strict';

/* eslint new-cap: 0, dot-notation: 0, no-param-reassign: 0 */

// @TODO: add ability to have user-defined names for 'styles' variable via pragma comments for example
const stylesName = 'styles';

exports.__esModule = true;

function findClassNameAttr(node) {
  let classNameAttr = null;
  node.attributes = node.attributes.filter(attr => {
    if (attr.name && attr.name.name == 'className') {
      classNameAttr = attr;
      return false;
    }
    return true;
  });

  return classNameAttr;
}

function getNodeValue(node) {
  return node.value.type == 'StringLiteral' ? node.value : node.value.expression;
}

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
          const classNameAttr = findClassNameAttr(path.parent);

          node.name.name = 'className';
          node.value = t.JSXExpressionContainer(
            t.MemberExpression(
              t.Identifier('styles'),
              getNodeValue(node),
              true
            )
          );

          if (classNameAttr) {
            path.node.value = t.BinaryExpression(
              '+',
              t.BinaryExpression(
                '+',
                getNodeValue(classNameAttr),
                t.StringLiteral(' ')
              ),
              getNodeValue(node)
            );
          }
        }
      },
    },
  };
};

module.exports = exports['default'];
