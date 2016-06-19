
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import MultiSelectRow from '../../table/multi-select-row';
import OriginType from '../../common/origin-type';

const setCellContents = (txn, field) => {
  if (field === 'debit') {
    return (-1) * _.get(txn, field, null);
  }
  if (field === 'transaction') {
    return (
      <OriginType value={txn}/>
    );
  }
  return _.get(txn, field, null);
};

const StoreCreditTransactionRow = props => {
  const { storeCreditTransaction, columns, params } = props;

  const key = `sc-transaction-${storeCreditTransaction.id}`;

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      row={storeCreditTransaction}
      setCellContents={setCellContents}
      params={params} />
  );
};

StoreCreditTransactionRow.propTypes = {
  storeCreditTransaction: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired,
  params: PropTypes.object.isRequired,
};

export default StoreCreditTransactionRow;
