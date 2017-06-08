import React from 'react';

import AddressDetails from '../addresses/address-details';
import ContentBox from 'components/core/content-box';

type Props = {
  order: Object,
};

const OrderShippingAddress = (props: Props) => {
  const address = props.order.shippingAddress;
  const content = address
    ? <AddressDetails address={address} />
    : <div className="fc-content-box-notice">No shipping address applied.</div>;

  return (
    <ContentBox
      className="fc-shipping-address"
      title="Shipping Address"
      indentContent={true}>
      {content}
    </ContentBox>
  );
};

export default OrderShippingAddress;
