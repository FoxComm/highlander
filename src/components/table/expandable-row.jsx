// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import classNames from 'classnames';

// components
import TableCell from '../table/cell';
import TableRow from '../table/row';
import Drawer from './drawer';

class ExpandableRow extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      isOpen: false,
    };
  }

  get drawer() {
    const { setDrawerContent, columns, row, params } = this.props;
    const content = setDrawerContent(row, params);
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
    const parentRowClass = classNames('fc-expandable-table__parent-row', {
      '_drawer-open': this.state.isOpen
    });

    return (
      <TableRow className={parentRowClass} {...rest}>
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
  params: PropTypes.object,
};

export default ExpandableRow;
