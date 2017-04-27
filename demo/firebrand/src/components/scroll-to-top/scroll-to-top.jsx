import React from 'react';

import styles from './scroll-to-top.css';


class ScrollToTop extends React.Component {
  state = {
    isScrolled: false,
  };

  componentDidMount() {
    this.checkScroll();
    window.addEventListener('scroll', this.checkScroll);
  }

  componentWillUnmount() {
    window.removeEventListener('scroll', this.checkScroll);
  }

  checkScroll = () => {
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
    const isScrolled = scrollTop > 320;

    this.setState({isScrolled});
  };

  scrollToTop = () => {
    document.body.scrollTop = 0;
    document.documentElement.scrollTop = 0;
  };

  render() {
    const styleName = this.state.isScrolled ? 'scroller' : 'scroller-hidden';
    return (
      <div styleName={styleName} onClick={this.scrollToTop}>Back to<br/> Top</div>
    );
  }
}

export default ScrollToTop;
