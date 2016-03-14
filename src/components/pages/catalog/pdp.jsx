/* @flow */

import React, { Component } from 'react';
import cssModules from 'react-css-modules';
import styles from './pdp.css';

import Button from 'ui/buttons';
import Counter from 'ui/forms/counter';

class Pdp extends Component {
  render() {
    const imageUrl = 'https://placehold.it/500x500';
    return (
      <div styleName="container">
        <div styleName="links">
          <div>
            SHOP / LOREM IPSUM
          </div>
          <div>
            NEXT >
          </div>
        </div>
        <div styleName="details">
          <div styleName="images">
            <img src={imageUrl} styleName="preview-image" />
            <img src={imageUrl} styleName="preview-image" />
            <img src={imageUrl} styleName="preview-image" />
            <img src={imageUrl} styleName="preview-image" />
          </div>
          <div styleName="info">
            <h1 styleName="name">LOREM IPSUM</h1>
            <div styleName="price">
              $75
            </div>
            <div styleName="description">
              Nullam quis risus eget urna mollis ornare vel eu leo.
              Nulla vitae elit libero, a pharetra augue. Vivamus sagittis
              lacus vel augue laoreet rutrum faucibus dolor auctor.
              Cum sociis natoque penatibus et magnis dis parturient montes,
              nascetur ridiculus mus.
            </div>
            <div>
              <label>QUANTITY</label>
              <div styleName="counter">
                <Counter />
              </div>
            </div>
            <div styleName="add-to-cart">
              <Button>ADD TO CART</Button>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default cssModules(Pdp, styles);
