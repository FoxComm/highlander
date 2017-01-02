/** Libs */
import { get } from 'lodash';
import React, { PropTypes } from 'react';

/** Components */
import MultiSelectRow from '../../table/multi-select-row';


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
const CustomerItemsRow = props => {
  const { item, columns, params } = props;

  const key = `customer-items-${item.id}`;

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="product-variant-inventory-details"
      linkParams={{productVariantId: item.id}}
      row={item}
      setCellContents={setCellContents}
      params={params}/>
  );
};

/** CustomerItemsRow expected props types */
CustomerItemsRow.propTypes = {
  item: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired,
  params: PropTypes.object,
};

export default CustomerItemsRow;
