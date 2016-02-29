import React, { PropTypes } from 'react';
import { Link } from '../link';

import { transitionTo } from '../../route-helpers';

export default class Home extends React.Component {

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  componentDidMount() {
    transitionTo(this.context.history, 'orders');
  }

  render() {
    return (
      <div>
        <div><Link to='home' className="logo" /></div>
        <div>This is home</div>
      </div>
    );
  }
}
