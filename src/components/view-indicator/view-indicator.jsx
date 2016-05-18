import React from 'react';

import styles from './view-indicator.css';

type Props = {
  totalItems: number;
  viewedItems: number;
  countViewedItems: Function;
}

export default class ViewIndicator extends React.Component {
  props: Props;

  state = {
    isScrolled: false,
  };

  componentDidMount() {
    this.handleScroll();
    window.addEventListener('scroll', this.handleScroll);
  }

  componentWillUnmount() {
    window.removeEventListener('scroll', this.handleScroll);
  }

  handleScroll = () => {
    this.props.countViewedItems();
    this.checkScroll();
  };

  checkScroll = () => {
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
    const isScrolled = scrollTop > 320;

    this.setState({isScrolled});
  };

  rotateWheel() {
    const degrees = this.getViewedPercent() * 360 / 100;
    return {
      transform: `rotate(${degrees}deg)`,
    };
  }

  getViewedPercent() {
    return this.props.viewedItems / this.props.totalItems * 100;
  }

  render() {
    const indicatorStyleName = this.state.isScrolled ? 'view-indicator_fixed' : 'view-indicator_static';
    const wrapStyleName = (this.getViewedPercent() > 50) ? 'wheel-wrap_51-100' : 'wheel-wrap';
    return (
      <span styleName={indicatorStyleName}>
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
