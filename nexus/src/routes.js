/* @flow */

import React from 'react';
import { Route } from 'react-router';

const Hello = () => {
  return <div>Nexus</div>;
};

export default function makeRoutes() {
  return <Route path="/" component={Hello} />;
}
