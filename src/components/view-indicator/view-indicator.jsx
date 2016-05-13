import React, {PropTypes} from 'react';

import styles from './view-indicator.css';

export default class ViewIndicator extends React.Component {
  static propTypes = {};

  state = {
    allItems: 80,
    viewedItems: 25,
  };

  rotateWheel() {
    const degrees = this.getViewedPercent() * 360 / 100;
    return {
      transform: `rotate(${degrees}deg)`
    };
  }

  getViewedPercent() {
    return this.state.viewedItems / this.state.allItems * 100;
  }

  render() {
    const wheel1StyleName = (this.getViewedPercent() > 50) ? 'wheel1_51-100' : 'wheel1';
    return (
      <span styleName="viewIndicator">
        <span styleName="spinner">
          <span styleName={wheel1StyleName}/>
          <span styleName="wheel2"  style={this.rotateWheel()}/>
        </span>
        <span styleName="text">Total<br /> items</span>
      </span>
    );
  }
}