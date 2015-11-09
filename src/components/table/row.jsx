'use strict';

import React, { PropTypes } from 'react';

const TableRow = props => {
  return (
    <tr className="fc-table-tr">
      {props.children}
    </tr>
  );
};

TableRow.propTypes = {
  children: PropTypes.node
};

export default TableRow;
