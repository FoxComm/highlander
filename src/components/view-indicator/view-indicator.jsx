import React, {PropTypes} from 'react';

import styles from './view-indicator.css';

type Props = {
  totalItems: number;
  viewedItems: number;
  countViewedItems: Function;
}

export default class ViewIndicator extends React.Component {
  props: Props;

  componentDidMount() {
    this.props.countViewedItems();
    window.addEventListener('scroll', this.props.countViewedItems);
  }

  componentWillUnmount() {
    window.removeEventListener('scroll', this.props.countViewedItems);
  }

  rotateWheel() {
    const degrees = this.getViewedPercent() * 360 / 100;
    return {
      transform: `rotate(${degrees}deg)`
    };
  }

  getViewedPercent() {
    return this.props.viewedItems / this.props.totalItems * 100;
  }

  render() {
    const wheel1StyleName = (this.getViewedPercent() > 50) ? 'wheel1_51-100' : 'wheel1';
    return (
      <span styleName="viewIndicator">
        <span styleName="spinner">
          <span styleName={wheel1StyleName}/>
          <span styleName="wheel2"  style={this.rotateWheel()}/>
        </span>
        <span styleName="text">Total<br /> items {this.props.totalItems}</span>
      </span>
    );
  }
}