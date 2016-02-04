
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import MultiSelectRow from '../../table/multi-select-row';

const setCellContents = (txn, field) => {
  if (field === 'debit') {
    return (-1) * _.get(txn, field, null);
  }
  return _.get(txn, field, null);
}

const StoreCreditTransactionRow = props => {
  const { storeCreditTransaction, columns } = props;

  const key = `sc-transaction-${storeCreditTransaction.id}`;

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      row={storeCreditTransaction}
      setCellContents={setCellContents} />
  );
};

StoreCreditTransactionRow.propTypes = {
  storeCreditTransaction: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired
};

export default StoreCreditTransactionRow;
