import React from 'react';

import { IndexLink } from '../link';

const Home = props => {
  return (
    <div>
      <div><IndexLink to='home' className="logo" /></div>
      <div>This is home</div>
    </div>
  );
};

export default Home;
