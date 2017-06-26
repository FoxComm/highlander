
import React from 'react';
import styles from './profile.css';

import AccountDetails from './account-details/account-details';
import OrdersBlock from './orders/orders-block';
import AddressBlock from './shipping-addresses/address-block';
import ReviewsBlock from './reviews/reviews-block';

const Profile = () => {
  return (
    <div styleName="profile">
      <div styleName="details-addresses">
        <AccountDetails
          styleName="account-details"
        />
        <AddressBlock
          styleName="shipping-addresses"
        />
      </div>
      <div styleName="orders">
        <OrdersBlock />
      </div>
      <ReviewsBlock />
    </div>
  );
};

export default Profile;
