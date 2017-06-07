
import React from 'react';
import styles from './profile.css';

import Details from './blocks/details';
import MyOrders from './blocks/my-orders';
import MyReviews from './blocks/my-reviews';
import MyShippingAddresses from './blocks/shipping-addresses';

const Profile = () => {
  return (
    <div styleName="profile">
      <Details />
      <MyOrders />
      <MyShippingAddresses />
      <MyReviews />
    </div>
  );
};

export default Profile;
