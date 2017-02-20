/* @flow */
import _ from 'lodash';
import React, { Element, Component } from 'react';

// components
import wrapModal from '../modal/wrapper';
import ContentBox from '../content-box/content-box';

class Coupon extends Component {
  props: Props;

  constructor(props: Props) {
    super(props);
  }

  render() {
    return (
      <div className="fc-promotion-coupon">
        <div className="fc-modal-container">
          <ContentBox title="Add">
          </ContentBox>
        </div>
      </div>
    );
  }  
}

export default wrapModal(Coupon);
