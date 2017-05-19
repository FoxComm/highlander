
import React from 'react';
import styles from './profile.css';

import AccountDetails from './account-details/account-details';
// import MyOrders from './blocks/my-orders';
import AddressBlock from './shipping-addresses/address-block';

const Profile = () => {
  /* to be added later
  <MyOrders />
  */
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
    </div>
  );
};

export default Profile;
