'use strict';

import React, { PropTypes } from 'react';

const TableCell = props => {
  return (
    <td className="fc-table-td" colSpan={props.colspan}>
      {props.children}
    </td>
  );
};

TableCell.propTypes = {
  children: PropTypes.node,
  colspan: PropTypes.number
};

export default TableCell;
