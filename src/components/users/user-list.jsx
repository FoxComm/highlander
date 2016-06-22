/* @flow */

// libs
import React, { Element } from 'react';

// components
import { ListPageContainer } from '../list-page';

type Props = {
  children: Element,
};

const UserListPage = (props: Props) => {
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

export default UserListPage;
