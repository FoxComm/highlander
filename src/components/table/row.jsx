import classNames from 'classnames';
import React, { PropTypes } from 'react';

const TableRow = (props) => {
  return (
    <tr className={ classNames('fc-table-tr', {'is-new': props.isNew }) } >
      {props.children}
    </tr>
  );
};

TableRow.propTypes = {
  children: PropTypes.node,
  isNew: PropTypes.bool
};

export default TableRow;
