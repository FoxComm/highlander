/**
 * @flow
 */

//libs
import React, { Element } from 'react';
import _ from 'lodash';

// components
import StatePill from '../object-page/state-pill';
import MultiSelectRow from '../table/multi-select-row';

// helpers
import { isArchived } from 'paragons/common';

type Props = {
  promotion: Object,
  columns: Array<string>,
  params: Object,
};

const setCellContents = (promotion: Object, field: string) => {
  switch (field) {
    case 'state':
      return <StatePill object={promotion} />;
    case 'storefrontName':
      return <div dangerouslySetInnerHTML={{__html: _.get(promotion, field)}} />;
    default:
      return _.get(promotion, field);
  }
};

const PromotionRow = (props: Props) => {
  const { promotion, columns, params } = props;
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
