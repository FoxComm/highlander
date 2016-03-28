/** Libs */
import { get } from 'lodash';
import React, { PropTypes } from 'react';

/** Components */
import MultiSelectRow from '../../table/multi-select-row';

import { transitionTo } from '../../../route-helpers';


function setCellContents(order, field) {
  const value = get(order, field);

  switch (field) {
    case 'savedForLaterAt':
      return !!value ? <i className="icon-heart"/> : null;
    case 'image':
      return 'https://placeholdit.imgix.net/~text?txtsize=8&txt=IMAGE&w=50&h=50';
    default:
      return value;
  }
}

/**
 * CustomerItemsRow Component
 *
 * TODO: Fix image url when it is added to ES result
 * TODO: Fix link to product-details page after productId would be added ES result
 */
const CustomerItemsRow = (props, context) => {
  const { item, columns, params } = props;

  const clickAction = () => {
    transitionTo(context.history, 'inventory-item-details', { code: item.skuCode });
  };

  const key = `customer-items-${item.id}`;

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      row={item}
      setCellContents={setCellContents}
      params={params}/>
  );
};

/** CustomerItemsRow expected props types */
CustomerItemsRow.propTypes = {
  item: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired,
};

/** CustomerItemsRow expected context types */
CustomerItemsRow.contextTypes = {
  history: PropTypes.object.isRequired
};

export default CustomerItemsRow;
