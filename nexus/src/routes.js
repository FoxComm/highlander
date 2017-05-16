/* @flow */

import React from 'react';
import { Route } from 'react-router';

import Layout from './components/layout/layout.jsx';

const Hello = () => {
  return <div>Nexus</div>;
};

export default function makeRoutes() {
  return (
    <Layout>
      <Route path="/" component={Hello} />
    </Layout>
  );
};
