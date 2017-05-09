
import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';

import MultiSelectRow from '../table/multi-select-row';
import OriginType from '../common/origin-type';

const setCellContents = (giftCard, field) => {
  if (field === 'originType') {
    return <OriginType value={giftCard} />;
  }

  return _.get(giftCard, field);
};

const GiftCardRow = (props) => {
  const { giftCard, columns, params } = props;
  const key = `gift-card-${giftCard.id}`;

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="giftcard"
      linkParams={{giftCard: giftCard.code}}
      row={giftCard}
      setCellContents={setCellContents}
      params={params} />
  );
};

GiftCardRow.propTypes = {
  giftCard: PropTypes.object.isRequired,
  columns: PropTypes.array,
  params: PropTypes.object.isRequired,
};

export default GiftCardRow;
