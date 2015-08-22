'use strict';

import React from 'react';

export default class ItemChecked extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      checked: false
    }
  }

  onChange() {
    this.setState({
      checked: !this.state.checked
    });
  }

  render() {
    return (
      <input type="checkbox" value={this.state.checked} onChange={this.onChange.bind(this)} />
    );
  }
}

ItemChecked.propTypes = {
  model: React.PropTypes.object
};
