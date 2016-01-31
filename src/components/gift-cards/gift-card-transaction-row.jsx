
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (giftCard, field) => _.get(giftCard, field, null);

const GiftCardTransactionRow = props => {
  const { giftCard, columns } = props;

  const key = `gc-transaction-${giftCard.code}`;

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      row={giftCard}
      setCellContents={setCellContents} />
  );
};

GiftCardTransactionRow.propTypes = {
  giftCard: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired
};

export default GiftCardTransactionRow;
