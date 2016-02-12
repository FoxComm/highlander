
import _ from 'lodash';
import React from 'react';
import { connect } from 'react-redux';

import * as catActions from '../modules/cat';

@connect(state => _.get(state, 'cat', {}), catActions)
class Cat extends React.Component {
  componentWillMount() {
    if (!this.props.loading && !this.props.name) {
      this.props.findName();
    }
  }

  render() {
    console.log('render cat');
    const greet = this.props.children || 'hello';

    return (
      <div>
        <strong>{this.props.name}</strong> says: {greet}
      </div>
    );
  }
}

export default Cat;
