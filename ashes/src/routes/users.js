/* @flow */

import React, { Component, Element } from 'react';
import { Route, IndexRoute } from 'react-router';

import UsersListPage from 'components/users/user-list';
import Users from 'components/users/users';
import User from 'components/users/user';
import UserForm from 'components/users/user-form';

const userRoutes = () => {
  return (
    <Route name="user-base" path="users">
      <Route name="users-list-page" component={UsersListPage}>
        <IndexRoute name="users" component={Users} />
      </Route>
      <Route name="user" path=":userId" component={User}>
        <IndexRoute name="user-form" component={UserForm} />
        <Route name="user-activity-trail" path="activity-trail" component={UserForm} />
      </Route>
    </Route>
  )
};

export default userRoutes;
