'use strict';

import React from 'react';

export default class Selected extends React.Component {
  render() {
    return (
      <input type="checkbox" checked={this.props.getValue(this.props.model)} onChange={this.props.onChange(this.props.model)} />
    );
  }
}

Selected.propTypes = {
  getValue: React.PropTypes.func,
  onChange: React.PropTypes.func,
  model: React.PropTypes.any
};
