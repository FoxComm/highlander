'use strict';

import React from 'react';
import { Link } from 'react-router';

class Home extends React.Component {
  render() {
    return (
      <div>
        <div><Link to='home' className="logo" /></div>
        <div>This is home</div>
      </div>
    );
  }
}

export default Home;
