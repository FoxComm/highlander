/* @flow */

import React, { Element } from 'react';

//libs
import _ from 'lodash';
import { activeStatus, isArchived } from 'paragons/common';

// components
import { RoundedPill } from 'components/core/rounded-pill';
import MultiSelectRow from '../table/multi-select-row';

type Props = {
  promotion: Object,
  columns: Columns,
  params: Object,
};

const PromotionRow = (props: Props) => {
  const { promotion, columns, params } = props;

  const setCellContents = (promotion: Object, field: string) => {
    switch (field) {
      case 'state':
        return <RoundedPill text={activeStatus(promotion)} />;
      case 'storefrontName':
        return <div dangerouslySetInnerHTML={{__html: _.get(promotion, field)}} />;
      default:
        return _.get(promotion, field);
    }
  };

  const commonParams = {
    columns,
    row: promotion,
    setCellContents,
    params,
  };

  if (isArchived(promotion)) {
    return <MultiSelectRow {...commonParams} />;
  }

  return (
    <MultiSelectRow
      {...commonParams}
      linkTo="promotion-details"
      linkParams={{promotionId: promotion.id}}
    />
  );
};

export default PromotionRow;
