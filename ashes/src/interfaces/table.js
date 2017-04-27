declare type Column = {
  field: string,
  text?: string,
  type?: string,
};

declare type Columns = Array<Column>;

declare type Row = { [key: string]: string | number };

declare type TableNode = {
  row: Row,
  parentId: ?Identifier,
  collapsed: boolean,
  level: number,
};

declare type TableTreeNode = TNode<TableNode>;

declare type TableTree = Array<TableTreeNode>;
