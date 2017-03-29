/* @flow */

// libs
import { eq, noop, values } from 'lodash';
import classNames from 'classnames';
import { assoc, update } from 'sprout-data';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// helpers
import { findNode, updateNodes } from 'paragons/tree';

// components
import MultiSelectTable from 'components/table/multi-select-table';
import { Button } from 'components/common/buttons';

// styles
import styles from './tree-table.css';

type Row = { [key: string]: string | number };

type TTableNode = {
  row: Row,
  parentId: ?Identifier,
  collapsed: boolean,
  level: number,
};

type TableTreeNode = TNode<TTableNode>;

type TableTree = { [key: number]: TableTreeNode };

type Props = {
  columns: Columns,
  data: {
    rows: Array<Row>,
    total: number,
    from: number,
    size: number,
  },
  collapseField: string,
  renderRow: (row: any, index: number, columns: Columns, params: Object) => Element<*>,
  setState?: (state: Object) => any,
  hasActionsColumn: boolean,
  isLoading: boolean,
  failed: boolean,
  emptyMessage: string,
  className: string,
  idField: string,
  headerControls: Array<Element<*>>,
};

type State = {
  root: Array<TableTreeNode>,
}

function buildTree(arr: Array<Row>, idField: string) {
  const tree: Array<TableTreeNode> = [];

  const acc: TableTree = arr.reduce((acc: TableTree, row: Row) => assoc(acc, row[idField], {
    children: [],
    node: {
      id: row[idField],
      row,
      parentId: row['parentId'],
      collapsed: true,
      level: 0,
    },

  }), {});

  values(acc).forEach(function (node: TableTreeNode) {
    if (node.node.parentId) {
      acc[node.node.parentId].children.push(node);
    } else {
      tree.push(node);
    }
  });

  return tree;
}

const collapseNode = (tree: Array<TableTreeNode>, id: Identifier) => {
  let node = findNode(tree, id);

  if (node) {
    node.node.collapsed = !node.node.collapsed;
  }

  return tree;
};

const toggleAll = (tree: Array<TableTreeNode>, collapse: boolean) =>
  updateNodes(tree, (n: TableTreeNode) => n.node.collapsed = collapse);

class TreeTable extends Component {
  props: Props;

  state: State = {
    root: buildTree(this.props.data.rows, this.props.idField),
  };

  static defaultProps = {
    idField: 'id',
    collapseField: 'name',
    isLoading: false,
    failed: false,
    className: '',
  };

  componentWillReceiveProps(nextProps: Props) {
    if (!eq(nextProps.data.rows, this.props.data.rows)) {
      this.setState({ root: buildTree(nextProps.data.rows, nextProps.idField) });
    }
  }

  @autobind
  expandAll() {
    this.setState({ root: toggleAll(this.state.root, false) });
  }

  @autobind
  collapseAll() {
    this.setState({ root: toggleAll(this.state.root, true) });
  }

  get rows(): Array<TableTreeNode> {
    const _reduce = (level: number) => (nodes: Array<TableTreeNode>, node: TableTreeNode) => {
      const children = !node.node.collapsed ? node.children.reduce(_reduce(level + 1), []) : [];

      nodes.push(assoc(node, ['node', 'level'], level));

      children.forEach((child: TableTreeNode) => nodes.push(child));

      return nodes;
    };

    return this.state.root.reduce(_reduce(0), []);
  }

  @autobind
  handleCollapse(node: TableTreeNode, event: MouseEvent) {
    event.preventDefault();
    event.stopPropagation();

    const root = collapseNode(this.state.root, node.node.id);

    this.setState({ root });
  }

  @autobind
  renderRow(node: TableTreeNode, index: number, columns: Columns, params: Object) {
    const el = this.props.renderRow(node.node.row, index, columns, params);

    const processCell = (content: Element<*>, col: Column) => {
      const collapsible = !!node.children.length;
      const collapsed = node.node.collapsed;
      const level = node.node.level;

      let cellContents = content;

      if (col.field === this.props.collapseField) {
        const iconClassName = classNames(({
          'icon-category': !collapsible,
          'icon-category-expand': collapsible && collapsed,
          'icon-category-collapse': collapsible && !collapsed,
        }));

        cellContents = (
          <span className="fc-collapse" style={{ paddingLeft: `${level * 20}px` }}>
              <i className={iconClassName} onClick={this.handleCollapse.bind(this, node)} />
            {content}
            </span>
        );
      }

      return cellContents;
    };

    return React.cloneElement(el, { processCell });
  }

  get headerControls(): Array<Element<*>> {
    return [
      <Button className={styles.headerButton} onClick={this.expandAll}>Expand All</Button>,
      <Button className={styles.headerButton} onClick={this.collapseAll}>Collapse All</Button>,
      ...this.props.headerControls,
    ];
  }

  render() {
    const { data } = this.props;

    return (
      <div className={styles.tree}>
        <MultiSelectTable
          {...this.props}
          data={{
            ...data,
            rows: this.rows,
          }}
          headerControls={this.headerControls}
          renderRow={this.renderRow}
        />
      </div>
    );
  }
}

export default TreeTable;
