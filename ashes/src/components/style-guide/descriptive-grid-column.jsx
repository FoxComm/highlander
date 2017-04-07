import React, { PropTypes } from 'react';

export default class DescriptiveGridColumn extends React.Component {
  static propTypes = {
    size: PropTypes.string,
    numerator: PropTypes.number,
    denominator: PropTypes.number
  }

  get clsName() {
    return `fc-col-${this.props.size}-${this.props.numerator}-${this.props.denominator}`;
  }

  render() {
    return (
      <div className={ this.clsName }>
        <div className='content-box'>
          { this.clsName }
        </div>
      </div>
    );
  }
}
