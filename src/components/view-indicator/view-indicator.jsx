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
    const wrapStyleName = (this.getViewedPercent() > 50) ? 'wheel-wrap_51-100' : 'wheel-wrap';
    return (
      <span styleName="view-indicator">
        <span styleName="spinner">
          <span styleName={wrapStyleName}>
            <span styleName="wheel" style={this.rotateWheel()} />
          </span>
          <span styleName="number">
            {this.props.viewedItems}
          </span>
        </span>
        <span styleName="text">Total<br /> items</span>
      </span>
    );
  }
}