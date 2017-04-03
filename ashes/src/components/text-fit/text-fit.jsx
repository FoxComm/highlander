/** Libs */
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import React, {PropTypes, Component} from 'react';
import ReactDOM from 'react-dom';

/** helpers */
import {addResizeListener, removeResizeListener} from '../../lib/resize';

function getPropertyValue(element, property) {
  let val;

  try {
    val = parseFloat(window.getComputedStyle(element, null).getPropertyValue(property));
  } catch (e) {
    val = parseFloat(element.currentStyle[property]);
  }

  return val;
}
/**
 * TODO: reset font-size on window resize
 */
export default class TextFit extends Component {
  static propTypes = {
    fontSize: PropTypes.number,
    minFontSize: PropTypes.number,
    maxFontSize: PropTypes.number,
    units: PropTypes.string,
    className: PropTypes.string,
    children: PropTypes.node,
  };

  static defaultProps = {
    fontSize: 1.3,
    minFontSize: 1,
    maxFontSize: 1.7,
    units: 'rem',
    className: '',
  };

  state = {
    fontSize: this.props.fontSize,
  };

  componentWillMount() {
    addResizeListener(this.setFontSize);
  }

  componentWillUnmount() {
    removeResizeListener(this.setFontSize);
  }

  @autobind
  setFontSize() {
    const span = ReactDOM.findDOMNode(this);
    const parent = span.parentNode;

    const parentPaddingLeft = getPropertyValue(parent, 'padding-left');
    const parentPaddingRight = getPropertyValue(parent, 'padding-right');

    const width = span.offsetWidth;
    const parentWidth = parent.offsetWidth - parentPaddingLeft - parentPaddingRight;

    if (width > parentWidth) {
      let fitFontSize = parentWidth / width * this.props.fontSize;

      fitFontSize = Math.min(fitFontSize, this.props.maxFontSize);
      fitFontSize = Math.max(fitFontSize, this.props.minFontSize);

      this.setState({
        fontSize: fitFontSize
      });
    }
  }

  componentDidMount() {
    this.setFontSize();
  }

  render() {
    const style = {
      fontSize: this.state.fontSize + this.props.units,
    };

    const cls = classNames('fc-text-fit', this.props.className);

    return (
      <span className={cls} style={style}>{this.props.children}</span>
    );
  }
}
