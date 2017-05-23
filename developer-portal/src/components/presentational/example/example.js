import React, { Component } from 'react';
import './example.css';

class Example extends Component {
  render() {
    return (
      <div className="example">
        {this.props.children}
      </div>
    );
  }
}

export default Example;
