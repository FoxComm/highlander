import React, { PropTypes } from 'react';
import moment from 'moment';
import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

import MultiSelectRow from '../table/multi-select-row';

function isActive(activeFrom, activeTo) {
  const now = moment();

  if (!activeFrom) {
    return false;
  } else if (now.diff(activeFrom) < 0) {
    return false;
  } else if (activeTo && now.diff(activeTo) > 0) {
    return false;
  }

  return true;
}

function setCellContents(product, field) {
  switch (field) {
    case 'image':
      return _.get(product, ['images', 0]);
    case 'state':
      const activeFromStr = _.get(product, 'activefrom');
      const activeToStr = _.get(product, 'activeto');
      const activeFrom = activeFromStr ? moment.utc(activeFromStr) : null;
      const activeTo = activeToStr ? moment.utc(activeToStr) : null;
      return isActive(activeFrom, activeTo) ? 'Active' : 'Inactive';
    default:
      return _.get(product, field);
  }
}

const ProductRow = (props, context) => {
  const { product, columns, params } = props;
  const key = `product-${product.id}`;
  const clickAction = () => {
    transitionTo(context.history, 'product-details', { productId: product.id });
  };

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      row={product}
      setCellContents={setCellContents}
      params={params} />
  );
};

ProductRow.propTypes = {
  product: PropTypes.object.isRequired,
  columns: PropTypes.array,
  params: PropTypes.object.isRequired,
};

ProductRow.contextTypes = {
  history: PropTypes.object.isRequired
};

export default ProductRow;
