// libs
import classNames from 'classnames';
import React, { PropTypes } from 'react';

const TableRow = props => {
  const { children, isNew, className, ...rest } = props;
  const RowTag = rest.href ? 'a' : 'tr';

  return (
    <RowTag className={classNames('fc-table-tr', {'is-new': isNew }, className)} {...rest}>
      {children}
    </RowTag>
  );
};

TableRow.propTypes = {
  children: PropTypes.node,
  isNew: PropTypes.bool
};

export default TableRow;
