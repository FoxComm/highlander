import React from 'react';
import { Route, IndexRoute } from 'react-router';

const Hello = () => {
  return <div>Nexus</div>;
};

export default function makeRoutes(store) {
  return <Route path="/" component={Hello} />;
}
