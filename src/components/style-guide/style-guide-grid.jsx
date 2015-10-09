'use strict';

import React, { PropTypes } from 'react';
import DescriptiveGridColumn from './descriptive-grid-column';

export default class StyleGuideGrid extends React.Component {
  static propTypes = {
    name: PropTypes.string,
    description: PropTypes.string,
    size: PropTypes.string
  }

  render() {
    let rows = [];
    for (var i = 0; i < 12; i++) {
      rows.push(<DescriptiveGridColumn size={ this.props.size } numerator="1" denominator="12" />);
    }

    return (
      <div className='fc-style-guide'>
        <div className='fc-grid'>
          <div className='fc-col-sm-1-1'>
            <h2>{ this.props.name }</h2>
            <p>{ this.props.description }</p>
          </div>
        </div>
        <div className='fc-grid'>
          {rows}
        </div>
        <div className='fc-grid'>
          <DescriptiveGridColumn size={ this.props.size } numerator="1" denominator="6" />
          <DescriptiveGridColumn size={ this.props.size } numerator="5" denominator="6" />
        </div>
        <div className='fc-grid'>
          <DescriptiveGridColumn size={ this.props.size } numerator="1" denominator="4" />
          <DescriptiveGridColumn size={ this.props.size } numerator="3" denominator="4" />
        </div>
        <div className='fc-grid'>
          <DescriptiveGridColumn size={ this.props.size } numerator="1" denominator="3" />
          <DescriptiveGridColumn size={ this.props.size } numerator="2" denominator="3" />
        </div>
        <div className='fc-grid'>
          <DescriptiveGridColumn size={ this.props.size } numerator="5" denominator="12" />
          <DescriptiveGridColumn size={ this.props.size } numerator="7" denominator="12" />
        </div>
        <div className='fc-grid'>
          <DescriptiveGridColumn size={ this.props.size } numerator="1" denominator="2" />
          <DescriptiveGridColumn size={ this.props.size } numerator="1" denominator="2" />
        </div>
        <div className='fc-grid'>
          <DescriptiveGridColumn size={ this.props.size } numerator="1" denominator="1" />
        </div>
      </div>
      );
  }
}