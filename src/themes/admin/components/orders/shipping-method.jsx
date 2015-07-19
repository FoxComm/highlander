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
      innerContent = <ul></ul>;
    } else {
      innerContent = (
        <ul>
          {this.state.methods.map((method) => {
            return (
              <li>
                <input type="radio" />
                {method.name}
                {method.price}
              </li>
            );
           })}
        </ul>
      );
    }

    return innerContent;
  }
}

OrderShippingMethod.propTypes = {
  order: React.PropTypes.object,
  isEditing: React.PropTypes.bool
};
