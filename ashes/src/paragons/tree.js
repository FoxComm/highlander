/* @flow */
import values from 'lodash/values';

export function findNode(tree: Tree, id: Identifier): ?TreeNode {
  const _find = (nodes: Array<TreeNode>, id: Identifier) =>
    nodes.reduce((res: ?TreeNode, node): ?TreeNode => {
      if (res) {
        return res;
      }

      return node.id === id ? node : _find(node.children, id);
    }, null);

  const res = _find(values(tree), id);

  return res;
}

export function collapseNode(tree: Tree, id: Identifier) {
  const node = findNode(tree, id);

  if (node) {
    node.collapsed = !node.collapsed;
  }

  return tree;
}

export function updateNodes(tree: Tree, updater: (node: TreeNode) => any) {
  const traverse = (nodes: Array<TreeNode>) =>
    nodes.forEach((node: TreeNode) => {
      if (node.children) traverse(node.children);

      updater(node);
    });

  traverse(values(tree));

  return tree;
}
