import React from 'react';
import { Link } from '../link';

export default class Home extends React.Component {
  render() {
    return (
      <div>
        <div><Link to='home' className="logo" /></div>
        <div>This is home</div>
      </div>
    );
  }
}
