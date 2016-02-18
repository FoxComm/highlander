
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (giftCard, field) => {
  const r = _.get(giftCard, field, null);
  return field == 'debit' ?  -r : r;
}

const GiftCardTransactionRow = props => {
  const { giftCard, columns, params } = props;

  const key = `gc-transaction-${giftCard.code}`;

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      row={giftCard}
      setCellContents={setCellContents}
      params={params} />
  );
};

GiftCardTransactionRow.propTypes = {
  giftCard: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired,
  params: PropTypes.object.isRequired,
};

export default GiftCardTransactionRow;
