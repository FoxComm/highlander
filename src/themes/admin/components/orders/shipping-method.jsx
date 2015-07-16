'use strict';

import React from 'react';
import Api from '../../lib/api';

export default class OrderShippingMethod extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      methods: []
    };
  }

  componentDidMount() {
    Api.get('/shipping-methods')
       .then((methods) => {
         this.setState({
           methods: methods
         });
       })
       .catch((err) => { console.log(err); });
  }

  render() {
    let innerContent = null;

    if (this.props.isEditing) {
      innerContent = null;
    } else {
      innerContent = (
        {this.state.methods.map((method) => {
          <li>
            <input type="radio">
            {method.name}
            {method.price}
          </li>
        })}
      );
    }

    return <ul>{innerContent}</ul>;
  }
}

OrderShippingMethod.propTypes = {
  order: React.PropTypes.object,
  isEditing: React.PropTypes.bool
};
