'use strict';

import React from 'react';

export default class Selected extends React.Component {
  render() {
    return (
      <input type="checkbox"/>
    );
  }
}

Selected.propTypes = {
  getValue: React.PropTypes.func,
  onChange: React.PropTypes.func,
  model: React.PropTypes.any
};
