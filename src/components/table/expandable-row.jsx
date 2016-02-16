// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import classNames from 'classnames';

// components
import TableCell from '../table/cell';
import TableRow from '../table/row';

class Drawer extends React.Component {
  get drawerClass() {
    return classNames('fc-expandable-table__drawer', {
      '_shown': this.props.isVisible,
      '_hidden': !this.props.isVisible,
    });
  }

  render() {
    console.log(this.props);
    return (
      <TableRow className={this.drawerClass}>
        <td colSpan={this.props.colspan}>
          {this.props.children}
        </td>
      </TableRow>
    );
  }
}

class ExpandableRow extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      isOpen: false,
    };
  }

  get drawer() {
    const { setDrawerContent, columns } = this.props;
    const content = setDrawerContent();
    return (
      <Drawer isVisible={this.state.isOpen} colspan={columns.length}>{content}</Drawer>
    );
  }

  get cells() {
    const { columns, row, setCellContents, ...rest } = this.props;

    const cells = _.reduce(columns, (visibleCells, col) => {
      const cellKey = `row-${col.field}`;

      const cellClickAction = () => this.setState({isOpen: !this.state.isOpen});
      const cellContents = setCellContents(row, col.field);

      visibleCells.push(
        <TableCell onClick={cellClickAction} key={cellKey} column={col}>
          {cellContents}
        </TableCell>
      );

      return visibleCells;
    }, []);

    return cells;
  }

  render() {
    const { columns, row, setCellContents, ...rest } = this.props;

    return (
      <TableRow {...rest}>
        {this.cells}
        {this.drawer}
      </TableRow>
    );
  }
};

ExpandableRow.propTypes = {
  columns: PropTypes.array.isRequired,
  row: PropTypes.object.isRequired,
  setCellContents: PropTypes.func.isRequired,
  setDrawerContent: PropTypes.func.isRequired,
};

export default ExpandableRow;
