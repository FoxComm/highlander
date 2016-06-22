// libs
import React, { PropTypes } from 'react';

// components
import { ListPageContainer } from '../list-page';

const UserListPage = props => {
  const navLinks = [
    { title: 'Users', to: 'users' },
    { title: 'Activity Trail', to: 'home' },
  ];

  return (
    <ListPageContainer
      title="Users"
      navLinks={navLinks}
    >
      {props.children}
    </ListPageContainer>
  );
};

UserListPage.propTypes = {
  children: PropTypes.node,
};

export default UserListPage;
