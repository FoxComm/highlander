import React, { PropTypes } from 'react';

import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (giftCard, field) => {
  switch (field) {
    case 'id':
      return giftCard.id;
    case 'code':
      return giftCard.code;
    case 'originType':
      return giftCard.originType;
    case 'originalBalance':
      return giftCard.originalBalance;
    case 'availableBalance':
      return giftCard.availableBalance;
    case 'currentBalance':
      return giftCard.currentBalance;
    case 'status':
      return giftCard.status;
    case 'createdAt':
      return giftCard.createdAt;
  }
};

const GiftCardRow = (props, context) => {
  const { giftCard, columns } = props;
  const key = `gift-card-${giftCard.id}`;
  const clickAction = () => {
    transitionTo(context.history, 'giftcard', { giftCard: giftCard.code });
  };

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      row={giftCard}
      setCellContents={setCellContents} />
  );
};

GiftCardRow.propTypes = {
  giftCard: PropTypes.object,
  columns: PropTypes.array
};

GiftCardRow.contextTypes = {
  history: PropTypes.object.isRequired
};

export default GiftCardRow;
