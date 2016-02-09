import React, { PropTypes } from 'react';
import _ from 'lodash';

import { PrimaryButton } from '../common/buttons';

const Checkout = props => {
  const { errors, warnings } = props.order.validations;
  const totalCount = errors.length + warnings.length;
  const refNum = _.get(props, 'order.currentOrder.referenceNumber');

  return (
    <div className="fc-order-checkout fc-col-md-1-1">
      <PrimaryButton onClick={() => props.checkout(refNum)} disabled={totalCount > 0}>
        Place order
      </PrimaryButton>
    </div>
  );
};

Checkout.propTypes = {
  checkout: PropTypes.func.isRequired,
  order: PropTypes.object.isRequired,
};

export default Checkout;
