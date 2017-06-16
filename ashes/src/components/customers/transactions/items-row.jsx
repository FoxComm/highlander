/* @flow */

import React from 'react';

// libs
import _ from 'lodash';

// components
import MultiSelectRow from 'components/table/multi-select-row';

type Props = {
  item: Object,
  columns: Columns,
  params: Object,
};

/**
 * CustomerItemsRow Component
 *
 * TODO: Fix image url when it is added to ES result
 * TODO: Fix link to product-details page after productId would be added ES result
 */
const CustomerItemsRow = (props: Props) => {
  const { item, columns, params } = props;

  const setCellContents = (order: Object, field: string) => {
    const value = _.get(order, field);

    switch (field) {
      case 'savedForLaterAt':
        return value ? <i className="icon-heart" /> : null;
      case 'image':
        return 'https://placeholdit.imgix.net/~text?txtsize=8&txt=IMAGE&w=50&h=50';
      default:
        return value;
    }
  };

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="sku-inventory-details"
      linkParams={{skuCode: item.skuCode}}
      row={item}
      setCellContents={setCellContents}
      params={params} />
  );
};

export default CustomerItemsRow;
