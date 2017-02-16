/* @flow */

// libs
import React, { Element} from 'react';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer } from '../list-page';

type Props = {
  children: Element<*>,
};

const UserListPage = (props: Props) => {
  const navLinks = [
    { title: 'Users', to: 'users' },
    { title: 'Activity Trail', to: 'users-activity-trail' },
  ];
  const addAction = () => transitionTo('user', {userId: 'new'});

  return (
    <ListPageContainer
      title="Users"
      navLinks={navLinks}
      addTitle="User"
      handleAddAction={addAction}
    >
      {props.children}
    </ListPageContainer>
  );
};

export default UserListPage;
