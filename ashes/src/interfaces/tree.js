declare type Identifier = number | string;

declare type TreeNode = {
  id: Identifier,
  parentId: ?Identifier,
  collapsed: boolean,
  children: Array<TreeNode>,
  level: number,
};

declare type Tree = { [key: Identifier]: TreeNode };
