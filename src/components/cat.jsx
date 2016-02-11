

import React from 'react';

class Cat extends React.Component {
  render() {
    return (
      <div>
        <strong>{this.props.children}</strong> says: hello
      </div>
    );
  }
}

export default Cat;
