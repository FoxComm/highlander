/* @flow */

import React from 'react';

// libs
import _ from 'lodash';

// components
import MultiSelectRow from 'components/table/multi-select-row';
import OriginType from 'components/common/origin-type';

type Props = {
  giftCard: Object,
  columns: Columns,
  params: Object,
};

const GiftCardRow = (props: Props) => {
  const { giftCard, columns, params } = props;

  const setCellContents = (giftCard: Object, field: string) => {
    if (field === 'originType') {
      return (
        <OriginType value={giftCard} />
      );
    }

    return _.get(giftCard, field);
  };

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

export default GiftCardRow;
