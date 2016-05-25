// libs
import classNames from 'classnames';
import React, { PropTypes } from 'react';

const TableRow = props => {
  const { children, isNew, className, ...rest } = props;

  return (
    <tr className={classNames('fc-table-tr', {'is-new': isNew }, className)} {...rest}>
      {children}
    </tr>
  );
};

TableRow.propTypes = {
  children: PropTypes.node,
  isNew: PropTypes.bool
};

export default TableRow;
