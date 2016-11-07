
import React from 'react';
import styles from './profile.css';

import Details from './blocks/details';
import MyOrders from './blocks/my-orders';

const Profile = () => {
  return (
    <div styleName="profile">
      <Details/>
      <MyOrders/>
    </div>
  );
};

export default Profile;
