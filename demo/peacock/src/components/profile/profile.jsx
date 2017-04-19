
import React from 'react';
import styles from './profile.css';

import AccountDetails from './account-details/account-details';
// import MyOrders from './blocks/my-orders';
// import MyShippingAddresses from './blocks/shipping-addresses';

const Profile = () => {
  /* to be added later
  <MyShippingAddresses />
  <MyOrders />
  */
  return (
    <div styleName="profile">
      <AccountDetails
        styleName="account-details"
      />
    </div>
  );
};

export default Profile;
