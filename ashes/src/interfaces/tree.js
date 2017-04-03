declare type Identifier = number;

declare type TreeNode = {
  id: Identifier,
  parentId: ?Identifier,
  collapsed: boolean,
  children: Array<TreeNode>,
  level: number,
};

declare type Tree = { [key: Identifier]: TreeNode };

declare type TNodeData = {
  id: Identifier,
  [key: string]: any,
};

declare type TNode<T> = {
  node: T & { id: Identifier },
  children: Array<TNode<T>>,
};
