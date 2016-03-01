/** Libs */
import cx from 'classnames';
import React, {PropTypes, Component} from 'react';
import ReactDOM from 'react-dom';

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
class TextFit extends Component {
  static propTypes = {
    content: PropTypes.string.isRequired,
    fontSize: PropTypes.number,
    minFontSize: PropTypes.number,
    maxFontSize: PropTypes.number,
    units: PropTypes.string,
    className: PropTypes.string,
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

  _setFontSize() {
    const span = ReactDOM.findDOMNode(this);
    const parent = span.parentNode;

    const parentPaddingLeft = getPropertyValue(parent, 'padding-left');
    const parentPaddingRight = getPropertyValue(parent, 'padding-right');

    const width = span.offsetWidth;
    const parentWidth = parent.offsetWidth - parentPaddingLeft - parentPaddingRight;

    console.log(this.props.content, width, parentWidth);

    if (width > parentWidth) {
      let fitFontSize = parentWidth / width * this.props.fontSize;

      fitFontSize = fitFontSize > this.props.maxFontSize ? this.props.maxFontSize : fitFontSize;
      fitFontSize = fitFontSize < this.props.minFontSize ? this.props.minFontSize : fitFontSize;

      this.setState({
        fontSize: fitFontSize
      });
    }
  }

  componentDidMount() {
    this._setFontSize();
  };

  render() {
    const style = {
      fontSize: this.state.fontSize + this.props.units,
    };

    const cls = cx('fc-text-fit', this.props.className);

    return (
      <span className={cls} style={style}>{this.props.content}</span>
    );
  }
}

export default TextFit;
