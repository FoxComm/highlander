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
      return !!value ? 'faved' : 'not faved';
    case 'image':
      // TODO: fix image
      return 'https://placeholdit.imgix.net/~text?txtsize=8&txt=IMAGE&w=50&h=50';
    default:
      return get(order, field);
  }
}

/** CustomerItemsRow Component */
const CustomerItemsRow = (props, context) => {
  const { item, columns, params } = props;

  const clickAction = () => {
    // TODO: fix link to product-details page after productId would be set to ES result
    transitionTo(context.history, 'products');
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
