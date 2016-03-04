
import React from 'react';

const Home = props => {
  return (
    <div>
      <h2>Storefront Demo</h2>
      <p>Current category: {props.params.categoryName ? props.params.categoryName : 'all'}</p>
    </div>
  );
};

export default Home;
