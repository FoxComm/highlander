/* @flow */

export function findNode(tree: Array<TNode<any>>, id: Identifier): ?TNode<any> {
  return tree.reduce((res: ?TNode<any>, node: TNode<any>): ?TNode<any> => {
    if (res) {
      return res;
    }

    if (node.node.id === id) {
      return node;
    }

    if (node.children) {
      return findNode(node.children, id);
    }

    return null;
  }, null);
}

export function updateNodes(tree: Array<TNode<any>>, updater: (node: TNode<any>) => any) {
  const traverse = (nodes: Array<TNode<any>>) =>
    nodes.forEach(node => {
      if (node.children) traverse(node.children);

      updater(node);
    });

  traverse(tree);

  return tree;
}
